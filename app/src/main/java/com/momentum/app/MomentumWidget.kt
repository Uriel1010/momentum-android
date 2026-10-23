package com.momentum.app
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.Locale

class MomentumWidget:AppWidgetProvider() {
 override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray){refresh(context)}
 override fun onReceive(context:Context,intent:Intent){
  if(intent.action=="com.momentum.REFRESH_WIDGET")refresh(context) else super.onReceive(context,intent)
 }
 private fun refresh(context:Context){
  val pending=goAsync()
  (context.applicationContext as MomentumApp).scope.launch {try{updateAll(context)}finally{pending.finish()}}
 }
 companion object {
  fun captureIntent(context:Context)=Intent(context,MainActivity::class.java).setAction("com.momentum.CAPTURE").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
  fun focusIntent(context:Context,id:String?)=Intent(context,MainActivity::class.java).setAction("com.momentum.WIDGET_FOCUS").putExtra("focusId",id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
  suspend fun views(context:Context):RemoteViews {
   val app=context.applicationContext as MomentumApp
   val prefs=app.prefs.flow.first()
   val local=context.createConfigurationContext(Configuration(context.resources.configuration).apply{setLocale(Locale.forLanguageTag(if(prefs.english)"en" else "he"))})
   val dao=app.db.dao()
   val tasks=dao.allTasks();val projects=dao.allProjects();val events=dao.allEvents()
   val next=if(prefs.suggestions)NowPolicy.candidates(tasks,projects,System.currentTimeMillis(),0,false,"",events).firstOrNull()?.task else null
   val flags=PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
   return RemoteViews(context.packageName,R.layout.momentum_widget).apply {
    setTextViewText(R.id.widget_title,next?.title?:local.getString(R.string.widget_empty))
    setTextViewText(R.id.widget_step,next?.nextAction?.ifBlank{local.getString(R.string.widget_open)}?:local.getString(R.string.local_only))
    setTextViewText(R.id.widget_capture,local.getString(R.string.widget_capture))
    setTextViewText(R.id.widget_refresh,local.getString(R.string.widget_refresh))
    setContentDescription(R.id.widget_action,local.getString(R.string.widget_open)+": "+next?.title.orEmpty())
    setInt(R.id.widget_root,"setLayoutDirection",if(prefs.english)android.view.View.LAYOUT_DIRECTION_LTR else android.view.View.LAYOUT_DIRECTION_RTL)
    setOnClickPendingIntent(R.id.widget_action,PendingIntent.getActivity(context,201,focusIntent(context,next?.id),flags))
    setOnClickPendingIntent(R.id.widget_capture,PendingIntent.getActivity(context,202,captureIntent(context),flags))
    setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(context,203,Intent(context,MomentumWidget::class.java).setAction("com.momentum.REFRESH_WIDGET"),flags))
   }
  }
  suspend fun updateAll(context:Context){
   val manager=AppWidgetManager.getInstance(context)
   val ids=manager.getAppWidgetIds(ComponentName(context,MomentumWidget::class.java))
   if(ids.isNotEmpty())manager.updateAppWidget(ids,views(context))
  }
 }
}
