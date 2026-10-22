package com.momentum.app

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

fun developmentIdeasText(ideas:List<DevelopmentIdea>):String = buildString {
 appendLine("Momentum — development suggestions")
 ideas.forEach { appendLine();appendLine("[${it.status}] ${it.title}");if(it.notes.isNotBlank())appendLine(it.notes) }
}

@Composable fun DevelopmentScreen(vm:MomentumViewModel) {
 val ideas by vm.developmentIdeas.collectAsStateWithLifecycle()
 val context=LocalContext.current
 var filter by rememberSaveable {mutableStateOf("ALL")}
 var editing by rememberSaveable {mutableStateOf(false)}
 var id by rememberSaveable {mutableStateOf<String?>(null)}
 var title by rememberSaveable {mutableStateOf("")}
 var notes by rememberSaveable {mutableStateOf("")}
 var status by rememberSaveable {mutableStateOf("IDEA")}
 var deleting by rememberSaveable {mutableStateOf(false)}
 var saving by remember {mutableStateOf(false)}
 val labels=listOf("IDEA" to tr(R.string.dev_idea),"PLANNED" to tr(R.string.dev_planned),"DONE" to tr(R.string.dev_done))
 fun open(idea:DevelopmentIdea?) {id=idea?.id;title=idea?.title.orEmpty();notes=idea?.notes.orEmpty();status=idea?.status?:"IDEA";editing=true}
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(vertical=20.dp)) {
  item {Heading(tr(R.string.dev_title))}
  item {Text(tr(R.string.dev_intro),color=MaterialTheme.colorScheme.onSurfaceVariant)}
  item {Button({open(null)},Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text(tr(R.string.dev_add))}}
  item {Choices(listOf("ALL" to tr(R.string.dev_all))+labels,filter){filter=it}}
  val visible=ideas.filter{filter=="ALL" || it.status==filter}
  if(visible.isEmpty())item {Text(tr(R.string.dev_empty),Modifier.padding(vertical=24.dp))}
  items(visible,key={it.id}) {idea->
   OutlinedCard(onClick={open(idea)},modifier=Modifier.fillMaxWidth().testTag("idea_"+idea.id)) {
    Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Text(labels.first{it.first==idea.status}.second,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
     Text(idea.title,style=MaterialTheme.typography.titleLarge)
     if(idea.notes.isNotBlank())Text(idea.notes,style=MaterialTheme.typography.bodyMedium)
     Text(dateText(idea.updated),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
  }
  if(ideas.isNotEmpty())item {OutlinedButton({context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,developmentIdeasText(ideas))},null))},Modifier.fillMaxWidth()){Icon(Icons.Default.Share,null);Spacer(Modifier.width(8.dp));Text(tr(R.string.dev_share))}}
 }
 if(editing && !deleting)AlertDialog(onDismissRequest={if(!saving)editing=false},title={Text(tr(if(id==null)R.string.dev_add else R.string.dev_edit))},text={
  androidx.compose.foundation.lazy.LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
   item {OutlinedTextField(title,{title=it.take(200)},Modifier.fillMaxWidth().testTag("ideaTitle"),label={Text(tr(R.string.dev_name))},maxLines=3)}
   item {OutlinedTextField(notes,{notes=it.take(10000)},Modifier.fillMaxWidth().testTag("ideaNotes"),label={Text(tr(R.string.dev_notes))},minLines=3,maxLines=6)}
   item {Choices(labels,status){status=it}}
   if(id!=null)item {TextButton({deleting=true},enabled=!saving){Text(tr(R.string.delete))}}
  }
 },confirmButton={TextButton({
  saving=true
  vm.work {try {
   val old=ideas.find{it.id==id}
   vm.repo.saveDevelopmentIdea((old?:DevelopmentIdea(title=title)).copy(title=title,notes=notes,status=status))
   editing=false
  }finally{saving=false}}
 },enabled=title.isNotBlank() && !saving){Text(tr(R.string.save))}},dismissButton={TextButton({editing=false},enabled=!saving){Text(tr(R.string.cancel))}})
 if(deleting)AlertDialog(onDismissRequest={deleting=false},title={Text(tr(R.string.dev_delete))},text={Text(tr(R.string.dev_delete_body))},confirmButton={TextButton({vm.work {id?.let{vm.repo.dao.deleteDevelopmentIdea(it)};deleting=false;editing=false}}){Text(tr(R.string.delete))}},dismissButton={TextButton({deleting=false}){Text(tr(R.string.cancel))}})
}
