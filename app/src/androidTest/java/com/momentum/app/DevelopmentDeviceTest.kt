package com.momentum.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import java.io.File

class DevelopmentDeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 @get:Rule val migrations=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),MomentumDb::class.java,emptyList(),FrameworkSQLiteOpenHelperFactory())
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa"))}
 @Before fun setup()=runBlocking {app.db.dao().clearDevelopmentIdeas();app.prefs.bool("english",true);app.prefs.bool("onboarded",true)}
 private fun waitText(text:String)=compose.waitUntil(10000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}
 private fun open(){waitText("Capture a thought");compose.onNodeWithContentDescription("Settings").performClick();waitText("Suggestions for Momentum");compose.onNodeWithText("Suggestions for Momentum").performClick();waitText("Add a suggestion")}
 @Test fun editFilterRecreateAndDelete(){
  open();compose.onNodeWithText("Add a suggestion").performClick()
  compose.onNodeWithText("Save").assertIsNotEnabled()
  compose.onNodeWithTag("ideaTitle").performTextInput("Improve the weekly review")
  compose.onNodeWithTag("ideaNotes").performTextInput("Add a gentle Sunday summary. רעיון לבדיקה")
  compose.activityRule.scenario.recreate()
  compose.onNodeWithTag("ideaTitle").assertTextContains("Improve the weekly review")
  compose.onAllNodesWithText("Planned").onLast().performScrollTo().performClick()
  compose.onNodeWithText("Save").performClick()
  compose.waitUntil(10000){runBlocking{app.db.dao().allDevelopmentIdeas().size==1}}
  compose.onNodeWithText("Improve the weekly review").performClick()
  compose.onNodeWithTag("ideaTitle").performTextReplacement("Weekly review reminder")
  compose.onNodeWithText("Save").performClick()
  compose.waitUntil(10000){runBlocking{app.db.dao().allDevelopmentIdeas().single().title=="Weekly review reminder"}}
  compose.onAllNodesWithText("Done").onFirst().performClick()
  compose.onNodeWithText("Weekly review reminder").assertDoesNotExist()
  compose.onAllNodesWithText("All").onFirst().performClick()
  compose.activityRule.scenario.recreate();waitText("Weekly review reminder")
  compose.onRoot().captureToImage().asAndroidBitmap().let {bitmap->File(app.getExternalFilesDir(null),"development-en.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}
  compose.onNodeWithText("Weekly review reminder").performClick()
  compose.onNodeWithTag("ideaEditorList").performScrollToNode(hasText("Delete"));compose.onNodeWithText("Delete").performClick()
  compose.onNodeWithText("Cancel").performClick()
  assertEquals(1,runBlocking{app.db.dao().allDevelopmentIdeas().size})
  compose.onNodeWithTag("ideaEditorList").performScrollToNode(hasText("Delete"));compose.onNodeWithText("Delete").performClick()
  compose.onAllNodesWithText("Delete").onLast().performClick()
  compose.waitUntil(10000){runBlocking{app.db.dao().allDevelopmentIdeas().isEmpty()}}
 }
 @Test fun backupValidationLegacyAndShare()=runBlocking {
  val idea=DevelopmentIdea(title="Widget",notes="One action on the home screen",status="PLANNED")
  app.repo.saveDevelopmentIdea(idea)
  val backup=Backup.export(app.repo)
  app.db.dao().clearDevelopmentIdeas();Backup.restore(app.repo,backup)
  assertEquals("Widget",app.db.dao().allDevelopmentIdeas().single().title)
  val invalid=JSONObject(backup).apply{getJSONArray("developmentIdeas").getJSONObject(0).put("status","INVALID")}
  try {Backup.restore(app.repo,invalid.toString());fail("Invalid status accepted")}catch(_:IllegalArgumentException){}
  assertEquals(1,app.db.dao().allDevelopmentIdeas().size)
  val legacy=JSONObject(backup).apply{put("schema",1);remove("developmentIdeas")}
  Backup.restore(app.repo,legacy.toString())
  assertEquals("Legacy backup preserves newer suggestions",1,app.db.dao().allDevelopmentIdeas().size)
  assertTrue(developmentIdeasText(app.db.dao().allDevelopmentIdeas()).contains("[PLANNED] [FEATURE] [priority 1] Widget"))
  try{app.repo.saveDevelopmentIdea(DevelopmentIdea(title=" "));fail()}catch(_:IllegalArgumentException){}
 }
 @Test fun migrationTwoToThreePreservesExistingRows(){
  migrations.createDatabase("development-migration",2).apply{
   execSQL("INSERT INTO events (id,taskId,projectId,kind,note,created) VALUES ('history',NULL,NULL,'advanced','keep',1)")
   close()
  }
  migrations.runMigrationsAndValidate("development-migration",3,true,MomentumDb.MIGRATION_2_3).apply{
   query("SELECT note FROM events WHERE id='history'").use{assertTrue(it.moveToFirst());assertEquals("keep",it.getString(0))}
   query("SELECT COUNT(*) FROM development_ideas").use{assertTrue(it.moveToFirst());assertEquals(0,it.getInt(0))}
   close()
  }
 }
 @Test fun hebrewScreenCapture(){
  runBlocking{app.repo.saveDevelopmentIdea(DevelopmentIdea(title="סיכום שבועי עדין",notes="מקום לחזור לרעיונות ולבחור מה לפתח בהמשך",status="PLANNED"))}
  open();runBlocking{app.prefs.bool("english",false)}
  waitText("הצעות לשיפור מומנטום");compose.waitForIdle()
  compose.onRoot().captureToImage().asAndroidBitmap().let{bitmap->File(app.getExternalFilesDir(null),"development-he.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}
 }
}
