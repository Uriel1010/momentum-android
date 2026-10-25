package com.momentum.app

import android.app.Application
import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

fun uuid(): String = UUID.randomUUID().toString()
@Entity(tableName="tasks")
data class Task(
 @PrimaryKey val id:String=uuid(), val title:String, val notes:String="",
 val estimate:Int?=null, val priority:Int=1, val status:String="INBOX",
 val projectId:String?=null, val due:Long?=null, val available:Long=0,
 val contexts:String="", val nextAction:String="", val reminder:Long?=null,
 val created:Long=System.currentTimeMillis(), val updated:Long=created,
 val completed:Long?=null, val lastConsidered:Long=0,
 @ColumnInfo(defaultValue="'NONE'") val repeatMode:String="NONE",
 @ColumnInfo(defaultValue="'DAY'") val repeatUnit:String="DAY",
 @ColumnInfo(defaultValue="1") val repeatEvery:Int=1,
 @ColumnInfo(defaultValue="0") val repeatAnchor:Long=0
)
@Entity(tableName="projects")
data class Project(@PrimaryKey val id:String=uuid(),val title:String,val description:String="",val summary:String="",val nextStep:String="",val paused:Boolean=false,val review:Long?=null,val created:Long=System.currentTimeMillis(),val updated:Long=created,@ColumnInfo(defaultValue="''") val emoji:String="",@ColumnInfo(defaultValue="'PURPLE'") val color:String="PURPLE")
@Entity(tableName="events")
data class Event(@PrimaryKey val id:String=uuid(),val taskId:String?=null,val projectId:String?=null,val kind:String,val note:String="",val created:Long=System.currentTimeMillis())
@Entity(tableName="deliveries")
data class Delivery(@PrimaryKey val id:String,val taskId:String,val created:Long=System.currentTimeMillis())
@Entity(tableName="suggestions")
data class SuggestionRecord(@PrimaryKey val id:String=uuid(),val taskId:String,val created:Long=System.currentTimeMillis(),val outcome:String="suggested")
@Entity(tableName="development_ideas")
data class DevelopmentIdea(@PrimaryKey val id:String=uuid(),val title:String,val notes:String="",val status:String="IDEA",val created:Long=System.currentTimeMillis(),val updated:Long=created,@ColumnInfo(defaultValue="'FEATURE'") val category:String="FEATURE",@ColumnInfo(defaultValue="1") val priority:Int=1,@ColumnInfo(defaultValue="''") val screenshot:String="")
@Dao
interface MomentumDao {
 @Query("SELECT * FROM development_ideas ORDER BY updated DESC") fun developmentIdeas():Flow<List<DevelopmentIdea>>
 @Query("SELECT * FROM development_ideas ORDER BY updated DESC") suspend fun allDevelopmentIdeas():List<DevelopmentIdea>
 @Upsert suspend fun saveDevelopmentIdea(idea:DevelopmentIdea)
 @Query("DELETE FROM development_ideas WHERE id=:id") suspend fun deleteDevelopmentIdea(id:String)
 @Query("DELETE FROM development_ideas") suspend fun clearDevelopmentIdeas()
 @Query("SELECT * FROM tasks ORDER BY created DESC") fun tasks():Flow<List<Task>>
 @Query("SELECT * FROM projects ORDER BY updated DESC") fun projects():Flow<List<Project>>
 @Query("SELECT * FROM events ORDER BY created DESC") fun events():Flow<List<Event>>
 @Query("SELECT * FROM tasks") suspend fun allTasks():List<Task>
 @Query("SELECT * FROM projects") suspend fun allProjects():List<Project>
 @Query("SELECT * FROM events") suspend fun allEvents():List<Event>
 @Query("SELECT * FROM deliveries") suspend fun allDeliveries():List<Delivery>
 @Query("SELECT * FROM suggestions") suspend fun allSuggestions():List<SuggestionRecord>
 @Query("SELECT * FROM tasks WHERE id=:id") suspend fun task(id:String):Task?
 @Upsert suspend fun save(task:Task)
 @Upsert suspend fun saveProject(project:Project)
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun event(event:Event)
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun delivery(delivery:Delivery):Long
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun suggestion(record:SuggestionRecord)
 @Query("SELECT COUNT(*) FROM deliveries WHERE created>=:start") suspend fun deliveredSince(start:Long):Int
 @Query("SELECT COUNT(*) FROM deliveries WHERE id=:id") suspend fun delivered(id:String):Int
 @Query("DELETE FROM events WHERE id=:id") suspend fun deleteEvent(id:String)
 @Query("DELETE FROM tasks WHERE id=:id") suspend fun delete(id:String)
 @Query("DELETE FROM tasks") suspend fun clearTasks()
 @Query("DELETE FROM projects") suspend fun clearProjects()
 @Query("DELETE FROM events") suspend fun clearEvents()
 @Query("DELETE FROM deliveries") suspend fun clearDeliveries()
 @Query("DELETE FROM events WHERE kind LIKE 'metric_%' AND created<:before") suspend fun pruneMetrics(before:Long)
 @Query("DELETE FROM suggestions") suspend fun clearSuggestions()
 @Query("DELETE FROM events WHERE kind LIKE 'metric_%' OR kind IN ('captured','suggested','dismissed','notification_shown','notification_action')") suspend fun clearMetrics()
}
@Database(entities=[Task::class,Project::class,Event::class,Delivery::class,SuggestionRecord::class,DevelopmentIdea::class],version=6,exportSchema=true)
abstract class MomentumDb:RoomDatabase() {
 abstract fun dao():MomentumDao
 companion object {
  val MIGRATION_5_6=object:Migration(5,6){
   override fun migrate(db:SupportSQLiteDatabase){db.execSQL("ALTER TABLE projects ADD COLUMN color TEXT NOT NULL DEFAULT 'PURPLE'")}
  }
  val MIGRATION_4_5=object:Migration(4,5) {
   override fun migrate(db:SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE tasks ADD COLUMN repeatMode TEXT NOT NULL DEFAULT 'NONE'")
    db.execSQL("ALTER TABLE tasks ADD COLUMN repeatUnit TEXT NOT NULL DEFAULT 'DAY'")
    db.execSQL("ALTER TABLE tasks ADD COLUMN repeatEvery INTEGER NOT NULL DEFAULT 1")
    db.execSQL("ALTER TABLE tasks ADD COLUMN repeatAnchor INTEGER NOT NULL DEFAULT 0")
    db.execSQL("ALTER TABLE projects ADD COLUMN emoji TEXT NOT NULL DEFAULT ''")
   }
  }
  val MIGRATION_3_4=object:Migration(3,4) {
   override fun migrate(db:SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE development_ideas ADD COLUMN category TEXT NOT NULL DEFAULT 'FEATURE'")
    db.execSQL("ALTER TABLE development_ideas ADD COLUMN priority INTEGER NOT NULL DEFAULT 1")
    db.execSQL("ALTER TABLE development_ideas ADD COLUMN screenshot TEXT NOT NULL DEFAULT ''")
   }
  }
  val MIGRATION_2_3=object:Migration(2,3) {
   override fun migrate(db:SupportSQLiteDatabase) {
    db.execSQL("CREATE TABLE IF NOT EXISTS development_ideas (id TEXT NOT NULL, title TEXT NOT NULL, notes TEXT NOT NULL, status TEXT NOT NULL, created INTEGER NOT NULL, updated INTEGER NOT NULL, PRIMARY KEY(id))")
   }
  }
  val MIGRATION_1_2=object:Migration(1,2) {
   override fun migrate(db:SupportSQLiteDatabase) {
    db.execSQL("CREATE TABLE IF NOT EXISTS suggestions (id TEXT NOT NULL, taskId TEXT NOT NULL, created INTEGER NOT NULL, outcome TEXT NOT NULL, PRIMARY KEY(id))")
   }
  }
 }
}
private val Context.preferences by preferencesDataStore("momentum")
data class Prefs(val english:Boolean=false,val onboarded:Boolean=false,val suggestions:Boolean=true,val feedback:Boolean=true,val dynamic:Boolean=false,val notifications:Boolean=true,val quietStart:Int=22,val quietEnd:Int=8,val dailyCap:Int=3,val metrics:Boolean=false,val reducedMotion:Boolean=false)
class Preferences(private val context:Context) {
 val flow=context.preferences.data.map { p -> Prefs(p[booleanPreferencesKey("english")]?:false,p[booleanPreferencesKey("onboarded")]?:false,p[booleanPreferencesKey("suggestions")]?:true,p[booleanPreferencesKey("feedback")]?:true,p[booleanPreferencesKey("dynamic")]?:false,p[booleanPreferencesKey("notifications")]?:true,p[intPreferencesKey("quietStart")]?:22,p[intPreferencesKey("quietEnd")]?:8,p[intPreferencesKey("dailyCap")]?:3,p[booleanPreferencesKey("metrics")]?:false,p[booleanPreferencesKey("reducedMotion")]?:false) }
 suspend fun bool(key:String,value:Boolean) { context.preferences.edit { it[booleanPreferencesKey(key)]=value } }
 suspend fun number(key:String,value:Int) { context.preferences.edit { it[intPreferencesKey(key)]=value } }
 suspend fun restore(p:Prefs) { context.preferences.edit {
  it[booleanPreferencesKey("english")]=p.english; it[booleanPreferencesKey("onboarded")]=p.onboarded
  it[booleanPreferencesKey("suggestions")]=p.suggestions; it[booleanPreferencesKey("feedback")]=p.feedback
  it[booleanPreferencesKey("dynamic")]=p.dynamic; it[booleanPreferencesKey("notifications")]=p.notifications
  it[booleanPreferencesKey("metrics")]=p.metrics;it[booleanPreferencesKey("reducedMotion")]=p.reducedMotion
  it[intPreferencesKey("quietStart")]=p.quietStart; it[intPreferencesKey("quietEnd")]=p.quietEnd; it[intPreferencesKey("dailyCap")]=p.dailyCap
 } }
}
class MomentumApp:Application(), androidx.work.Configuration.Provider {
 val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 val db by lazy { Room.databaseBuilder(this,MomentumDb::class.java,"momentum.db").addMigrations(MomentumDb.MIGRATION_1_2,MomentumDb.MIGRATION_2_3,MomentumDb.MIGRATION_3_4,MomentumDb.MIGRATION_4_5,MomentumDb.MIGRATION_5_6).build() }
 val prefs by lazy { Preferences(this) }
 val repo by lazy { Repository(this,db,prefs) }
 override val workManagerConfiguration get()=androidx.work.Configuration.Builder().setMinimumLoggingLevel(android.util.Log.WARN).build()
 override fun onCreate() { super.onCreate(); scope.launch { repo.reschedule() }
  scope.launch {
   combine(db.dao().tasks(),db.dao().projects(),db.dao().events(),prefs.flow){_,_,_,_->Unit}.conflate().collect {
    try { MomentumWidget.updateAll(this@MomentumApp);CaptureWidget.updateAll(this@MomentumApp);TodoListWidget.updateAll(this@MomentumApp) } catch(_:Exception) { /* Widget can retry at the next update. */ }
    try { FocusNotification.refresh(this@MomentumApp) } catch(_:Exception) { /* Retry on next task change or launch. */ }
   }
  }
 }
}
class Repository(val context:Context,val db:MomentumDb,val prefs:Preferences) {
 val dao=db.dao()
 val analytics=LocalMetrics(dao,prefs)
 suspend fun saveDevelopmentIdea(idea:DevelopmentIdea) {
  require(idea.title.isNotBlank() && idea.title.length<=200 && idea.notes.length<=10000)
  require(idea.status in setOf("IDEA","PLANNED","DONE"))
  require(idea.category in setOf("FEATURE","BUG") && idea.priority in 0..2)
  require(SuggestionImages.valid(idea.screenshot))
  dao.saveDevelopmentIdea(idea.copy(title=idea.title.trim(),notes=idea.notes.trim(),updated=System.currentTimeMillis()))
 }
 suspend fun metric(kind:String,id:String?=null) { if(prefs.flow.first().metrics) dao.event(Event(taskId=id,kind=kind)) }
 suspend fun save(t:Task) {
  require(t.title.isNotBlank());Recurrence.validate(t)
  val now=System.currentTimeMillis()
  val saved=t.copy(title=t.title.trim(),updated=now,completed=if(t.status=="DONE")t.completed?:now else null)
  var next:Task?=null
  var removed:String?=null
  db.withTransaction {
   val before=dao.task(t.id)
   dao.save(saved)
   if(before==null)analytics.record("created",t.id)
   if(saved.status=="DONE" && before?.status!="DONE"){
    dao.event(Event(taskId=t.id,projectId=t.projectId,kind="completed",note=t.title,created=saved.completed!!))
    analytics.record("completed",t.id)
    val candidate=Recurrence.next(saved,now)
    if(candidate!=null && dao.task(candidate.id)==null){dao.save(candidate);next=candidate}
   }
   if(saved.status=="IN_PROGRESS" && before?.status!="IN_PROGRESS"){
    dao.event(Event(taskId=t.id,projectId=t.projectId,kind="started",note=t.title))
    analytics.record("started",t.id)
   }
   if(saved.status!="DONE" && before?.status=="DONE") {
    val child=dao.task(Recurrence.childId(t.id))
    if(child!=null && child.status=="READY" && child.updated==child.created && dao.allEvents().none{it.taskId==child.id}){dao.delete(child.id);removed=child.id}
   }
   if(saved.status!="DONE" && before?.status=="DONE")dao.allEvents().filter{it.taskId==t.id && it.kind in setOf("completed","metric_completed") && it.created>=(before.completed?:Long.MAX_VALUE)}.forEach{dao.deleteEvent(it.id)}
  }
  schedule(context,saved);next?.let{schedule(context,it)};removed?.let{cancelReminder(context,it)}
 }
 suspend fun complete(t:Task) { db.withTransaction {
  val current=dao.task(t.id)?:return@withTransaction
  if(current.status!="DONE")save(current.copy(status="DONE"))
 } }
 suspend fun undo(t:Task) { save(t) }
 suspend fun delete(t:Task) { dao.delete(t.id); cancelReminder(context,t.id) }
 suspend fun start(t:Task) {db.withTransaction {
  val current=dao.task(t.id)?:return@withTransaction
  if(current.status in setOf("DONE","ARCHIVED","WAITING"))return@withTransaction
  if(current.status!="IN_PROGRESS") {
   save(current.copy(status="IN_PROGRESS"))
  }
  t.projectId?.let{id->dao.allProjects().find{it.id==id}?.let{p->
   if(p.paused || System.currentTimeMillis()-p.updated>=7*86_400_000L)resumeProject(p)
  }}
 }}
 suspend fun resumeProject(p:Project) {
  saveProject(p.copy(paused=false,updated=System.currentTimeMillis()))
  dao.event(Event(projectId=p.id,kind="resumed",note=p.title))
  analytics.record("resumed")
 }
 suspend fun saveProject(p:Project){require(p.title.isNotBlank() && p.emoji.length<=32 && p.color in ProjectColors.keys);dao.saveProject(p);scheduleProject(context,p)}

