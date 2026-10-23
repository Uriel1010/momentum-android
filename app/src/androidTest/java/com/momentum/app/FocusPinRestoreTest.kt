package com.momentum.app
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
class FocusPinRestoreTest {
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa"))}
 @Test fun stage()=runBlocking{val task=Task(id="qa-focus-restore",title="Continue the draft");app.repo.save(task);assertTrue(FocusNotification.pin(app,task.id))}
 @Test fun verifyAndClean():Unit=runBlocking{
  assertEquals("qa-focus-restore",FocusNotification.pinnedId(app));assertTrue(FocusNotification.refresh(app))
  assertTrue(app.getSystemService(NotificationManager::class.java).activeNotifications.any{it.tag==FocusNotification.TAG})
  FocusNotification.remove(app);app.db.dao().task("qa-focus-restore")?.let{app.repo.delete(it)}
 }
}
