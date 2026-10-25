package com.momentum.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File
import android.graphics.Bitmap
class Features47UiTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa"))}
 @Before fun setup():Unit=runBlocking{FocusNotification.remove(app);app.db.dao().clearTasks();app.db.dao().clearProjects();app.db.dao().clearEvents();app.prefs.bool("english",true);app.prefs.bool("onboarded",true);app.prefs.bool("suggestions",true)}
 private fun waitTag(tag:String){compose.waitUntil(10000){compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()}}
 private fun shot(name:String){compose.mainClock.advanceTimeBy(1000);compose.waitForIdle();compose.onRoot().captureToImage().asAndroidBitmap().let{b->File(app.filesDir,"features47-"+name+".png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}}}
 @Test fun saveOpensDetailsAndHourAndRepeatAreEditable(){
  waitTag("homePrimary");compose.onNodeWithTag("homePrimary").performClick();waitTag("quickCapture");compose.onNodeWithTag("quickCapture").performTextInput("Read a chapter");compose.onNodeWithTag("quickCapture").performImeAction();waitTag("taskDetailsTitle")
  compose.onNodeWithText("1 hour").performScrollTo().performClick();compose.onNodeWithText("After completion").performScrollTo().performClick();compose.waitForIdle()
  runBlocking{assertEquals(60,app.db.dao().allTasks().single().estimate);assertEquals("AFTER",app.db.dao().allTasks().single().repeatMode);assertEquals("READY",app.db.dao().allTasks().single().status)}
  shot("detail")
 }
 @Test fun saveAndPinShowsOngoingNotification(){
  waitTag("homePrimary");compose.onNodeWithTag("homePrimary").performClick();waitTag("quickCapture");compose.onNodeWithTag("quickCapture").performTextInput("Write the first paragraph")
  compose.onNodeWithTag("saveAndPin").performScrollTo().performClick();waitTag("taskDetailsTitle")
  compose.waitUntil(10000){FocusNotification.pinnedId(app)!=null}
  runBlocking{assertEquals(app.db.dao().allTasks().single().id,FocusNotification.pinnedId(app));assertEquals("IN_PROGRESS",app.db.dao().allTasks().single().status)}
  assertTrue(app.getSystemService(android.app.NotificationManager::class.java).activeNotifications.isNotEmpty());shot("pinned")
 }
 @Test fun personalScopeExcludesProjectsAndAllLabelsThem(){
  runBlocking{val p=Project(title="Summer trip",emoji="✈️");app.repo.saveProject(p);app.repo.save(Task(title="Book the hotel",projectId=p.id));repeat(8){app.repo.save(Task(title="Personal step ${it+1}"))}}
  waitTag("homePrimary");shot("home");compose.onNode(hasText("Tasks") and hasClickAction()).performClick();waitTag("taskList")
  compose.onNodeWithText("Book the hotel").assertDoesNotExist();shot("tasks")
  compose.onNodeWithTag("taskScope").performClick();compose.onNodeWithText("All tasks").performClick();compose.onNodeWithText("Show results").performClick()
  compose.onNodeWithTag("taskList").performScrollToNode(hasText("Book the hotel"));compose.onNodeWithText("Book the hotel").assertIsDisplayed();compose.onNodeWithText("✈️ Summer trip",substring=true).assertExists()
 }
}
