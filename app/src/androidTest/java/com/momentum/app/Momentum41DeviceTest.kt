package com.momentum.app
import android.app.Notification
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File

class Momentum41DeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa"))}
 private val manager get()=app.getSystemService(NotificationManager::class.java)
 private fun active()=manager.activeNotifications.firstOrNull{it.tag==FocusNotification.TAG}?.notification
 @Before fun setup()=runBlocking{FocusNotification.remove(app);app.db.dao().clearTasks();app.db.dao().clearProjects();app.db.dao().clearEvents();app.prefs.bool("english",true);app.prefs.bool("onboarded",true);app.prefs.bool("suggestions",true)}
 @After fun cleanup()=runBlocking{FocusNotification.remove(app)}
 private fun shot(views:RemoteViews,name:String,widthDp:Int,heightDp:Int?=null):View {
  val parent=FrameLayout(compose.activity);parent.setBackgroundColor(if(name.contains("notification"))android.graphics.Color.rgb(32,33,36) else android.graphics.Color.rgb(250,247,242))
  val view=views.apply(compose.activity,parent);parent.addView(view)
  val density=app.resources.displayMetrics.density;val width=(widthDp*density).toInt()
  parent.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(((heightDp?:400)*density).toInt(),if(heightDp==null)View.MeasureSpec.AT_MOST else View.MeasureSpec.EXACTLY))
  parent.layout(0,0,width,parent.measuredHeight)
  val bitmap=Bitmap.createBitmap(width,parent.height,Bitmap.Config.ARGB_8888);parent.draw(Canvas(bitmap))
  File(app.filesDir,name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
  return view
 }
 @Test fun compactWidgetCapturesAtOneCell(){
  val provider=AppWidgetManager.getInstance(app).installedProviders.single{it.provider==ComponentName(app,CaptureWidget::class.java)}
  assertEquals(R.layout.capture_widget,provider.previewLayout);assertEquals(R.drawable.capture_widget_preview,provider.previewImage);assertNotNull(provider.loadPreviewImage(app,0));assertEquals(1,provider.targetCellWidth);assertEquals(1,provider.targetCellHeight)
  val views=runBlocking{CaptureWidget.views(app)}
  compose.runOnUiThread{
   val view=shot(views,"v41-widget.png",64,64)
   assertEquals("Catch a thought · 1×1",view.contentDescription)
   assertTrue(view.height>=48*app.resources.displayMetrics.density)
   view.performClick()
  }
  compose.waitUntil(10000){compose.onAllNodesWithTag("quickCapture").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithTag("quickCapture").assertIsFocused().performTextInput("An idea from one tap")
  compose.onNodeWithTag("quickCapture").performImeAction()
  compose.waitUntil(10000){runBlocking{app.db.dao().allTasks().any{it.title=="An idea from one tap"}}}
 }
 @Test fun focusPinIsSilentPrivateAndOpensCurrentTask(){
  val task=Task(title="Write the opening paragraph",nextAction="Open the outline and write three lines")
  runBlocking{app.repo.save(task)}
  compose.waitUntil(10000){compose.onAllNodesWithText("Let’s start").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Let’s start").performScrollTo().performClick()
  compose.onNodeWithText("Keep in notifications").performScrollTo().performClick()
  compose.waitUntil(10000){active()!=null}
  val notification=active()!!
  assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT!=0)
  assertEquals(Notification.VISIBILITY_PRIVATE,notification.visibility)
  assertFalse(notification.publicVersion.extras.toString().contains(task.title))
  assertEquals(NotificationManager.IMPORTANCE_LOW,manager.getNotificationChannel(FocusNotification.CHANNEL).importance)
  assertNull(manager.getNotificationChannel(FocusNotification.CHANNEL).sound)
  assertEquals(task.title,notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
  compose.runOnUiThread{shot(Notification.Builder.recoverBuilder(app,notification).createBigContentView(),"v41-notification.png",360)}
  notification.contentIntent.send()
  compose.waitUntil(10000){compose.onAllNodesWithText("Remove from notifications").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Remove from notifications").performScrollTo().performClick()
  compose.waitUntil(10000){active()==null && FocusNotification.pinnedId(app)==null}
  assertEquals("IN_PROGRESS",runBlocking{app.db.dao().task(task.id)!!.status})
 }
 @Test fun replacementRejectsStaleActionsAndDismissalUnpins(){
  val first=Task(title="First task");val second=Task(title="Second task")
  runBlocking{app.repo.save(first);app.repo.save(second);assertTrue(FocusNotification.pin(app,first.id))}
  val stale=active()!!.actions.last().actionIntent
  runBlocking{assertTrue(FocusNotification.pin(app,second.id))}
  stale.send();android.os.SystemClock.sleep(300)
  assertEquals(second.id,FocusNotification.pinnedId(app));assertEquals("Second task",active()!!.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
  active()!!.deleteIntent.send()
  compose.waitUntil(10000){FocusNotification.pinnedId(app)==null && active()==null}
  runBlocking{FocusNotification.refresh(app)};assertNull(active())
 }
 @Test fun changesCompletionWaitingAndDeletionClearPin(){
  val task=Task(title="Original")
  runBlocking{app.repo.save(task);assertTrue(FocusNotification.pin(app,task.id));app.repo.save(app.db.dao().task(task.id)!!.copy(title="Revised"))}
  compose.waitUntil(10000){active()?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()=="Revised"}
  runBlocking{app.repo.complete(app.db.dao().task(task.id)!!)}
  compose.waitUntil(10000){active()==null && FocusNotification.pinnedId(app)==null}
  runBlocking{app.repo.save(task);assertTrue(FocusNotification.pin(app,task.id));app.repo.block(app.db.dao().task(task.id)!!,"waiting")}
  compose.waitUntil(10000){active()==null && FocusNotification.pinnedId(app)==null}
  runBlocking{app.repo.save(task);assertTrue(FocusNotification.pin(app,task.id));app.repo.delete(task)}
  compose.waitUntil(10000){active()==null && FocusNotification.pinnedId(app)==null}
 }
 @Test fun postponedAndPausedProjectsClearPin(){
  val project=Project(title="A project");val task=Task(title="Project task",projectId=project.id)
  runBlocking{app.repo.saveProject(project);app.repo.save(task);assertTrue(FocusNotification.pin(app,task.id));app.repo.postpone(app.db.dao().task(task.id)!!,900000)}
  compose.waitUntil(10000){active()==null && FocusNotification.pinnedId(app)==null}
  runBlocking{app.repo.save(task);assertTrue(FocusNotification.pin(app,task.id));app.repo.saveProject(project.copy(paused=true))}
  compose.waitUntil(10000){active()==null && FocusNotification.pinnedId(app)==null}
 }
}
