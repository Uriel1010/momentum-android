package com.momentum.app

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.*
import androidx.compose.ui.draw.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*

object Neo {
 val Lime=Color(0xFFD7FF3F);val Purple=Color(0xFFC9B8FF);val Paper=Color(0xFFFAF7F2)
 val Ink=Color(0xFF0F0F10);val Concrete=Color(0xFFE6E6E6)
}
object Rhythm {val page=24.dp;val gap=16.dp;val card=RoundedCornerShape(20.dp);const val transition=220}
val LocalReduceMotion=staticCompositionLocalOf {false}
val MomentumFont=FontFamily(Font(R.font.heebo_regular,FontWeight.Normal),Font(R.font.heebo_medium,FontWeight.Medium),Font(R.font.heebo_bold,FontWeight.Bold),Font(R.font.heebo_black,FontWeight.Black))
private fun type(size:Int,line:Int,weight:FontWeight=FontWeight.Normal)=TextStyle(fontFamily=MomentumFont,fontSize=size.sp,lineHeight=line.sp,fontWeight=weight)
val MomentumTypography=Typography(
 displayLarge=type(60,66,FontWeight.Black),displayMedium=type(48,54,FontWeight.Black),displaySmall=type(38,44,FontWeight.Black),
 headlineLarge=type(34,40,FontWeight.Black),headlineMedium=type(28,34,FontWeight.Black),headlineSmall=type(24,30,FontWeight.Bold),
 titleLarge=type(22,28,FontWeight.Bold),titleMedium=type(18,25,FontWeight.Bold),titleSmall=type(16,22,FontWeight.Bold),
 bodyLarge=type(17,26),bodyMedium=type(15,23),bodySmall=type(14,21),
 labelLarge=type(15,22,FontWeight.Bold),labelMedium=type(13,19,FontWeight.Medium),labelSmall=type(12,18,FontWeight.Medium))
fun momentumColors(dark:Boolean)=if(dark)darkColorScheme(
 primary=Neo.Lime,onPrimary=Neo.Ink,primaryContainer=Neo.Lime,onPrimaryContainer=Neo.Ink,
 secondary=Neo.Purple,onSecondary=Neo.Ink,secondaryContainer=Neo.Purple,onSecondaryContainer=Neo.Ink,
 tertiary=Neo.Purple,onTertiary=Neo.Ink,tertiaryContainer=Color(0xFF352D47),onTertiaryContainer=Neo.Paper,
 background=Neo.Ink,onBackground=Neo.Paper,surface=Neo.Ink,onSurface=Neo.Paper,
 surfaceVariant=Color(0xFF29292B),onSurfaceVariant=Color(0xFFC7C5C2),outline=Color(0xFF94918D),outlineVariant=Color(0xFF48474A),
 surfaceContainerLowest=Color(0xFF080809),surfaceContainerLow=Color(0xFF1B1B1D),surfaceContainer=Color(0xFF222224),surfaceContainerHigh=Color(0xFF29292B),surfaceContainerHighest=Color(0xFF343437))
else lightColorScheme(
 primary=Neo.Ink,onPrimary=Neo.Paper,primaryContainer=Neo.Lime,onPrimaryContainer=Neo.Ink,
 secondary=Color(0xFF55416F),onSecondary=Color.White,secondaryContainer=Neo.Purple,onSecondaryContainer=Neo.Ink,
 tertiary=Color(0xFF55416F),onTertiary=Color.White,tertiaryContainer=Color(0xFFE9E0FF),onTertiaryContainer=Neo.Ink,
 background=Neo.Paper,onBackground=Neo.Ink,surface=Neo.Paper,onSurface=Neo.Ink,
 surfaceVariant=Neo.Concrete,onSurfaceVariant=Color(0xFF565351),outline=Color(0xFF74706D),outlineVariant=Color(0xFFC9C6C1),
 surfaceContainerLowest=Color.White,surfaceContainerLow=Color(0xFFF0EDE8),surfaceContainer=Color(0xFFECE8E3),surfaceContainerHigh=Neo.Concrete,surfaceContainerHighest=Color(0xFFDAD6D1))
@Composable fun words(he:String,en:String)=if(LocalEnglish.current)en else he

