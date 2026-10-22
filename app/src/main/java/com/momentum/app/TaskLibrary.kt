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
@Composable fun TaskScreen(tasks:List<Task>,projects:List<Project>,onEdit:(Task)->Unit,onStart:(Task)->Unit,onComplete:(Task)->Unit) {
 val keyboard=LocalSoftwareKeyboardController.current
 var search by rememberSaveable {mutableStateOf("")}
 var filter by rememberSaveable {mutableStateOf("open")}
 var project by rememberSaveable {mutableStateOf("")}
 var duration by rememberSaveable {mutableIntStateOf(0)}
 var context by rememberSaveable {mutableStateOf("")}
 var filters by rememberSaveable {mutableStateOf(false)}
 val filtered=remember(tasks,search,filter,project,duration,context) {tasks.filter{
  (it.title.contains(search,true)||it.notes.contains(search,true)) &&
  when(filter){"all"->true;"open"->it.status !in setOf("DONE","ARCHIVED");"scheduled"->(it.reminder!=null || it.available>System.currentTimeMillis()) && it.status !in setOf("DONE","ARCHIVED");else->it.status==filter} &&
  (project.isEmpty()||it.projectId==project) && (duration==0||it.estimate?.let{n->n<=duration}==true) &&
  (context.isBlank()||it.contexts.contains(context,true))
 }.sortedWith(compareBy<Task>{if(it.status=="IN_PROGRESS")0 else if(it.priority==2)1 else 2}.thenByDescending{it.created})}
 LazyColumn(Modifier.fillMaxSize().testTag("taskList"),contentPadding=PaddingValues(start=24.dp,end=24.dp,top=20.dp,bottom=100.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  item {Heading(tr(R.string.tasks))}
  item {Text(tr(R.string.ux_everything_here_one_step_at_a_time_4b7da),color=MaterialTheme.colorScheme.onSurfaceVariant)}
  item {OutlinedTextField(search,{search=it},Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(imeAction=androidx.compose.ui.text.input.ImeAction.Search),keyboardActions=androidx.compose.foundation.text.KeyboardActions(onSearch={keyboard?.hide()}),shape=RoundedCornerShape(18.dp),label={Text(tr(R.string.search))},leadingIcon={Icon(Icons.Default.Search,null)},trailingIcon={IconButton({filters=!filters}){Icon(Icons.Default.Tune,tr(R.string.conditions))}})}
  item {Choices(listOf("open" to tr(R.string.open),"IN_PROGRESS" to tr(R.string.task_in_progress),"WAITING" to tr(R.string.waiting),"DONE" to tr(R.string.done),"scheduled" to tr(R.string.scheduled),"all" to tr(R.string.all),"ARCHIVED" to tr(R.string.archived)),filter,{filter=it})}
  if(filters)item {QuietPanel {
   Text(tr(R.string.select_project))
   Choices(listOf("" to tr(R.string.all))+projects.map{it.id to it.title},project,{project=it})
   Text(tr(R.string.max_duration))
   Choices(listOf(0,5,15,30).map{it.toString() to if(it==0)tr(R.string.any) else "$it ${tr(R.string.minutes)}"},duration.toString(),{duration=it.toInt()})
   OutlinedTextField(context,{context=it},Modifier.fillMaxWidth(),label={Text(tr(R.string.context))})
  }}
  item {Text("${filtered.size} · ${tr(if(filter=="open")R.string.open else R.string.tasks)}",Modifier.padding(vertical=12.dp),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)}
  if(filtered.isEmpty())item {QuietPanel {Text(tr(R.string.ux_a_clear_space_281e6),style=MaterialTheme.typography.headlineMedium);Text(if(search.isNotBlank())tr(R.string.ux_no_matches_try_another_word_6c33c) else tr(R.string.empty_tasks))}}
  itemsIndexed(filtered,key={_,t->t.id}) {index,t->
   val group=if(t.status=="IN_PROGRESS")R.string.task_in_progress else if(t.priority==2)R.string.high else R.string.tasks
   val previous=filtered.getOrNull(index-1)
   val previousGroup=if(previous?.status=="IN_PROGRESS")R.string.task_in_progress else if(previous?.priority==2)R.string.high else R.string.tasks
   if(index==0 || previousGroup!=group)Eyebrow(tr(group))
   val active=t.status !in setOf("DONE","ARCHIVED")
   val dismiss=rememberSwipeToDismissBoxState(confirmValueChange={value->if(value!=SwipeToDismissBoxValue.Settled && active){onComplete(t)};false})
   SwipeToDismissBox(state=dismiss,modifier=if(LocalReduceMotion.current)Modifier else Modifier.animateItem(),enableDismissFromStartToEnd=active,enableDismissFromEndToStart=false,
    backgroundContent={Surface(Modifier.fillMaxSize(),shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.primaryContainer){Box(Modifier.padding(20.dp),contentAlignment=Alignment.CenterStart){Icon(Icons.Default.Check,tr(R.string.finish))}}}) {
    Surface(onClick={onEdit(t)},shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surfaceContainerLow) {
     Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
      IconButton({if(active)onComplete(t) else onEdit(t)},Modifier.semantics{contentDescription=t.title}){Icon(if(t.status=="DONE")Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,tr(R.string.finish),tint=MaterialTheme.colorScheme.primary)}
      Column(Modifier.weight(1f).padding(vertical=12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
       Text(t.title,style=MaterialTheme.typography.titleMedium,maxLines=3,overflow=TextOverflow.Ellipsis)
       val meta=listOfNotNull(projects.find{it.id==t.projectId}?.title,t.estimate?.let{"$it ${tr(R.string.minutes)}"})
       if(meta.isNotEmpty())Text(meta.joinToString(" · "),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
       if(t.status=="WAITING")Text(tr(R.string.waiting),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.tertiary)
      }
      if(active && t.status!="WAITING")IconButton({onStart(t)}){Icon(Icons.Default.PlayArrow,tr(R.string.start),tint=MaterialTheme.colorScheme.primary)}
     }
    }
   }
  }
 }
}
