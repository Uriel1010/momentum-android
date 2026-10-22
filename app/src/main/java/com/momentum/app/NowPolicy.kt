package com.momentum.app
/** Eligibility is strict; ranking uses only explicit, local evidence. */
object NowPolicy {
 fun candidates(tasks:List<Task>,projects:List<Project>,now:Long,minutes:Int,easy:Boolean,context:String,events:List<Event> = emptyList()):List<Suggestion> {
  val eligible=tasks.filter{t->t.status!="WAITING" && projects.none{it.id==t.projectId && it.paused} && (minutes==0 || t.estimate==null || t.estimate<=minutes) && (!easy || (t.estimate!=null && t.estimate<=5) || t.nextAction.isNotBlank())}
  return SuggestionEngine.rank(eligible,now,minutes.takeIf{it>0},context).map{s->
   val t=s.task
   val project=projects.find{it.id==t.projectId}
   val ready=t.nextAction.isNotBlank() || t.status=="READY"
   val projectNext=project?.nextStep?.takeIf{it.isNotBlank()}?.let{it==t.nextAction || it==t.title}==true
   val postponed=events.count{it.taskId==t.id && it.kind=="postponed"}
   val small=t.estimate?.let{it<=5}==true || t.nextAction.isNotBlank()
   s.copy(score=s.score+(if(ready)12 else 0)+(if(projectNext)18 else 0)+(if(small)10 else 0)-(if(postponed>=3 && !small)20 else 0),
    reasons=s.reasons+(if(projectNext)listOf("project_next")else emptyList())+(if(ready)listOf("ready")else emptyList()))
  }.sortedWith(compareByDescending<Suggestion>{it.score}.thenBy{it.task.created}.thenBy{it.task.id})
 }
}
