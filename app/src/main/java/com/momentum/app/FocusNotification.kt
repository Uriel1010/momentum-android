package com.momentum.app
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

/** Explicit, silent session pin. No foreground service or recurring background polling. */
object FocusNotification {
 const val CHANNEL="focus-session"
 const val TAG="momentum-focus"
 const val ID=7001
 private val mutex=Mutex()
 fun store(context:Context)=context.getSharedPreferences("focus_notification",Context.MODE_PRIVATE)
 fun pinnedId(context:Context)=store(context).getString("task",null)
 fun canPost(context:Context):Boolean {
  val manager=context.getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel(CHANNEL,context.getString(R.string.focus_notification_channel),NotificationManager.IMPORTANCE_LOW).apply{setShowBadge(false);enableVibration(false);setSound(null,null)})
  return NotificationManagerCompat.from(context).areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL).importance!=NotificationManager.IMPORTANCE_NONE && (Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
 }
 suspend fun pin(context:Context,id:String):Boolean=mutex.withLock {
  if(!canPost(context))return@withLock false
  val app=context.applicationContext as MomentumApp
  val task=app.db.dao().task(id)?:return@withLock false
  if(task.status in setOf("DONE","ARCHIVED","WAITING") || task.available>System.currentTimeMillis())return@withLock false
  if(task.projectId!=null && app.db.dao().allProjects().any{it.id==task.projectId && it.paused})return@withLock false
  app.repo.start(task)
  store(context).edit().putString("task",id).putString("token",uuid()).commit()
  refreshLocked(context)
 }
 suspend fun remove(context:Context,taskId:String?=null,token:String?=null)=mutex.withLock {
  if((taskId==null || pinnedId(context)==taskId) && (token==null || store(context).getString("token",null)==token))clear(context)
 }
 private fun clear(context:Context){store(context).edit().clear().commit();NotificationManagerCompat.from(context).cancel(TAG,ID)}
 suspend fun refresh(context:Context)=mutex.withLock{refreshLocked(context)}
 private suspend fun refreshLocked(context:Context):Boolean {
  val id=pinnedId(context)?:return false
  val app=context.applicationContext as MomentumApp
  val task=app.db.dao().task(id)
  if(task==null || task.status!="IN_PROGRESS" || task.available>System.currentTimeMillis() || app.db.dao().allProjects().any{it.id==task.projectId && it.paused}){clear(context);return false}
  if(!canPost(context)){NotificationManagerCompat.from(context).cancel(TAG,ID);return false}
  val english=app.prefs.flow.first().english
  val local=context.createConfigurationContext(android.content.res.Configuration(context.resources.configuration).apply{setLocale(Locale.forLanguageTag(if(english)"en" else "he"))})
  val token=store(context).getString("token","")!!
  val flags=PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
  val open=PendingIntent.getActivity(context,701,MomentumWidget.focusIntent(context,id).setData("momentum://pinned/$id".toUri()),flags)
  val stop=PendingIntent.getBroadcast(context,702,Intent(context,FocusNotificationReceiver::class.java).setAction("com.momentum.UNPIN_FOCUS").setData("momentum://unpin/$token".toUri()).putExtra("token",token),flags)
  val publicVersion=NotificationCompat.Builder(local,CHANNEL).setSmallIcon(R.drawable.ic_notification_momentum).setContentTitle("Momentum").setContentText(local.getString(R.string.focus_notification_public)).build()
  val notification=NotificationCompat.Builder(local,CHANNEL).setSmallIcon(R.drawable.ic_notification_momentum)
   .setContentTitle(task.title).setContentText(task.nextAction.ifBlank{local.getString(R.string.focus_notification_public)})
   .setStyle(NotificationCompat.BigTextStyle().bigText(task.nextAction.ifBlank{local.getString(R.string.focus_notification_public)}))
   .setSubText(local.getString(R.string.focus_notification_channel)).setContentIntent(open)
   .setOngoing(true).setAutoCancel(false).setSilent(true).setOnlyAlertOnce(true).setShowWhen(false)
   .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(publicVersion)
   .addAction(0,local.getString(R.string.focus_notification_return),open)
   .addAction(0,local.getString(R.string.focus_notification_remove),stop).setDeleteIntent(stop).build()
  return try{NotificationManagerCompat.from(context).notify(TAG,ID,notification);true}catch(_:SecurityException){false}
 }
}
class FocusNotificationReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  if(intent.action!="com.momentum.UNPIN_FOCUS")return
  val token=intent.getStringExtra("token")?:return
  val pending=goAsync();(context.applicationContext as MomentumApp).scope.launch{try{FocusNotification.remove(context,token=token)}finally{pending.finish()}}
 }
}
