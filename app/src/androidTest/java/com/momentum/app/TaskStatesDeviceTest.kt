package com.momentum.app
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
class TaskStatesDeviceTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa"))}
 @Before fun setup():Unit=runBlocking{app.db.dao().clearTasks();app.db.dao().clearProjects();app.db.dao().clearEvents();app.prefs.bool("english",true);app.prefs.bool("onboarded",true)}
 private fun open(id:String){
  compose.waitUntil(10000){compose.onAllNodesWithText("Tasks").fetchSemanticsNodes().isNotEmpty()}
  compose.onNodeWithText("Tasks").performClick()
  if(id.startsWith("task:")){compose.onNodeWithTag("taskStatePicker").performClick();compose.onNodeWithTag("taskStateList").performScrollToNode(hasText("Archive"));compose.onNodeWithText("Archive").performClick();compose.onNodeWithTag("taskList").performScrollToNode(hasText("Bring this back"));compose.onNodeWithText("Bring this back").performClick()}
 }
 @Test fun completedTaskCanBeReopenedFromList(){
  val task=Task(id="reopen-test",title="Reopen this task",notes="Keep my details",available=System.currentTimeMillis()+86400000)
  runBlocking{app.repo.save(task);app.repo.complete(task)};open("list:tasks")
  compose.onNodeWithTag("taskStatePicker").performClick()
  compose.onNodeWithTag("taskStateList").performScrollToNode(hasText("Completed"));compose.onNodeWithText("Completed").performClick()
  compose.onNodeWithTag("taskList").performScrollToNode(hasTestTag("reopen-reopen-test"));compose.onNodeWithTag("reopen-reopen-test").performClick()
  compose.waitUntil(10000){runBlocking{app.db.dao().task(task.id)?.status=="READY"}}
  runBlocking{val saved=app.db.dao().task(task.id)!!;assertNull(saved.completed);assertEquals(0L,saved.available);assertEquals(task.notes,saved.notes);assertFalse(app.db.dao().allEvents().any{it.taskId==task.id && it.kind=="completed"})}
  compose.onNodeWithTag("taskStatePicker").performClick();compose.onNodeWithText("Unfinished",useUnmergedTree=true).performScrollTo().performClick()
  compose.onNodeWithText(task.title).assertExists()
 }
 @Test fun editorStatusIsVisibleWithoutAdvancedAndArchiveRestores(){
  val task=Task(id="archive-test",title="Bring this back",status="ARCHIVED");runBlocking{app.repo.save(task)};open("task:"+task.id)
  compose.waitUntil(10000){compose.onAllNodesWithTag("restoreTask").fetchSemanticsNodes().isNotEmpty()}
  compose.onNode(hasText("To do") and hasAnyAncestor(isDialog())).assertExists();compose.onNodeWithTag("restoreTask").performScrollTo().performClick()
  compose.waitUntil(10000){runBlocking{app.db.dao().task(task.id)?.status=="READY"}}
 }
}
