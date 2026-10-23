package com.momentum.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
class Momentum3DeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa")){"Fixture tests require -PmomentumQa=true"}}
 @Before fun setup()=runBlocking {
  check(app.packageName.endsWith(".qa")){"QA package required"}
  app.db.dao().clearTasks();app.db.dao().clearProjects();app.db.dao().clearEvents();app.db.dao().clearDeliveries()
  app.prefs.bool("english",true);app.prefs.bool("onboarded",true);app.prefs.bool("metrics",false)
 }
 private fun click(text:String)=compose.onNodeWithText(text).performScrollTo().performClick()
 private fun waitText(text:String)=compose.waitUntil(10000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}
 @Test fun captureFocusDraftRestorationAndBlankValidation(){
  waitText("Capture a thought");compose.onNodeWithText("Capture a thought").performClick()
  compose.waitUntil(5000){compose.onAllNodes(hasTestTag("quickCapture") and isFocused()).fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithTag("quickCapture").assertIsFocused()
  compose.onNodeWithText("Save").assertIsNotEnabled()
  compose.onNodeWithTag("quickCapture").performTextInput("Tiny mixed עברית step")
  compose.activityRule.scenario.recreate()
  compose.onNodeWithTag("quickCapture").assertTextContains("Tiny mixed עברית step")
  compose.onNodeWithTag("quickCapture").performImeAction()
  compose.waitUntil(5000){runBlocking{app.db.dao().allTasks().singleOrNull()?.title=="Tiny mixed עברית step"}}
 }
 @Test fun analyticsOptInClearAndDistinctStarts()=runBlocking {
  val t=Task(title="Sensitive title",notes="Sensitive notes")
  app.repo.save(t);app.repo.start(t)
  assertFalse(app.db.dao().allEvents().any{it.kind.startsWith("metric_")})
  app.prefs.bool("metrics",true);app.repo.analytics.open()
  app.repo.save(t.copy(status="READY"))
  app.repo.start(t);app.repo.start(t)
  app.repo.advance(t,"Private progress");app.repo.complete(t)
  val e=app.db.dao().allEvents()
  assertEquals(1,e.count{it.kind=="metric_started"})
  assertTrue(e.filter{it.kind.startsWith("metric_")}.all{it.note.isEmpty() || it.note.toLongOrNull()!=null})
  assertEquals(1,e.count{it.kind=="metric_completed"})
  app.repo.analytics.clear()
  assertFalse(app.db.dao().allEvents().any{it.kind.startsWith("metric_")})
  assertTrue(app.db.dao().allEvents().any{it.kind=="advanced" && it.note=="Private progress"})
 }
 @Test fun waitingAtReviewTimeStillHasAnExit(){
  runBlocking{app.repo.save(Task(title="Awaiting a reply",status="WAITING",available=System.currentTimeMillis()-1000))}
  waitText("Waiting for a response.")
  click("Browse tasks");compose.onNodeWithText("Awaiting a reply").assertExists()
 }
 @Test fun oldProjectResumptionRecordedOnce()=runBlocking {
  val p=Project(title="Old project",updated=System.currentTimeMillis()-8*86_400_000L)
  app.repo.saveProject(p)
  val t=Task(title="Resume",projectId=p.id);app.repo.save(t)
  app.prefs.bool("metrics",true)
  app.repo.start(t);app.repo.start(t)
  assertEquals(1,app.db.dao().allEvents().count{it.kind=="resumed"})
  assertEquals(1,app.db.dao().allEvents().count{it.kind=="metric_resumed"})
 }
 @Test fun retentionKeepsHistoryAndDeletesOnlyOldMetrics()=runBlocking {
  val old=System.currentTimeMillis()-91L*86_400_000
  app.db.dao().event(Event(kind="metric_open",created=old))
  app.db.dao().event(Event(kind="advanced",note="Keep this",created=old))
  app.prefs.bool("metrics",true);app.repo.analytics.open()
  assertFalse(app.db.dao().allEvents().any{it.kind=="metric_open" && it.created==old})
  assertTrue(app.db.dao().allEvents().any{it.note=="Keep this"})
 }
 @Test fun weekAwayAndFirstCompletionAreRealHistory(){
  runBlocking{
   val t=Task(title="Return after a week")
   app.repo.save(t)
   app.db.dao().event(Event(kind="advanced",note="Last week",created=System.currentTimeMillis()-8*86_400_000L))
   app.repo.start(t);app.repo.complete(t)
  }
  waitText("Capture a thought")
  compose.onNodeWithText("Progress").performClick()
  compose.onNodeWithText("You came back. Continue from here.").assertExists()
  val entries=runBlocking{app.db.dao().allEvents().filter{it.kind in setOf("started","completed")}}
  assertEquals(2,entries.size)
  entries.forEach{event->
   compose.onNode(hasScrollToIndexAction()).performScrollToKey(event.id)
   compose.onNode(hasText(if(event.kind=="started")"Started" else "Completed") and hasAnySibling(hasText("Return after a week"))).assertExists()
  }
 }
}
