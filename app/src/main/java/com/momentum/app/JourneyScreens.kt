package com.momentum.app
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.*

@Composable fun ProjectList(projects:List<Project>,onOpen:(Project)->Unit,onAdd:()->Unit) {
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(24.dp,20.dp,24.dp,100.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
  item {Heading(tr(R.string.projects))}
  item {Text(tr(R.string.ux_keep_the_thread_return_to_the_next_ste_abf5f),color=MaterialTheme.colorScheme.onSurfaceVariant)}
  item {OutlinedButton(onAdd,Modifier.heightIn(min=52.dp)){Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text(tr(R.string.new_project))}}
  if(projects.isEmpty())item {QuietPanel{Trail(0);Text(tr(R.string.project_empty))}}
  items(projects,key={it.id}) {p->
   Surface(onClick={onOpen(p)},shape=Rhythm.card,color=MaterialTheme.colorScheme.surfaceContainerLow) {
    Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
      Icon(Icons.Default.Route,null,tint=MaterialTheme.colorScheme.primary)
      Text(if(p.paused)tr(R.string.ux_paused_c7dfb) else tr(R.string.ux_active_a733b),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
     }
     Text(p.title,style=MaterialTheme.typography.headlineMedium)
     if(p.description.isNotBlank())Text(p.description,style=MaterialTheme.typography.bodyMedium)
     HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
     Eyebrow(tr(R.string.next_action))
     Text(p.nextStep.ifBlank{tr(R.string.ux_choose_a_project_step_70297)},style=MaterialTheme.typography.titleMedium)
     Text(dateText(p.updated),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
  }
 }
}
@Composable fun ProjectScreen(vm:MomentumViewModel,p:Project,tasks:List<Task>,events:List<Event>,onEdit:()->Unit,onAdd:()->Unit,onTask:(Task)->Unit,onStart:(Task)->Unit) {
 val next=tasks.firstOrNull{it.status !in setOf("DONE","ARCHIVED","WAITING") && it.available<=System.currentTimeMillis()}
 var expandedSteps by rememberSaveable {mutableStateOf(false)}
 var expandedHistory by rememberSaveable {mutableStateOf(false)}
 var celebrated by rememberSaveable {mutableStateOf(false)}
 var milestone by rememberSaveable {mutableStateOf(false)}
 var note by rememberSaveable {mutableStateOf("")}
 val prefs by vm.prefs.collectAsStateWithLifecycle()
 val context=LocalContext.current
 val meaningful=events.filter{it.kind in setOf("completed","advanced","milestone")}
 val blocked=tasks.filter{it.status=="WAITING"}
 Page(spacing=22.dp) {
  Eyebrow(if(p.paused)tr(R.string.ux_paused_c7dfb) else tr(R.string.project))
  Text(p.title,style=MaterialTheme.typography.displaySmall)
  if(p.description.isNotBlank())Text(p.description,style=MaterialTheme.typography.bodyLarge)
  QuietPanel {
   Eyebrow(tr(R.string.where_stopped))
   Text(p.summary.ifBlank{tr(R.string.ux_your_progress_will_be_kept_here_4459a)},style=MaterialTheme.typography.bodyLarge)
   HorizontalDivider()
   Eyebrow(tr(R.string.next_action))
   Text(p.nextStep.ifBlank{next?.nextAction?.ifBlank{next.title}?:tr(R.string.ux_what_will_the_first_step_be_78bb2)},style=MaterialTheme.typography.headlineSmall)
   if(next!=null && !p.paused)PrimaryAction(tr(R.string.start),{onStart(next)})
   else if(!p.paused)TextButton(onAdd){Text(tr(R.string.add))}
   if(p.review!=null)Text(tr(R.string.review)+": "+dateText(p.review))
  }
  Row {TextButton(onEdit){Text(tr(R.string.edit_project))};TextButton({vm.work{if(p.paused)vm.repo.resumeProject(p) else vm.repo.saveProject(p.copy(paused=true,updated=System.currentTimeMillis()))}}){Text(tr(if(p.paused)R.string.resume_project else R.string.pause_project))}}
  if(blocked.isNotEmpty())QuietPanel {Eyebrow(tr(R.string.ux_waiting_to_move_forward_e4b8a));blocked.take(5).forEach{t->TextButton({onTask(t)}){Text(t.title)};events.firstOrNull{it.taskId==t.id && it.kind=="blocked"}?.note?.takeIf{it !in setOf("waiting","later","time") && it.isNotBlank()}?.let{Text(it)};Text(tr(R.string.review)+": "+dateText(t.available),style=MaterialTheme.typography.bodySmall)}}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
   Heading(tr(R.string.steps));TextButton(onAdd){Icon(Icons.Default.Add,null);Text(tr(R.string.add))}
  }
  Text("${tasks.count{it.status=="DONE"}} "+tr(R.string.ux_steps_completed_880fd),color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
  if(tasks.size>5)TextButton({expandedSteps=!expandedSteps}){Text(tr(if(expandedSteps)R.string.v3_less else R.string.v3_all_steps)+" · "+tasks.size)}
  (if(expandedSteps)tasks else tasks.take(5)).forEach {t->Surface(onClick={onTask(t)},shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceContainerLow) {
   Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
    Icon(if(t.status=="DONE")Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,null,tint=MaterialTheme.colorScheme.primary)
    Column(Modifier.weight(1f)){Text(t.title,style=MaterialTheme.typography.titleMedium);Text(tr(statusLabel(t.status)),style=MaterialTheme.typography.bodySmall)}
   }
  }}
  Heading(tr(R.string.history))
  TextButton({milestone=true}){Icon(Icons.Default.Flag,null);Spacer(Modifier.width(8.dp));Text(tr(R.string.ux_mark_a_milestone_d161d))}
  if(meaningful.isEmpty())Text(tr(R.string.ux_the_steps_you_take_will_appear_here_dc44c))
  val history=events.filter{it.kind in setOf("started","completed","advanced","milestone","blocked","resumed")}
  if(history.size>5)TextButton({expandedHistory=!expandedHistory}){Text(tr(if(expandedHistory)R.string.v3_less else R.string.history))}
  (if(expandedHistory)history else history.take(5)).forEach {e->TimelineEntry(e)}
 }
 if(celebrated)MilestoneMoment(onDismiss={celebrated=false})
 if(milestone)AlertDialog(onDismissRequest={milestone=false},title={Text(tr(R.string.ux_what_was_achieved_f80fa))},text={OutlinedTextField(note,{note=it},label={Text(tr(R.string.ux_a_real_milestone_38e3e))})},confirmButton={TextButton({vm.work{vm.repo.milestone(p,note);celebrated=true};pulse(context,prefs.feedback,Touch.MILESTONE);milestone=false;note=""},enabled=note.isNotBlank()){Text(tr(R.string.save))}},dismissButton={TextButton({milestone=false}){Text(tr(R.string.cancel))}})
}
@Composable fun TimelineEntry(e:Event) {
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
  Column(horizontalAlignment=Alignment.CenterHorizontally) {
   Icon(when(e.kind){"milestone"->Icons.Default.Flag;"completed"->Icons.Default.CheckCircle;else->Icons.Default.Adjust},null,Modifier.size(24.dp),tint=MaterialTheme.colorScheme.primary)
   Box(Modifier.width(1.dp).height(48.dp).background(MaterialTheme.colorScheme.outlineVariant))
  }
  Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
   Text(when(e.kind){"completed"->tr(R.string.done);"started"->tr(R.string.ux_started_faa9e);"milestone"->tr(R.string.ux_milestone_ee37d);"blocked"->tr(R.string.waiting);"resumed"->tr(R.string.v3_project_return);else->tr(R.string.advanced_progress)},style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
   if(e.note.isNotBlank() && e.kind!="blocked")Text(e.note,style=MaterialTheme.typography.bodyLarge)
   Text(dateText(e.created),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}
@Composable fun ProgressScreen(tasks:List<Task>,projects:List<Project>,events:List<Event>,prefs:Prefs) {
 val week=LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
 val history=events.filter{it.kind in setOf("started","advanced","completed","milestone","resumed")}.sortedByDescending{it.created}
 val recent=history.filter{it.created>=week}
 val returned=history.zipWithNext().any{(a,b)->a.created>=week && a.created-b.created>=7*86_400_000L}
 var all by rememberSaveable{mutableStateOf(false)}
 val visible=if(all)history else recent
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(24.dp,20.dp,24.dp,110.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
  item {Eyebrow(tr(R.string.progress))}
  item {Text(tr(R.string.v3_trail_title),style=MaterialTheme.typography.displaySmall)}
  item {Trail(recent.count{it.kind in setOf("advanced","completed","milestone")})}
  item {
   Text(if(returned)tr(R.string.ux_you_came_back_continue_from_here_2d8dd) else tr(R.string.v3_trail_reflection),style=MaterialTheme.typography.bodyLarge)
   Spacer(Modifier.height(12.dp))
   val done=recent.count{it.kind=="completed"}
   val steps=recent.count{it.kind=="advanced"}
   Text(tr(R.string.v3_this_week)+" · "+tr(R.string.v3_completed_short)+": "+done+" · "+tr(R.string.v3_progress_short)+": "+steps,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
  item {Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
   FilterChip(!all,{all=false},label={Text(tr(R.string.v3_this_week))})
   FilterChip(all,{all=true},label={Text(tr(R.string.history))})
  }}
  if(visible.isEmpty())item {Text(tr(R.string.no_progress));Text(tr(R.string.v3_no_history),color=MaterialTheme.colorScheme.onSurfaceVariant)}
  items(visible,key={it.id}){e->
   Column {
    if(e.kind=="started" && tasks.any{it.id==e.taskId && e.created-it.created>=7*86_400_000L})Eyebrow(tr(R.string.ux_what_waited_is_moving_5b64b))
    TimelineEntry(e)
   }
  }
 }
}
@Composable fun MilestoneMoment(onDismiss:()->Unit) {
 val reduced=LocalReduceMotion.current
 var entered by remember{mutableStateOf(false)}
 LaunchedEffect(Unit){entered=true}
 val scale by androidx.compose.animation.core.animateFloatAsState(if(entered)1f else .88f,if(reduced)androidx.compose.animation.core.snap() else androidx.compose.animation.core.spring(dampingRatio=.7f),label="milestone arrival")
 AlertDialog(onDismissRequest=onDismiss,icon={Icon(Icons.Default.Flag,null,Modifier.size(42.dp).graphicsLayer{scaleX=scale;scaleY=scale},tint=MaterialTheme.colorScheme.primary)},title={Text(tr(R.string.v3_milestone_title))},text={Column{Trail(1);Text(tr(R.string.v3_milestone_body))}},confirmButton={TextButton(onDismiss){Text(tr(R.string.v3_continue))}})
}
