package com.momentum.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
class NeoDeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa"))}
 @Before fun setup()=runBlocking{app.db.dao().clearTasks();app.db.dao().clearProjects();app.db.dao().clearEvents();app.prefs.bool("english",true);app.prefs.bool("onboarded",true);app.prefs.bool("suggestions",true)}
 private fun waitText(text:String)=compose.waitUntil(10000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}
 @Test fun recoveryKeepsTaskAndExcludesBreakFromTimer(){
  val task=Task(title="A small draft",nextAction="Write one line");runBlocking{app.repo.save(task)}
  waitText("Let’s start");compose.onNodeWithText("Let’s start").performScrollTo().performClick()
  compose.onNodeWithText("Optional timer").performScrollTo().performClick()
  val timer=app.getSharedPreferences("focus_timer",android.content.Context.MODE_PRIVATE)
  val before=timer.getLong(task.id,0);assertTrue(before>0)
  compose.onNodeWithText("Take a breather").performScrollTo().performClick();waitText("Take a breath.")
  android.os.SystemClock.sleep(1100)
  compose.onNodeWithText("Continue when ready").performScrollTo().performClick()
  compose.onNodeWithText("Write one line").assertExists()
  compose.waitUntil(5000){timer.getLong(task.id,0)>=before+1000}
  assertEquals("IN_PROGRESS",runBlocking{app.db.dao().task(task.id)!!.status})
 }
 @Test fun homeBlockerExcludesWaitingFromRecommendation(){
  val task=Task(title="Await a reply");runBlocking{app.repo.save(task)}
  waitText("I’m stuck");compose.onNodeWithText("I’m stuck").performScrollTo().performClick()
  compose.onNodeWithText("Waiting for someone / something").performScrollTo().performClick()
  compose.waitUntil(10000){runBlocking{app.db.dao().task(task.id)?.status=="WAITING"}}
  assertTrue(runBlocking{NowPolicy.candidates(app.db.dao().allTasks(),emptyList(),System.currentTimeMillis(),0,false,"").isEmpty()})
 }
}
