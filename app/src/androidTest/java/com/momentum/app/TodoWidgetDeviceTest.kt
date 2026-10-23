package com.momentum.app
import android.appwidget.*
import android.content.ComponentName
import android.graphics.*
import android.view.View
import android.widget.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File
class TodoWidgetDeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa"))}
 @Before fun setup():Unit=runBlocking{app.db.dao().clearTasks();app.db.dao().clearProjects();app.prefs.bool("english",true);app.prefs.bool("onboarded",true)}
 private fun seed():Unit=runBlocking{listOf("Write the opening paragraph","Plan a weekend walk","Book a dentist appointment","Read ten pages","Send the project update","Water the plants","Sketch a new idea","Review this week").forEachIndexed{i,t->app.repo.save(Task(id="todo-$i",title=t,nextAction="One small step is enough",status=if(i==0)"IN_PROGRESS" else "INBOX",created=i.toLong()))}}
 private fun provider(tall:Boolean)=AppWidgetManager.getInstance(app).installedProviders.single{it.provider==ComponentName(app,if(tall)TallTodoListWidget::class.java else TodoListWidget::class.java)}
 private fun host(tall:Boolean):AppWidgetHostView {
  val snapshot=runBlocking{TodoListWidget.snapshot(app)}
  return AppWidgetHostView(compose.activity).apply{setAppWidget(842,provider(tall));setPadding(0,0,0,0);updateAppWidget(TodoListWidget.views(app,842,snapshot))}
 }
 private fun layout(view:View,w:Int,h:Int){val d=app.resources.displayMetrics.density;view.measure(View.MeasureSpec.makeMeasureSpec((w*d).toInt(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec((h*d).toInt(),View.MeasureSpec.EXACTLY));view.layout(0,0,view.measuredWidth,view.measuredHeight)}
 private fun shot(view:View,name:String){val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(b));File(app.filesDir,name).outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}}
 @Test fun presetsRenderScrollAndOpenTask(){
  seed()
  for(tall in listOf(false,true)){
   val p=provider(tall);assertEquals(if(tall)2 else 4,p.targetCellWidth);assertEquals(if(tall)4 else 2,p.targetCellHeight);assertEquals(3,p.resizeMode);assertTrue(p.previewLayout!=0);assertNotNull(p.loadPreviewImage(app,0))
   compose.runOnUiThread{val h=host(tall);layout(h,if(tall)156 else 320,if(tall)320 else 156);val list=h.findViewById<ListView>(R.id.todo_widget_list);assertNotNull(list);assertEquals(8,list.adapter.count);assertTrue(list.childCount>0);shot(h,if(tall)"v42-tall.png" else "v42-wide.png");list.setSelection(7);layout(h,if(tall)156 else 320,if(tall)320 else 156);assertEquals(7,list.lastVisiblePosition)}
  }
  compose.runOnUiThread{val h=host(false);layout(h,320,156);val list=h.findViewById<ListView>(R.id.todo_widget_list);assertTrue(list.performItemClick(list.getChildAt(0),0,list.adapter.getItemId(0)))}
  compose.waitUntil(10000){compose.onAllNodesWithText("Write the opening paragraph").fetchSemanticsNodes().isNotEmpty()}
  assertEquals("IN_PROGRESS",runBlocking{app.db.dao().task("todo-0")!!.status})
 }
 @Test fun emptyOverflowAndLegacyFactory():Unit=runBlocking{
  assertEquals(0,TodoListWidget.rowCount(TodoListWidget.snapshot(app)))
  repeat(103){app.repo.save(Task(id="many-$it",title="Example $it"))}
  val snap=TodoListWidget.snapshot(app);assertEquals(101,TodoListWidget.rowCount(snap))
  val factory=TodoListFactory(app);factory.onDataSetChanged();assertEquals(101,factory.count);assertTrue(factory.hasStableIds());assertNotNull(factory.getViewAt(100));assertNull(factory.getViewAt(101));assertEquals(Long.MIN_VALUE,factory.getItemId(100))
  app.repo.complete(app.db.dao().task("many-0")!!);assertEquals(102,TodoListWidget.snapshot(app).tasks.size);factory.onDestroy();assertEquals(0,factory.count)
 }
 @Test fun captureButtonOpensFocusedInput(){seed();compose.runOnUiThread{val h=host(false);layout(h,320,156);assertTrue(h.findViewById<View>(R.id.todo_widget_capture).performClick())};compose.waitUntil(10000){compose.onAllNodesWithTag("quickCapture").fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithTag("quickCapture").assertIsFocused()}
}
