package com.momentum.app
import androidx.compose.foundation.*
import androidx.core.content.edit
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import androidx.lifecycle.repeatOnLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun FocusScreen(vm:MomentumViewModel,t:Task,prefs:Prefs,onComplete:()->Unit,onEdit:()->Unit,onExit:()->Unit) {
 var waitingNote by rememberSaveable {mutableStateOf(false)}
 var blocker by rememberSaveable {mutableStateOf(false)}
 var micro by rememberSaveable {mutableStateOf(false)}
 var progress by rememberSaveable {mutableStateOf(false)}
 var note by rememberSaveable {mutableStateOf("")}
 val lifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
 val context=LocalContext.current
 val timer=remember(t.id){context.getSharedPreferences("focus_timer",android.content.Context.MODE_PRIVATE)}
 var timerStart by rememberSaveable(t.id){mutableLongStateOf(timer.getLong(t.id,0))}
 var elapsed by remember {mutableLongStateOf(0)}
 val events by vm.events.collectAsStateWithLifecycle()
 val steps=events.count{it.taskId==t.id && it.kind=="advanced"}
 fun stop(){timer.edit {remove(t.id)};timerStart=0}
 LaunchedEffect(timerStart,lifecycle){lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED){while(timerStart>0){elapsed=((System.currentTimeMillis()-timerStart)/1000).coerceAtLeast(0);delay(1000)}}}
 Page(spacing=24.dp) {
  Eyebrow(tr(R.string.focus))
  Text(t.title,style=MaterialTheme.typography.displaySmall)
  Column(Modifier.fillMaxWidth().padding(vertical=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Trail(steps)
   Eyebrow(tr(R.string.next_action))
   Text(t.nextAction.ifBlank{tr(R.string.ux_just_begin_you_can_stop_after_a_moment_60275)},style=MaterialTheme.typography.headlineSmall)
   if(t.nextAction.isBlank())TextButton({micro=true;note=""}){Text(tr(R.string.ux_define_a_small_step_80d1e))}
  }
  if(t.notes.isNotBlank())Text(t.notes,style=MaterialTheme.typography.bodyLarge)
  if(steps>0)Text("$steps · "+tr(R.string.ux_steps_you_have_taken_34f7d),color=MaterialTheme.colorScheme.primary)
  if(timerStart>0) {
   Text("%02d:%02d".format(elapsed/60,elapsed%60),style=MaterialTheme.typography.displayMedium)
   Text(tr(R.string.ux_your_pace_no_countdown_7239e),color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
  TextButton({if(timerStart==0L){timerStart=System.currentTimeMillis();timer.edit {putLong(t.id,timerStart)}}else stop()}){Icon(Icons.Default.Timer,null);Spacer(Modifier.width(8.dp));Text(tr(if(timerStart==0L)R.string.timer else R.string.stop_timer))}
  PrimaryAction(tr(R.string.finish),{stop();onComplete()})
  OutlinedButton({progress=true;note=""},Modifier.fillMaxWidth().heightIn(min=56.dp)){Text(tr(R.string.advanced_progress))}
  TextButton({blocker=true},Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(tr(R.string.stuck))}
  HorizontalDivider()
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
   TextButton({vm.work{vm.repo.postpone(t,900_000)};stop();onExit()}){Text(tr(R.string.ux_pause_for_15_min_e99c1))}
   TextButton(onEdit){Text(tr(R.string.edit))}
  }
 }
 if(blocker)ModalBottomSheet(onDismissRequest={blocker=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
  Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Heading(tr(R.string.ux_what_would_help_now_9c0e0))
   Text(tr(R.string.ux_change_the_step_not_the_goal_cb524),color=MaterialTheme.colorScheme.onSurfaceVariant)
   listOf("big" to tr(R.string.ux_it_feels_too_big_20e8d),"time" to tr(R.string.no_time),"waiting" to tr(R.string.waiting_for),"unclear" to tr(R.string.unclear),"later" to tr(R.string.not_right)).forEach{(key,label)->
    OutlinedButton({blocker=false;if(key in setOf("big","unclear")){micro=true;note=t.nextAction}else if(key=="waiting"){waitingNote=true;note=""}else{vm.block(t,key);stop();onExit()}},Modifier.fillMaxWidth().heightIn(min=56.dp)){Text(label)}
   }
   TextButton({blocker=false;onExit()},Modifier.fillMaxWidth()){Text(tr(R.string.other))}
   Text(tr(R.string.ux_no_time_one_hour_waiting_review_in_a_w_dec4e),style=MaterialTheme.typography.bodySmall)
  }
 }
 if(waitingNote)AlertDialog(onDismissRequest={waitingNote=false},title={Text(tr(R.string.v3_blocker_note))},text={OutlinedTextField(note,{note=it},label={Text(tr(R.string.notes))})},confirmButton={TextButton({vm.work{vm.repo.block(t,"waiting",note.trim());waitingNote=false;stop();onExit()}},Modifier.testTag("confirmWaiting")){Text(tr(R.string.waiting))}},dismissButton={TextButton({waitingNote=false}){Text(tr(R.string.cancel))}})
 if(micro || progress)AlertDialog(onDismissRequest={micro=false;progress=false},title={Text(tr(if(micro)R.string.small_step else R.string.progress_note))},text={
  Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text(if(micro)tr(R.string.ux_something_you_can_actually_do_for_exam_ed1de) else tr(R.string.ux_what_changed_one_sentence_keeps_your_p_30151))
   OutlinedTextField(note,{note=it},label={Text(tr(if(micro)R.string.next_action else R.string.notes))})
  }
 },confirmButton={TextButton({
  vm.work {if(micro)vm.repo.save(t.copy(nextAction=note.trim())) else {vm.repo.advance(t,note.trim());vm.messages.emit(R.string.completed_step)}}
  if(!micro)pulse(context,prefs.feedback,Touch.COMPLETE)
  micro=false;progress=false;note=""
 },enabled=note.isNotBlank()){Text(tr(R.string.save))}},dismissButton={TextButton({micro=false;progress=false}){Text(tr(R.string.cancel))}})
}
