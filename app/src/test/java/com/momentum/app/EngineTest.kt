package com.momentum.app
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class EngineTest {
 private val now=1_800_000_000_000L
 private fun task(id:String="a",status:String="READY",estimate:Int?=null,priority:Int=1,available:Long=0,due:Long?=null,last:Long=0,created:Long=now)=Task(id=id,title=id,status=status,estimate=estimate,priority=priority,available=available,due=due,lastConsidered=last,created=created)
 @Test fun excludesDone() {assertTrue(SuggestionEngine.rank(listOf(task(status="DONE")),now).isEmpty())}
 @Test fun excludesArchived() {assertTrue(SuggestionEngine.rank(listOf(task(status="ARCHIVED")),now).isEmpty())}
 @Test fun waitingWithoutReviewExcluded() {assertTrue(SuggestionEngine.rank(listOf(task(status="WAITING")),now).isEmpty())}
 @Test fun waitingBeforeReviewExcluded() {assertTrue(SuggestionEngine.rank(listOf(task(status="WAITING",available=now+1000)),now).isEmpty())}
 @Test fun waitingReviewDoesNotMakeBlockedTaskActionable() {assertTrue(SuggestionEngine.rank(listOf(task(status="WAITING",available=now)),now).isEmpty())}
 @Test fun futureTasksExcluded() {assertTrue(SuggestionEngine.rank(listOf(task(available=now+1)),now).isEmpty())}
 @Test fun unknownEstimateNotPenalized() {assertEquals(1,SuggestionEngine.rank(listOf(task()),now,5).size)}
 @Test fun onlyTrueFitReason() {assertFalse(SuggestionEngine.rank(listOf(task(estimate=30)),now,5).single().reasons.contains("fits"))}
 @Test fun fitsSelectedTime() {assertEquals("a",SuggestionEngine.rank(listOf(task("b",estimate=30),task("a",estimate=5)),now,5).first().task.id)}
 @Test fun priorityRanks() {assertEquals("a",SuggestionEngine.rank(listOf(task("b",priority=0),task("a",priority=2)),now).first().task.id)}
 @Test fun imminentDueRanks() {assertEquals("a",SuggestionEngine.rank(listOf(task("b"),task("a",due=now+1000)),now).first().task.id)}
 @Test fun cooldownExcludes() {assertTrue(SuggestionEngine.rank(listOf(task(last=now-100)),now).isEmpty())}
 @Test fun cooldownExpires() {assertEquals(1,SuggestionEngine.rank(listOf(task(last=now-60_000)),now).size)}
 @Test fun somethingElseChangesCandidate() {val tasks=listOf(task("a",last=now),task("b"));assertEquals("b",SuggestionEngine.rank(tasks,now).first().task.id)}
 @Test fun stableTies() {assertEquals("a",SuggestionEngine.rank(listOf(task("b"),task("a")),now).first().task.id)}
 @Test fun olderTaskGetsBonus() {assertEquals("b",SuggestionEngine.rank(listOf(task("a"),task("b",created=now-10*86_400_000)),now).first().task.id)}
 @Test fun contextIsManual() {assertTrue(SuggestionEngine.rank(listOf(task().copy(contexts="home")),now,context="office").isEmpty())}
 @Test fun noContextAlwaysAvailable() {assertEquals(1,SuggestionEngine.rank(listOf(task()),now,context="office").size)}
 @Test fun emptyListHandled() {assertTrue(SuggestionEngine.rank(emptyList(),now).isEmpty())}
 @Test fun thousandTasksFast() {val start=System.nanoTime();assertEquals(1000,SuggestionEngine.rank((1..1000).map{task(it.toString())},now).size);assertTrue((System.nanoTime()-start)<2_000_000_000)}
 private fun ms(s:String)=Instant.parse(s).toEpochMilli()
 @Test fun quietCrossesMidnight() {assertEquals(ms("2026-10-22T08:00:00Z"),ReminderPolicy.nextAllowed(ms("2026-10-21T23:00:00Z"),22,8,ZoneOffset.UTC))}
 @Test fun morningQuiet() {assertEquals(ms("2026-10-22T08:00:00Z"),ReminderPolicy.nextAllowed(ms("2026-10-22T05:00:00Z"),22,8,ZoneOffset.UTC))}
 @Test fun outsideQuietUnchanged() {val t=ms("2026-10-22T10:00:00Z");assertEquals(t,ReminderPolicy.nextAllowed(t,22,8,ZoneOffset.UTC))}
 @Test fun quietDisabledWhenSame() {val t=ms("2026-10-22T23:00:00Z");assertEquals(t,ReminderPolicy.nextAllowed(t,8,8,ZoneOffset.UTC))}
 @Test fun daytimeQuiet() {assertEquals(ms("2026-10-22T17:00:00Z"),ReminderPolicy.nextAllowed(ms("2026-10-22T12:00:00Z"),9,17,ZoneOffset.UTC))}
 @Test fun timezoneApplied() {assertEquals(ms("2026-10-22T05:00:00Z"),ReminderPolicy.nextAllowed(ms("2026-10-21T21:30:00Z"),22,8,ZoneId.of("Asia/Jerusalem")))}
 @Test fun dayBoundaryLocal() {assertEquals(ms("2026-10-21T21:00:00Z"),ReminderPolicy.dayStart(ms("2026-10-22T02:00:00Z"),ZoneId.of("Asia/Jerusalem")))}
 @Test fun daylightSaving() {val zone=ZoneId.of("Europe/Berlin");val result=ReminderPolicy.nextAllowed(ms("2026-10-24T23:30:00Z"),22,8,zone);assertEquals(8,Instant.ofEpochMilli(result).atZone(zone).hour);assertTrue(result>ms("2026-10-24T23:30:00Z"))}
}
