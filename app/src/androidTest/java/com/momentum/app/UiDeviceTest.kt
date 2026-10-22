package com.momentum.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiDeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa")){"Fixture tests require -PmomentumQa=true"}}
 private var original=""
 @Before fun setup() {
  runBlocking {
   original=Backup.export(app.repo)
   app.db.dao().clearTasks();app.db.dao().clearProjects();app.db.dao().clearEvents()
   app.prefs.bool("english",true);app.prefs.bool("onboarded",true)
  }
  compose.waitUntil(10_000){compose.onAllNodesWithText("Capture a thought").fetchSemanticsNodes().isNotEmpty()}
 }
 @After fun cleanup() {runBlocking {if(original.isNotEmpty())Backup.restore(app.repo,original)}}
 @Test fun captureAndRecreate() {
  compose.onNodeWithText("Capture a thought").performClick()
  compose.onNodeWithText("What do you need to do?").performTextInput("UI capture survives")
  compose.onNodeWithText("Save").performScrollTo().performClick()
  compose.waitUntil(5000){runBlocking {app.db.dao().allTasks().any{it.title=="UI capture survives"}}}
  compose.activityRule.scenario.recreate()
  compose.onNodeWithText("UI capture survives").assertExists()
  compose.onNodeWithText("Tasks",useUnmergedTree=false).performClick()
  compose.onNodeWithText("UI capture survives").assertExists()
 }
 @Test fun alternateSuggestionAndFocusCompletion() {
  runBlocking {app.repo.save(Task(title="First candidate",priority=2));app.repo.save(Task(title="Second candidate",priority=0))}
  compose.waitUntil(5000){compose.onAllNodesWithText("First candidate").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Something else").performScrollTo().performClick()
  compose.waitUntil(5000){compose.onAllNodesWithText("Second candidate").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Let’s start").performScrollTo().performClick()
  compose.onNodeWithText("One thing at a time").assertExists()
  compose.onNodeWithText("I’m done").performScrollTo().performClick()
  compose.waitUntil(5000){runBlocking{app.db.dao().allTasks().any{it.title=="Second candidate" && it.status=="DONE"}}}
  compose.onNodeWithText("Undo").performClick()
  compose.waitUntil(5000){runBlocking{app.db.dao().allTasks().any{it.title=="Second candidate" && it.status!="DONE"}}}
 }
 @Test fun blockerDefersWithRealStateChange() {
  runBlocking {app.repo.save(Task(title="Blocker test"))}
  compose.waitUntil(5000){compose.onAllNodesWithText("Blocker test").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Let’s start").performScrollTo().performClick()
  compose.onNodeWithText("I’m stuck").performScrollTo().performClick()
  compose.waitUntil(5000){compose.onAllNodesWithText("Waiting for someone / something").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Waiting for someone / something").performClick()
  compose.onNodeWithText("Notes").performTextInput("Awaiting an answer")
  compose.onNodeWithTag("confirmWaiting").performClick()
  compose.waitUntil(5000){runBlocking{app.db.dao().allTasks().any{it.title=="Blocker test" && it.status=="WAITING" && it.available>System.currentTimeMillis()}}}
  compose.onNodeWithText("Waiting for a response.").assertExists()
 }
 @Test fun projectHistoryVisibleAfterRecreate() {
  runBlocking {
   val p=Project(title="Return to project",summary="Saved stopping point",nextStep="Open the file")
   app.db.dao().saveProject(p);app.db.dao().event(Event(projectId=p.id,kind="advanced",note="Previous progress"))
  }
  compose.onNodeWithText("Projects").performClick()
  compose.onNodeWithText("Return to project").performClick()
  compose.onNodeWithText("Saved stopping point").assertExists()
  compose.onNodeWithText("Open the file").assertExists()
  compose.activityRule.scenario.recreate()
  compose.onNodeWithText("Saved stopping point").assertExists()
  compose.onNodeWithText("Previous progress").performScrollTo().assertIsDisplayed()
 }
 @Test fun hebrewRtlCapture() {
  runBlocking{app.prefs.bool("english",false)}
  compose.waitUntil(5000){compose.onAllNodesWithText("לתפוס מחשבה").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("לתפוס מחשבה").performClick()
  compose.onNodeWithText("מה צריך לעשות?").performTextInput("משימה בעברית")
  compose.onNodeWithText("שמירה").performScrollTo().performClick()
  compose.waitUntil(5000){runBlocking{app.db.dao().allTasks().any{it.title=="משימה בעברית"}}}
  compose.onNodeWithText("משימה בעברית").assertExists()
  compose.onNodeWithText("מתחילים").assertExists()
 }
}
