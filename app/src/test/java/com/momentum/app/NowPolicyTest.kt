package com.momentum.app
import org.junit.Test
import org.junit.Assert.*
class NowPolicyTest {
 @Test fun fiveMinutesExcludesKnownLongTasks() {
  val tasks=listOf(Task(title="long",estimate=30,priority=2),Task(title="tiny",estimate=5))
  assertEquals("tiny",NowPolicy.candidates(tasks,emptyList(),System.currentTimeMillis(),5,false,"").single().task.title)
 }
 @Test fun easierRequiresKnownSmallEffortOrUserStep() {
  val tasks=listOf(Task(title="unknown"),Task(title="first",nextAction="Open file"),Task(title="small",estimate=2))
  assertEquals(setOf("first","small"),NowPolicy.candidates(tasks,emptyList(),System.currentTimeMillis(),0,true,"").map{it.task.title}.toSet())
 }
 @Test fun pausedProjectNeverSuggested() {
  val p=Project(title="paused",paused=true)
  assertTrue(NowPolicy.candidates(listOf(Task(title="step",projectId=p.id)),listOf(p),System.currentTimeMillis(),0,false,"").isEmpty())
 }
}
