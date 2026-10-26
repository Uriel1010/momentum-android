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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

/** One explicit, silent Today list. Selection does not start tasks or expire at midnight. */
object FocusNotification {
 const val CHANNEL="focus-session"
 const val TAG="momentum-focus"
 const val ID=7001
 private val mutex=Mutex()
 private var pendingPost:Job?=null
 private var lastSignature:String?=null
 fun store(context:Context)=context.getSharedPreferences("focus_notification",Context.MODE_PRIVATE)
 fun pinnedIds(context:Context):List<String> {
  val prefs=store(context)
  if(!prefs.contains("tasks"))return listOfNotNull(prefs.getString("task",null))
  return try{val array=org.json.JSONArray(prefs.getString("tasks","[]"));(0 until array.length()).map{array.getString(it)}.distinct()}catch(_:org.json.JSONException){emptyList()}
 }
 // Compatibility for older callers; Today membership must use pinnedIds.
 fun pinnedId(context:Context)=pinnedIds(context).firstOrNull()
 private fun write(context:Context,ids:List<String>){store(context).edit().remove("task").putString("tasks",org.json.JSONArray(ids.distinct()).toString()).putString("token",uuid()).commit()}
 fun canPost(context:Context):Boolean {
  val manager=context.getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel(CHANNEL,context.getString(R.string.focus_notification_channel),NotificationManager.IMPORTANCE_LOW).apply{setShowBadge(false);enableVibration(false);setSound(null,null)})
  return NotificationManagerCompat.from(context).areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL).importance!=NotificationManager.IMPORTANCE_NONE && (Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
 }
 suspend fun pin(context:Context,id:String):Boolean=mutex.withLock {
  if(!canPost(context))return@withLock false
  val app=context.applicationContext as MomentumApp
  val task=app.db.dao().task(id)?:return@withLock false
  if(task.status in setOf("DONE","ARCHIVED"))return@withLock false
  val ids=pinnedIds(context)
  if(id !in ids || !store(context).contains("tasks"))write(context,(ids+id).distinct())
  refreshLocked(context)
 }
 suspend fun remove(context:Context,taskId:String?=null,token:String?=null)=mutex.withLock {
  if(token!=null && store(context).getString("token",null)!=token)return@withLock
  if(taskId==null)clear(context) else {
   val ids=pinnedIds(context).filterNot{it==taskId}
   if(ids.isEmpty())clear(context) else {write(context,ids);refreshLocked(context)}
  }
 }
 private fun clear(context:Context){pendingPost?.cancel();pendingPost=null;lastSignature=null;store(context).edit().clear().commit();NotificationManagerCompat.from(context).cancel(TAG,ID)}
 suspend fun refresh(context:Context)=mutex.withLock{refreshLocked(context)}
 private suspend fun refreshLocked(context:Context):Boolean {
  val ids=pinnedIds(context)
  if(ids.isEmpty()){pendingPost?.cancel();pendingPost=null;lastSignature=null;NotificationManagerCompat.from(context).cancel(TAG,ID);return false}
  val app=context.applicationContext as MomentumApp
  val byId=app.db.dao().allTasks().associateBy{it.id}
  val tasks=ids.mapNotNull{byId[it]}.filter{it.status !in setOf("DONE","ARCHIVED")}
  if(tasks.isEmpty()){clear(context);return false}
  if(tasks.map{it.id}!=ids || !store(context).contains("tasks"))write(context,tasks.map{it.id})
  if(!canPost(context)){pendingPost?.cancel();pendingPost=null;lastSignature=null;NotificationManagerCompat.from(context).cancel(TAG,ID);return false}
  val english=app.prefs.flow.first().english
  val local=context.createConfigurationContext(android.content.res.Configuration(context.resources.configuration).apply{setLocale(Locale.forLanguageTag(if(english)"en" else "he"))})
  val token=store(context).getString("token","")!!
  val flags=PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
  val intent=Intent(context,MainActivity::class.java).putExtra("focusId","list:today").setData("momentum://today".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
  val open=PendingIntent.getActivity(context,701,intent,flags)
  val stop=PendingIntent.getBroadcast(context,702,Intent(context,FocusNotificationReceiver::class.java).setAction("com.momentum.UNPIN_FOCUS").setData("momentum://unpin/$token".toUri()).putExtra("token",token),flags)
  val publicVersion=NotificationCompat.Builder(local,CHANNEL).setSmallIcon(R.drawable.ic_notification_momentum).setContentTitle("Momentum").setContentText(local.getString(R.string.focus_notification_public)).build()
  val projects=app.db.dao().allProjects().associateBy{it.id}
  val lines=tasks.map{t->listOfNotNull(t.title,projects[t.projectId]?.displayName()).joinToString(" · ")}
  val title=local.getString(R.string.today_count,tasks.size)
  val style=NotificationCompat.InboxStyle().setBigContentTitle(title)
  lines.take(6).forEach{style.addLine(it)}
  if(tasks.size>6)style.setSummaryText(local.getString(R.string.today_more,tasks.size-6))
  val notification=NotificationCompat.Builder(local,CHANNEL).setSmallIcon(R.drawable.ic_notification_momentum)
   .setContentTitle(title).setContentText(lines.take(2).joinToString(" • ")).setStyle(style)
   .setSubText(local.getString(R.string.today)).setContentIntent(open).setNumber(tasks.size)
   .setOngoing(true).setAutoCancel(false).setSilent(true).setOnlyAlertOnce(true).setShowWhen(false)
   .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(publicVersion)
   .addAction(0,local.getString(R.string.today_open),open)
   .addAction(0,local.getString(R.string.today_clear),stop).build()
  // Coalesce rapid selection/Room updates so Android does not drop the final list.
  val signature=token+"|"+english+"|"+lines.joinToString("\n")
  if(signature==lastSignature && context.getSystemService(NotificationManager::class.java).activeNotifications.any{it.id==ID && it.tag==TAG}){pendingPost?.cancel();pendingPost=null;return true}
  pendingPost?.cancel()
  pendingPost=app.scope.launch{
   delay(400)
   mutex.withLock{
    if(store(app).getString("token",null)!=token || !canPost(app))return@withLock
    try{NotificationManagerCompat.from(app).notify(TAG,ID,notification);lastSignature=signature}catch(_:SecurityException){lastSignature=null}
   }
  }
  return true
 }
}
class FocusNotificationReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  if(intent.action!="com.momentum.UNPIN_FOCUS")return
  val token=intent.getStringExtra("token")?:return
  val pending=goAsync();(context.applicationContext as MomentumApp).scope.launch{try{FocusNotification.remove(context,token=token)}finally{pending.finish()}}
 }
}
