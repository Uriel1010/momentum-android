package com.momentum.app
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
object ProjectColors {
 val keys=listOf("LIME","PURPLE","BLUE","PEACH","YELLOW")
 fun color(key:String)=when(key){"LIME"->Neo.Lime;"BLUE"->Color(0xFFBDE5EE);"PEACH"->Color(0xFFFFD5B5);"YELLOW"->Color(0xFFF2EB8D);else->Neo.Purple}
 fun label(key:String)=when(key){"LIME"->R.string.color_lime;"BLUE"->R.string.color_blue;"PEACH"->R.string.color_peach;"YELLOW"->R.string.color_yellow;else->R.string.color_purple}
}
@Composable fun ProjectBadge(project:Project,modifier:Modifier=Modifier){
 Surface(modifier,shape=RoundedCornerShape(8.dp),color=ProjectColors.color(project.color),contentColor=Neo.Ink){Text(project.displayName(),Modifier.padding(horizontal=8.dp,vertical=4.dp),style=MaterialTheme.typography.labelMedium,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}
}
