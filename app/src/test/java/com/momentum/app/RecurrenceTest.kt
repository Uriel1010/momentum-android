package com.momentum.app
import org.junit.Test
import org.junit.Assert.*
import java.time.*
class RecurrenceTest {
 private val zone=ZoneId.of("America/New_York")
 private fun time(v:String)=LocalDateTime.parse(v).atZone(zone).toInstant().toEpochMilli()
 @Test fun fixedMonthlyKeepsOriginalDay(){
  val t=Task(title="Rent",repeatMode="FIXED",repeatUnit="MONTH",repeatAnchor=time("2026-01-31T09:00"))
  val feb=Recurrence.next(t,time("2026-02-01T10:00"),zone)!!
  assertEquals(time("2026-02-28T09:00"),feb.available)
  assertEquals(time("2026-03-31T09:00"),Recurrence.next(feb,time("2026-02-28T10:00"),zone)!!.available)
 }
 @Test fun fixedSkipsMissedDatesAndPreservesLocalTimeAcrossDst(){
  val t=Task(title="Walk",repeatMode="FIXED",repeatAnchor=time("2026-03-07T09:00"))
  assertEquals(time("2026-03-09T09:00"),Recurrence.next(t,time("2026-03-08T12:00"),zone)!!.available)
 }
 @Test fun afterCompletionStartsFromCompletion(){
  val t=Task(title="Water",repeatMode="AFTER",repeatEvery=2,repeatUnit="WEEK",created=1)
  assertEquals(time("2026-05-15T12:00"),Recurrence.next(t,time("2026-05-01T12:00"),zone)!!.available)
 }
 @Test fun noRepeatAndStableIdentity(){
  val t=Task(title="One off");assertNull(Recurrence.next(t,100,zone))
  assertEquals(Recurrence.childId(t.id),Recurrence.childId(t.id))
 }
}
