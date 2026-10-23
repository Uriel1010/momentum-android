package com.momentum.app
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Base64
import android.widget.FrameLayout
import android.widget.TextView
import android.view.View
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
import java.io.ByteArrayOutputStream

class Momentum32DeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 @get:Rule val migrations=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),MomentumDb::class.java,emptyList(),FrameworkSQLiteOpenHelperFactory())
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa"))}
 @Before fun setup()=runBlocking {
  app.db.dao().clearTasks();app.db.dao().clearProjects();app.db.dao().clearEvents();app.db.dao().clearDevelopmentIdeas()
  app.prefs.bool("english",true);app.prefs.bool("onboarded",true);app.prefs.bool("suggestions",true)
 }
 private fun waitText(text:String)=compose.waitUntil(10000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}
 private fun screenshot(name:String){compose.waitForIdle();compose.onRoot().captureToImage().asAndroidBitmap().let{b->File(app.getExternalFilesDir(null),name).outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}}}
 private fun fixtureImage():String {val b=Bitmap.createBitmap(160,100,Bitmap.Config.ARGB_8888);b.eraseColor(android.graphics.Color.rgb(34,80,55));val out=ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,75,out);b.recycle();return Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP)}
 @Test fun screenshotMetadataBackupAndLegacy()=runBlocking {
  val image=fixtureImage();val idea=DevelopmentIdea(title="Widget contrast",notes="Make the next action clearer",category="BUG",priority=2,screenshot=image)
  app.repo.saveDevelopmentIdea(idea);val backup=Backup.export(app.repo)
  app.db.dao().clearDevelopmentIdeas();Backup.restore(app.repo,backup)
  val loaded=app.db.dao().allDevelopmentIdeas().single()
  assertEquals(image,loaded.screenshot);assertEquals("BUG",loaded.category);assertEquals(2,loaded.priority)
  val broken=JSONObject(backup);broken.getJSONArray("developmentIdeas").getJSONObject(0).put("screenshot","broken")
  try{Backup.restore(app.repo,broken.toString());fail()}catch(_:IllegalArgumentException){}
  assertEquals(image,app.db.dao().allDevelopmentIdeas().single().screenshot)
  val legacy=JSONObject(backup).apply{put("schema",2);getJSONArray("developmentIdeas").getJSONObject(0).apply{remove("category");remove("priority");remove("screenshot")}}
  Backup.restore(app.repo,legacy.toString());val old=app.db.dao().allDevelopmentIdeas().single()
  assertEquals("FEATURE",old.category);assertEquals(1,old.priority);assertEquals("",old.screenshot)
  assertTrue(githubDraftUrl(loaded).getQueryParameter("body")!!.contains("Make the next action clearer"))
  assertFalse(githubDraftUrl(loaded).toString().contains(image))
 }
 @Test fun migrationThreeToFourPreservesIdeas(){
  migrations.createDatabase("v32-migration",3).apply{
   execSQL("INSERT INTO development_ideas (id,title,notes,status,created,updated) VALUES ('idea','Keep me','Notes','PLANNED',1,2)");close()
  }
  migrations.runMigrationsAndValidate("v32-migration",4,true,MomentumDb.MIGRATION_3_4).apply{
   query("SELECT title,category,priority,screenshot FROM development_ideas WHERE id='idea'").use{assertTrue(it.moveToFirst());assertEquals("Keep me",it.getString(0));assertEquals("FEATURE",it.getString(1));assertEquals(1,it.getInt(2));assertEquals("",it.getString(3))};close()
  }
 }
 @Test fun returnCardShowsProgressAndContinues(){
  runBlocking{val task=Task(title="Write the introduction",nextAction="Open the outline",status="IN_PROGRESS");app.repo.save(task);app.repo.advance(task,"Three key points are ready")}
  waitText("Where you left off");compose.onNodeWithText("Last progress: Three key points are ready").assertExists()
  screenshot("v32-resume.png")
  compose.onNodeWithText("Continue this step").performScrollTo().performClick();waitText("Open the outline")
 }
 @Test fun suggestionEditorCategoriesAndScreenshot(){
  runBlocking{app.repo.saveDevelopmentIdea(DevelopmentIdea(title="Clearer widget action",notes="A synthetic example for the development backlog",category="BUG",priority=2,screenshot=fixtureImage()))}
  waitText("Capture a thought");compose.onNodeWithContentDescription("Settings").performClick();compose.onNodeWithText("Suggestions for Momentum").performClick();waitText("Clearer widget action")
  compose.onNodeWithContentDescription("Attached screenshot").assertExists();screenshot("v32-suggestions.png")
  compose.onNodeWithText("Clearer widget action").performClick()
  compose.onAllNodesWithText("Feature").onLast().performScrollTo().performClick()
  compose.onNodeWithText("Low").performScrollTo().performClick()
  compose.onNodeWithText("Save").performClick()
  compose.waitUntil(10000){runBlocking{app.db.dao().allDevelopmentIdeas().single().let{it.category=="FEATURE" && it.priority==0}}}
 }
 @Test fun widgetRendersAndActionsWork(){
  val task=Task(title="One small next step",nextAction="Open the notebook")
  runBlocking{app.repo.save(task)}
  val views=runBlocking{MomentumWidget.views(app)}
  compose.runOnUiThread {
   val view=views.apply(compose.activity,FrameLayout(compose.activity))
   assertEquals("One small next step",view.findViewById<TextView>(R.id.widget_title).text.toString())
   val width=(320*app.resources.displayMetrics.density).toInt();val height=(250*app.resources.displayMetrics.density).toInt()
   view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height)
   val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap));File(app.getExternalFilesDir(null),"v32-widget.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
   assertTrue(view.findViewById<View>(R.id.widget_capture).height>=48*app.resources.displayMetrics.density)
   view.findViewById<View>(R.id.widget_capture).performClick()
  }
  compose.waitUntil(10000){compose.onAllNodesWithTag("quickCapture").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithTag("quickCapture").performTextInput("From the widget");compose.onNodeWithTag("quickCapture").performImeAction()
  compose.waitUntil(10000){runBlocking{app.db.dao().allTasks().any{it.title=="From the widget"}}}
 }
 @Test fun widgetExcludesWaitingAndRespectsRecommendationsSetting(){
  runBlocking{app.repo.save(Task(title="Blocked",status="WAITING"))}
  fun title():String {val views=runBlocking{MomentumWidget.views(app)};var text="";compose.runOnUiThread{text=views.apply(compose.activity,FrameLayout(compose.activity)).findViewById<TextView>(R.id.widget_title).text.toString()};return text}
  assertEquals("A little room for your next step",title())
  runBlocking{app.repo.save(Task(title="Ready"));app.prefs.bool("suggestions",false)}
  assertEquals("A little room for your next step",title())
 }
}
