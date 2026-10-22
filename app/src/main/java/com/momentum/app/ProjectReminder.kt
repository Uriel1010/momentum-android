package com.momentum.app
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.*
import androidx.core.content.ContextCompat
import androidx.work.*
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
fun scheduleProject(context:Context,p:Project) {
 val manager=WorkManager.getInstance(context)
 manager.cancelUniqueWork("project-review-"+p.id)
 NotificationManagerCompat.from(context).cancel(("project:"+p.id).hashCode())
 if(p.paused || p.review==null)return
 manager.enqueueUniqueWork("project-review-"+p.id,ExistingWorkPolicy.REPLACE,
  OneTimeWorkRequestBuilder<ProjectReviewWorker>().setInputData(workDataOf("id" to p.id,"stamp" to p.review))
   .setInitialDelay((p.review-System.currentTimeMillis()).coerceAtLeast(0),TimeUnit.MILLISECONDS).build())
}
class ProjectReviewWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
 override suspend fun doWork():Result {
  val app=applicationContext as MomentumApp
  return app.db.withTransaction {
  val id=inputData.getString("id")?:return@withTransaction Result.success()
  val p=app.db.dao().allProjects().find{it.id==id}?:return@withTransaction Result.success()
  val stamp=inputData.getLong("stamp",0)
  if(p.paused || p.review!=stamp)return@withTransaction Result.success()
  val prefs=app.prefs.flow.first()
  val nm=NotificationManagerCompat.from(app)
  if(!prefs.notifications || !nm.areNotificationsEnabled() || (Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(app,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED))return@withTransaction Result.success()
  val now=System.currentTimeMillis()
  val next=ReminderPolicy.nextAllowed(now,prefs.quietStart,prefs.quietEnd)
  if(next>now || app.db.dao().deliveredSince(ReminderPolicy.dayStart(now))>=prefs.dailyCap) {
   val at=if(next>now)next else ReminderPolicy.nextAllowed(java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault()).toLocalDate().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),prefs.quietStart,prefs.quietEnd)
   WorkManager.getInstance(app).enqueueUniqueWork("project-review-"+id,ExistingWorkPolicy.REPLACE,OneTimeWorkRequestBuilder<ProjectReviewWorker>().setInputData(inputData).setInitialDelay((at-now).coerceAtLeast(1),TimeUnit.MILLISECONDS).build())
   return@withTransaction Result.success()
  }
  val delivery="project:$id:$stamp"
  if(app.db.dao().delivered(delivery)>0)return@withTransaction Result.success()
  app.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("gentle","Momentum",NotificationManager.IMPORTANCE_DEFAULT))
  val intent=Intent(app,MainActivity::class.java).putExtra("projectId",id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
  val open=PendingIntent.getActivity(app,("project:"+id).hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  try {
   nm.notify(("project:"+id).hashCode(),NotificationCompat.Builder(app,"gentle").setSmallIcon(R.drawable.ic_momentum).setContentTitle(p.title).setContentText(if(prefs.english)"Pick up where you left off." else "נמשיך מאיפה שעצרת?").setContentIntent(open).setAutoCancel(true).build())
   app.db.dao().delivery(Delivery(delivery,id))
  }catch(_:SecurityException){}
  Result.success()
  }
 }
}
