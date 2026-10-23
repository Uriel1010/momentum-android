package com.momentum.app
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import java.time.LocalTime

@Composable fun QuickCapture(onCapture:(String)->Unit,modifier:Modifier=Modifier) {
 var capture by rememberSaveable {mutableStateOf("")}
 val keyboard=LocalSoftwareKeyboardController.current
 fun save(){if(capture.isNotBlank()){onCapture(capture.trim());capture="";keyboard?.hide()}}
 OutlinedTextField(capture,{capture=it},modifier.fillMaxWidth(),label={Text(tr(R.string.capture))},
  shape=RoundedCornerShape(18.dp),maxLines=3,
  keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={save()}),
  trailingIcon={if(capture.isNotBlank())IconButton({save()}){Icon(Icons.Default.Add,tr(R.string.save))}})
 if(capture.isNotBlank())TextButton({save()},Modifier.fillMaxWidth()){Text(tr(R.string.save))}
}
@Composable fun WelcomeScreen(prefs:Prefs,onLanguage:(Boolean)->Unit,onSkip:()->Unit,onBegin:(String)->Unit) {
 var title by rememberSaveable {mutableStateOf("")}
 Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(28.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
   Eyebrow("MOMENTUM");TextButton(onSkip){Text(tr(R.string.ux_skip_for_now_6fc09))}
  }
  Spacer(Modifier.height(24.dp))
  Trail(0)
  Text(tr(R.string.ux_from_intention_to_a_first_step_e1885),style=MaterialTheme.typography.displaySmall)
  Text(tr(R.string.ux_you_don_t_need_to_organize_everything__48b4d),style=MaterialTheme.typography.bodyLarge)
  OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text(tr(R.string.capture))},shape=RoundedCornerShape(18.dp),maxLines=4)
  PrimaryAction(tr(R.string.ux_save_and_start_4074f),{onBegin(title.trim())},enabled=title.isNotBlank())
  Toggle(tr(R.string.language),prefs.english,onLanguage)
  Text(tr(R.string.local_only),color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun NowScreen(vm:MomentumViewModel,tasks:List<Task>,projects:List<Project>,prefs:Prefs,onStart:(Task)->Unit,onBrowse:()->Unit,onCapture:(String)->Unit,onLater:(Task)->Unit,onQuickCapture:()->Unit,onProject:(Project)->Unit) {
 var minutes by rememberSaveable {mutableIntStateOf(0)}
 var easy by rememberSaveable {mutableStateOf(false)}
 var selectedContext by rememberSaveable {mutableStateOf("")}
 var showConditions by rememberSaveable {mutableStateOf(false)}
 var now by remember {mutableLongStateOf(System.currentTimeMillis())}
 var stuck by remember {mutableStateOf<Task?>(null)}
 var note by rememberSaveable {mutableStateOf("")}
 val events by vm.events.collectAsStateWithLifecycle()
 LaunchedEffect(Unit){while(true){delay(15_000);now=System.currentTimeMillis()}}
 val candidates=remember(tasks,projects,minutes,easy,selectedContext,now,events){NowPolicy.candidates(tasks,projects,now,minutes,easy,selectedContext,events)}
 val suggestion=if(prefs.suggestions)candidates.firstOrNull() else null
 val resume=ResumePolicy.latest(tasks,projects,events,now)
 LaunchedEffect(suggestion?.task?.id){suggestion?.let{vm.repo.analytics.record("recommended",it.task.id)}}
 Page(spacing=18.dp) {
  Column {Text(tr(R.string.neo_now_heading),style=MaterialTheme.typography.displaySmall);InkUnderline()}
  Text(tr(R.string.neo_now_body),color=MaterialTheme.colorScheme.onSurfaceVariant)
  AnimatedContent(targetState=suggestion?.task?.id,label="next invitation",transitionSpec={if(prefs.reducedMotion)EnterTransition.None togetherWith ExitTransition.None else fadeIn() togetherWith fadeOut()}) {id->
   val t=candidates.find{it.task.id==id}?.task
   if(t!=null)ActionPanel {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
     Text(tr(R.string.neo_next_small),style=MaterialTheme.typography.labelMedium,modifier=Modifier.weight(1f))
     Icon(Icons.Default.NorthEast,null,Modifier.size(24.dp))
    }
    Text(t.title,style=MaterialTheme.typography.headlineLarge)
    Text(t.nextAction.ifBlank{tr(R.string.ux_start_without_committing_to_finish_2562a)},style=MaterialTheme.typography.bodyMedium)
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     Icon(Icons.Default.Schedule,null,Modifier.size(18.dp))
     Text(t.estimate?.let{"$it "+tr(R.string.minutes)}?:tr(R.string.ux_your_pace_79575),style=MaterialTheme.typography.labelMedium)
     projects.find{it.id==t.projectId}?.let{Text("· "+it.title,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}
    }
    if(events.count{it.taskId==t.id && it.kind=="postponed"}>=3)Text(tr(R.string.ux_this_has_waited_a_few_times_just_the_f_307f9),style=MaterialTheme.typography.bodySmall)
    PrimaryAction(tr(R.string.start),{onStart(t)},ink=true)
   } else QuietPanel {
    Text(when {
     tasks.isEmpty()->tr(R.string.ux_a_little_room_in_your_mind_806b8)
     tasks.all{it.status in setOf("DONE","ARCHIVED")}->tr(R.string.ux_what_was_here_is_behind_you_cf4a3)
     tasks.filter{it.status !in setOf("DONE","ARCHIVED")}.all{it.status=="WAITING"}->tr(R.string.v3_all_waiting)
     else->tr(R.string.no_suggestion)
    },style=MaterialTheme.typography.headlineMedium)
    Text(when {
     tasks.isEmpty()->tr(R.string.ux_something_small_to_remember_capture_it_e2308)
     tasks.all{it.status in setOf("DONE","ARCHIVED")}->tr(R.string.ux_you_can_stop_here_the_next_step_can_wa_7fd03)
     tasks.any{it.status=="WAITING"} && candidates.isEmpty()->tr(R.string.v3_review_waiting)
     minutes>0 || easy->tr(R.string.ux_nothing_matches_this_choice_yet_change_4f14a)
     else->tr(R.string.ux_scheduled_tasks_return_when_you_chose__025ff)
    })
   }
  }
  suggestion?.task?.let{t->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
   TextButton({vm.defer(t,false);now=System.currentTimeMillis()}){Text(tr(R.string.other))}
   TextButton({stuck=t;note=""}){Text(tr(R.string.stuck))}
   TextButton({onLater(t);now=System.currentTimeMillis()}){Text(tr(R.string.later))}
  }}
  if(resume!=null)ResumeCard(resume){onStart(resume.task)}
  Choices(listOf(0,2,5,15,30).map{it.toString() to if(it==0)tr(R.string.ux_your_pace_79575) else "$it ${tr(R.string.minutes)}"},minutes.toString(),{minutes=it.toInt()})
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton({easy=!easy}){Text((if(easy)"✓ " else "")+tr(R.string.ux_something_easier_2fe57))};TextButton({showConditions=!showConditions}){Text(tr(R.string.conditions))}}
  AnimatedVisibility(showConditions){OutlinedTextField(selectedContext,{selectedContext=it},Modifier.fillMaxWidth(),label={Text(tr(R.string.context))})}
  TextButton(onBrowse){Text(tr(R.string.ux_browse_tasks_a1a73))}
  val stale=projects.firstOrNull{!it.paused && now-it.updated>7*86_400_000L && tasks.any{t->t.projectId==it.id && t.status !in setOf("DONE","ARCHIVED")}}
  if(stale!=null)QuietPanel {Eyebrow(tr(R.string.ux_pick_up_the_thread_fd538));Text(stale.title,style=MaterialTheme.typography.titleMedium);Text(stale.nextStep.ifBlank{stale.summary});TextButton({onProject(stale)}){Text(tr(R.string.ux_find_a_project_step_c0b7a))}}
 }
 stuck?.let{t->ModalBottomSheet(onDismissRequest={stuck=null},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)){
  Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
   Heading(tr(R.string.ux_what_would_help_now_9c0e0))
   Text(tr(R.string.ux_change_the_step_not_the_goal_cb524))
   OutlinedTextField(note,{note=it},Modifier.fillMaxWidth(),label={Text(tr(R.string.v3_blocker_note))})
   PrimaryAction(tr(R.string.ux_define_a_small_step_80d1e),{stuck=null;onStart(t)})
   TextButton({vm.work{vm.repo.block(t,"time",note)};stuck=null}){Text(tr(R.string.no_time))}
   TextButton({vm.work{vm.repo.block(t,"waiting",note)};stuck=null}){Text(tr(R.string.waiting_for))}
  }
 }}
}
