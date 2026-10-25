package com.momentum.app
import android.Manifest
import android.app.*
import android.content.*
import android.content.res.Configuration
import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.*
import androidx.compose.ui.semantics.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.*
import androidx.navigation.compose.*
import kotlinx.coroutines.*
import java.util.Locale
import java.text.DateFormat

class MainActivity:ComponentActivity() {
 private var restoringActivity=false
 var captureRequested by mutableStateOf(false)
 var sharedText by mutableStateOf("")
 var focusId by mutableStateOf<String?>(null)
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState);restoringActivity=savedInstanceState!=null;enableEdgeToEdge();if(savedInstanceState==null)readIntent(intent)
  setContent {
   val vm:MomentumViewModel=viewModel(factory=object:ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass:Class<T>):T=MomentumViewModel(application as MomentumApp) as T
   })
   Momentum(vm,sharedText,focusId,{sharedText=""},{focusId=null},captureRequested,{captureRequested=false})
  }
 }
 override fun onStart(){super.onStart();val restoring=restoringActivity;restoringActivity=false;(application as MomentumApp).scope.launch{(application as MomentumApp).repo.analytics.open(restoring=restoring);FocusNotification.refresh(this@MainActivity)}}
 override fun onNewIntent(intent:Intent) {super.onNewIntent(intent);setIntent(intent);readIntent(intent)}
 private fun readIntent(intent:Intent) {
  captureRequested=intent.action=="com.momentum.CAPTURE"
  if(intent.action==Intent.ACTION_SEND && intent.type=="text/plain")sharedText=intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
  focusId=intent.getStringExtra("focusId")?:intent.getStringExtra("projectId")?.let{"project:"+it}
 }
}
val LocalEnglish=staticCompositionLocalOf {false}
@Composable fun tr(id:Int):String {
 val context=LocalContext.current
 val english=LocalEnglish.current
 val configuration=LocalConfiguration.current
 val resources=remember(context,english,configuration) {
  context.createConfigurationContext(Configuration(configuration).apply {setLocale(Locale.forLanguageTag(if(english)"en" else "he"))}).resources
 }
 return resources.getString(id)
}
@Composable fun dateText(time:Long?):String {val locale=Locale.forLanguageTag(if(LocalEnglish.current)"en" else "he");return time?.let {DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT,locale).format(java.util.Date(it))}.orEmpty()}
@Composable fun Momentum(vm:MomentumViewModel,shared:String,focusId:String?,consumeShare:()->Unit,consumeFocus:()->Unit,captureRequested:Boolean=false,consumeCapture:()->Unit={}) {
 val prefs by vm.prefs.collectAsStateWithLifecycle()
 val base=LocalContext.current
 val systemConfig=LocalConfiguration.current
 val localizedConfig=remember(systemConfig,prefs.english) {Configuration(systemConfig).apply {setLocale(Locale.forLanguageTag(if(prefs.english)"en" else "he"))}}
 val localized=remember(base,localizedConfig){android.view.ContextThemeWrapper(base,R.style.Theme_Momentum).apply {applyOverrideConfiguration(localizedConfig)}}
 val dark=isSystemInDarkTheme()
 val brand=momentumColors(dark)
 val colors=if(prefs.dynamic && Build.VERSION.SDK_INT>=31) {if(dark)dynamicDarkColorScheme(base) else dynamicLightColorScheme(base)} else brand

 CompositionLocalProvider(LocalReduceMotion provides prefs.reducedMotion,LocalEnglish provides prefs.english,LocalContext provides localized,LocalConfiguration provides localizedConfig,LocalLayoutDirection provides if(prefs.english)LayoutDirection.Ltr else LayoutDirection.Rtl) {
 MaterialTheme(colorScheme=colors.copy(primary=brand.primary,onPrimary=brand.onPrimary,primaryContainer=Neo.Lime,onPrimaryContainer=Neo.Ink,secondaryContainer=Neo.Purple,onSecondaryContainer=Neo.Ink),typography=MomentumTypography,shapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(16.dp),large=RoundedCornerShape(22.dp),extraLarge=RoundedCornerShape(26.dp))) { Surface(Modifier.fillMaxSize()) { AppContent(vm,shared,focusId,consumeShare,consumeFocus,captureRequested,consumeCapture) } }
 }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AppContent(vm:MomentumViewModel,shared:String,focusId:String?,consumeShare:()->Unit,consumeFocus:()->Unit,captureRequested:Boolean=false,consumeCapture:()->Unit={}) {
 val prefs by vm.prefs.collectAsStateWithLifecycle()
 val tasks by vm.tasks.collectAsStateWithLifecycle()
 val projects by vm.projects.collectAsStateWithLifecycle()
 val events by vm.events.collectAsStateWithLifecycle()
 val loaded by vm.loaded.collectAsStateWithLifecycle()
 val nav=rememberNavController()
 val route=nav.currentBackStackEntryAsState().value?.destination?.route?:"now"
 val snackbar=remember {SnackbarHostState()}
 val scope=rememberCoroutineScope()
 val context=LocalContext.current
 val systemDark=isSystemInDarkTheme()
 SideEffect {
  var host:Context?=context
  while(host is ContextWrapper && host !is Activity)host=host.baseContext
  (host as? Activity)?.let{activity->androidx.core.view.WindowCompat.getInsetsController(activity.window,activity.window.decorView).apply{
   isAppearanceLightStatusBars=!route.startsWith("focus") && !systemDark
   isAppearanceLightNavigationBars=!route.startsWith("focus") && !systemDark
  }}
 }
 var editorId by rememberSaveable {mutableStateOf<String?>(null)}
 var newTask by rememberSaveable {mutableStateOf(false)}
 var initialText by rememberSaveable {mutableStateOf("")}
 var taskProject by rememberSaveable {mutableStateOf<String?>(null)}
 var projectEditor by rememberSaveable {mutableStateOf<String?>(null)}
 var newProject by rememberSaveable {mutableStateOf(false)}
 var restoreConfirm by remember {mutableStateOf(false)}
 val undoLabel=tr(R.string.undo)
 val savedLabel=tr(R.string.saved)
 val deletedLabel=tr(R.string.deleted)
 val completedLabel=tr(R.string.completed_feedback)
 val smallDone=tr(R.string.ux_one_less_thing_on_your_mind_2932f)
 val importantDone=tr(R.string.ux_you_made_room_for_something_that_matte_e8538)
 fun message(id:Int) {scope.launch {snackbar.showSnackbar(context.getString(id))}}
 LaunchedEffect(vm) {vm.messages.collect {message(it)}}
 LaunchedEffect(captureRequested,loaded,prefs.onboarded){if(captureRequested && loaded && prefs.onboarded){initialText="";newTask=true;consumeCapture()}}
 LaunchedEffect(shared) {if(shared.isNotBlank()){initialText=shared;newTask=true;consumeShare()}}
 LaunchedEffect(focusId,loaded) {if(loaded && focusId!=null) {
  when {
   focusId=="list:tasks"->nav.navigate("tasks"){launchSingleTop=true}
   focusId.startsWith("task:")-> {val id=focusId.removePrefix("task:");if(vm.repo.dao.task(id)!=null)nav.navigate("detail/$id"){launchSingleTop=true}}
   else->nav.navigate(if(focusId.startsWith("project:"))"project/"+focusId.removePrefix("project:") else "focus/$focusId")
  };consumeFocus()
 }}
 val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {uri->
  if(uri!=null)vm.work {
   val data=withContext(Dispatchers.IO){Backup.export(vm.repo)}
   withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {it.write(data)}?:error("No output")}
   message(R.string.exported)
  }
 }
 val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->
  if(uri!=null)vm.work {
   withContext(Dispatchers.IO) {
    val bytes=context.contentResolver.openInputStream(uri)?.use { input -> val output=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192); var count=input.read(buffer); while(count!=-1) {require(output.size()+count<=20_000_000); output.write(buffer,0,count); count=input.read(buffer)};output.toByteArray() }?:error("No input")
    require(bytes.size<=20_000_000)
    Backup.restore(vm.repo,bytes.toString(Charsets.UTF_8))
   };message(R.string.restored)
  }
 }
 fun openTask(t:Task){nav.navigate("detail/${t.id}"){launchSingleTop=true}}
 fun edit(t:Task){editorId=t.id;newTask=false}
 fun complete(t:Task,after:()->Unit={}) {
  vm.work {
   vm.repo.complete(t)
   after()
   pulse(context,prefs.feedback,Touch.COMPLETE)
   if(snackbar.showSnackbar(if(t.priority==2)importantDone else if((t.estimate?:99)<=5)smallDone else completedLabel,undoLabel,duration=SnackbarDuration.Short)==SnackbarResult.ActionPerformed)vm.repo.undo(t)
  }
 }

 if(!loaded){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){CircularProgressIndicator();Text(tr(R.string.loading))}};return}
 if(!prefs.onboarded) {
  WelcomeScreen(prefs,onLanguage={vm.pref("english",it)},onSkip={vm.pref("onboarded",true)},onBegin={title->
   vm.work {val task=Task(title=title);vm.repo.save(task);vm.repo.start(task);vm.app.prefs.bool("onboarded",true);nav.navigate("focus/"+task.id)}
  });return
 }

 MaterialTheme(colorScheme=if(route.startsWith("focus"))momentumColors(true) else MaterialTheme.colorScheme) {
 Scaffold(
  topBar={TopAppBar(title={Column {if(!route.startsWith("focus"))Text("MOMENTUM",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);}},
   navigationIcon={if(route !in listOf("now","tasks","projects","progress"))IconButton({nav.popBackStack()}){Icon(Icons.AutoMirrored.Filled.ArrowBack,tr(R.string.back))}},
   actions={if(!route.startsWith("focus"))IconButton({nav.navigate("settings"){launchSingleTop=true}}){Icon(Icons.Default.Tune,tr(R.string.settings))}})},
  bottomBar={if(route in listOf("now","tasks","projects","progress"))Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)){
   TextButton(onClick={initialText="";taskProject=null;newTask=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text(tr(R.string.v3_capture))}
   HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
   Row(Modifier.fillMaxWidth().navigationBarsPadding().selectableGroup()) {
    val destinations=listOf(Triple("now",R.string.now,Icons.Default.Adjust),Triple("tasks",R.string.tasks,Icons.AutoMirrored.Filled.List),Triple("projects",R.string.projects,Icons.Default.FolderOpen),Triple("progress",R.string.progress,Icons.Default.BarChart))
    destinations.forEach{(r,label,icon)->Column(Modifier.weight(1f).heightIn(min=56.dp).selectable(selected=route==r,role=Role.Tab,onClick={nav.navigate(r){popUpTo("now"){saveState=true};launchSingleTop=true;restoreState=true}}).padding(horizontal=4.dp,vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
     Icon(icon,null,Modifier.size(24.dp),tint=if(route==r)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
     Text(tr(label),style=MaterialTheme.typography.labelSmall,maxLines=2,textAlign=androidx.compose.ui.text.style.TextAlign.Center,color=if(route==r)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
     Box(Modifier.width(18.dp).height(3.dp).background(if(route==r)Neo.Lime else Color.Transparent))
    }}
  }}},
  snackbarHost={SnackbarHost(snackbar) {data->AccomplishmentSnackbar(data)}},
  floatingActionButton={}
 ) {padding->
  NavHost(nav,startDestination="now",modifier=Modifier.padding(padding).imePadding(),enterTransition={if(prefs.reducedMotion)androidx.compose.animation.EnterTransition.None else if(targetState.destination.route?.startsWith("focus")==true)androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(240))+androidx.compose.animation.slideInVertically(androidx.compose.animation.core.spring(dampingRatio=.9f)){it/12} else androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220))},exitTransition={if(prefs.reducedMotion)androidx.compose.animation.ExitTransition.None else androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160))}) {
   composable("now") { NowScreen(vm,tasks,projects,prefs,onStart={t->pulse(context,prefs.feedback,Touch.START);vm.work {vm.repo.start(t);nav.navigate("focus/${t.id}")}},onBrowse={nav.navigate("tasks")},onCapture={value->vm.work {
    val t=Task(title=value);vm.repo.save(t);vm.repo.metric("captured",t.id)
    scope.launch {if(snackbar.showSnackbar(savedLabel,undoLabel)==SnackbarResult.ActionPerformed)vm.work{vm.repo.delete(t)}}
   }},onQuickCapture={newTask=true;initialText="";taskProject=null},onProject={nav.navigate("project/"+it.id)},onProjects={nav.navigate("projects")},onProgress={nav.navigate("progress")},onLater={t->vm.work {vm.repo.postpone(t,3_600_000);if(snackbar.showSnackbar(context.getString(R.string.defer_hour),undoLabel)==SnackbarResult.ActionPerformed)vm.repo.undoPostpone(t)}}) }
   composable("tasks") {TaskScreen(tasks,projects,onEdit=::openTask,onStart={pulse(context,prefs.feedback,Touch.START);vm.work{vm.repo.start(it);nav.navigate("focus/${it.id}")}},onComplete={complete(it)},onReopen={task->vm.work{vm.repo.dao.task(task.id)?.let{vm.repo.save(TaskStates.reopened(it))}};message(R.string.state_reopened)})}
   composable("detail/{id}"){entry->tasks.find{it.id==entry.arguments?.getString("id")}?.let{t->TaskDetails(vm,t,projects,onEdit={edit(t)},onStart={vm.work{vm.repo.start(t);nav.navigate("focus/${t.id}")}},onComplete={complete(t)})}}
   composable("focus/{id}") {entry->val task=tasks.find{it.id==entry.arguments?.getString("id")}
    if(task!=null)FocusScreen(vm,task,prefs,onComplete={complete(task){nav.popBackStack()}},onEdit={edit(task)},onExit={nav.popBackStack()})
   }
   composable("projects") {ProjectList(projects,onOpen={nav.navigate("project/${it.id}")},onAdd={newProject=true},tasks=tasks)}
   composable("project/{id}") {entry->projects.find{it.id==entry.arguments?.getString("id")}?.let {p->
    ProjectScreen(vm,p,tasks.filter{it.projectId==p.id},events.filter{it.projectId==p.id},onEdit={projectEditor=p.id},onAdd={taskProject=p.id;initialText="";newTask=true},onTask=::openTask,onStart={pulse(context,prefs.feedback,Touch.START);vm.work{vm.repo.start(it);nav.navigate("focus/${it.id}")}})
   }}
   composable("progress") {ProgressScreen(tasks,projects,events,prefs)}
   composable("settings") {SettingsScreen(vm,prefs,onExport={export.launch("momentum-backup.json")},onRestore={restoreConfirm=true},onSuggestions={nav.navigate("development")})}
   composable("development") {DevelopmentScreen(vm)}
  }
 }
 }
 var pinAfterSave by rememberSaveable{mutableStateOf<String?>(null)}
 val pinPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
  val id=pinAfterSave;pinAfterSave=null
  if(granted && id!=null)vm.work{if(!FocusNotification.pin(context,id))vm.messages.emit(R.string.focus_notification_permission)}
  else message(R.string.focus_notification_permission)
 }
 fun saveCaptured(t:Task,pin:Boolean){vm.work{
  vm.repo.save(t);newTask=false;initialText="";taskProject=null
  nav.navigate("detail/${t.id}"){launchSingleTop=true}
  if(pin){
   if(FocusNotification.canPost(context)){if(!FocusNotification.pin(context,t.id))message(R.string.focus_notification_permission)}
   else if(Build.VERSION.SDK_INT>=33){pinAfterSave=t.id;pinPermission.launch(Manifest.permission.POST_NOTIFICATIONS)}
   else message(R.string.focus_notification_permission)
  }
 }}
 if(newTask)CaptureSheet(initialText,taskProject,projects,onDismiss={newTask=false;initialText="";taskProject=null},onSave={saveCaptured(it,false)},onSavePinned={saveCaptured(it,true)})
 if(editorId!=null) {
  val old=tasks.find{it.id==editorId}
  TaskEditor(old,initialText,taskProject,projects,onDismiss={newTask=false;editorId=null;initialText="";taskProject=null},
   onSave={t->vm.work{vm.repo.save(t);if(old==null)vm.repo.metric("captured",t.id);nav.navigate("detail/${t.id}"){launchSingleTop=true}};newTask=false;editorId=null;initialText="";taskProject=null;message(R.string.saved)},
   onDelete={t->vm.work{vm.repo.delete(t)};newTask=false;editorId=null;scope.launch {if(snackbar.showSnackbar(deletedLabel,undoLabel)==SnackbarResult.ActionPerformed)vm.work{vm.repo.save(t)}}})
 }
 if(newProject || projectEditor!=null)ProjectEditor(projects.find{it.id==projectEditor},onDismiss={newProject=false;projectEditor=null},onSave={p->vm.work{vm.repo.saveProject(p)};newProject=false;projectEditor=null})
 if(restoreConfirm)AlertDialog(onDismissRequest={restoreConfirm=false},title={Text(tr(R.string.restore))},text={Text(tr(R.string.restore_warning))},confirmButton={TextButton({restoreConfirm=false;restore.launch(arrayOf("application/json","text/plain","application/octet-stream"))}){Text(tr(R.string.restore))}},dismissButton={TextButton({restoreConfirm=false}){Text(tr(R.string.cancel))}})
}
@Composable fun Page(spacing:Dp=18.dp,content:@Composable ColumnScope.()->Unit) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=Rhythm.page,vertical=20.dp),verticalArrangement=Arrangement.spacedBy(spacing),content=content)
}
@Composable fun Heading(text:String) {Text(text,modifier=Modifier.semantics{heading()},style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)}
@Composable fun Toggle(label:String,value:Boolean,onChange:(Boolean)->Unit) {
 Row(Modifier.fillMaxWidth().heightIn(min=56.dp).toggleable(value=value,role=Role.Switch,onValueChange=onChange),verticalAlignment=Alignment.CenterVertically) {Text(label,Modifier.weight(1f));Switch(value,null)}
}
@Composable fun Choices(items:List<Pair<String,String>>,selected:String,onSelect:(String)->Unit) {SegmentChoices(items,selected,onSelect)}
