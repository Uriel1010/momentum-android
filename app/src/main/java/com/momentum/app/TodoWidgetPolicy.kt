package com.momentum.app
object TodoWidgetPolicy {
 const val LIMIT=100
 fun ordered(tasks:List<Task>,projects:List<Project>,now:Long):List<Task> {
  val paused=projects.filter{it.paused}.map{it.id}.toSet()
  fun group(t:Task)=if(t.status=="WAITING" || t.available>now || t.projectId in paused)2 else if(t.status=="IN_PROGRESS")0 else 1
  return tasks.filter{it.status !in setOf("DONE","ARCHIVED")}.sortedWith(compareBy<Task>{group(it)}.thenByDescending{it.priority}.thenBy{it.due?:Long.MAX_VALUE}.thenBy{it.created}.thenBy{it.id})
 }
}
