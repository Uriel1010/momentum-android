package com.momentum.app
import android.Manifest
import androidx.core.net.toUri
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

fun cancelReminder(context:Context,id:String) {
 WorkManager.getInstance(context).cancelUniqueWork("reminder-$id")
 NotificationManagerCompat.from(context).cancel(id.hashCode())
}
fun enqueue(context:Context,id:String,at:Long,stamp:Long) {
 val data=workDataOf("id" to id,"stamp" to stamp)
 WorkManager.getInstance(context).enqueueUniqueWork("reminder-$id",ExistingWorkPolicy.REPLACE,
  OneTimeWorkRequestBuilder<ReminderWorker>().setInputData(data).setInitialDelay((at-System.currentTimeMillis()).coerceAtLeast(0),TimeUnit.MILLISECONDS).build())
}
fun schedule(context:Context,t:Task) {
 cancelReminder(context,t.id)
 if(t.reminder!=null && t.status !in setOf("DONE","ARCHIVED")) enqueue(context,t.id,t.reminder,t.reminder)
}
class ReminderWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
 override suspend fun doWork():Result {
  val app=applicationContext as MomentumApp
  val id=inputData.getString("id")?:return Result.success()
  val stamp=inputData.getLong("stamp",0)
  val t=app.db.dao().task(id)?:return Result.success()
  if(t.status in setOf("DONE","ARCHIVED") || t.reminder!=stamp)return Result.success()
  val p=app.prefs.flow.first()
  val nm=NotificationManagerCompat.from(applicationContext)
  if(!p.notifications || !nm.areNotificationsEnabled() ||
   (Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(applicationContext,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED))return Result.success()
  val now=System.currentTimeMillis()
  val next=ReminderPolicy.nextAllowed(now,p.quietStart,p.quietEnd)
  if(next>now) { enqueue(applicationContext,id,next,stamp);return Result.success() }
  val deliveryId="$id:$stamp"
  var capped=false
  app.db.withTransaction {
   val dao=app.db.dao()
   val current=dao.task(id)
   if(current==null || current.status in setOf("DONE","ARCHIVED") || current.reminder!=stamp || dao.delivered(deliveryId)>0)return@withTransaction
   if(dao.deliveredSince(ReminderPolicy.dayStart(now))>=p.dailyCap) {capped=true;return@withTransaction}
   val manager=applicationContext.getSystemService(NotificationManager::class.java)
   manager.createNotificationChannel(NotificationChannel("gentle","Momentum",NotificationManager.IMPORTANCE_DEFAULT).apply { description=if(p.english)"Gentle task reminders" else "תזכורות עדינות למשימות" })
   fun action(name:String):PendingIntent {
    val intent=Intent(applicationContext,ReminderActionReceiver::class.java).setAction(name).putExtra("id",id).putExtra("stamp",stamp)
    intent.data="momentum://$name/$id/$stamp".toUri()
    return PendingIntent.getBroadcast(applicationContext,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   }
   val open=Intent(applicationContext,MainActivity::class.java).putExtra("focusId",id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
   val content=PendingIntent.getActivity(applicationContext,id.hashCode(),open,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   val notification=NotificationCompat.Builder(applicationContext,"gentle").setSmallIcon(R.drawable.ic_momentum)
    .setContentTitle(t.title).setContentText(if(p.english)"A small step, when it suits you." else "צעד קטן, כשמתאים לך.")
    .setContentIntent(content).setAutoCancel(true)
    .addAction(0,if(p.english)"Done" else "סיימתי",action("done"))
    .addAction(0,if(p.english)"Snooze 15m" else "עוד 15 דקות",action("snooze"))
    .addAction(0,if(p.english)"Start" else "מתחילים",content).build()
   try {
    nm.notify(id.hashCode(),notification)
    dao.delivery(Delivery(deliveryId,id))
    if(p.metrics)dao.event(Event(taskId=id,kind="notification_shown"))
   } catch(e:SecurityException) { android.util.Log.w("Momentum","Notification permission unavailable") }
  }
  if(capped) {
   val tomorrow=java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault()).toLocalDate().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
   enqueue(applicationContext,id,ReminderPolicy.nextAllowed(tomorrow,p.quietStart,p.quietEnd),stamp)
  }
  return Result.success()
 }
}
class ReminderActionReceiver:BroadcastReceiver() {
 override fun onReceive(context:Context,intent:Intent) {
  val pending=goAsync(); val app=context.applicationContext as MomentumApp
  app.scope.launch { try {
   val id=intent.getStringExtra("id")?:return@launch
   val t=app.db.dao().task(id)?:return@launch
   if(t.status=="DONE" || t.reminder!=intent.getLongExtra("stamp",0))return@launch
   when(intent.action) {
    "done"->app.repo.complete(t)
    "snooze"->app.repo.save(t.copy(reminder=System.currentTimeMillis()+900_000))
   }
   app.repo.metric("notification_action",id)
  } catch(e:Exception) { android.util.Log.e("Momentum","Reminder action failed",e) } finally { pending.finish() } }
 }
}
class RescheduleReceiver:BroadcastReceiver() {
 override fun onReceive(context:Context,intent:Intent) {
  if(intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED,Intent.ACTION_TIMEZONE_CHANGED,Intent.ACTION_TIME_CHANGED))return
  val pending=goAsync(); val app=context.applicationContext as MomentumApp
  app.scope.launch {try {app.repo.reschedule();FocusNotification.refresh(context)} finally {pending.finish()}}
 }
}
