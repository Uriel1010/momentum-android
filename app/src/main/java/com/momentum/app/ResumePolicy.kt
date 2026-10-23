package com.momentum.app
data class ResumePoint(val task:Task,val note:String,val nextStep:String,val lastAt:Long)
object ResumePolicy {
 fun latest(tasks:List<Task>,projects:List<Project>,events:List<Event>,now:Long):ResumePoint? {
  val eligible=tasks.filter{it.status in setOf("IN_PROGRESS","READY","INBOX") && it.available<=now && projects.none{p->p.id==it.projectId && p.paused}}
  val latest=events.filter{it.kind in setOf("advanced","started") && eligible.any{t->t.id==it.taskId}}.maxByOrNull{it.created}?:return null
  val task=eligible.first{it.id==latest.taskId}
  val note=events.filter{it.taskId==task.id && it.kind=="advanced"}.maxByOrNull{it.created}?.note.orEmpty()
  val next=task.nextAction.ifBlank{projects.find{it.id==task.projectId}?.nextStep.orEmpty()}
  return ResumePoint(task,note,next,latest.created)
 }
}
