package com.momentum.app
import androidx.lifecycle.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MomentumViewModel(val app:MomentumApp):ViewModel() {
 var developmentDraftImage by androidx.compose.runtime.mutableStateOf("")
 val repo=app.repo
 val developmentIdeas=repo.dao.developmentIdeas().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 val tasks=repo.dao.tasks().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 val projects=repo.dao.projects().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 val events=repo.dao.events().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 val prefs=app.prefs.flow.stateIn(viewModelScope,SharingStarted.Eagerly,Prefs())
 val loaded=MutableStateFlow(false)
 val messages=MutableSharedFlow<Int>(extraBufferCapacity=10)
 init { work { repo.dao.allTasks();app.prefs.flow.first();loaded.value=true } }
 fun work(block:suspend ()->Unit) {viewModelScope.launch {try {block()} catch(e:Exception) {if(e is CancellationException)throw e;android.util.Log.e("Momentum","Action failed",e);messages.emit(R.string.error)}}}
 fun defer(t:Task,later:Boolean) {work {
  if(later)repo.postpone(t,3_600_000) else {repo.save(t.copy(lastConsidered=System.currentTimeMillis()));repo.metric("dismissed",t.id)}
  if(later)messages.emit(R.string.defer_hour)
 }}
 fun block(t:Task,kind:String) {work {
  repo.block(t,kind)
  messages.emit(if(kind=="waiting")R.string.review_week else if(kind=="later")R.string.defer_day else R.string.defer_hour)
 }}
 fun pref(key:String,v:Boolean) {work {app.prefs.bool(key,v);if(key=="notifications")repo.reschedule()}}
}
