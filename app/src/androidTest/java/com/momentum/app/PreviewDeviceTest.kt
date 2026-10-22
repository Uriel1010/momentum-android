package com.momentum.app
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import org.junit.*
import org.junit.runner.RunWith
import java.io.File
@RunWith(AndroidJUnit4::class)
class PreviewDeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa")){"Fixture tests require -PmomentumQa=true"}}
 private var original=""
 private val english get()=InstrumentationRegistry.getArguments().getString("english")=="true"
 private fun localized(he:String,en:String)=if(english)en else he
 @Before fun setup()=runBlocking {
  original=Backup.export(app.repo)
  app.db.dao().clearTasks();app.db.dao().clearProjects();app.db.dao().clearEvents()
  app.prefs.bool("english",english);app.prefs.bool("onboarded",true)
  val project=Project(title=localized("מרחב לכתיבה","A space for writing"),description=localized("לפרסם סיפור קצר שארצה לשתף.","Publish a short story worth sharing."),summary=localized("בחרתי נושא ופתחתי מסמך. בפעם הבאה אמשיך מהפסקה הראשונה.","The topic is chosen and the document is open. Next time, begin the opening paragraph."),nextStep=localized("לכתוב שלוש שורות","Write three lines"))
  app.db.dao().saveProject(project)
  app.repo.save(Task(title=localized("לכתוב את הפסקה הראשונה","Write the opening paragraph"),estimate=5,priority=2,projectId=project.id,nextAction=localized("לפתוח את המסמך ולכתוב שלוש שורות","Open the document and write three lines"),contexts=localized("בבית","At home")))
  app.repo.save(Task(title=localized("לקבוע תור לבדיקה","Book a checkup"),estimate=5,priority=1))
  app.db.dao().event(Event(projectId=project.id,kind="advanced",note=localized("בחרתי נושא ופתחתי מסמך","Chose a topic and opened a document")))
  val done=Task(title=localized("לאסוף שלושה רעיונות לסיפור","Collect three story ideas"),projectId=project.id,estimate=5)
  app.repo.save(done);app.repo.complete(done)
  app.repo.save(Task(title=localized("לסדר את המסמכים","Organize documents"),estimate=30,created=System.currentTimeMillis()-9*86_400_000L))
  app.repo.save(Task(title=localized("לקבל משוב על הטיוטה","Get feedback on the draft"),status="WAITING",projectId=project.id,available=System.currentTimeMillis()+86_400_000))

 }
 @After fun cleanup()=runBlocking {if(original.isNotEmpty())Backup.restore(app.repo,original)}
 private fun shot(name:String) {
  compose.waitForIdle()
  android.os.SystemClock.sleep(600)
  var dialogWindow:android.view.Window?=null
  compose.runOnUiThread {
   fun findWindow(view:android.view.View):android.view.Window? {
    if(view is androidx.compose.ui.window.DialogWindowProvider)return view.window
    if(view is android.view.ViewGroup)for(i in 0 until view.childCount)findWindow(view.getChildAt(i))?.let{return it}
    return null
   }
   dialogWindow=android.view.inspector.WindowInspector.getGlobalWindowViews().filter{it.isShown && it.hasWindowFocus()}.asReversed().firstNotNullOfOrNull{findWindow(it)}
  }
  val bitmap=if(dialogWindow!=null && android.os.Build.VERSION.SDK_INT>=34)InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot(dialogWindow!!)?:error("Window screenshot unavailable") else compose.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap()
  InstrumentationRegistry.getInstrumentation().sendStatus(2,android.os.Bundle().apply{putString("capture_window",name+if(dialogWindow!=null)":dialog" else ":activity")})
  File(app.filesDir,"preview-"+(InstrumentationRegistry.getArguments().getString("variant")?:"normal")+"-$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
 }
 @Test fun captureMainScreens() {
  compose.waitUntil(5000){compose.onAllNodesWithText(localized("לכתוב את הפסקה הראשונה","Write the opening paragraph")).fetchSemanticsNodes().isNotEmpty()}
  shot("now")
  if(InstrumentationRegistry.getArguments().getString("captureOnly")=="true"){
   compose.onNodeWithText(localized("לתפוס מחשבה","Capture a thought")).performClick()
   compose.waitUntil(5000){compose.onAllNodes(hasTestTag("quickCapture") and isFocused()).fetchSemanticsNodes().isNotEmpty()}
   compose.onNodeWithTag("quickCapture").performTextInput(localized("לשלוח את המסמך","Send the document"))
   shot("capture")
   compose.onNodeWithText(localized("עוד פרטים","More details")).performClick()
   shot("capture-details")
   return
  }
  if(InstrumentationRegistry.getArguments().getString("interactions")=="true") {
   compose.onNodeWithText(localized("מתחילים","Let’s start")).performScrollTo().performClick()
   compose.onNodeWithText(localized("נתקעתי","I’m stuck")).performScrollTo().performClick()
   shot("blockers")
   InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
   compose.onNodeWithText(localized("סיימתי","I’m done")).performScrollTo().performClick()
   compose.waitUntil(5000){runBlocking{app.db.dao().allTasks().any{it.title==localized("לכתוב את הפסקה הראשונה","Write the opening paragraph") && it.status=="DONE"}}}
   shot("completion")
   compose.onNodeWithText(localized("ביטול פעולה","Undo")).performClick()
   compose.onNodeWithText(localized("פרויקטים","Projects")).performClick()
   compose.onNodeWithText(localized("מרחב לכתיבה","A space for writing")).performClick()
   compose.onNodeWithText(localized("לציין אבן דרך","Mark a milestone")).performScrollTo().performClick()
   compose.onNodeWithText(localized("אבן דרך אמיתית","A real milestone")).performTextInput(localized("הטיוטה הראשונה מוכנה","The first draft is ready"))
   shot("milestone")
   compose.onNodeWithText(localized("שמירה","Save")).performClick()
   shot("milestone-feedback")
   return
  }

  compose.onNodeWithText(localized("מתחילים","Let’s start")).performScrollTo().performClick()
  compose.onNodeWithText(localized("דבר אחד בכל פעם","One thing at a time")).assertExists();shot("focus")
  compose.onNodeWithContentDescription(localized("חזרה","Back")).performClick()
  compose.onNodeWithText(localized("פרויקטים","Projects")).performClick();shot("projects")
  compose.onNodeWithText(localized("מרחב לכתיבה","A space for writing")).performClick()
  compose.onNodeWithText(localized("איפה עצרתי","Where I stopped")).assertExists();shot("project")
  compose.onNodeWithContentDescription(localized("חזרה","Back")).performClick()
  compose.onNodeWithText(localized("המשימות","Tasks")).performClick();shot("tasks")
  compose.onNodeWithText(localized("התקדמות","Progress")).performClick();shot("progress")
  compose.onNodeWithContentDescription(localized("הגדרות","Settings")).performClick();shot("settings")
  runBlocking {app.prefs.bool("onboarded",false)}
  compose.waitUntil(5000){compose.onAllNodesWithText(localized("לשמור ולהתחיל","Save and start")).fetchSemanticsNodes().isNotEmpty()}
  shot("onboarding")

 }
}
