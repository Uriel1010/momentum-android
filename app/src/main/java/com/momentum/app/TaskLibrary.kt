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
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TaskScreen(tasks:List<Task>,projects:List<Project>,onEdit:(Task)->Unit,onStart:(Task)->Unit,onComplete:(Task)->Unit,onReopen:(Task)->Unit) {
 val keyboard=LocalSoftwareKeyboardController.current
 var search by rememberSaveable {mutableStateOf("")}
 var filter by rememberSaveable {mutableStateOf("open")}
 var project by rememberSaveable {mutableStateOf("personal")}
 var duration by rememberSaveable {mutableIntStateOf(0)}
 var context by rememberSaveable {mutableStateOf("")}
 var filters by rememberSaveable {mutableStateOf(false)}
 var searchOpen by rememberSaveable {mutableStateOf(false)}
 var statesOpen by rememberSaveable {mutableStateOf(false)}
 val states=listOf("open" to tr(R.string.state_unfinished),"READY" to tr(R.string.state_todo),"IN_PROGRESS" to tr(R.string.task_in_progress),"WAITING" to tr(R.string.waiting),"DONE" to tr(R.string.done),"scheduled" to tr(R.string.scheduled),"all" to tr(R.string.all),"ARCHIVED" to tr(R.string.archived))
 val extraCount=listOf(project!="personal",duration!=0,context.isNotBlank()).count{it}
 val filtered=remember(tasks,search,filter,project,duration,context) {tasks.filter{
  (it.title.contains(search,true)||it.notes.contains(search,true)) &&
  TaskStates.matches(it,filter,System.currentTimeMillis()) &&
  (project.isEmpty() || (project=="personal" && it.projectId==null) || it.projectId==project) && (duration==0||it.estimate?.let{n->n<=duration}==true) &&
  (context.isBlank()||it.contexts.contains(context,true))
 }.sortedWith(compareBy<Task>{if(it.status=="IN_PROGRESS")0 else if(it.priority==2)1 else 2}.thenByDescending{it.created})}
 if(statesOpen)ModalBottomSheet(onDismissRequest={statesOpen=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)){
  LazyColumn(Modifier.fillMaxWidth().testTag("taskStateList"),contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=32.dp)){
   item {Text(tr(R.string.task_view),style=MaterialTheme.typography.titleLarge);Text(tr(R.string.state_places),Modifier.padding(vertical=8.dp),style=MaterialTheme.typography.bodySmall)}
   items(states,key={it.first}){(id,label)->
    val selected=filter==id
    val colors=MaterialTheme.colorScheme
    val foreground=if(selected)colors.onSecondaryContainer else colors.onSurface
    val supporting=if(selected)colors.onSecondaryContainer else colors.onSurfaceVariant
    Surface(onClick={filter=id;statesOpen=false},shape=RoundedCornerShape(12.dp),color=if(selected)colors.secondaryContainer else colors.surface,contentColor=foreground){
     ListItem(headlineContent={Text(label)},supportingContent={Text(tr(stateHelp(id)))},trailingContent={if(filter==id)Icon(Icons.Default.Check,null)},colors=ListItemDefaults.colors(containerColor=androidx.compose.ui.graphics.Color.Transparent,headlineColor=foreground,supportingColor=supporting,trailingIconColor=foreground))
    }
   }
  }
 }
 if(filters)ModalBottomSheet(onDismissRequest={filters=false}){
  Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Text(tr(R.string.task_filters),style=MaterialTheme.typography.titleLarge)
   Text(tr(R.string.select_project))
   Text(tr(R.string.scope_help),style=MaterialTheme.typography.bodySmall)
   Choices(listOf("personal" to tr(R.string.personal_tasks),"" to tr(R.string.all_tasks_scope))+projects.map{it.id to it.displayName()},project,{project=it})
   Text(tr(R.string.max_duration))
   Choices(listOf(0,5,15,30,60).map{it.toString() to if(it==0)tr(R.string.any) else "$it ${tr(R.string.minutes)}"},duration.toString(),{duration=it.toInt()})
   OutlinedTextField(context,{context=it},Modifier.fillMaxWidth(),label={Text(tr(R.string.context))})
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton({project="personal";duration=0;context=""}){Text(tr(R.string.task_clear_filters))};Button({filters=false;keyboard?.hide()}){Text(tr(R.string.task_show_results))}}
  }
 }
 LazyColumn(Modifier.fillMaxSize().testTag("taskList"),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=4.dp,bottom=16.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
  item {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
    Text(tr(R.string.tasks),style=MaterialTheme.typography.titleLarge)
    Text(" · ${filtered.size}",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    IconButton({searchOpen=!searchOpen;if(!searchOpen){search="";keyboard?.hide()}},Modifier.testTag("taskSearchToggle")){Icon(if(searchOpen)Icons.Default.Close else Icons.Default.Search,tr(R.string.search))}
    IconButton({filters=true},Modifier.testTag("taskFilters")){BadgedBox(badge={if(extraCount>0)Badge{Text(extraCount.toString())}}){Icon(Icons.Default.Tune,tr(R.string.task_filters))}}
   }
  }
  if(searchOpen)item {OutlinedTextField(search,{search=it},Modifier.fillMaxWidth().testTag("taskSearch"),singleLine=true,keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(imeAction=androidx.compose.ui.text.input.ImeAction.Search),keyboardActions=androidx.compose.foundation.text.KeyboardActions(onSearch={keyboard?.hide()}),shape=RoundedCornerShape(12.dp),label={Text(tr(R.string.search))})}
  item {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
    FilledTonalButton({statesOpen=true},Modifier.testTag("taskStatePicker")){Text(states.first{it.first==filter}.second);Spacer(Modifier.width(8.dp));Icon(Icons.Default.ExpandMore,null)}
    TextButton({filters=true},Modifier.testTag("taskScope")){Text(if(project=="personal")tr(R.string.personal_tasks) else if(project.isEmpty())tr(R.string.all_tasks_scope) else projects.find{it.id==project}?.displayName()?:tr(R.string.project),maxLines=1)}
   }
  }
  if(filtered.isEmpty())item {QuietPanel {Text(tr(R.string.ux_a_clear_space_281e6),style=MaterialTheme.typography.headlineMedium);Text(if(search.isNotBlank())tr(R.string.ux_no_matches_try_another_word_6c33c) else tr(R.string.empty_tasks))}}
  items(filtered,key={it.id}) {t->
   val active=t.status !in setOf("DONE","ARCHIVED")
   val dismiss=rememberSwipeToDismissBoxState(confirmValueChange={value->if(value!=SwipeToDismissBoxValue.Settled && active){onComplete(t)};false})
   SwipeToDismissBox(state=dismiss,modifier=if(LocalReduceMotion.current)Modifier else Modifier.animateItem(),enableDismissFromStartToEnd=active,enableDismissFromEndToStart=false,
    backgroundContent={Surface(Modifier.fillMaxSize(),shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primaryContainer){Box(Modifier.padding(20.dp),contentAlignment=Alignment.CenterStart){Icon(Icons.Default.Check,tr(R.string.finish))}}}) {
    Surface(onClick={onEdit(t)},modifier=Modifier.testTag("taskRow-"+t.id),shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceContainerLow) {
     Row(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=0.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
      IconButton({if(active)onComplete(t) else onReopen(t)},Modifier.semantics{contentDescription=t.title}){Icon(if(t.status=="DONE")Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,tr(if(active)R.string.finish else R.string.state_reopen),tint=MaterialTheme.colorScheme.primary)}
      Column(Modifier.weight(1f).padding(vertical=2.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
       Text(t.title,style=MaterialTheme.typography.bodyLarge,maxLines=2,overflow=TextOverflow.Ellipsis)
       val meta=listOfNotNull(tr(statusLabel(t.status)),projects.find{it.id==t.projectId}?.displayName(),t.estimate?.let{"$it ${tr(R.string.minutes)}"})
       if(meta.isNotEmpty())Text(meta.joinToString(" · "),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
       if(!active)TextButton({onReopen(t)},Modifier.testTag("reopen-"+t.id)){Text(tr(if(t.status=="DONE")R.string.state_reopen else R.string.state_restore))}
      }
      if(active && t.status!="WAITING")IconButton({onStart(t)}){Icon(Icons.Default.PlayArrow,tr(R.string.start),tint=MaterialTheme.colorScheme.primary)}
     }
    }
   }
  }
 }
}
