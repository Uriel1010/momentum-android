package com.momentum.app

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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

object Rhythm {
 val page=24.dp
 val gap=16.dp
 val card=RoundedCornerShape(28.dp)
 const val transition=220
}
val LocalReduceMotion=staticCompositionLocalOf {false}
val MomentumFont=FontFamily(Font(R.font.heebo_regular,FontWeight.Normal),Font(R.font.heebo_medium,FontWeight.Medium),Font(R.font.heebo_bold,FontWeight.Bold))
private fun type(size:Int,line:Int,weight:FontWeight=FontWeight.Normal)=TextStyle(fontFamily=MomentumFont,fontSize=size.sp,lineHeight=line.sp,fontWeight=weight)
val MomentumTypography=Typography(
 displayLarge=type(64,70,FontWeight.Medium),displayMedium=type(52,60,FontWeight.Medium),displaySmall=type(40,48,FontWeight.Medium),
 headlineLarge=type(34,42,FontWeight.Medium),headlineMedium=type(28,36,FontWeight.Medium),headlineSmall=type(24,32,FontWeight.Medium),
 titleLarge=type(22,30,FontWeight.Medium),titleMedium=type(18,26,FontWeight.Medium),titleSmall=type(16,24,FontWeight.Medium),
 bodyLarge=type(17,26),bodyMedium=type(15,23),bodySmall=type(14,21),
 labelLarge=type(15,22,FontWeight.Medium),labelMedium=type(14,20,FontWeight.Medium),labelSmall=type(12,18,FontWeight.Medium))
fun momentumColors(dark:Boolean)=if(dark)darkColorScheme(
 primary=Color(0xFFBED8A3),onPrimary=Color(0xFF20351E),primaryContainer=Color(0xFF2E4934),onPrimaryContainer=Color(0xFFE7F0DC),
 secondary=Color(0xFFC8C4AF),onSecondary=Color(0xFF302F25),secondaryContainer=Color(0xFF393E32),onSecondaryContainer=Color(0xFFE4E5D7),
 tertiary=Color(0xFFE7BD82),onTertiary=Color(0xFF422D10),tertiaryContainer=Color(0xFF4D3C24),onTertiaryContainer=Color(0xFFFFDFB2),
 background=Color(0xFF151C18),onBackground=Color(0xFFF0EFE5),surface=Color(0xFF151C18),onSurface=Color(0xFFF0EFE5),
 surfaceVariant=Color(0xFF333D33),onSurfaceVariant=Color(0xFFBDC6B9),outline=Color(0xFF8E998B),outlineVariant=Color(0xFF3D483D),
 surfaceContainerLowest=Color(0xFF101612),surfaceContainerLow=Color(0xFF1C251F),surfaceContainer=Color(0xFF222D25),surfaceContainerHigh=Color(0xFF2A352D),surfaceContainerHighest=Color(0xFF323F35))
else lightColorScheme(
 primary=Color(0xFF315D3B),onPrimary=Color.White,primaryContainer=Color(0xFFE0EACF),onPrimaryContainer=Color(0xFF213D27),
 secondary=Color(0xFF5F6654),onSecondary=Color.White,secondaryContainer=Color(0xFFE9E9DA),onSecondaryContainer=Color(0xFF353E2E),
 tertiary=Color(0xFF78552D),onTertiary=Color.White,tertiaryContainer=Color(0xFFF3DFC2),onTertiaryContainer=Color(0xFF513618),
 background=Color(0xFFF8F6EE),onBackground=Color(0xFF252D24),surface=Color(0xFFF8F6EE),onSurface=Color(0xFF252D24),
 surfaceVariant=Color(0xFFE4E7DA),onSurfaceVariant=Color(0xFF596250),outline=Color(0xFF747E6D),outlineVariant=Color(0xFFD0D6C5),
 surfaceContainerLowest=Color.White,surfaceContainerLow=Color(0xFFF1F0E5),surfaceContainer=Color(0xFFECEDE0),surfaceContainerHigh=Color(0xFFE5E8D9),surfaceContainerHighest=Color(0xFFDDE2D1))
@Composable fun words(he:String,en:String)=if(LocalEnglish.current)en else he

@Composable fun PrimaryAction(label:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true) {
 val interaction=remember {MutableInteractionSource()}
 val pressed by interaction.collectIsPressedAsState()
 val scale by animateFloatAsState(if(pressed && !LocalReduceMotion.current).975f else 1f,spring(dampingRatio=.72f,stiffness=500f),label="action press")
 Button(onClick,modifier.fillMaxWidth().heightIn(min=58.dp).graphicsLayer{scaleX=scale;scaleY=scale},enabled=enabled,
  shape=RoundedCornerShape(18.dp),interactionSource=interaction,contentPadding=PaddingValues(16.dp)) {Text(label,style=MaterialTheme.typography.titleMedium)}
}
@Composable fun Eyebrow(text:String) {Text(text,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)}
@Composable fun QuietPanel(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
 Surface(modifier.fillMaxWidth(),shape=Rhythm.card,color=MaterialTheme.colorScheme.surfaceContainerLow) {
  Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
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