 suspend fun advance(t:Task,note:String) { require(note.isNotBlank());db.withTransaction {
  analytics.record("progress",t.id)
  dao.event(Event(taskId=t.id,projectId=t.projectId,kind="advanced",note=note))
  t.projectId?.let { id -> dao.allProjects().find { it.id==id }?.let { dao.saveProject(it.copy(summary=note,nextStep=t.nextAction,updated=System.currentTimeMillis())) } }
 } }
 suspend fun postpone(t:Task,delay:Long) {
  save(t.copy(status="READY",available=System.currentTimeMillis()+delay))
  dao.event(Event(taskId=t.id,projectId=t.projectId,kind="postponed"))
  analytics.record("postponed",t.id)
 }
 suspend fun undoPostpone(t:Task) {
  db.withTransaction {
   val current=dao.task(t.id)?:return@withTransaction
   dao.save(t)
   dao.allEvents().filter{it.taskId==t.id && it.kind in setOf("postponed","metric_postponed") && it.created>=current.updated}.forEach{dao.deleteEvent(it.id)}
  }
  schedule(context,t)
 }
 suspend fun block(t:Task,kind:String,note:String="") {
  if(kind=="waiting") {val at=System.currentTimeMillis()+7*86_400_000L;save(t.copy(status="WAITING",available=at,reminder=at))}
  else postpone(t,if(kind=="later")86_400_000 else 3_600_000)
  dao.event(Event(taskId=t.id,projectId=t.projectId,kind="blocked",note=note.ifBlank{kind}))
  analytics.record("blocked",t.id)
 }
 suspend fun milestone(p:Project,note:String) {require(note.isNotBlank());db.withTransaction {
  dao.event(Event(projectId=p.id,kind="milestone",note=note.trim()))
  dao.saveProject(p.copy(summary=note.trim(),updated=System.currentTimeMillis()))
 }}
 suspend fun reschedule() { dao.allTasks().forEach { schedule(context,it) };dao.allProjects().forEach{scheduleProject(context,it)} }
}
