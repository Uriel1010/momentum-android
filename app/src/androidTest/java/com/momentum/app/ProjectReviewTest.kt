package com.momentum.app
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
class ProjectReviewTest {
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa"))}
 @Before fun setup()=runBlocking{
  app.db.dao().clearProjects();app.db.dao().clearDeliveries()
  app.prefs.bool("notifications",true);app.prefs.number("quietStart",0);app.prefs.number("quietEnd",0);app.prefs.number("dailyCap",3)
 }
 private suspend fun deliver(p:Project){
  val worker=TestListenableWorkerBuilder<ProjectReviewWorker>(app).setInputData(workDataOf("id" to p.id,"stamp" to p.review!!)).build()
  assertEquals(ListenableWorker.Result.success(),worker.doWork())
 }
 @Test fun projectReviewDeliveredOnce()=runBlocking{
  val p=Project(title="Synthetic project review",review=System.currentTimeMillis()-1)
  app.db.dao().saveProject(p);deliver(p);deliver(p)
  assertEquals(1,app.db.dao().allDeliveries().size)
  scheduleProject(app,p.copy(paused=true))
 }
 @Test fun pausedAndChangedReviewNeverDelivered()=runBlocking{
  val p=Project(title="Paused review",paused=true,review=System.currentTimeMillis()-1)
  app.db.dao().saveProject(p);deliver(p)
  app.db.dao().saveProject(p.copy(paused=false,review=p.review!!+86_400_000));deliver(p)
  assertTrue(app.db.dao().allDeliveries().isEmpty())
 }
 @Test fun sharedDailyCapAppliesToProjects()=runBlocking{
  app.prefs.number("dailyCap",1)
  app.db.dao().delivery(Delivery("synthetic-task-delivery","synthetic"))
  val p=Project(title="Capped review",review=System.currentTimeMillis()-1)
  app.db.dao().saveProject(p);deliver(p)
  assertEquals(1,app.db.dao().allDeliveries().size)
  scheduleProject(app,p.copy(paused=true))
 }
}
