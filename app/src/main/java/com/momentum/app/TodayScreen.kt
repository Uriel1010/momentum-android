package com.momentum.app
import android.content.Intent
import android.provider.Settings
import android.Manifest
import android.content.SharedPreferences
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
@Composable fun TodayScreen(vm:MomentumViewModel,tasks:List<Task>,projects:List<Project>,onOpen:(Task)->Unit){
 val context=LocalContext.current
 val store=remember{FocusNotification.store(context)}
 var ids by remember{mutableStateOf(FocusNotification.pinnedIds(context))}
 var query by rememberSaveable{mutableStateOf("")}
 var pending by rememberSaveable{mutableStateOf<String?>(null)}
 DisposableEffect(store){val listener=SharedPreferences.OnSharedPreferenceChangeListener{_,_->ids=FocusNotification.pinnedIds(context)};store.registerOnSharedPreferenceChangeListener(listener);onDispose{store.unregisterOnSharedPreferenceChangeListener(listener)}}
 fun add(id:String){vm.work{if(!FocusNotification.pin(context,id))vm.messages.emit(R.string.focus_notification_permission)}}
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->val id=pending;pending=null;if(granted && id!=null)add(id) else vm.work{vm.messages.emit(R.string.focus_notification_permission)}}
 fun toggle(t:Task){
  if(t.id in ids)vm.work{FocusNotification.remove(context,taskId=t.id)}
  else if(FocusNotification.canPost(context))add(t.id)
  else if(Build.VERSION.SDK_INT>=33 && pending==null){pending=t.id;permission.launch(Manifest.permission.POST_NOTIFICATIONS)}
  else vm.work{vm.messages.emit(R.string.focus_notification_permission)}
 }
 val eligible=tasks.filter{it.status !in setOf("DONE","ARCHIVED")}
 val selected=ids.mapNotNull{id->eligible.find{it.id==id}}
 val choices=eligible.filter{it.id !in ids && (it.title.contains(query,true) || projects.find{p->p.id==it.projectId}?.displayName()?.contains(query,true)==true)}
 LazyColumn(Modifier.fillMaxSize().testTag("todayList"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
  item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(tr(R.string.today)+" · "+selected.size,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge);if(selected.isNotEmpty())TextButton({vm.work{FocusNotification.remove(context)}},Modifier.testTag("clearToday")){Text(tr(R.string.today_clear))}}}
  if(!FocusNotification.canPost(context))item{TextButton({context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))}){Text(tr(R.string.focus_notification_settings))}}
  item{Text(tr(if(selected.isEmpty())R.string.today_empty else R.string.today_hint),style=MaterialTheme.typography.bodySmall)}
  items(selected,key={"selected-"+it.id}){t->TodayRow(t,projects,true,{toggle(t)},{onOpen(t)})}
  item{Text(tr(R.string.today_choose),Modifier.padding(top=12.dp),style=MaterialTheme.typography.titleMedium)}
  item{OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().testTag("todaySearch"),singleLine=true,label={Text(tr(R.string.search))})}
  items(choices,key={"choice-"+it.id}){t->TodayRow(t,projects,false,{toggle(t)},{onOpen(t)})}
 }
}
@Composable private fun TodayRow(t:Task,projects:List<Project>,selected:Boolean,onToggle:()->Unit,onOpen:()->Unit){
 Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceContainerLow){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
   val toggleLabel=tr(if(selected)R.string.today_remove else R.string.today_add)+": "+t.title
   Checkbox(selected,{onToggle()},Modifier.testTag("todayToggle-"+t.id).semantics{contentDescription=toggleLabel})
   Column(Modifier.weight(1f).clickable(onClick=onOpen).padding(vertical=10.dp,horizontal=6.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){
    Text(t.title,style=MaterialTheme.typography.bodyLarge)
    projects.find{it.id==t.projectId}?.let{ProjectBadge(it)}
    if(t.status=="WAITING" || t.available>System.currentTimeMillis())Text(tr(if(t.status=="WAITING")R.string.waiting else R.string.scheduled),style=MaterialTheme.typography.labelSmall)
   }
  }
 }
}
