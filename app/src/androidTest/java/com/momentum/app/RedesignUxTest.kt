package com.momentum.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.io.File

class RedesignUxTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa")){"Fixture tests require -PmomentumQa=true"}}
 private var original=""
 @Before fun setup() {
  runBlocking {
   original=Backup.export(app.repo)
   app.db.dao().clearTasks();app.db.dao().clearProjects();app.db.dao().clearEvents()
   app.prefs.bool("english",true);app.prefs.bool("onboarded",true)
  }
  compose.waitUntil(10000){compose.onAllNodesWithText("Capture a thought").fetchSemanticsNodes().isNotEmpty()}
 }
 @After fun restore(){runBlocking{Backup.restore(app.repo,original)}}
 private fun waitText(text:String){compose.waitUntil(5000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}}
 private fun click(text:String){compose.onNodeWithText(text).performScrollTo().performClick()}
 @Test fun firstTaskOnboardingStartsRealTask() {
  runBlocking{app.prefs.bool("onboarded",false)}
  waitText("Save and start")
  compose.onNodeWithText("What do you need to do?").performTextInput("My first real step")
  click("Save and start")
  waitText("One thing at a time")
  assertEquals("IN_PROGRESS",runBlocking{app.db.dao().allTasks().single().status})
 }
 @Test fun tinyErrandUsesKeyboardDone() {
  compose.onNodeWithText("Capture a thought").performClick()
  compose.onNodeWithText("What do you need to do?").performTextInput("Buy tea")
  compose.onNodeWithText("What do you need to do?").performImeAction()
  compose.waitUntil(5000){runBlocking{app.db.dao().allTasks().any{it.title=="Buy tea"}}}
 }
 @Test fun fiveMinutesAndLowEnergyRespectChoice() {
  runBlocking {
   app.repo.save(Task(title="Large task",estimate=90,priority=2))
   app.repo.save(Task(title="Small step",estimate=3,priority=1))
  }
  waitText("Large task")
  click("5 min")
  waitText("Small step")
  compose.onNodeWithText("Large task").assertDoesNotExist()
  click("Something easier")
  compose.onNodeWithText("Small step").assertExists()
 }
 @Test fun repeatedPostponementOffersSmallStepWithoutGuilt() {
  runBlocking {
   val t=Task(title="Waiting intention")
   app.repo.save(t)
   repeat(3){app.db.dao().event(Event(taskId=t.id,kind="postponed"))}
  }
  waitText("This has waited a few times. Just the first step is enough.")
  click("Not now")
  compose.waitUntil(5000){runBlocking{app.db.dao().allTasks().single().available>System.currentTimeMillis()}}
 }
 @Test fun bigBlockerStoresOwnStepAndRealProgress() {
  runBlocking{app.repo.save(Task(title="Complex task"))}
  waitText("Complex task");click("Let’s start");click("I’m stuck");click("It feels too big")
  compose.onNode(hasText("First small step") and hasSetTextAction()).performTextInput("Open the document")
  compose.onNodeWithText("Save").performClick()
  waitText("Open the document")
  click("I made progress")
  compose.onNodeWithText("Notes").performTextInput("Document opened")
  compose.onNodeWithText("Save").performClick()
  compose.waitUntil(5000){runBlocking{app.db.dao().allEvents().any{it.kind=="advanced" && it.note=="Document opened"}}}
  assertEquals("Open the document",runBlocking{app.db.dao().allTasks().single().nextAction})
 }
 @Test fun fiftyTasksSearchAndEditLongTitle() {
  runBlocking {repeat(50){app.repo.save(Task(title="Errand "+it))}}
  compose.onNodeWithText("Tasks").performClick()
  compose.onNodeWithText("Search tasks").performTextInput("Errand 49")
  compose.onNodeWithText("Search tasks").performImeAction()
  compose.onNodeWithTag("taskList").performScrollToNode(hasText("Errand 49") and !hasSetTextAction())
  compose.onNode(hasText("Errand 49") and !hasSetTextAction()).performScrollTo().performClick()
  val long="A long intention with mixed עברית and English that still needs a clear editable title"
  compose.onNodeWithText("Title").performTextReplacement(long)
  compose.onNodeWithText("Save").performClick()
  compose.onNodeWithText("Search tasks").performTextReplacement("mixed")
  compose.onNodeWithText(long).assertExists()
  compose.activityRule.scenario.recreate()
  compose.onNodeWithText(long).assertExists()
 }
 @Test fun projectResumeAndExplicitMilestone() {
  runBlocking {
   val p=Project(title="A meaningful project",description="Publish a short story",summary="Outline ready",nextStep="Write three lines")
   app.db.dao().saveProject(p);app.repo.save(Task(title="Write the opening",projectId=p.id,nextAction="Write three lines"))
  }
  compose.onNodeWithText("Projects").performClick();compose.onNodeWithText("A meaningful project").performClick()
  compose.onNodeWithText("Outline ready").assertExists()
  click("Mark a milestone")
  compose.onNodeWithText("A real milestone").performTextInput("First draft finished")
  compose.onNodeWithText("Save").performClick()
  compose.waitUntil(5000){runBlocking{app.db.dao().allEvents().any{it.kind=="milestone"}}}
  assertEquals("First draft finished",runBlocking{app.db.dao().allProjects().single().summary})
 }
 @Test fun timerSurvivesRecreationAndReducedMotionWorks() {
  runBlocking{app.prefs.bool("reducedMotion",true);app.repo.save(Task(title="Quiet focus"))}
  waitText("Quiet focus");click("Let’s start");click("Optional timer")
  compose.activityRule.scenario.recreate()
  compose.onNodeWithText("Stop timer").assertExists()
  click("Stop timer")
 }

 @Test fun landscapeKeepsPrimaryActionReachable() {
  runBlocking {app.repo.save(Task(title="A step in landscape"))}
  waitText("A step in landscape")
  val orientation=compose.activity.requestedOrientation
  try {
   compose.activity.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
   compose.waitForIdle()
   compose.onNodeWithText("A step in landscape").assertExists()
   click("Let’s start")
   click("I’m done")
   compose.waitUntil(5000){runBlocking{app.db.dao().allTasks().single().status=="DONE"}}
  } finally {compose.activity.requestedOrientation=orientation}
 }

 @Test fun swipeCompletionHasUndo() {
  runBlocking {app.repo.save(Task(title="Swipe this step"))}
  waitText("Swipe this step")
  compose.onNodeWithText("Tasks").performClick()
  compose.onNode(hasText("Swipe this step") and !hasSetTextAction()).performTouchInput {swipeRight()}
  compose.waitUntil(5000){runBlocking{app.db.dao().allTasks().single().status=="DONE"}}
  compose.onNodeWithText("Undo").performClick()
  compose.waitUntil(5000){runBlocking{app.db.dao().allTasks().single().status!="DONE"}}
 }

 @Test fun accessibleSettingsRowTogglesPreference() {
  runBlocking{app.prefs.bool("reducedMotion",false)}
  compose.onNodeWithContentDescription("Settings").performClick()
  compose.onNodeWithText("Reduce motion").performScrollTo().performClick()
  compose.waitUntil(5000){runBlocking{app.prefs.flow.first().reducedMotion}}
 }
}
