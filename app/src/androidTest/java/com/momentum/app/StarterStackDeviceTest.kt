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
class StarterStackDeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa"))}
 @Before fun setup():Unit=runBlocking{app.db.dao().clearTasks();app.db.dao().clearProjects();app.db.dao().clearEvents();app.prefs.bool("english",true);app.prefs.bool("onboarded",true);app.prefs.bool("suggestions",true);listOf("Read five pages","Write the opening paragraph","Plan a short walk").forEachIndexed{i,title->app.repo.save(Task(id="stack-$i",title=title,nextAction="Take one small step",estimate=5,status="READY",created=100L+i))}}
 private fun waitText(text:String){compose.waitUntil(10000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}}
 @Test fun browsingDoesNotMutateTasksAndStartUsesVisibleCard(){
  waitText("Task 1 of 3");compose.mainClock.advanceTimeBy(1000);compose.waitForIdle()
  compose.onRoot().captureToImage().asAndroidBitmap().let{b->File(app.filesDir,"starter-stack.png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}}
  compose.onNodeWithTag("stackNext").performScrollTo().performClick();waitText("Task 2 of 3");compose.onNodeWithText("Write the opening paragraph").assertExists()
  compose.onNodeWithTag("stackPrevious").performScrollTo().performClick();waitText("Task 1 of 3")
  compose.onNodeWithTag("starterStack").performTouchInput{swipe(androidx.compose.ui.geometry.Offset(width*.8f,centerY),androidx.compose.ui.geometry.Offset(width*.2f,centerY),400)};waitText("Task 2 of 3")
  runBlocking{assertTrue(app.db.dao().allTasks().all{it.status=="READY" && it.available==0L});assertFalse(app.db.dao().allEvents().any{it.kind in setOf("started","postponed","completed")})}
  compose.onNodeWithTag("homePrimary").performScrollTo().performClick();waitText("I’m done")
  assertEquals("IN_PROGRESS",runBlocking{app.db.dao().task("stack-1")!!.status});assertEquals("READY",runBlocking{app.db.dao().task("stack-0")!!.status})
 }
 @Test fun shortDragReturnsAndReducedMotionStillNavigates(){
  waitText("Task 1 of 3")
  compose.onNodeWithTag("starterStack").performTouchInput{swipe(center,center+androidx.compose.ui.geometry.Offset(-25f,0f),300)}
  compose.mainClock.advanceTimeBy(1000);compose.onNodeWithText("Task 1 of 3").assertExists()
  runBlocking{app.prefs.bool("reducedMotion",true)}
  compose.onNodeWithTag("stackNext").performScrollTo().performClick();waitText("Task 2 of 3")
  runBlocking{app.prefs.bool("reducedMotion",false)}
 }
 @Test fun removingVisibleCardLeavesValidSelection(){
  waitText("Task 1 of 3");compose.onNodeWithTag("stackNext").performScrollTo().performClick();waitText("Task 2 of 3")
  runBlocking{app.repo.complete(app.db.dao().task("stack-1")!!)};waitText("Task 1 of 2");compose.onNodeWithText("Read five pages").assertExists()
  runBlocking{app.repo.complete(app.db.dao().task("stack-2")!!)};compose.waitUntil(10000){compose.onAllNodesWithTag("stackNext").fetchSemanticsNodes().isEmpty()};compose.onNodeWithTag("homePrimary").assertExists()
 }
}
