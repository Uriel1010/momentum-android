package com.momentum.app
import android.Manifest
import android.app.*
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.*
import java.util.Calendar

fun statusLabel(status:String)=when(status) {
 "INBOX","READY"->R.string.state_todo;"IN_PROGRESS"->R.string.task_in_progress;"WAITING"->R.string.waiting;"DONE"->R.string.done;else->R.string.archived
}
@Composable fun DateField(label:String,value:Long?,onChange:(Long?)->Unit) {
 val context=LocalContext.current
 Column {
  OutlinedButton({
   val cal=Calendar.getInstance().apply {timeInMillis=value?:System.currentTimeMillis()}
   DatePickerDialog(context,{_,y,m,d->cal.set(y,m,d);TimePickerDialog(context,{_,h,min->cal.set(Calendar.HOUR_OF_DAY,h);cal.set(Calendar.MINUTE,min);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);onChange(cal.timeInMillis)},cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE),true).show()},cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)).show()
  },Modifier.fillMaxWidth()){Text(label+if(value==null)"" else ": "+dateText(value))}
  if(value!=null)TextButton({onChange(null)}){Text(tr(R.string.clear))}
 }
}
@Composable fun EditorShell(title:String,onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit) {
 Dialog(onDismissRequest=onDismiss,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
  Surface(Modifier.fillMaxWidth().fillMaxHeight(.92f).padding(12.dp).imePadding(),shape=RoundedCornerShape(24.dp)) {
   Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
    Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
    content()
   }
  }
 }
}
@Composable fun TaskEditor(old:Task?,initial:String,projectId:String?,projects:List<Project>,onDismiss:()->Unit,onSave:(Task)->Unit,onDelete:(Task)->Unit) {
 var title by rememberSaveable(old?.id){mutableStateOf(old?.title?:initial)}
 var notes by rememberSaveable(old?.id){mutableStateOf(old?.notes?:"")}
 var estimate by rememberSaveable(old?.id){mutableStateOf(old?.estimate?.toString()?:"")}
 var priority by rememberSaveable(old?.id){mutableIntStateOf(old?.priority?:1)}
 var status by rememberSaveable(old?.id){mutableStateOf(TaskStates.visible(old?.status?:"READY"))}
 var project by rememberSaveable(old?.id){mutableStateOf(old?.projectId?:projectId?:"")}
 var next by rememberSaveable(old?.id){mutableStateOf(old?.nextAction?:"")}
 var contexts by rememberSaveable(old?.id){mutableStateOf(old?.contexts?:"")}
 var due by rememberSaveable(old?.id){mutableStateOf(old?.due)}
 var reminder by rememberSaveable(old?.id){mutableStateOf(old?.reminder)}
 var available by rememberSaveable(old?.id){mutableStateOf(old?.available?.takeIf{it>0})}
 var advanced by rememberSaveable {mutableStateOf(false)}
 var invalid by remember {mutableStateOf(false)}
 var deleteConfirm by remember {mutableStateOf(false)}
 val app=LocalContext.current.applicationContext as MomentumApp
 val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){app.scope.launch{app.repo.reschedule()}}
 EditorShell(tr(if(old==null)R.string.add else R.string.edit),onDismiss) {
  OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text(tr(R.string.title))},isError=invalid && title.isBlank(),maxLines=4)
  if(invalid)Text(tr(R.string.invalid),color=MaterialTheme.colorScheme.error)
  Button({
   val duration=estimate.toIntOrNull()
   if(title.isBlank() || (estimate.isNotBlank()&&(duration==null||duration !in 1..10080))){invalid=true}
   else onSave((old?:Task(title=title)).copy(title=title,notes=notes,estimate=duration,priority=priority,status=status,projectId=project.takeIf{it.isNotBlank()},nextAction=next,contexts=contexts,due=due,reminder=reminder,available=if(status=="READY" && old?.status in setOf("DONE","ARCHIVED","WAITING"))0 else available?:0,completed=if(status=="DONE")old?.completed?:System.currentTimeMillis() else null))
  },Modifier.fillMaxWidth().heightIn(min=52.dp)){Text(tr(R.string.save))}
  Text(tr(R.string.status),style=MaterialTheme.typography.titleMedium)
  StateChoices(listOf("READY","IN_PROGRESS","WAITING","DONE","ARCHIVED").map{it to tr(statusLabel(it))},status,{status=it})
  Text(tr(stateHelp(status)),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  if(old?.status in setOf("DONE","ARCHIVED"))OutlinedButton({onSave(TaskStates.reopened(requireNotNull(old)))},Modifier.fillMaxWidth().testTag("restoreTask")){Text(tr(if(old?.status=="DONE")R.string.state_reopen else R.string.state_restore))}
  TextButton({advanced=!advanced}){Text(tr(R.string.advanced))}
  if(advanced) {
   OutlinedTextField(notes,{notes=it},Modifier.fillMaxWidth(),label={Text(tr(R.string.notes))})
   OutlinedTextField(next,{next=it},Modifier.fillMaxWidth(),label={Text(tr(R.string.next_action))})
   OutlinedTextField(estimate,{estimate=it.filter(Char::isDigit).take(5)},Modifier.fillMaxWidth(),label={Text(tr(R.string.estimate))},keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Number))
   Text(tr(R.string.priority))
   Choices(listOf("0" to tr(R.string.low),"1" to tr(R.string.normal),"2" to tr(R.string.high)),priority.toString(),{priority=it.toInt()})
   Text(tr(R.string.project))
   Choices(listOf("" to tr(R.string.none))+projects.map{it.id to it.title},project,{project=it})
   OutlinedTextField(contexts,{contexts=it},Modifier.fillMaxWidth(),label={Text(tr(R.string.context))})
   DateField(tr(R.string.due),due,{due=it})
   DateField(tr(R.string.later),available,{available=it})
   DateField(tr(R.string.reminder),reminder,{reminder=it})
   Text(tr(R.string.timing_notice),style=MaterialTheme.typography.bodySmall)
   if(Build.VERSION.SDK_INT>=33)TextButton({request.launch(Manifest.permission.POST_NOTIFICATIONS)}){Text(tr(R.string.allow_notifications))}
   if(old!=null)TextButton({deleteConfirm=true}){Text(tr(R.string.delete),color=MaterialTheme.colorScheme.error)}
  }
  TextButton(onDismiss,Modifier.fillMaxWidth()){Text(tr(R.string.cancel))}
 }
 if(deleteConfirm)AlertDialog(onDismissRequest={deleteConfirm=false},text={Text(tr(R.string.confirm_delete))},confirmButton={TextButton({onDelete(requireNotNull(old))}){Text(tr(R.string.delete))}},dismissButton={TextButton({deleteConfirm=false}){Text(tr(R.string.cancel))}})
}
