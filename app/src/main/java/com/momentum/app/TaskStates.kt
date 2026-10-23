package com.momentum.app
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
/** Presentation merges legacy capture/readiness states without rewriting stored records. */
object TaskStates {
 fun visible(status:String)=if(status in setOf("INBOX","READY"))"READY" else status
 fun reopened(task:Task)=task.copy(status="READY",completed=null,available=0)
 fun matches(task:Task,filter:String,now:Long)=when(filter){
  "open"->task.status !in setOf("DONE","ARCHIVED")
  "all"->true
  "scheduled"->task.status !in setOf("DONE","ARCHIVED") && (task.reminder!=null || task.available>now)
  else->visible(task.status)==filter
 }
}
fun stateHelp(status:String)=when(status){
 "READY"->R.string.state_help_todo
 "IN_PROGRESS"->R.string.state_help_doing
 "WAITING"->R.string.state_help_waiting
 "DONE"->R.string.state_help_done
 "ARCHIVED"->R.string.state_help_archived
 "scheduled"->R.string.state_help_scheduled
 "all"->R.string.state_help_all
 else->R.string.state_help_unfinished
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun StateChoices(items:List<Pair<String,String>>,selected:String,onSelect:(String)->Unit){
 FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
  items.forEach{(id,label)->FilterChip(selected=id==selected,onClick={onSelect(id)},label={Text(label)},modifier=Modifier.heightIn(min=48.dp))}
 }
}
