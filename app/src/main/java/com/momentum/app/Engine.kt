package com.momentum.app
import java.time.*

data class Suggestion(val task:Task,val score:Int,val reasons:List<String>)
data class EngineConfig(val cooldown:Long=60_000,val priorityWeight:Int=25,val dueWeight:Int=60,val fitWeight:Int=20)
object SuggestionEngine {
 fun rank(tasks:List<Task>,now:Long,minutes:Int?=null,context:String="",config:EngineConfig=EngineConfig()):List<Suggestion> =
 tasks.filter {
  it.status !in setOf("DONE","ARCHIVED") && it.available<=now &&
  it.status!="WAITING" &&
  (it.lastConsidered==0L || now-it.lastConsidered>=config.cooldown) &&
  (context.isBlank() || it.contexts.isBlank() || it.contexts.split(',').map(String::trim).contains(context))
 }.map { t ->
  val reason=mutableListOf<String>()
  var score=t.priority*config.priorityWeight
  if(t.priority==2) reason+="priority"
  if(t.due!=null && t.due-now<=86_400_000L) {score+=config.dueWeight;reason+="due"}
  if(minutes!=null && t.estimate!=null && t.estimate<=minutes) {score+=config.fitWeight;reason+="fits"}
  if(t.estimate==null) reason+="start"
  if(reason.isEmpty()) reason+="available"
  score+=((now-t.created).coerceAtLeast(0)/86_400_000L).coerceAtMost(30).toInt()
  Suggestion(t,score,reason)
 }.sortedWith(compareByDescending<Suggestion> {it.score}.thenBy {it.task.created}.thenBy {it.task.id})
}
object ReminderPolicy {
 fun nextAllowed(now:Long,start:Int,end:Int,zone:ZoneId=ZoneId.systemDefault()):Long {
  if(start==end)return now
  val z=Instant.ofEpochMilli(now).atZone(zone)
  val quiet=if(start<end)z.hour in start until end else z.hour>=start || z.hour<end
  if(!quiet)return now
  var next=z.toLocalDate().atTime(end,0).atZone(zone)
  if(!next.isAfter(z)) next=next.plusDays(1)
  return next.toInstant().toEpochMilli()
 }
 fun dayStart(now:Long,zone:ZoneId=ZoneId.systemDefault()):Long=Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
}
