package com.momentum.app
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.widget.RemoteViews
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.Locale

/** One launcher cell, one action. Opens the existing capture sheet and keyboard. */
class CaptureWidget:AppWidgetProvider() {
 override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray){
  val pending=goAsync();(context.applicationContext as MomentumApp).scope.launch{try{updateAll(context)}finally{pending.finish()}}
 }
 companion object {
  suspend fun views(context:Context):RemoteViews {
   val english=(context.applicationContext as MomentumApp).prefs.flow.first().english
   val local=context.createConfigurationContext(Configuration(context.resources.configuration).apply{setLocale(Locale.forLanguageTag(if(english)"en" else "he"))})
   return RemoteViews(context.packageName,R.layout.capture_widget).apply{
    setContentDescription(R.id.capture_widget_action,local.getString(R.string.capture_widget_name))
    setOnClickPendingIntent(R.id.capture_widget_action,PendingIntent.getActivity(context,204,MomentumWidget.captureIntent(context),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
   }
  }
  suspend fun updateAll(context:Context){
   val manager=AppWidgetManager.getInstance(context);val ids=manager.getAppWidgetIds(ComponentName(context,CaptureWidget::class.java))
   if(ids.isNotEmpty())manager.updateAppWidget(ids,views(context))
  }
 }
}
