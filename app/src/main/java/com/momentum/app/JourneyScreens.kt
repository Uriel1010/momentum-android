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
import androidx.compose.ui.semantics.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.*

@Composable fun ProjectList(projects:List<Project>,onOpen:(Project)->Unit,onAdd:()->Unit,tasks:List<Task> = emptyList()) {
 var filter by rememberSaveable{mutableStateOf("all")}
 val visible=projects.filter{filter=="all" || (filter=="paused")==it.paused}
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(24.dp,20.dp,24.dp,32.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
  item {Text(tr(R.string.neo_projects_heading),style=MaterialTheme.typography.displaySmall)}
  item {Text(tr(R.string.ux_keep_the_thread_return_to_the_next_ste_abf5f),color=MaterialTheme.colorScheme.onSurfaceVariant)}
  item {Choices(listOf("all" to tr(R.string.dev_all),"active" to tr(R.string.ux_active_a733b),"paused" to tr(R.string.ux_paused_c7dfb)),filter){filter=it}}
  item {TextButton(onAdd,Modifier.heightIn(min=48.dp)){Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text(tr(R.string.new_project))}}
  if(visible.isEmpty())item {QuietPanel{Text(tr(R.string.project_empty))}}
  items(visible,key={it.id}) {p->
   val steps=tasks.filter{it.projectId==p.id && it.status!="ARCHIVED"};val done=steps.count{it.status=="DONE"}
   Column(Modifier.fillMaxWidth().clickable(role=androidx.compose.ui.semantics.Role.Button,onClick={onOpen(p)}).padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
    Row(verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
     Box(Modifier.size(52.dp).background(if(p.paused)Neo.Purple else MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){Icon(if(p.paused)Icons.Default.Pause else Icons.Default.FolderOpen,null,tint=if(p.paused)Neo.Ink else MaterialTheme.colorScheme.onSurface)}
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
      Text(p.displayName(),style=MaterialTheme.typography.titleLarge)
      Text(p.nextStep.ifBlank{tr(R.string.ux_choose_a_project_step_70297)},style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
      Text("\u2066$done / ${steps.size}\u2069 · "+tr(R.string.ux_steps_completed_880fd),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
      LinearProgressIndicator(progress={if(steps.isEmpty())0f else done.toFloat()/steps.size},modifier=Modifier.fillMaxWidth().height(7.dp),color=if(p.paused)Neo.Purple else if(androidx.compose.foundation.isSystemInDarkTheme())Neo.Lime else Neo.Ink,trackColor=MaterialTheme.colorScheme.surfaceVariant)
     }
    }
    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
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
  Text(p.displayName(),style=MaterialTheme.typography.displaySmall)
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
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(24.dp,20.dp,24.dp,32.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item {Column{Text(tr(R.string.neo_progress_heading),style=MaterialTheme.typography.displaySmall);InkUnderline()}}
  item {Text(if(returned)tr(R.string.ux_you_came_back_continue_from_here_2d8dd) else tr(R.string.neo_small_steps),color=MaterialTheme.colorScheme.onSurfaceVariant)}
  item {Choices(listOf("week" to tr(R.string.v3_this_week),"all" to tr(R.string.history)),if(all)"all" else "week"){all=it=="all"}}
  item {
   val metrics=listOf(Triple(tr(R.string.neo_starts),visible.count{it.kind=="started"},Neo.Lime),Triple(tr(R.string.neo_advances),visible.count{it.kind=="advanced"},Neo.Purple),Triple(tr(R.string.neo_completions),visible.count{it.kind=="completed"},MaterialTheme.colorScheme.surfaceVariant))
   if(androidx.compose.ui.platform.LocalConfiguration.current.fontScale>1.2f)Column(verticalArrangement=Arrangement.spacedBy(12.dp)){metrics.forEach{(label,count,color)->MovementStat(label,count,color,Modifier.fillMaxWidth())}}
   else Row(Modifier.height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(10.dp)){metrics.forEach{(label,count,color)->MovementStat(label,count,color,Modifier.weight(1f).fillMaxHeight())}}
  }
  item {ActivityBars(history)}
  item {HorizontalDivider();Spacer(Modifier.height(12.dp));Eyebrow(tr(R.string.history))}
  if(visible.isEmpty())item {Text(tr(R.string.no_progress));Text(tr(R.string.v3_no_history),color=MaterialTheme.colorScheme.onSurfaceVariant)}
  items(visible,key={it.id}){e->Column{if(e.kind=="started" && tasks.any{it.id==e.taskId && e.created-it.created>=7*86_400_000L})Eyebrow(tr(R.string.ux_what_waited_is_moving_5b64b));TimelineEntry(e)}}
 }
}
@Composable fun MovementStat(label:String,count:Int,color:androidx.compose.ui.graphics.Color,modifier:Modifier=Modifier) {
 val on=if(color==Neo.Lime || color==Neo.Purple)Neo.Ink else MaterialTheme.colorScheme.onSurface
 Column(modifier.background(color,RoundedCornerShape(16.dp)).padding(horizontal=14.dp,vertical=20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Text(count.toString(),style=MaterialTheme.typography.displaySmall,color=on)
  Text(label,style=MaterialTheme.typography.labelMedium,color=on)
 }
}
@Composable fun ActivityBars(history:List<Event>) {
 val days=(6 downTo 0).map{LocalDate.now().minusDays(it.toLong())}
 val counts=days.map{day->history.count{Instant.ofEpochMilli(it.created).atZone(ZoneId.systemDefault()).toLocalDate()==day}}
 val peak=counts.maxOrNull()?.coerceAtLeast(1)?:1
 val actionLabel=tr(R.string.neo_activity_desc)
 val locale=java.util.Locale.forLanguageTag(if(LocalEnglish.current)"en" else "he")
 Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Text(tr(R.string.neo_activity),style=MaterialTheme.typography.titleMedium)
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.Bottom) {
   days.forEachIndexed{i,day->Column(Modifier.weight(1f).clearAndSetSemantics{contentDescription=day.toString()+": "+counts[i]+" "+actionLabel},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Box(Modifier.height(76.dp).fillMaxWidth(),contentAlignment=Alignment.BottomCenter){
     Box(Modifier.width(16.dp).height(if(counts[i]==0)3.dp else (76f*counts[i]/peak).dp).background(if(counts[i]==0)MaterialTheme.colorScheme.outlineVariant else Neo.Purple,RoundedCornerShape(3.dp)))
    }
    Text(day.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW,locale),style=MaterialTheme.typography.labelSmall)
    Text(counts[i].toString(),style=MaterialTheme.typography.labelSmall)
   }}
  }
 }
}
@Composable fun MilestoneMoment(onDismiss:()->Unit) {
 val reduced=LocalReduceMotion.current
 var entered by remember{mutableStateOf(false)}
 LaunchedEffect(Unit){entered=true}
 val scale by androidx.compose.animation.core.animateFloatAsState(if(entered)1f else .88f,if(reduced)androidx.compose.animation.core.snap() else androidx.compose.animation.core.spring(dampingRatio=.7f),label="milestone arrival")
 AlertDialog(onDismissRequest=onDismiss,icon={Icon(Icons.Default.Flag,null,Modifier.size(42.dp).graphicsLayer{scaleX=scale;scaleY=scale},tint=Neo.Ink)},containerColor=Neo.Purple,titleContentColor=Neo.Ink,textContentColor=Neo.Ink,title={Text(tr(R.string.v3_milestone_title))},text={Column{Trail(1);Text(tr(R.string.v3_milestone_body))}},confirmButton={TextButton(onDismiss){Text(tr(R.string.v3_continue),color=Neo.Ink)}})
}
