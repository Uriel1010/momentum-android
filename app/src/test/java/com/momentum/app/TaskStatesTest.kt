package com.momentum.app
import org.junit.Test
import org.junit.Assert.*
class TaskStatesTest {
 @Test fun legacyStatesShareOneFilter(){for(state in listOf("INBOX","READY")){val t=Task(title="Task",status=state);assertTrue(TaskStates.matches(t,"READY",0));assertTrue(TaskStates.matches(t,"open",0));assertEquals("READY",TaskStates.visible(state))}}
 @Test fun unfinishedDoesNotHideWaitingOrScheduled(){for(state in listOf("INBOX","READY","WAITING","IN_PROGRESS"))assertTrue(TaskStates.matches(Task(title="T",status=state,available=99),"open",0));for(state in listOf("DONE","ARCHIVED"))assertFalse(TaskStates.matches(Task(title="T",status=state),"open",0))}
 @Test fun reopenKeepsDetailsAndRemovesCompletionAndDeferral(){val t=Task(title="T",status="DONE",completed=1,available=99,notes="Keep",projectId="project",reminder=200);assertEquals(t.copy(status="READY",completed=null,available=0),TaskStates.reopened(t))}
}
