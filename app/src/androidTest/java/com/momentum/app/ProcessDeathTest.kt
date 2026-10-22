package com.momentum.app
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
@RunWith(AndroidJUnit4::class)
class ProcessDeathTest {
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa")){"Fixture tests require -PmomentumQa=true"}}
 @Test fun stage()=runBlocking {
  File(app.filesDir,"process-death-backup.json").writeText(Backup.export(app.repo))
  val p=Project(title="Process death project",summary="Persistent last stopping point",nextStep="Persistent next step")
  app.db.dao().saveProject(p)
  val t=Task(title="Process death task",projectId=p.id,nextAction="Open file")
  app.repo.save(t);app.repo.advance(t,"History persists")
  val timerStart=System.currentTimeMillis()
  app.getSharedPreferences("focus_timer",android.content.Context.MODE_PRIVATE).edit().putLong(t.id,timerStart).commit()
  File(app.filesDir,"process-death-ids").writeText(t.id+"\n"+p.id+"\n"+timerStart)
  app.prefs.bool("onboarded",true)
 }
 @Test fun verifyAndRestore()=runBlocking {
  val backup=File(app.filesDir,"process-death-backup.json")
  try {
   val ids=File(app.filesDir,"process-death-ids").readLines()
   assertEquals(ids[2].toLong(),app.getSharedPreferences("focus_timer",android.content.Context.MODE_PRIVATE).getLong(ids[0],0))
   app.getSharedPreferences("focus_timer",android.content.Context.MODE_PRIVATE).edit().remove(ids[0]).commit()
   assertEquals("Process death task",app.db.dao().task(ids[0])?.title)
   val project=app.db.dao().allProjects().single{it.id==ids[1]}
   assertEquals("History persists",project.summary)
   assertEquals("Open file",project.nextStep)
   assertTrue(app.db.dao().allEvents().any{it.projectId==project.id && it.note=="History persists"})
  } finally {
   Backup.restore(app.repo,backup.readText());backup.delete();File(app.filesDir,"process-death-ids").delete()
  }
  Unit
 }
}
