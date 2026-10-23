package com.momentum.app
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

/** Short, dismissible feedback; no modal reward screen or delayed next action. */
@Composable fun AccomplishmentSnackbar(data:SnackbarData) {
 var entered by remember(data){mutableStateOf(false)}
 LaunchedEffect(data){entered=true}
 val scale by animateFloatAsState(if(entered)1f else .7f,if(LocalReduceMotion.current)snap() else spring(dampingRatio=.7f),label="step acknowledged")
 Snackbar(modifier=Modifier.padding(12.dp).semantics{liveRegion=LiveRegionMode.Polite},
  action={data.visuals.actionLabel?.let{TextButton({data.performAction()}){Text(it,color=MaterialTheme.colorScheme.onSecondaryContainer)}}},
  dismissAction={val dismiss=tr(R.string.ux_dismiss_70afe);IconButton({data.dismiss()}){Text("×",Modifier.semantics{contentDescription=dismiss},color=MaterialTheme.colorScheme.onSecondaryContainer)}},
  shape=Rhythm.card,containerColor=MaterialTheme.colorScheme.secondaryContainer,contentColor=MaterialTheme.colorScheme.onSecondaryContainer) {
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
   Icon(if(data.visuals.message==tr(R.string.error))Icons.Default.Info else Icons.Default.Check,null,Modifier.size(24.dp).graphicsLayer{scaleX=scale;scaleY=scale})
   Text(data.visuals.message,style=MaterialTheme.typography.bodyMedium)
  }
 }
}
