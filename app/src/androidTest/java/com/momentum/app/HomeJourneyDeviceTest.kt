package com.momentum.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File
class HomeJourneyDeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa"))}
 @Before fun setup():Unit=runBlocking{app.db.dao().clearTasks();app.db.dao().clearProjects();app.db.dao().clearEvents();app.prefs.bool("english",true);app.prefs.bool("onboarded",true);app.prefs.bool("suggestions",true)}
 private fun waitText(text:String){compose.waitUntil(10000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}}
 private fun shot(name:String){compose.mainClock.advanceTimeBy(1000);compose.waitForIdle();compose.onRoot().captureToImage().asAndroidBitmap().let{b->File(app.filesDir,"home-"+name+".png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}}}
 @Test fun firstTaskHasOneClearCaptureThenStart(){
  waitText("Add your first task");compose.onNodeWithTag("homePrimary").assertIsDisplayed();shot("empty")
  compose.onNodeWithTag("homePrimary").performClick();waitText("Capture a thought");compose.onNodeWithTag("quickCapture").assertIsFocused().performTextInput("Read five pages");compose.onNodeWithTag("quickCapture").performImeAction()
  waitText("Read five pages");compose.onNodeWithTag("homePrimary").assertIsDisplayed();shot("ready");compose.onNodeWithTag("homePrimary").performClick();waitText("I’m done")
 }
 @Test fun returnPrioritizesOneTaskWithoutDuplicateCards(){
  runBlocking{val task=Task(title="Write the opening paragraph",nextAction="Open the outline");app.repo.save(task);app.repo.start(task);app.repo.advance(task,"Three key points are ready");app.repo.save(Task(title="Another task",priority=2))}
  waitText("Continue this step");compose.onAllNodesWithText("Write the opening paragraph").assertCountEquals(1);compose.onNodeWithText("Last progress: Three key points are ready").assertExists();shot("resume");compose.onNodeWithTag("homePrimary").performClick();waitText("Open the outline")
 }
 @Test fun disabledSuggestionsAndWaitingLeadToTasks(){
  runBlocking{app.repo.save(Task(title="Waiting for a reply",status="WAITING"));app.prefs.bool("suggestions",false)}
  waitText("Suggestions are off. Open your task list and pick the task you want to start.");compose.onNodeWithTag("homePrimary").performClick();compose.onNodeWithTag("taskList").assertExists()
 }
 @Test fun guideExplainsAndOpensProjects(){
  waitText("Add your first task");compose.onNodeWithTag("homeGuide").performScrollTo().performClick();compose.onNode(hasText("Projects") and hasAnyAncestor(isDialog())).performScrollTo().performClick();waitText("New project")
 }
}
