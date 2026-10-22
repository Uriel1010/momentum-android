package com.momentum.app
import android.app.NotificationManager
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderDeviceTest {
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa")){"Fixture tests require -PmomentumQa=true"}}
 private var backup=""
 private val ids=mutableListOf<String>()
 @Before fun setup()=runBlocking {
  backup=Backup.export(app.repo)
  app.db.dao().clearTasks();app.db.dao().clearDeliveries()
  app.prefs.bool("notifications",true);app.prefs.number("quietStart",0);app.prefs.number("quietEnd",0);app.prefs.number("dailyCap",3)
  assertTrue("Grant notifications before this test suite",NotificationManagerCompat.from(app).areNotificationsEnabled())
 }
 @After fun cleanup()=runBlocking {ids.forEach{cancelReminder(app,it)};if(backup.isNotEmpty())Backup.restore(app.repo,backup)}
 private suspend fun task():Task {val t=Task(title="Momentum notification test",reminder=System.currentTimeMillis()-1000);app.db.dao().save(t);ids+=t.id;return t}
 private suspend fun deliver(t:Task) {val worker=TestListenableWorkerBuilder<ReminderWorker>(app).setInputData(workDataOf("id" to t.id,"stamp" to t.reminder!!)).build();assertEquals(ListenableWorker.Result.success(),worker.doWork())}
 @Test fun dedupAndCompletedSuppression()=runBlocking {
  val t=task();deliver(t);deliver(t)
  assertEquals(1,app.db.dao().allDeliveries().size)
  delay(300)
  assertTrue(app.getSystemService(NotificationManager::class.java).activeNotifications.any{it.id==t.id.hashCode()})
  app.repo.complete(t);deliver(t)
  assertEquals(1,app.db.dao().allDeliveries().size)
  delay(300)
  assertFalse(app.getSystemService(NotificationManager::class.java).activeNotifications.any{it.id==t.id.hashCode()})
 }
 @Test fun dailyCapEnforced()=runBlocking {
  app.prefs.number("dailyCap",1)
  deliver(task());deliver(task())
  assertEquals(1,app.db.dao().allDeliveries().size)
 }
 @Test fun quietHoursDeferDelivery()=runBlocking {
  val hour=java.time.ZonedDateTime.now().hour
  app.prefs.number("quietStart",hour);app.prefs.number("quietEnd",(hour+1)%24)
  deliver(task());assertTrue(app.db.dao().allDeliveries().isEmpty())
 }
 @Test fun disabledRemindersDoNotDeliver()=runBlocking {
  app.prefs.bool("notifications",false)
  deliver(task());assertTrue(app.db.dao().allDeliveries().isEmpty())
 }
}
@RunWith(AndroidJUnit4::class)
class DeniedPermissionTest {
 @Test fun denialDoesNotBreakTasksOrConsumeReminder()=runBlocking {
  val app=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa")){"Fixture tests require -PmomentumQa=true"}}
  assertFalse("Revoke notifications before running this test",NotificationManagerCompat.from(app).areNotificationsEnabled())
  val t=Task(title="Denied permission test",reminder=System.currentTimeMillis()-1000)
  val p=app.prefs.flow.first()
  try {
   app.prefs.bool("notifications",true)
   app.db.dao().save(t)
   val worker=TestListenableWorkerBuilder<ReminderWorker>(app).setInputData(workDataOf("id" to t.id,"stamp" to t.reminder!!)).build()
   assertEquals(ListenableWorker.Result.success(),worker.doWork())
   assertEquals(t.title,app.db.dao().task(t.id)?.title)
   assertEquals(0,app.db.dao().delivered("${t.id}:${t.reminder}"))
  } finally {app.repo.delete(t);app.prefs.restore(p)}
 }
}
