package com.momentum.app
import android.app.PendingIntent
import android.appwidget.*
import android.content.*
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.net.toUri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

open class TodoListWidget:AppWidgetProvider(){
 override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray)=refresh(context)
 override fun onAppWidgetOptionsChanged(context:Context,manager:AppWidgetManager,id:Int,options:Bundle)=refresh(context)
 private fun refresh(context:Context){val pending=goAsync();(context.applicationContext as MomentumApp).scope.launch{try{updateAll(context)}finally{pending.finish()}}}
 companion object {
  private val mutex=Mutex()
  data class Snapshot(val local:Context,val tasks:List<Task>,val projects:List<Project>,val english:Boolean,val now:Long)
  suspend fun snapshot(context:Context):Snapshot {
   val app=context.applicationContext as MomentumApp;val p=app.prefs.flow.first();val dao=app.db.dao();val now=System.currentTimeMillis();val projects=dao.allProjects()
   val local=context.createConfigurationContext(Configuration(context.resources.configuration).apply{setLocale(Locale.forLanguageTag(if(p.english)"en" else "he"))})
   return Snapshot(local,TodoWidgetPolicy.ordered(dao.allTasks(),projects,now),projects,p.english,now)
  }
  fun stableId(id:String):Long=java.security.MessageDigest.getInstance("SHA-256").digest(id.toByteArray()).take(8).fold(0L){a,b->(a shl 8) or (b.toLong() and 255)}
  fun fillIn(id:String)=Intent().putExtra("focusId","task:"+id)
  fun row(snapshot:Snapshot,position:Int):RemoteViews {
   val context=snapshot.local;val task=snapshot.tasks.getOrNull(position)?.takeIf{position<TodoWidgetPolicy.LIMIT}
   val title=task?.title?.take(200)?:context.getString(R.string.todo_widget_all,snapshot.tasks.size)
   val detail=when {
    task==null->context.getString(R.string.todo_widget_more)
    task.status=="WAITING"->context.getString(R.string.waiting)
    snapshot.projects.any{it.id==task.projectId && it.paused}->context.getString(R.string.ux_paused_c7dfb)
    task.available>snapshot.now->context.getString(R.string.todo_widget_scheduled)
    task.status=="IN_PROGRESS"->context.getString(R.string.todo_widget_in_progress)
    else->task.nextAction.take(160)
   }
   return RemoteViews(context.packageName,R.layout.todo_widget_row).apply {
    setTextViewText(R.id.todo_title,title);setTextViewText(R.id.todo_detail,detail)
    setViewVisibility(R.id.todo_detail,if(detail.isEmpty())android.view.View.GONE else android.view.View.VISIBLE)
    setTextColor(R.id.todo_marker,if(task?.status=="IN_PROGRESS")0xFFD7FF3F.toInt() else 0xFFC7C5C2.toInt())
    setInt(R.id.todo_row,"setLayoutDirection",if(snapshot.english)android.view.View.LAYOUT_DIRECTION_LTR else android.view.View.LAYOUT_DIRECTION_RTL)
    setContentDescription(R.id.todo_row,if(detail.isEmpty())title else "$title. $detail")
    setOnClickFillInIntent(R.id.todo_row,task?.let{fillIn(it.id)}?:Intent().putExtra("focusId","list:tasks"))
   }
  }
  fun rowCount(snapshot:Snapshot)=minOf(snapshot.tasks.size,TodoWidgetPolicy.LIMIT)+(if(snapshot.tasks.size>TodoWidgetPolicy.LIMIT)1 else 0)
  fun views(context:Context,id:Int,snapshot:Snapshot):RemoteViews {
   val flags=PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
   val template=Intent(context,MainActivity::class.java).setAction("com.momentum.TODO_ROW").setData("momentum://todo/widget/$id".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
   return RemoteViews(context.packageName,R.layout.todo_list_widget).apply{
    setInt(R.id.todo_widget_root,"setLayoutDirection",if(snapshot.english)android.view.View.LAYOUT_DIRECTION_LTR else android.view.View.LAYOUT_DIRECTION_RTL)
    setTextViewText(R.id.todo_widget_heading,snapshot.local.getString(R.string.todo_widget_heading,snapshot.tasks.size))
    setTextViewText(R.id.todo_widget_empty,snapshot.local.getString(R.string.todo_widget_empty))
    setContentDescription(R.id.todo_widget_heading,snapshot.local.getString(R.string.todo_widget_all,snapshot.tasks.size))
    setContentDescription(R.id.todo_widget_capture,snapshot.local.getString(R.string.capture))
    setOnClickPendingIntent(R.id.todo_widget_heading,PendingIntent.getActivity(context,501,MomentumWidget.focusIntent(context,"list:tasks").setData("momentum://todo/all".toUri()),flags))
    setOnClickPendingIntent(R.id.todo_widget_capture,PendingIntent.getActivity(context,202,MomentumWidget.captureIntent(context),flags))
    // Collection row extras require a mutable template; the destination component and data are fixed.
    setPendingIntentTemplate(R.id.todo_widget_list,PendingIntent.getActivity(context,5000+id,template,PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT>=31)PendingIntent.FLAG_MUTABLE else 0))
    setEmptyView(R.id.todo_widget_list,R.id.todo_widget_empty)
    if(Build.VERSION.SDK_INT>=31){
     val items=RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(1)
     repeat(rowCount(snapshot)){index->items.addItem(if(index<TodoWidgetPolicy.LIMIT)stableId(snapshot.tasks[index].id) else Long.MIN_VALUE,row(snapshot,index))}
     setRemoteAdapter(R.id.todo_widget_list,items.build())
    }else {
     @Suppress("DEPRECATION")
     setRemoteAdapter(R.id.todo_widget_list,Intent(context,TodoListService::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).setData("momentum://todo/adapter/$id".toUri()))
    }
   }
  }
  suspend fun updateAll(context:Context)=mutex.withLock {
   val manager=AppWidgetManager.getInstance(context)
   val ids=listOf(TodoListWidget::class.java,TallTodoListWidget::class.java).flatMap{manager.getAppWidgetIds(ComponentName(context,it)).toList()}
   if(ids.isNotEmpty()){
    val snapshot=snapshot(context);ids.forEach{manager.updateAppWidget(it,views(context,it,snapshot))}
    if(Build.VERSION.SDK_INT<31){@Suppress("DEPRECATION") manager.notifyAppWidgetViewDataChanged(ids.toIntArray(),R.id.todo_widget_list)}
   }
  }
 }
}
class TallTodoListWidget:TodoListWidget()
class TodoListService:RemoteViewsService(){override fun onGetViewFactory(intent:Intent)=TodoListFactory(applicationContext)}
/** Compatibility collection provider for Android 10 and 11. */
class TodoListFactory(private val context:Context):RemoteViewsService.RemoteViewsFactory {
 private var snapshot:TodoListWidget.Companion.Snapshot?=null
 override fun onCreate(){}
 override fun onDataSetChanged(){snapshot=runBlocking(Dispatchers.IO){TodoListWidget.snapshot(context)}}
 override fun onDestroy(){snapshot=null}
 override fun getCount()=snapshot?.let{TodoListWidget.rowCount(it)}?:0
 override fun getViewAt(position:Int)=snapshot?.takeIf{position in 0 until count}?.let{TodoListWidget.row(it,position)}
 override fun getLoadingView():RemoteViews?=null
 override fun getViewTypeCount()=1
 override fun getItemId(position:Int)=snapshot?.tasks?.getOrNull(position)?.takeIf{position<TodoWidgetPolicy.LIMIT}?.let{TodoListWidget.stableId(it.id)}?:Long.MIN_VALUE
 override fun hasStableIds()=true
}
