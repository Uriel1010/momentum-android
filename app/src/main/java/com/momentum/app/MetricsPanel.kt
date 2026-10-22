package com.momentum.app
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
@Composable fun MetricsPanel(vm:MomentumViewModel) {
 val events by vm.events.collectAsStateWithLifecycle()
 val m=MetricSummary.from(events)
 val en=LocalEnglish.current
 fun fraction(n:Int,d:Int)=if(d==0)"—" else "$n / $d"
 Text(tr(R.string.v3_metrics_window),style=MaterialTheme.typography.labelLarge)
 Text(tr(R.string.v3_open_start)+": "+(m.latencySeconds?.let{"$it "+if(en)"s" else "ש׳"}?:"—"))
 Text(tr(R.string.v3_initiation)+": "+fraction(events.count{it.kind=="metric_started" && it.note.toLongOrNull()!=null},m.opens))
 Text(tr(R.string.v3_completion_rate)+": "+fraction(events.filter{it.kind=="metric_started"}.mapNotNull{it.taskId}.distinct().count{id->events.any{it.kind=="metric_completed" && it.taskId==id}},events.filter{it.kind=="metric_started"}.mapNotNull{it.taskId}.distinct().size))
 Text(tr(R.string.v3_useful)+": "+fraction(m.useful,m.recommendations))
 Text(tr(R.string.v3_repeated)+": "+m.repeated)
 Text(tr(R.string.v3_resumed)+": "+m.resumed)
}
