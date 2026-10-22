package com.momentum.app
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataDeviceTest {
 private val context=InstrumentationRegistry.getInstrumentation().targetContext
 private lateinit var db:MomentumDb
 private lateinit var repo:Repository
 @Before fun setup() {db=Room.inMemoryDatabaseBuilder(context,MomentumDb::class.java).build();repo=Repository(context,db,Preferences(context))}
 @After fun teardown() {db.close()}
 @get:Rule val migrations=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),MomentumDb::class.java,emptyList(),FrameworkSQLiteOpenHelperFactory())
 @Test fun migrationPreservesTask() {
  migrations.createDatabase("migration-check",1).apply {
   execSQL("INSERT INTO tasks (id,title,notes,estimate,priority,status,projectId,due,available,contexts,nextAction,reminder,created,updated,completed,lastConsidered) VALUES ('persist','kept','',NULL,1,'INBOX',NULL,NULL,0,'','',NULL,1,1,NULL,0)")
   close()
  }
  migrations.runMigrationsAndValidate("migration-check",2,true,MomentumDb.MIGRATION_1_2).apply {
   query("SELECT title FROM tasks WHERE id='persist'").use {assertTrue(it.moveToFirst());assertEquals("kept",it.getString(0))}
   close()
  }
 }
 @Test fun taskRoundtripAndCompletionIsIdempotent()=runBlocking {
  val t=Task(title="בדיקת משימה");repo.save(t)
  assertEquals(t.title,db.dao().task(t.id)?.title)
  repo.complete(t);repo.complete(t)
  assertEquals("DONE",db.dao().task(t.id)?.status)
  assertEquals(1,db.dao().allEvents().count{it.kind=="completed"})
  repo.undo(t);assertEquals("INBOX",db.dao().task(t.id)?.status)
  assertEquals(0,db.dao().allEvents().count{it.kind=="completed"})
 }
 @Test fun backupRoundtripAndMalformedAtomicity()=runBlocking {
  val p=Project(title="פרויקט",summary="כאן עצרתי",nextStep="צעד קטן");db.dao().saveProject(p)
  val t=Task(title="משימה",projectId=p.id,nextAction="לפתוח מסמך");repo.save(t)
  repo.advance(t,"התקדמות נשמרת")
  db.dao().suggestion(SuggestionRecord(taskId=t.id))
  val before=Backup.export(repo)
  db.dao().clearTasks();db.dao().clearProjects();db.dao().clearEvents()
  Backup.restore(repo,before)
  assertEquals(t.title,db.dao().task(t.id)?.title)
  assertEquals("התקדמות נשמרת",db.dao().allProjects().single().summary)
  assertEquals(1,db.dao().allSuggestions().size)
  try {Backup.restore(repo,before.replace("\"schema\": 2","\"schema\": 99"));fail()} catch(_:IllegalArgumentException){}
  assertEquals(t.title,db.dao().task(t.id)?.title)
  try {Backup.restore(repo,"{broken");fail()} catch(_:org.json.JSONException){}
  assertEquals(t.title,db.dao().task(t.id)?.title)
 }
 @Test fun waitingTaskNotSuggestedUntilReview()=runBlocking {
  val now=System.currentTimeMillis();val t=Task(title="Waiting",status="WAITING",available=now+1000)
  repo.save(t)
  assertTrue(SuggestionEngine.rank(db.dao().allTasks(),now).isEmpty())
  assertTrue(SuggestionEngine.rank(db.dao().allTasks(),now+1000).isEmpty())
 }
 @Test fun thousandTasksPersistAndSuggest()=runBlocking {
  val start=System.currentTimeMillis()
  repeat(1000) {db.dao().save(Task(title="Task $it"))}
  assertEquals(1000,db.dao().allTasks().size)
  assertEquals(1000,SuggestionEngine.rank(db.dao().allTasks(),System.currentTimeMillis()).size)
  InstrumentationRegistry.getInstrumentation().sendStatus(2,android.os.Bundle().apply {putLong("performance_1000_ms",System.currentTimeMillis()-start)});Unit
 }
 @Test fun uniqueDeliveryDeduplicates()=runBlocking {
  val delivery=Delivery("unique",uuid());db.dao().delivery(delivery);db.dao().delivery(delivery)
  assertEquals(1,db.dao().allDeliveries().size)
 }
 @Test fun databaseSurvivesReopen()=runBlocking {
  val name="persistence-test.db"
  context.deleteDatabase(name)
  var persistent=Room.databaseBuilder(context,MomentumDb::class.java,name).addMigrations(MomentumDb.MIGRATION_1_2).build()
  val t=Task(title="Survives restart")
  persistent.dao().save(t);persistent.close()
  persistent=Room.databaseBuilder(context,MomentumDb::class.java,name).addMigrations(MomentumDb.MIGRATION_1_2).build()
  assertEquals(t.title,persistent.dao().task(t.id)?.title)
  persistent.close();context.deleteDatabase(name);Unit
 }

 @Test fun postponementUndoAndBlockerOutcomes()=runBlocking {
  val t=Task(title="Deferred intention");repo.save(t)
  repo.postpone(t,3_600_000)
  assertTrue(db.dao().task(t.id)!!.available>System.currentTimeMillis())
  repo.undoPostpone(t)
  assertEquals(0L,db.dao().task(t.id)!!.available)
  assertTrue(db.dao().allEvents().none{it.kind=="postponed"})
  repo.block(t,"later")
  assertTrue(db.dao().task(t.id)!!.available>System.currentTimeMillis()+23*3_600_000L)
 }
 @Test fun milestoneIsExplicitAndSurvivesBackup()=runBlocking {
  val p=Project(title="Real outcome");db.dao().saveProject(p)
  repo.milestone(p,"Draft ready")
  val backup=Backup.export(repo);Backup.restore(repo,backup)
  assertEquals("Draft ready",db.dao().allProjects().single().summary)
  assertEquals("milestone",db.dao().allEvents().single().kind)
 }
}
