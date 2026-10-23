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
class CompactTasksDeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa"))}
 @Before fun setup():Unit=runBlocking{app.db.dao().clearTasks();app.db.dao().clearProjects();app.prefs.bool("english",true);app.prefs.bool("onboarded",true);listOf("Write the opening paragraph","Plan a weekend walk","Book a dentist appointment","Read ten pages","Send the project update","Water the plants").forEachIndexed{i,title->app.repo.save(Task(id="compact-$i",title=title,created=100L-i,estimate=5))}}
 @Test fun listStartsNearTopAndControlsStayAccessible(){
  compose.waitUntil(10000){compose.onAllNodesWithText("Tasks").fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithText("Tasks").performClick()
  compose.waitUntil(10000){compose.onAllNodesWithTag("taskRow-compact-0").fetchSemanticsNodes().isNotEmpty()}
  val list=compose.onNodeWithTag("taskList").fetchSemanticsNode().boundsInRoot
  val first=compose.onNodeWithTag("taskRow-compact-0").fetchSemanticsNode().boundsInRoot
  assertTrue("First task must start within 140dp of the list top",first.top-list.top<140*app.resources.displayMetrics.density)
  compose.onNodeWithTag("taskRow-compact-2").assertIsDisplayed()
  compose.onRoot().captureToImage().asAndroidBitmap().let{bitmap->File(app.filesDir,"v44-tasks.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}}
  compose.onNodeWithTag("taskSearchToggle").performClick();compose.onNodeWithTag("taskSearch").performTextInput("weekend")
  compose.onNodeWithText("Plan a weekend walk").assertExists();compose.onNodeWithText("Write the opening paragraph").assertDoesNotExist()
  compose.onNodeWithTag("taskSearchToggle").performClick();compose.onNodeWithText("Write the opening paragraph").assertExists()
  compose.onNodeWithTag("taskFilters").performClick();compose.onNodeWithText("Show results").performScrollTo().performClick();compose.onNodeWithTag("taskRow-compact-0").assertExists()
 }
}
