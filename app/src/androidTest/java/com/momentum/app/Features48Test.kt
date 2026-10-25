package com.momentum.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import android.app.Notification
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
class Features48Test {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 @get:Rule val migration=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),MomentumDb::class.java,emptyList(),FrameworkSQLiteOpenHelperFactory())
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa"))}
 @Before fun setup():Unit=runBlocking{FocusNotification.remove(app);app.db.dao().clearTasks();app.db.dao().clearProjects();app.db.dao().clearEvents();app.prefs.bool("english",true);app.prefs.bool("onboarded",true)}
 private fun waitTag(tag:String){compose.waitUntil(10000){compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()}}
 private fun shot(name:String){compose.mainClock.advanceTimeBy(1000);compose.waitForIdle();compose.onRoot().captureToImage().asAndroidBitmap().let{b->File(app.filesDir,"features48-"+name+".png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}}}
 @Test fun existingProjectTaskOpensDetailsAndPinsWithProject(){
  val p=Project(title="Summer trip",emoji="✈️",color="BLUE");val t=Task(title="Book hotel",projectId=p.id,nextAction="Compare rooms",status="READY")
  runBlocking{app.repo.saveProject(p);app.repo.save(t)}
  compose.waitUntil(10000){compose.onAllNodesWithText("Tasks").fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithText("Tasks").performClick();waitTag("taskScope")
  compose.onNodeWithTag("taskScope").performClick();compose.onNodeWithText("All tasks").performClick();compose.onNodeWithText("Show results").performClick()
  compose.onNodeWithText("Book hotel",useUnmergedTree=true).performClick();waitTag("taskDetailsTitle")
  compose.onNodeWithText("✈️ Summer trip").assertIsDisplayed();compose.onNodeWithText("Keep in notifications").performClick()
  compose.waitUntil(10000){FocusNotification.pinnedId(app)==t.id}
  compose.waitUntil(10000){app.getSystemService(android.app.NotificationManager::class.java).activeNotifications.isNotEmpty()}
  shot("task")
  val n=app.getSystemService(android.app.NotificationManager::class.java).activeNotifications.single().notification
  assertEquals("Book hotel",n.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
  assertTrue(n.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("✈️ Summer trip"))
  assertFalse(n.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("Summer trip"))
  runBlocking{app.repo.saveProject(p.copy(title="Autumn trip",emoji="🍂"));FocusNotification.refresh(app)}
  compose.waitUntil(10000){app.getSystemService(android.app.NotificationManager::class.java).activeNotifications.any{it.notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("🍂 Autumn trip")}}
 }
 @Test fun projectEditorColorPersistsAndBacksUp(){
  val p=Project(title="My project",emoji="📚");runBlocking{app.repo.saveProject(p)}
  compose.waitUntil(10000){compose.onAllNodesWithText("Projects").fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithText("Projects").performClick();compose.onNodeWithText("📚 My project",useUnmergedTree=true).performClick()
  compose.onNodeWithText("Your progress will be kept here.").assertDoesNotExist()
  compose.onNodeWithText("Edit project").performScrollTo().performClick();compose.onNodeWithText("Blue").performScrollTo().performClick();compose.onNode(hasText("Save") and hasAnyAncestor(isDialog())).performScrollTo().performClick()
  compose.waitUntil(10000){runBlocking{app.repo.dao.allProjects().single().color=="BLUE"}}
  shot("project")
  runBlocking{val backup=Backup.export(app.repo);Backup.restore(app.repo,backup);assertEquals("BLUE",app.repo.dao.allProjects().single().color)
   val legacy=org.json.JSONObject(backup);legacy.put("schema",4);legacy.getJSONArray("projects").getJSONObject(0).remove("color");Backup.restore(app.repo,legacy.toString());assertEquals("PURPLE",app.repo.dao.allProjects().single().color)}
 }
 @Test fun migrationPreservesProjectEmojiAndDefaultsColor(){
  migration.createDatabase("color-migration",5).apply{execSQL("INSERT INTO projects (id,title,description,summary,nextStep,paused,review,created,updated,emoji) VALUES ('p','Trip','','','',0,NULL,1,1,'plane')");close()}
  migration.runMigrationsAndValidate("color-migration",6,true,MomentumDb.MIGRATION_5_6).apply{query("SELECT title,emoji,color FROM projects WHERE id='p'").use{assertTrue(it.moveToFirst());assertEquals("Trip",it.getString(0));assertEquals("plane",it.getString(1));assertEquals("PURPLE",it.getString(2))};close()}
 }
}
