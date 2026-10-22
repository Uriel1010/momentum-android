package com.momentum.app
import org.junit.Test
import org.junit.Assert.*
class Momentum3Test {
 private val now=1_800_000_000_000L
 @Test fun waitingReviewIsNeverActionable(){
  assertTrue(NowPolicy.candidates(listOf(Task(title="Waiting",status="WAITING",available=now-1)),emptyList(),now,0,false,"").isEmpty())
 }
 @Test fun explicitProjectStepWinsEqualPriority(){
  val p=Project(title="Project",nextStep="Open file")
  val a=Task(id="a",title="Other",created=now)
  val b=Task(id="b",title="Next",projectId=p.id,nextAction="Open file",created=now)
  assertEquals("b",NowPolicy.candidates(listOf(a,b),listOf(p),now,0,false,"").first().task.id)
 }
 @Test fun repeatedLargePostponementFavorsReadySmallAction(){
  val a=Task(id="a",title="Large",estimate=45,created=now)
  val b=Task(id="b",title="Small",estimate=2,created=now)
  val history=List(3){Event(taskId="a",kind="postponed")}
  assertEquals("b",NowPolicy.candidates(listOf(a,b),emptyList(),now,0,false,"",history).first().task.id)
 }
 @Test fun tinyWindowAndFiftyTasks(){
  val tasks=List(50){Task(title="Task $it",estimate=it+1,created=now)}
  assertEquals(2,NowPolicy.candidates(tasks,emptyList(),now,2,false,"").size)
 }
 @Test fun recommendationUsefulnessRequiresMatchingFutureStartWithinDay(){
  val events=listOf(Event(taskId="a",kind="metric_recommended",created=now),Event(taskId="b",kind="metric_started",created=now+1),Event(taskId="a",kind="metric_started",created=now-1))
  assertEquals(0,MetricSummary.from(events).useful)
  assertEquals(1,MetricSummary.from(events+Event(taskId="a",kind="metric_started",created=now+2)).useful)
 }
 @Test fun metricsIgnoreOperationalNotes(){
  val m=MetricSummary.from(listOf(Event(kind="started",note="Private task"),Event(kind="metric_started",note="4000"),Event(kind="metric_open")))
  assertEquals(1,m.starts);assertEquals(4L,m.latencySeconds);assertEquals(1,m.opens)
 }
}
