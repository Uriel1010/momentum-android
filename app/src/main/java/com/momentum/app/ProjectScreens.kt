package com.momentum.app
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import java.time.*
import kotlinx.coroutines.flow.first

@Composable fun ProjectEditor(old:Project?,onDismiss:()->Unit,onSave:(Project)->Unit) {
 var title by rememberSaveable(old?.id){mutableStateOf(old?.title?:"")}
 var emoji by rememberSaveable(old?.id){mutableStateOf(old?.emoji?:"")}
 var description by rememberSaveable(old?.id){mutableStateOf(old?.description?:"")}
 var summary by rememberSaveable(old?.id){mutableStateOf(old?.summary?:"")}
 var next by rememberSaveable(old?.id){mutableStateOf(old?.nextStep?:"")}
 var review by rememberSaveable(old?.id){mutableStateOf(old?.review)}
 EditorShell(tr(if(old==null)R.string.new_project else R.string.edit_project),onDismiss) {
  OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text(tr(R.string.title))})
  OutlinedTextField(emoji,{emoji=it.take(32)},Modifier.fillMaxWidth(),label={Text(tr(R.string.project_emoji))},singleLine=true)
  Choices(listOf("✈️","🏠","💼","📚","💪","🎯").map{it to it},emoji,{emoji=it})
  OutlinedTextField(description,{description=it},Modifier.fillMaxWidth(),label={Text(tr(R.string.description))})
  OutlinedTextField(summary,{summary=it},Modifier.fillMaxWidth(),label={Text(tr(R.string.where_stopped))})
  OutlinedTextField(next,{next=it},Modifier.fillMaxWidth(),label={Text(tr(R.string.next_action))})
  DateField(tr(R.string.review),review,{review=it})
  Button({onSave((old?:Project(title=title)).copy(title=title.trim(),emoji=emoji.trim(),description=description,summary=summary,nextStep=next,review=review,updated=System.currentTimeMillis()))},Modifier.fillMaxWidth(),enabled=title.isNotBlank()){Text(tr(R.string.save))}
  TextButton(onDismiss){Text(tr(R.string.cancel))}
 }
}
@Composable fun SettingsScreen(vm:MomentumViewModel,p:Prefs,onExport:()->Unit,onRestore:()->Unit,onSuggestions:()->Unit) {
 var clearConfirm by rememberSaveable{mutableStateOf(false)}
 val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){vm.work{vm.repo.reschedule()}}
 var start by rememberSaveable {mutableStateOf(p.quietStart.toString())}
 var end by rememberSaveable {mutableStateOf(p.quietEnd.toString())}
 var cap by rememberSaveable {mutableStateOf(p.dailyCap.toString())}
 Page {
  Heading(tr(R.string.settings))
  FilledTonalButton(onSuggestions,Modifier.fillMaxWidth()){Icon(Icons.Default.Lightbulb,null);Spacer(Modifier.width(8.dp));Text(tr(R.string.dev_title))}
  Text(tr(R.string.ux_your_space_your_way_2dfe7),color=MaterialTheme.colorScheme.onSurfaceVariant)
  val widgetContext=androidx.compose.ui.platform.LocalContext.current
  OutlinedButton({
   val manager=android.appwidget.AppWidgetManager.getInstance(widgetContext)
   if(manager.isRequestPinAppWidgetSupported)manager.requestPinAppWidget(android.content.ComponentName(widgetContext,MomentumWidget::class.java),null,null)
   else android.widget.Toast.makeText(widgetContext,widgetContext.getString(R.string.widget_help),android.widget.Toast.LENGTH_LONG).show()
  },Modifier.fillMaxWidth()){Text(tr(R.string.widget_add))}
  OutlinedButton({
   val manager=android.appwidget.AppWidgetManager.getInstance(widgetContext)
   if(manager.isRequestPinAppWidgetSupported)manager.requestPinAppWidget(android.content.ComponentName(widgetContext,CaptureWidget::class.java),null,null)
   else android.widget.Toast.makeText(widgetContext,widgetContext.getString(R.string.widget_help),android.widget.Toast.LENGTH_LONG).show()
  },Modifier.fillMaxWidth()){Text(tr(R.string.capture_widget_add))}
  listOf(TodoListWidget::class.java to R.string.todo_widget_add_wide,TallTodoListWidget::class.java to R.string.todo_widget_add_tall).forEach{(provider,label)->
   OutlinedButton({
    val manager=android.appwidget.AppWidgetManager.getInstance(widgetContext)
    if(manager.isRequestPinAppWidgetSupported)manager.requestPinAppWidget(android.content.ComponentName(widgetContext,provider),null,null)
    else android.widget.Toast.makeText(widgetContext,widgetContext.getString(R.string.widget_help),android.widget.Toast.LENGTH_LONG).show()
   },Modifier.fillMaxWidth()){Text(tr(label))}
  }
  Eyebrow(tr(R.string.ux_experience_5b5aa))
  Toggle(tr(R.string.language),p.english){vm.pref("english",it)}
  Toggle(tr(R.string.recommendations),p.suggestions){vm.pref("suggestions",it)}
  Toggle(tr(R.string.feedback),p.feedback){vm.pref("feedback",it)}
  Toggle(tr(R.string.ux_reduce_motion_25a5a),p.reducedMotion){vm.pref("reducedMotion",it)}
  Toggle(tr(R.string.dynamic),p.dynamic){vm.pref("dynamic",it)}
  HorizontalDivider()
  Eyebrow(tr(R.string.ux_gentle_reminders_c941b))
  Toggle(tr(R.string.notifications),p.notifications){vm.pref("notifications",it)}
  Text(tr(R.string.timing_notice),style=MaterialTheme.typography.bodySmall)
  if(Build.VERSION.SDK_INT>=33)OutlinedButton({request.launch(Manifest.permission.POST_NOTIFICATIONS)}){Text(tr(R.string.allow_notifications))}
  val keyboard=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Number)
  OutlinedTextField(start,{start=it.filter(Char::isDigit).take(2)},Modifier.fillMaxWidth(),label={Text(tr(R.string.quiet_start))},keyboardOptions=keyboard)
  OutlinedTextField(end,{end=it.filter(Char::isDigit).take(2)},Modifier.fillMaxWidth(),label={Text(tr(R.string.quiet_end))},keyboardOptions=keyboard)
  OutlinedTextField(cap,{cap=it.filter(Char::isDigit).take(2)},Modifier.fillMaxWidth(),label={Text(tr(R.string.daily_cap))},keyboardOptions=keyboard)
  Button({
   vm.work {vm.app.prefs.number("quietStart",start.toInt());vm.app.prefs.number("quietEnd",end.toInt());vm.app.prefs.number("dailyCap",cap.toInt());vm.repo.reschedule();vm.messages.emit(R.string.saved)}
  },enabled=start.toIntOrNull() in 0..23 && end.toIntOrNull() in 0..23 && cap.toIntOrNull() in 1..20){Text(tr(R.string.apply))}
  HorizontalDivider()
  Toggle(tr(R.string.metrics),p.metrics){vm.pref("metrics",it)}
  Text(tr(R.string.v3_metrics_privacy),style=MaterialTheme.typography.bodySmall)
  if(p.metrics)MetricsPanel(vm)
  TextButton({clearConfirm=true}){Text(tr(R.string.clear_metrics))}
  HorizontalDivider()
  Eyebrow(tr(R.string.ux_your_data_48cd1))
  OutlinedButton(onExport,Modifier.fillMaxWidth()){Icon(Icons.Default.FileDownload,null);Text(tr(R.string.backup))}
  OutlinedButton(onRestore,Modifier.fillMaxWidth()){Icon(Icons.Default.Restore,null);Text(tr(R.string.restore))}
  Text(tr(R.string.no_geo),style=MaterialTheme.typography.bodySmall)
  Text(tr(R.string.local_only),color=MaterialTheme.colorScheme.secondary)
 }
 if(clearConfirm)AlertDialog(onDismissRequest={clearConfirm=false},title={Text(tr(R.string.clear_metrics))},text={Text(tr(R.string.v3_metrics_privacy))},confirmButton={TextButton({vm.work{vm.repo.analytics.clear()};clearConfirm=false}){Text(tr(R.string.clear))}},dismissButton={TextButton({clearConfirm=false}){Text(tr(R.string.cancel))}})
}
