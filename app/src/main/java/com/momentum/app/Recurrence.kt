package com.momentum.app
import java.time.*
import java.util.UUID

object Recurrence {
 fun validate(t:Task){require(t.repeatMode in setOf("NONE","FIXED","AFTER"));require(t.repeatUnit in setOf("DAY","WEEK","MONTH"));require(t.repeatEvery in 1..365);require(t.repeatAnchor>=0)}
 fun childId(id:String)=UUID.nameUUIDFromBytes(("momentum-repeat:"+id).toByteArray(Charsets.UTF_8)).toString()
 fun next(t:Task,completedAt:Long,zone:ZoneId=ZoneId.systemDefault()):Task? {
  validate(t);if(t.repeatMode=="NONE")return null
  val anchor=if(t.repeatMode=="AFTER")completedAt else t.repeatAnchor.takeIf{it>0}?:t.due?:t.reminder?:t.available.takeIf{it>0}?:t.created
  val base=Instant.ofEpochMilli(anchor).atZone(zone)
  fun at(n:Long):Long {val count=n*t.repeatEvery;return when(t.repeatUnit){"WEEK"->base.plusWeeks(count);"MONTH"->base.plusMonths(count);else->base.plusDays(count)}.toInstant().toEpochMilli()}
  var n=1L
  if(t.repeatMode=="FIXED") {
   val now=Instant.ofEpochMilli(completedAt).atZone(zone)
   val units=when(t.repeatUnit){"WEEK"->java.time.temporal.ChronoUnit.WEEKS.between(base,now);"MONTH"->java.time.temporal.ChronoUnit.MONTHS.between(base,now);else->java.time.temporal.ChronoUnit.DAYS.between(base,now)}
   n=(units/t.repeatEvery).coerceAtLeast(1)
   while(at(n)<=completedAt)n++
  }
  val date=at(n)
  return t.copy(id=childId(t.id),status="READY",available=date,due=if(t.due!=null)date else null,reminder=if(t.reminder!=null)date else null,created=completedAt,updated=completedAt,completed=null,lastConsidered=0,repeatAnchor=if(t.repeatMode=="FIXED")anchor else 0)
 }
}
fun Project.displayName()=listOf(emoji,title).filter{it.isNotBlank()}.joinToString(" ")
