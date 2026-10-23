package com.momentum.app
import org.junit.Test
import org.junit.Assert.*
class ResumePolicyTest {
 @Test fun mostRecentWorkIncludesLastProgressAndNextStep(){
  val a=Task(title="First",status="IN_PROGRESS",nextAction="Open draft")
  val b=Task(title="Second",status="READY")
  val point=ResumePolicy.latest(listOf(a,b),emptyList(),listOf(Event(taskId=a.id,kind="advanced",note="Outline ready",created=5),Event(taskId=b.id,kind="started",created=2)),10)!!
  assertEquals(a.id,point.task.id);assertEquals("Outline ready",point.note);assertEquals("Open draft",point.nextStep)
 }
 @Test fun completedWaitingFutureAndPausedAreExcluded(){
  val p=Project(title="Paused",paused=true)
  val tasks=listOf(Task(title="done",status="DONE"),Task(title="wait",status="WAITING"),Task(title="future",available=99),Task(title="paused",projectId=p.id))
  assertNull(ResumePolicy.latest(tasks,listOf(p),tasks.map{Event(taskId=it.id,kind="advanced")},10))
 }
 @Test fun missingTasksAndMetricsDoNotCreateResumeCard(){
  val t=Task(title="New")
  assertNull(ResumePolicy.latest(listOf(t),emptyList(),listOf(Event(taskId=t.id,kind="metric_started"),Event(taskId="deleted",kind="started")),10))
 }
 @Test fun projectNextStepIsFallback(){
  val p=Project(title="Project",nextStep="Review outline");val t=Task(title="Task",projectId=p.id)
  assertEquals("Review outline",ResumePolicy.latest(listOf(t),listOf(p),listOf(Event(taskId=t.id,kind="started")),System.currentTimeMillis())!!.nextStep)
 }
}
