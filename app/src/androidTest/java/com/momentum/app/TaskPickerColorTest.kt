package com.momentum.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File
class TaskPickerColorTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val app get()=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MomentumApp).also{check(it.packageName.endsWith(".qa"))}
 @Before fun setup():Unit=runBlocking{app.db.dao().clearTasks();app.db.dao().clearProjects();app.prefs.bool("english",true);app.prefs.bool("onboarded",true);app.prefs.bool("dynamic",false);app.repo.save(Task(title="A small next step"))}
 @Test fun selectedAndUnselectedRowsRemainReadable(){
  for(dark in listOf(false,true)){val c=momentumColors(dark);assertTrue(ColorUtils.calculateContrast(c.onSecondaryContainer.toArgb(),c.secondaryContainer.toArgb())>=4.5);assertTrue(ColorUtils.calculateContrast(c.onSurface.toArgb(),c.surface.toArgb())>=4.5);assertTrue(ColorUtils.calculateContrast(c.onSurfaceVariant.toArgb(),c.surface.toArgb())>=4.5)}
  compose.waitUntil(10000){compose.onAllNodesWithText("Tasks").fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithText("Tasks").performClick();compose.onNodeWithTag("taskStatePicker").performClick()
  compose.mainClock.advanceTimeBy(1000);compose.waitForIdle()
  compose.onNodeWithTag("taskStateList").captureToImage().asAndroidBitmap().let{bitmap->File(app.filesDir,"picker-colors.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}}
  compose.onNodeWithTag("taskStateList").performScrollToNode(hasText("Completed"));compose.onNodeWithText("Completed").performClick();compose.onNodeWithTag("taskStatePicker").assertExists()
 }
}
