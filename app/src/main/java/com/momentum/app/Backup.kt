package com.momentum.app
import org.json.*
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first

object Backup {
 private fun obj(vararg fields:Pair<String,Any?>)=JSONObject().apply { fields.forEach {put(it.first,it.second?:JSONObject.NULL)} }
 private fun <T> array(items:List<T>,f:(T)->JSONObject)=JSONArray().apply {items.forEach {put(f(it))}}
 private fun JSONObject.nullLong(key:String):Long?=if(isNull(key))null else getLong(key)
 private fun JSONObject.nullString(key:String):String?=if(isNull(key))null else getString(key)
 suspend fun export(repo:Repository):String {
  val p=repo.prefs.flow.first()
  val data=repo.db.withTransaction {
   val d=repo.dao
   obj("schema" to 4,
    "developmentIdeas" to array(d.allDevelopmentIdeas()){v->obj("id" to v.id,"title" to v.title,"notes" to v.notes,"status" to v.status,"created" to v.created,"updated" to v.updated,"category" to v.category,"priority" to v.priority,"screenshot" to v.screenshot)},"exportedAt" to System.currentTimeMillis(),
    "tasks" to array(d.allTasks()) {t->obj("id" to t.id,"title" to t.title,"notes" to t.notes,"estimate" to t.estimate,"priority" to t.priority,"status" to t.status,"projectId" to t.projectId,"due" to t.due,"available" to t.available,"contexts" to t.contexts,"nextAction" to t.nextAction,"reminder" to t.reminder,"created" to t.created,"updated" to t.updated,"completed" to t.completed,"lastConsidered" to t.lastConsidered,"repeatMode" to t.repeatMode,"repeatUnit" to t.repeatUnit,"repeatEvery" to t.repeatEvery,"repeatAnchor" to t.repeatAnchor)},
    "projects" to array(d.allProjects()) {v->obj("id" to v.id,"title" to v.title,"description" to v.description,"summary" to v.summary,"nextStep" to v.nextStep,"paused" to v.paused,"emoji" to v.emoji,"review" to v.review,"created" to v.created,"updated" to v.updated)},
    "events" to array(d.allEvents()){e->obj("id" to e.id,"taskId" to e.taskId,"projectId" to e.projectId,"kind" to e.kind,"note" to e.note,"created" to e.created)},
    "deliveries" to array(d.allDeliveries()){v->obj("id" to v.id,"taskId" to v.taskId,"created" to v.created)},
    "suggestions" to array(d.allSuggestions()){v->obj("id" to v.id,"taskId" to v.taskId,"created" to v.created,"outcome" to v.outcome)})
  }
  data.put("preferences",obj("english" to p.english,"onboarded" to p.onboarded,"suggestions" to p.suggestions,"feedback" to p.feedback,"dynamic" to p.dynamic,"notifications" to p.notifications,"quietStart" to p.quietStart,"quietEnd" to p.quietEnd,"dailyCap" to p.dailyCap,"metrics" to p.metrics,"reducedMotion" to p.reducedMotion))
  return data.toString(2).also {require(it.toByteArray(Charsets.UTF_8).size<=20_000_000){"Backup exceeds 20 MB; remove large suggestion screenshots before exporting"}}
 }
 suspend fun restore(repo:Repository,text:String) {
  require(text.length<=20_000_000) {"Backup is too large"}
  val root=JSONObject(text); require(root.getInt("schema") in 1..4) {"Unsupported schema"}
  fun <T> read(key:String,f:(JSONObject)->T):List<T> { val a=root.getJSONArray(key);require(a.length()<=100_000);return (0 until a.length()).map {f(a.getJSONObject(it))} }
  val ideas=if(root.getInt("schema")>=2)read("developmentIdeas"){v->DevelopmentIdea(v.getString("id"),v.getString("title"),v.getString("notes"),v.getString("status"),v.getLong("created"),v.getLong("updated"),v.optString("category","FEATURE"),v.optInt("priority",1),v.optString("screenshot",""))} else emptyList()
  val tasks=read("tasks"){t->Task(t.getString("id"),t.getString("title"),t.getString("notes"),t.nullLong("estimate")?.toInt(),t.getInt("priority"),t.getString("status"),t.nullString("projectId"),t.nullLong("due"),t.getLong("available"),t.getString("contexts"),t.getString("nextAction"),t.nullLong("reminder"),t.getLong("created"),t.getLong("updated"),t.nullLong("completed"),t.getLong("lastConsidered"),t.optString("repeatMode","NONE"),t.optString("repeatUnit","DAY"),t.optInt("repeatEvery",1),t.optLong("repeatAnchor",0))}
  val projects=read("projects"){p->Project(p.getString("id"),p.getString("title"),p.getString("description"),p.getString("summary"),p.getString("nextStep"),p.getBoolean("paused"),p.nullLong("review"),p.getLong("created"),p.getLong("updated"),p.optString("emoji",""))}
  val events=read("events"){e->Event(e.getString("id"),e.nullString("taskId"),e.nullString("projectId"),e.getString("kind"),e.getString("note"),e.getLong("created"))}
  val deliveries=read("deliveries"){d->Delivery(d.getString("id"),d.getString("taskId"),d.getLong("created"))}
  val suggestions=read("suggestions"){v->SuggestionRecord(v.getString("id"),v.getString("taskId"),v.getLong("created"),v.getString("outcome"))}
  fun validIds(ids:List<String>) {require(ids.distinct().size==ids.size);ids.forEach {java.util.UUID.fromString(it)}}
  validIds(ideas.map{it.id})
  ideas.forEach{require(it.title.isNotBlank() && it.title.length<=200 && it.notes.length<=10000 && it.status in setOf("IDEA","PLANNED","DONE") && it.category in setOf("FEATURE","BUG") && it.priority in 0..2 && SuggestionImages.valid(it.screenshot))}
  validIds(tasks.map{it.id});validIds(projects.map{it.id});validIds(events.map{it.id});validIds(suggestions.map{it.id})
  require(deliveries.map{it.id}.distinct().size==deliveries.size)
  tasks.forEach {Recurrence.validate(it);require(it.title.isNotBlank() && it.title.length<=10_000 && it.notes.length<=100_000 && it.priority in 0..2 && (it.estimate==null || it.estimate in 1..10080) && it.status in setOf("INBOX","READY","IN_PROGRESS","WAITING","DONE","ARCHIVED"));require(it.projectId==null || projects.any{p->p.id==it.projectId})}
  projects.forEach {require(it.title.isNotBlank() && it.emoji.length<=32)}
  val p=root.getJSONObject("preferences")
  val pref=Prefs(p.getBoolean("english"),p.getBoolean("onboarded"),p.getBoolean("suggestions"),p.getBoolean("feedback"),p.getBoolean("dynamic"),p.getBoolean("notifications"),p.getInt("quietStart"),p.getInt("quietEnd"),p.getInt("dailyCap"),p.getBoolean("metrics"),p.optBoolean("reducedMotion",false))
  require(pref.quietStart in 0..23 && pref.quietEnd in 0..23 && pref.dailyCap in 1..40)
  // Full replacement only: all rows validate before the first write; Room commits as one transaction.
  val old=repo.dao.allTasks()
  val oldProjects=repo.dao.allProjects()
  repo.db.withTransaction {
   val d=repo.dao;d.clearTasks();d.clearProjects();d.clearEvents();d.clearDeliveries();d.clearSuggestions();if(root.getInt("schema")>=2)d.clearDevelopmentIdeas()
   ideas.forEach{d.saveDevelopmentIdea(it)}
   projects.forEach {d.saveProject(it)};tasks.forEach {d.save(it)};events.forEach {d.event(it)};deliveries.forEach {d.delivery(it)};suggestions.forEach {d.suggestion(it)}
  }
  repo.prefs.restore(pref)
  old.forEach {cancelReminder(repo.context,it.id)}
  oldProjects.forEach{scheduleProject(repo.context,it.copy(paused=true))}
  repo.reschedule()
 }
}
