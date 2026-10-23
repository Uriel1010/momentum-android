package com.momentum.app
import android.Manifest
import android.content.*
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable fun FocusPinControl(vm:MomentumViewModel,task:Task){
 val context=LocalContext.current
 val store=remember{FocusNotification.store(context)}
 var pinned by remember{mutableStateOf(FocusNotification.pinnedId(context))}
 var available by remember{mutableStateOf(FocusNotification.canPost(context))}
 val lifecycle=LocalLifecycleOwner.current.lifecycle
 DisposableEffect(store,lifecycle){
  val listener=SharedPreferences.OnSharedPreferenceChangeListener{_,_->pinned=FocusNotification.pinnedId(context)}
  val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME){available=FocusNotification.canPost(context)}}
  store.registerOnSharedPreferenceChangeListener(listener);lifecycle.addObserver(observer)
  onDispose{store.unregisterOnSharedPreferenceChangeListener(listener);lifecycle.removeObserver(observer)}
 }
 fun pin(){vm.work{if(!FocusNotification.pin(context,task.id))vm.messages.emit(R.string.focus_notification_permission)}}
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->available=FocusNotification.canPost(context);if(granted)pin() else vm.work{vm.messages.emit(R.string.focus_notification_permission)}}
 Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
  OutlinedButton({
   if(pinned==task.id)vm.work{FocusNotification.remove(context,taskId=task.id)}
   else if(FocusNotification.canPost(context))pin()
   else if(Build.VERSION.SDK_INT>=33)permission.launch(Manifest.permission.POST_NOTIFICATIONS)
  },Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(tr(if(pinned==task.id)R.string.focus_notification_remove else R.string.focus_notification_pin))}
  Text(tr(R.string.focus_notification_hint),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  if(!available)TextButton({context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))}){Text(tr(R.string.focus_notification_settings))}
 }
}
