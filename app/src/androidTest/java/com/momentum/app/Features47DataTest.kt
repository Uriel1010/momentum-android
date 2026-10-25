package com.momentum.app
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
class Features47DataTest {
 private val context=InstrumentationRegistry.getInstrumentation().targetContext
 private lateinit var db:MomentumDb
 private lateinit var repo:Repository
 @get:Rule val migrations=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),MomentumDb::class.java,emptyList(),FrameworkSQLiteOpenHelperFactory())
 @Before fun setup(){check(context.packageName.endsWith(".qa"));db=Room.inMemoryDatabaseBuilder(context,MomentumDb::class.java).build();repo=Repository(context,db,Preferences(context))}
 @After fun teardown(){db.close()}
 @Test fun migrationKeepsExistingRows(){
  migrations.createDatabase("features47-migration",4).apply{
   execSQL("INSERT INTO tasks (id,title,notes,estimate,priority,status,projectId,due,available,contexts,nextAction,reminder,created,updated,completed,lastConsidered) VALUES ('kept','Keep this task','notes',60,2,'WAITING',NULL,NULL,0,'','step',NULL,1,1,NULL,0)")
   execSQL("INSERT INTO projects (id,title,description,summary,nextStep,paused,review,created,updated) VALUES ('p','Trip','','','',0,NULL,1,1)");close()
  }
  migrations.runMigrationsAndValidate("features47-migration",5,true,MomentumDb.MIGRATION_4_5).apply{
   query("SELECT title,estimate,status,repeatMode FROM tasks WHERE id='kept'").use{assertTrue(it.moveToFirst());assertEquals("Keep this task",it.getString(0));assertEquals(60,it.getInt(1));assertEquals("WAITING",it.getString(2));assertEquals("NONE",it.getString(3))}
   query("SELECT emoji FROM projects WHERE id='p'").use{assertTrue(it.moveToFirst());assertEquals("",it.getString(0))};close()
  }
 }
 @Test fun completionUndoAndRedoDoNotDuplicate():Unit=runBlocking{
  val t=Task(title="Daily",status="READY",repeatMode="AFTER");repo.save(t);repo.complete(t);repo.complete(t)
  assertEquals(2,repo.dao.allTasks().size);val next=repo.dao.task(Recurrence.childId(t.id))!!;assertTrue(next.available>System.currentTimeMillis());assertEquals("READY",next.status)
  repo.undo(t);assertEquals(1,repo.dao.allTasks().size);repo.complete(t);assertEquals(2,repo.dao.allTasks().size)
  repo.save(next.copy(title="Edited next"));repo.undo(t);assertEquals(2,repo.dao.allTasks().size);repo.complete(t);assertEquals(2,repo.dao.allTasks().size);assertEquals("Edited next",repo.dao.task(next.id)!!.title)
 }
 @Test fun backupPreservesRepeatsAndEmojiAndRejectsInvalid():Unit=runBlocking{
  val p=Project(title="Trip",emoji="✈️");repo.saveProject(p)
  val t=Task(title="Packing",projectId=p.id,repeatMode="FIXED",repeatUnit="MONTH",repeatEvery=2,repeatAnchor=1000);repo.save(t)
  val text=Backup.export(repo);Backup.restore(repo,text)
  assertEquals("✈️",repo.dao.allProjects().single().emoji);assertEquals(2,repo.dao.task(t.id)!!.repeatEvery)
  try{Backup.restore(repo,text.replace("\"repeatEvery\": 2","\"repeatEvery\": 0"));fail()}catch(_:IllegalArgumentException){}
  assertEquals(2,repo.dao.task(t.id)!!.repeatEvery)
 }
}
