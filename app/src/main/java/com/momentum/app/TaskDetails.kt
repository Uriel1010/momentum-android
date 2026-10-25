package com.momentum.app
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable fun RecurrenceFields(mode:String,unit:String,every:Int,anchor:Long?,onMode:(String)->Unit,onUnit:(String)->Unit,onEvery:(Int)->Unit,onAnchor:(Long?)->Unit){
 Text(tr(R.string.repeat_title),style=MaterialTheme.typography.titleMedium)
 StateChoices(listOf("NONE" to tr(R.string.repeat_none),"FIXED" to tr(R.string.repeat_fixed),"AFTER" to tr(R.string.repeat_after)),mode,onMode)
 if(mode!="NONE"){
  OutlinedTextField(every.toString(),{it.toIntOrNull()?.takeIf{n->n in 1..365}?.let(onEvery)},Modifier.fillMaxWidth(),label={Text(tr(R.string.repeat_every))},singleLine=true,keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Number))
  Choices(listOf("DAY" to tr(R.string.repeat_day),"WEEK" to tr(R.string.repeat_week),"MONTH" to tr(R.string.repeat_month)),unit,onUnit)
  if(mode=="FIXED")DateField(tr(R.string.repeat_anchor),anchor,onAnchor)
  Text(tr(if(mode=="FIXED")R.string.repeat_fixed_help else R.string.repeat_after_help),style=MaterialTheme.typography.bodySmall)
 }
}
@Composable fun TaskDetails(vm:MomentumViewModel,t:Task,projects:List<Project>,onEdit:()->Unit,onStart:()->Unit,onComplete:()->Unit){
 Page(spacing=10.dp){
  Text(tr(R.string.task_details),style=MaterialTheme.typography.labelLarge)
  Text(t.title,Modifier.testTag("taskDetailsTitle"),style=MaterialTheme.typography.headlineMedium)
  Text(tr(statusLabel(t.status)),style=MaterialTheme.typography.labelLarge)
  projects.find{it.id==t.projectId}?.let{ProjectBadge(it)}
  if(t.nextAction.isNotBlank())Text(t.nextAction)
  if(t.notes.isNotBlank())Text(t.notes)
  if(t.status !in setOf("DONE","ARCHIVED","WAITING") && t.available<=System.currentTimeMillis())FocusPinControl(vm,t)
  Text(tr(R.string.estimate),style=MaterialTheme.typography.titleMedium)
  Choices(listOf("" to tr(R.string.v3_no_estimate),"5" to "5", "15" to "15", "30" to "30", "60" to tr(R.string.one_hour)),t.estimate?.toString()?:"",{v->vm.work{vm.repo.save(t.copy(estimate=v.toIntOrNull()))}})
  DateField(tr(R.string.reminder),t.reminder,{v->vm.work{vm.repo.save(t.copy(reminder=v))}})
  RecurrenceFields(t.repeatMode,t.repeatUnit,t.repeatEvery,t.repeatAnchor.takeIf{it>0},{v->vm.work{vm.repo.save(t.copy(repeatMode=v))}},{v->vm.work{vm.repo.save(t.copy(repeatUnit=v))}},{v->vm.work{vm.repo.save(t.copy(repeatEvery=v))}},{v->vm.work{vm.repo.save(t.copy(repeatAnchor=v?:0))}})
  OutlinedButton(onEdit,Modifier.fillMaxWidth()){Text(tr(R.string.edit))}
  if(t.status !in setOf("DONE","ARCHIVED","WAITING"))PrimaryAction(tr(R.string.start),onStart,modifier=Modifier.testTag("detailStart"))
  if(t.status !in setOf("DONE","ARCHIVED"))TextButton(onComplete){Text(tr(R.string.finish))}
  else OutlinedButton({vm.work{vm.repo.save(TaskStates.reopened(t))}},Modifier.fillMaxWidth()){Text(tr(R.string.state_reopen))}
 }
}
