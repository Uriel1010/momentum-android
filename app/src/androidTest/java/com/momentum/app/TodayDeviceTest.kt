package com.momentum.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import android.app.Notification
import android.graphics.Bitmap
import java.io.File
class TodayDeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa"))}
 @Before fun setup():Unit=runBlocking{FocusNotification.remove(app);app.repo.dao.clearTasks();app.repo.dao.clearProjects();app.repo.dao.clearEvents();app.prefs.bool("english",true);app.prefs.bool("onboarded",true)}
 private fun notification()=app.getSystemService(android.app.NotificationManager::class.java).activeNotifications.firstOrNull{it.id==FocusNotification.ID}?.notification
 private fun waitCount(n:Int){compose.waitUntil(10000){notification()?.number==n}}
 @Test fun multiplePinsKeepOrderWithoutStartingAndOneRemovalPreservesOthers():Unit=runBlocking{
  val p=Project(title="Trip",emoji="✈️");app.repo.saveProject(p)
  val tasks=listOf(Task(title="Pack bag",status="READY",projectId=p.id),Task(title="Buy groceries",status="READY"),Task(title="Call Alex",status="WAITING"))
  tasks.forEach{app.repo.save(it);assertTrue(FocusNotification.pin(app,it.id))};FocusNotification.pin(app,tasks[0].id)
  assertEquals(tasks.map{it.id},FocusNotification.pinnedIds(app));assertEquals(listOf("READY","READY","WAITING"),tasks.map{app.repo.dao.task(it.id)!!.status});assertFalse(app.repo.dao.allEvents().any{it.kind=="started"})
  waitCount(3);val n=notification()!!;assertEquals(3,n.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)!!.size);assertTrue(n.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)!![0].contains("✈️ Trip"))
  assertEquals(1,app.getSystemService(android.app.NotificationManager::class.java).activeNotifications.count{it.id==FocusNotification.ID});assertFalse(n.publicVersion.extras.toString().contains("Pack bag"))
  val stale=FocusNotification.store(app).getString("token",null)!!
  FocusNotification.remove(app,taskId=tasks[1].id);waitCount(2);assertEquals(listOf(tasks[0].id,tasks[2].id),FocusNotification.pinnedIds(app))
  FocusNotification.remove(app,token=stale);assertEquals(2,FocusNotification.pinnedIds(app).size)
  app.repo.complete(tasks[0]);FocusNotification.refresh(app);waitCount(1);assertEquals(listOf(tasks[2].id),FocusNotification.pinnedIds(app))
  app.repo.delete(tasks[2]);FocusNotification.refresh(app);assertTrue(FocusNotification.pinnedIds(app).isEmpty())
 }
 @Test fun clearingDuringPendingPostDoesNotResurrectNotification():Unit=runBlocking{
  val t=Task(title="Quick change",status="READY");app.repo.save(t);FocusNotification.pin(app,t.id);FocusNotification.remove(app)
  kotlinx.coroutines.delay(800);assertTrue(FocusNotification.pinnedIds(app).isEmpty());assertNull(notification());assertEquals("READY",app.repo.dao.task(t.id)!!.status)
 }
 @Test fun legacyPinMigratesAndLargeListHasOverflow():Unit=runBlocking{
  val t=Task(title="Previously pinned",status="IN_PROGRESS");app.repo.save(t)
  FocusNotification.store(app).edit().clear().putString("task",t.id).putString("token","old").commit();FocusNotification.refresh(app)
  assertEquals(listOf(t.id),FocusNotification.pinnedIds(app));assertFalse(FocusNotification.store(app).contains("task"))
  repeat(8){val next=Task(title="Today step $it",status="READY");app.repo.save(next);FocusNotification.pin(app,next.id)}
  waitCount(9);assertEquals(6,notification()!!.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)!!.size);assertTrue(notification()!!.extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT).toString().contains("3"))
  FocusNotification.refresh(app);assertEquals(9,FocusNotification.pinnedIds(app).size)
 }
 @Test fun chooseSeveralFromTodayAndOpenFromNotification(){
  val tasks=listOf(Task(id="today-one",title="Read a chapter",status="READY"),Task(id="today-two",title="Buy groceries",status="READY"),Task(id="today-three",title="Plan tomorrow",status="READY"))
  runBlocking{tasks.forEach{app.repo.save(it)}}
  compose.waitUntil(10000){compose.onAllNodesWithTag("openToday").fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithTag("openToday").performClick()
  tasks.forEach{compose.onNodeWithTag("todayList").performScrollToNode(hasTestTag("todayToggle-"+it.id));compose.onNodeWithTag("todayToggle-"+it.id).performClick()}
  waitCount(3);assertEquals(3,FocusNotification.pinnedIds(app).size)
  compose.onNodeWithTag("todayList").performScrollToIndex(0);compose.mainClock.advanceTimeBy(1000);compose.waitForIdle();compose.onRoot().captureToImage().asAndroidBitmap().let{b->File(app.filesDir,"today-list.png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}}
  compose.onNodeWithText("Read a chapter",useUnmergedTree=true).performClick();compose.onNodeWithTag("taskDetailsTitle").assertExists();notification()!!.contentIntent.send()
  compose.waitUntil(10000){compose.onAllNodesWithTag("todayList").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithTag("todayToggle-today-two").performClick();waitCount(2);assertFalse("today-two" in FocusNotification.pinnedIds(app))
  compose.onNodeWithTag("clearToday").performClick();compose.waitUntil(10000){FocusNotification.pinnedIds(app).isEmpty()};assertEquals(3,runBlocking{app.repo.dao.allTasks().size})
 }
}
