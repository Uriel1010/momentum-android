package com.momentum.app
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
@Composable fun ResumeCard(point:ResumePoint,onResume:()->Unit) {
 OutlinedCard(Modifier.fillMaxWidth().testTag("resumeCard")) {
  Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Eyebrow(tr(R.string.resume_title))
   Text(point.task.title,style=MaterialTheme.typography.titleLarge)
   if(point.note.isNotBlank())Text(tr(R.string.resume_last)+" " +point.note,maxLines=3,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
   Text(tr(R.string.next_action)+": "+point.nextStep.ifBlank{tr(R.string.ux_just_begin_you_can_stop_after_a_moment_60275)})
   Text(dateText(point.lastAt),style=MaterialTheme.typography.labelSmall)
   TextButton(onResume){Text(tr(R.string.resume_action))}
  }
 }
}