@Composable fun PrimaryAction(label:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,ink:Boolean=false) {
 val interaction=remember {MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState()
 val offset by animateFloatAsState(if(pressed && !LocalReduceMotion.current)2f else 0f,if(LocalReduceMotion.current)snap() else spring(dampingRatio=1f,stiffness=600f),label="action press")
 val fill=if(!enabled)MaterialTheme.colorScheme.surfaceVariant else if(ink)Neo.Ink else Neo.Lime
 val foreground=if(!enabled)MaterialTheme.colorScheme.onSurfaceVariant else if(ink)Neo.Lime else Neo.Ink
 Box(modifier.fillMaxWidth().heightIn(min=56.dp).graphicsLayer{translationY=offset.dp.toPx()}.clip(RoundedCornerShape(16.dp)).background(fill)
  .clickable(enabled=enabled,role=Role.Button,interactionSource=interaction,indication=null,onClick=onClick).padding(horizontal=20.dp,vertical=16.dp),contentAlignment=androidx.compose.ui.Alignment.Center) {
  Text(label,style=MaterialTheme.typography.titleMedium,color=foreground,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
 }
}
@Composable fun Eyebrow(text:String) {Text(text,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)}
@Composable fun QuietPanel(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
 Column(modifier.fillMaxWidth().clip(Rhythm.card).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
}
@Composable fun ActionPanel(content:@Composable ColumnScope.()->Unit) {
 Box(Modifier.fillMaxWidth().padding(bottom=5.dp,end=5.dp).drawBehind {
  drawRoundRect(Neo.Ink,topLeft=Offset(5.dp.toPx(),5.dp.toPx()),cornerRadius=androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()))
 }) {
  Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Neo.Lime).border(2.dp,Neo.Ink,RoundedCornerShape(22.dp)).padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
   CompositionLocalProvider(LocalContentColor provides Neo.Ink,content={content()})
  }
 }
}
@Composable fun InkUnderline(modifier:Modifier=Modifier) {
 Canvas(modifier.width(118.dp).height(12.dp).clearAndSetSemantics{}) {
  drawLine(Neo.Lime,Offset(2f,size.height*.8f),Offset(size.width,size.height*.1f),4.dp.toPx(),StrokeCap.Round)
  drawLine(Neo.Lime,Offset(size.width*.25f,size.height*.9f),Offset(size.width*.82f,size.height*.6f),2.dp.toPx(),StrokeCap.Round)
 }
}
@Composable fun SegmentChoices(items:List<Pair<String,String>>,selected:String,onSelect:(String)->Unit) {
 Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  items.forEach{(id,label)->
   val active=id==selected
   Box(Modifier.heightIn(min=48.dp).clip(RoundedCornerShape(12.dp)).background(if(active)MaterialTheme.colorScheme.onSurface else Color.Transparent)
    .border(1.dp,if(active)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,RoundedCornerShape(12.dp))
    .selectable(selected=active,role=Role.RadioButton,onClick={onSelect(id)}).padding(horizontal=16.dp,vertical=12.dp),contentAlignment=androidx.compose.ui.Alignment.Center) {
    Text(label,style=MaterialTheme.typography.labelMedium,color=if(active)MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
 }
}
/** Trail nodes represent real events only. Decorative geometry is excluded from semantics. */
@Composable fun Trail(count:Int,modifier:Modifier=Modifier) {
 val color=MaterialTheme.colorScheme.primary
 val rtl=androidx.compose.ui.platform.LocalLayoutDirection.current==LayoutDirection.Rtl
 val muted=MaterialTheme.colorScheme.outlineVariant
 val reduced=LocalReduceMotion.current
 val arrival=remember{Animatable(1f)}
 LaunchedEffect(count,reduced){if(count>0 && !reduced){arrival.snapTo(0f);arrival.animateTo(1f,spring(dampingRatio=.8f,stiffness=100f))}else arrival.snapTo(1f)}
 val target=count.coerceIn(0,7).toFloat()
 val revealed by animateFloatAsState(target,if(LocalReduceMotion.current)snap() else spring(dampingRatio=.86f,stiffness=95f),label="recorded steps")
 Canvas(modifier.fillMaxWidth().height(64.dp).clearAndSetSemantics{}) {
  val inset=8.dp.toPx()
  val points=(0..6).map{i->Offset(inset+(size.width-2*inset)*(if(rtl)6-i else i)/6f,size.height*(if(i%2==0).68f else .30f))}
  val path=Path().apply{moveTo(points.first().x,points.first().y);points.zipWithNext().forEach{(a,b)->val mid=(a.x+b.x)/2;cubicTo(mid,a.y,mid,b.y,b.x,b.y)}}
  drawPath(path,muted,style=Stroke(1.5.dp.toPx(),cap=StrokeCap.Round))
  val measure=PathMeasure().apply{setPath(path,false)}
  val drawn=Path()
  measure.getSegment(0f,measure.length*((revealed-1)/6).coerceIn(0f,1f),drawn,true)
  drawPath(drawn,color,style=Stroke(3.dp.toPx(),cap=StrokeCap.Round))
  points.forEachIndexed{i,p->
   val fraction=(revealed-i).coerceIn(0f,1f)
   if(fraction>0)drawCircle(color,5.dp.toPx()*fraction,p)
   if(i==count.coerceIn(0,7)-1 && arrival.value<1f)drawCircle(color.copy(alpha=(1-arrival.value).coerceIn(0f,1f)),(7+arrival.value*9).dp.toPx(),p,style=Stroke(1.5.dp.toPx()))
  }
 }
}

enum class Touch { START, COMPLETE, MILESTONE }
fun pulse(context:Context,enabled:Boolean,kind:Touch) {
 if(!enabled || Settings.System.getInt(context.contentResolver,Settings.System.HAPTIC_FEEDBACK_ENABLED,1)!=1)return
 val effect=when(kind) {
  Touch.START->VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
  Touch.COMPLETE->VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
  Touch.MILESTONE->VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
 }
 context.getSystemService(Vibrator::class.java)?.vibrate(effect)
}
