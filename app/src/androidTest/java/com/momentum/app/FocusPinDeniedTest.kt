package com.momentum.app
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
class FocusPinDeniedTest {
 @Test fun deniedDoesNotPinOrStartTask()=runBlocking{
  val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp;check(app.packageName.endsWith(".qa"))
  FocusNotification.remove(app);assertFalse(FocusNotification.canPost(app))
  val task=Task(title="Permission test");app.repo.save(task)
  assertFalse(FocusNotification.pin(app,task.id));assertNull(FocusNotification.pinnedId(app));assertEquals("INBOX",app.db.dao().task(task.id)!!.status)
  app.repo.delete(task)
 }
}
