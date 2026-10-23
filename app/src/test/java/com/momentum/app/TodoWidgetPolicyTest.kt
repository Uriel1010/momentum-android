package com.momentum.app
import org.junit.Test
import org.junit.Assert.*
class TodoWidgetPolicyTest {
 @Test fun retainsBlockedTasksButExcludesFinished(){val p=Project(paused=true,title="Paused");val tasks=listOf(Task(id="done",title="Done",status="DONE"),Task(id="archive",title="Archive",status="ARCHIVED"),Task(id="wait",title="Waiting",status="WAITING"),Task(id="future",title="Future",available=200),Task(id="paused",title="Paused",projectId=p.id),Task(id="active",title="Active",status="IN_PROGRESS"),Task(id="inbox",title="Inbox"));val result=TodoWidgetPolicy.ordered(tasks,listOf(p),100);assertEquals(5,result.size);assertEquals("active",result[0].id);assertEquals("inbox",result[1].id)}
 @Test fun orderingIsStableAndPrioritizesDueDates(){val tasks=listOf(Task(id="b",title="B",created=1,due=100),Task(id="a",title="A",created=1,due=100),Task(id="c",title="C",created=1,due=90));assertEquals(listOf("c","a","b"),TodoWidgetPolicy.ordered(tasks,emptyList(),0).map{it.id});assertEquals(TodoWidgetPolicy.ordered(tasks,emptyList(),0),TodoWidgetPolicy.ordered(tasks.reversed(),emptyList(),0))}
}
