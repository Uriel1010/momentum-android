package com.momentum.app
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
@Composable fun RecoveryMoment(started:Long,onResume:()->Unit,onFinish:()->Unit) {
 var elapsed by remember{mutableLongStateOf(0)}
 LaunchedEffect(started){while(true){elapsed=((System.currentTimeMillis()-started)/1000).coerceAtLeast(0);delay(1000)}}
 Dialog(onDismissRequest=onResume,properties=DialogProperties(usePlatformDefaultWidth=false)) {
  MaterialTheme(colorScheme=momentumColors(false).copy(background=Neo.Purple,surface=Neo.Purple,onSurface=Neo.Ink)) {
   Surface(Modifier.fillMaxSize(),color=Neo.Purple) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp),verticalArrangement=Arrangement.spacedBy(28.dp),horizontalAlignment=Alignment.CenterHorizontally) {
     Text(tr(R.string.neo_rest),style=MaterialTheme.typography.labelLarge)
     Spacer(Modifier.height(32.dp))
     Icon(Icons.Default.PauseCircleOutline,null,Modifier.size(88.dp),tint=Neo.Ink)
     Text("%02d:%02d".format(elapsed/60,elapsed%60),style=MaterialTheme.typography.displayLarge,color=Neo.Ink)
     Text(tr(R.string.neo_breathe),style=MaterialTheme.typography.headlineLarge,color=Neo.Ink)
     Text(tr(R.string.neo_rest_body),style=MaterialTheme.typography.bodyLarge,color=Neo.Ink,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
     PrimaryAction(tr(R.string.neo_continue),onResume)
     TextButton(onFinish){Text(tr(R.string.neo_finish_today),color=Neo.Ink)}
    }
   }
  }
 }
}
