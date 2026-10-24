package com.momentum.app
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.LayoutDirection
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineStart
import androidx.compose.ui.graphics.Color
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
@Composable fun NowScreen(vm:MomentumViewModel,tasks:List<Task>,projects:List<Project>,prefs:Prefs,onStart:(Task)->Unit,onBrowse:()->Unit,onCapture:(String)->Unit,onLater:(Task)->Unit,onQuickCapture:()->Unit,onProject:(Project)->Unit,onProjects:()->Unit,onProgress:()->Unit) {
 var minutes by rememberSaveable {mutableIntStateOf(0)}
 var easy by rememberSaveable {mutableStateOf(false)}
 var selectedContext by rememberSaveable {mutableStateOf("")}
 var showConditions by rememberSaveable {mutableStateOf(false)}
 var showGuide by rememberSaveable {mutableStateOf(false)}
 var now by remember {mutableLongStateOf(System.currentTimeMillis())}
 var stuck by remember {mutableStateOf<Task?>(null)}
 var note by rememberSaveable {mutableStateOf("")}
 val events by vm.events.collectAsStateWithLifecycle()
 LaunchedEffect(Unit){while(true){delay(15_000);now=System.currentTimeMillis()}}
 val candidates=remember(tasks,projects,minutes,easy,selectedContext,now,events){NowPolicy.candidates(tasks,projects,now,minutes,easy,selectedContext,events)}
 val suggestion=if(prefs.suggestions)candidates.firstOrNull() else null
 val resume=ResumePolicy.latest(tasks,projects,events,now)
 val deck=(listOfNotNull(resume?.task)+(if(prefs.suggestions)candidates.map{it.task} else emptyList())).distinctBy{it.id}.take(5)
 var selectedTaskId by rememberSaveable {mutableStateOf<String?>(null)}
 val deckIndex=deck.indexOfFirst{it.id==selectedTaskId}.coerceAtLeast(0)
 val primary=deck.getOrNull(deckIndex)
 var slideDirection by remember {mutableIntStateOf(1)}
 var dragOffset by remember {mutableFloatStateOf(0f)}
 var dragging by remember {mutableStateOf(false)}
 var moving by remember {mutableStateOf(false)}
 var stackWidth by remember {mutableFloatStateOf(1f)}
 val motion=remember{Animatable(0f)}
 val scope=rememberCoroutineScope()
 val direction=LocalLayoutDirection.current
 val threshold=with(LocalDensity.current){60.dp.toPx()}
 val visualDrag=if(prefs.reducedMotion)0f else if(dragging)dragOffset else motion.value
 val promotion=(kotlin.math.abs(visualDrag)/stackWidth).coerceIn(0f,1f)
 val palette=listOf(Neo.Lime,Neo.Purple,Color(0xFFBDE5EE),Color(0xFFFFD5B5),Color(0xFFF2EB8D))
 fun nextCard(delta:Int){
  if(deck.size<2 || moving)return
  slideDirection=delta
  val target=deck[(deckIndex+delta+deck.size)%deck.size].id
  if(prefs.reducedMotion){selectedTaskId=target;dragOffset=0f;dragging=false;return}
  moving=true
  scope.launch(start=CoroutineStart.UNDISPATCHED){
   try{motion.snapTo(if(dragging)dragOffset else 0f);dragging=false
    motion.animateTo(-delta*(if(direction==LayoutDirection.Ltr)1 else -1)*stackWidth*1.25f,tween(280,easing=FastOutSlowInEasing))
    selectedTaskId=target;motion.snapTo(0f);dragOffset=0f
   }finally{moving=false}
  }
 }
 fun settleCard(){if(moving)return;scope.launch(start=CoroutineStart.UNDISPATCHED){motion.snapTo(dragOffset);dragging=false;motion.animateTo(0f,spring(dampingRatio=.85f,stiffness=500f));dragOffset=0f}}
 LaunchedEffect(primary?.id){if(prefs.suggestions)primary?.let{vm.repo.analytics.record("recommended",it.id)}}
 Page(spacing=14.dp) {
  Column {Text(tr(R.string.home_heading),style=MaterialTheme.typography.headlineLarge);InkUnderline()}
  Text(tr(when{tasks.isEmpty()->R.string.home_empty_intro;deck.size>1->R.string.stack_intro;resume?.task?.id==primary?.id && resume!=null->R.string.home_resume_intro;primary!=null->R.string.home_ready_intro;else->R.string.home_choose_intro}),color=MaterialTheme.colorScheme.onSurfaceVariant)
  if(primary!=null)Box(Modifier.fillMaxWidth().testTag("starterStack").padding(horizontal=8.dp,vertical=14.dp).onSizeChanged{stackWidth=it.width.toFloat().coerceAtLeast(1f)}.pointerInput(deck.map{it.id},deckIndex,direction,prefs.reducedMotion){
   detectHorizontalDragGestures(onDragStart={if(!moving){dragOffset=0f;dragging=true}},onDragCancel={settleCard()},onDragEnd={
    if(!moving){if(kotlin.math.abs(dragOffset)>threshold){val forward=if(direction==LayoutDirection.Ltr)dragOffset<0 else dragOffset>0;nextCard(if(forward)1 else -1)}else settleCard()}
   }){change,amount->if(deck.size>1 && !moving){change.consume();dragOffset=(dragOffset+amount).coerceIn(-stackWidth,stackWidth);slideDirection=if((dragOffset<0)==(direction==LayoutDirection.Ltr))1 else -1}}
  }){
   for(depth in (minOf(deck.size,3)-1) downTo 0){
    val index=(deckIndex+depth*slideDirection+deck.size)%deck.size
    val t=deck[index]
    val active=depth==0
    val effectiveDepth=if(active)0f else depth-promotion
    val angle=if(active)-1.2f else if(depth==1)3.5f else -4.5f
    Box(Modifier.fillMaxWidth().then(if(active)Modifier else Modifier.clearAndSetSemantics{}).graphicsLayer{
     translationX=if(active)visualDrag else effectiveDepth*4.dp.toPx()
     translationY=if(active)0f else effectiveDepth*8.dp.toPx()
     scaleX=if(active)1f else 1f-effectiveDepth*.025f
     scaleY=if(active)1f else 1f-effectiveDepth*.025f
     rotationZ=if(prefs.reducedMotion)0f else if(active)-1.2f+(visualDrag/stackWidth)*10f else angle+(-1.2f-angle)*promotion
    }){
    ActionPanel(fill=palette[index%palette.size]) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
     Text(tr(if(resume?.task?.id==t.id)R.string.resume_title else R.string.neo_next_small),style=MaterialTheme.typography.labelMedium,modifier=Modifier.weight(1f))
     Icon(Icons.Default.NorthEast,null,Modifier.size(24.dp))
    }
    Text(t.title,style=MaterialTheme.typography.headlineMedium)
    if(resume?.task?.id==t.id && resume.note.isNotBlank())Text(tr(R.string.resume_last)+" "+resume.note,style=MaterialTheme.typography.bodyMedium,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    Text(t.nextAction.ifBlank{tr(R.string.ux_start_without_committing_to_finish_2562a)},style=MaterialTheme.typography.bodyMedium)
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     Icon(Icons.Default.Schedule,null,Modifier.size(18.dp))
     Text(t.estimate?.let{"$it "+tr(R.string.minutes)}?:tr(R.string.ux_your_pace_79575),style=MaterialTheme.typography.labelMedium)
     projects.find{it.id==t.projectId}?.let{Text("· "+it.title,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}
    }
    if(events.count{it.taskId==t.id && it.kind=="postponed"}>=3)Text(tr(R.string.ux_this_has_waited_a_few_times_just_the_f_307f9),style=MaterialTheme.typography.bodySmall)
    PrimaryAction(tr(if(resume?.task?.id==t.id)R.string.resume_action else R.string.start),{if(active && !moving)onStart(t)},modifier=if(active)Modifier.testTag("homePrimary") else Modifier,ink=true)
    }
    }
   }
  } else QuietPanel {
    Text(when {
     !prefs.suggestions && tasks.any{it.status !in setOf("DONE","ARCHIVED")}->tr(R.string.home_manual_title)
     tasks.isEmpty()->tr(R.string.home_first_title)
     tasks.all{it.status in setOf("DONE","ARCHIVED")}->tr(R.string.ux_what_was_here_is_behind_you_cf4a3)
     tasks.filter{it.status !in setOf("DONE","ARCHIVED")}.all{it.status=="WAITING"}->tr(R.string.v3_all_waiting)
     else->tr(R.string.no_suggestion)
    },style=MaterialTheme.typography.headlineMedium)
    Text(when {
     !prefs.suggestions && tasks.any{it.status !in setOf("DONE","ARCHIVED")}->tr(R.string.home_manual_body)
     tasks.isEmpty()->tr(R.string.home_first_body)
     tasks.all{it.status in setOf("DONE","ARCHIVED")}->tr(R.string.ux_you_can_stop_here_the_next_step_can_wa_7fd03)
     tasks.any{it.status=="WAITING"} && candidates.isEmpty()->tr(R.string.v3_review_waiting)
     minutes>0 || easy || selectedContext.isNotBlank()->tr(R.string.ux_nothing_matches_this_choice_yet_change_4f14a)
     else->tr(R.string.ux_scheduled_tasks_return_when_you_chose__025ff)
    })
    val add=tasks.none{it.status !in setOf("DONE","ARCHIVED")}
    PrimaryAction(tr(if(tasks.isEmpty())R.string.home_add_first else if(add)R.string.home_add_next else R.string.home_review_tasks),if(add)onQuickCapture else onBrowse,modifier=Modifier.testTag("homePrimary"))
  }
  if(deck.size>1)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
   TextButton({nextCard(-1)},Modifier.testTag("stackPrevious")){Text(tr(R.string.stack_previous))}
   Text(tr(R.string.stack_position).format(deckIndex+1,deck.size),style=MaterialTheme.typography.labelMedium,modifier=Modifier.semantics{liveRegion=LiveRegionMode.Polite})
   TextButton({nextCard(1)},Modifier.testTag("stackNext")){Text(tr(R.string.stack_next))}
  }
  primary?.let{t->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
   TextButton({stuck=t;note=""}){Text(tr(R.string.stuck))}
   TextButton({onLater(t);now=System.currentTimeMillis()}){Text(tr(R.string.later))}
  }}
  if(primary!=null)OutlinedButton(onBrowse,Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(tr(R.string.home_choose_tasks))}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
   if(prefs.suggestions && tasks.isNotEmpty())TextButton({showConditions=true}){Text(tr(R.string.home_adjust))}
   TextButton({showGuide=true},Modifier.testTag("homeGuide")){Icon(Icons.Default.HelpOutline,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text(tr(R.string.home_guide))}
  }
 }
 if(showConditions)ModalBottomSheet(onDismissRequest={showConditions=false}){
  Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Text(tr(R.string.home_adjust),style=MaterialTheme.typography.titleLarge)
   Text(tr(R.string.home_adjust_body),style=MaterialTheme.typography.bodyMedium)
   Choices(listOf(0,2,5,15,30).map{it.toString() to if(it==0)tr(R.string.ux_your_pace_79575) else "$it ${tr(R.string.minutes)}"},minutes.toString(),{minutes=it.toInt()})
   Toggle(tr(R.string.ux_something_easier_2fe57),easy,{easy=it})
   OutlinedTextField(selectedContext,{selectedContext=it},Modifier.fillMaxWidth(),label={Text(tr(R.string.context))})
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton({minutes=0;easy=false;selectedContext=""}){Text(tr(R.string.task_clear_filters))};Button({showConditions=false}){Text(tr(R.string.task_show_results))}}
  }
 }
 if(showGuide)ModalBottomSheet(onDismissRequest={showGuide=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)){
  Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Text(tr(R.string.home_guide),style=MaterialTheme.typography.titleLarge)
   Text(tr(R.string.home_guide_intro))
   OutlinedButton({showGuide=false;onQuickCapture()},Modifier.fillMaxWidth()){Text(tr(R.string.v3_capture))}
   Text(tr(R.string.home_tasks_help));OutlinedButton({showGuide=false;onBrowse()},Modifier.fillMaxWidth()){Text(tr(R.string.tasks))}
   Text(tr(R.string.home_projects_help));OutlinedButton({showGuide=false;onProjects()},Modifier.fillMaxWidth()){Text(tr(R.string.projects))}
   Text(tr(R.string.home_progress_help));OutlinedButton({showGuide=false;onProgress()},Modifier.fillMaxWidth()){Text(tr(R.string.progress))}
  }
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
