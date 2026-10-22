package com.momentum.app
import kotlinx.coroutines.flow.first
/** Opt-in, local-only measurement. IDs and milliseconds only; never task text. */
class LocalMetrics(private val dao:MomentumDao,private val prefs:Preferences) {
 private var openedAt=0L
 private var awaitingStart=false
 suspend fun open(now:Long=System.currentTimeMillis(),restoring:Boolean=false) {
  if(restoring && openedAt>0)return
  openedAt=now;awaitingStart=true
  record("open",now=now)
 }
 suspend fun record(kind:String,taskId:String?=null,now:Long=System.currentTimeMillis()) {
  dao.pruneMetrics(now-90L*86_400_000)
  if(!prefs.flow.first().metrics)return
  val latency=if(kind=="started" && awaitingStart && openedAt>0)(now-openedAt).coerceAtLeast(0).toString() else ""
  if(kind=="started")awaitingStart=false
  dao.event(Event(taskId=taskId,kind="metric_"+kind,note=latency,created=now))
 }
 suspend fun clear(){dao.clearMetrics();dao.clearSuggestions();awaitingStart=false;openedAt=0}
}
data class MetricSummary(val opens:Int,val starts:Int,val completed:Int,val latencySeconds:Long?,val repeated:Int,val resumed:Int,val useful:Int,val recommendations:Int) {
 companion object {
  fun from(events:List<Event>):MetricSummary {
   val e=events.filter{it.kind.startsWith("metric_")}
   val latencies=e.filter{it.kind=="metric_started"}.mapNotNull{it.note.toLongOrNull()}.sorted()
   val shown=e.filter{it.kind=="metric_recommended"}
   return MetricSummary(e.count{it.kind=="metric_open"},e.count{it.kind=="metric_started"},
    e.count{it.kind=="metric_completed"},latencies.takeIf{it.isNotEmpty()}?.let{it[it.size/2]/1000},
    e.filter{it.kind=="metric_postponed"}.groupBy{it.taskId}.count{it.value.size>=3},
    e.count{it.kind=="metric_resumed"},
    shown.count{s->e.any{it.kind=="metric_started" && it.taskId==s.taskId && it.created>=s.created && it.created-s.created<=86_400_000}},
    shown.size)
  }
 }
}
