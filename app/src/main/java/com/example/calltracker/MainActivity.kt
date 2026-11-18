package com.example.calltracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.calltracker.data.CallLogRepository
import com.example.calltracker.data.CallLogSyncWorker
import com.example.calltracker.data.NumberSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var repo: CallLogRepository
    private val status = MutableStateFlow("Grant permissions to begin.")

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.READ_CALL_LOG] == true) {
            syncNow()
            CallLogSyncWorker.schedule(this)
        } else {
            status.value = "Call-log permission denied — nothing to show."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = CallLogRepository(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val summaries by repo.numberSummaries
                        .collectAsState(initial = emptyList())
                    val statusText by status.collectAsState()
                    HomeScreen(
                        summaries = summaries,
                        status = statusText,
                        onRefresh = { syncNow() }
                    )
                }
            }
        }

        requestNeededPermissions()
    }

    private fun requestNeededPermissions() {
        val needed = buildList {
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isEmpty()) {
            syncNow()
            CallLogSyncWorker.schedule(this)
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun syncNow() {
        lifecycleScope.launch {
            status.value = "Syncing call log…"
            val added = repo.sync()
            status.value = "Up to date — $added new call(s) this sync."
        }
    }
}

@Composable
private fun HomeScreen(
    summaries: List<NumberSummary>,
    status: String,
    onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Call Tracker", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onRefresh) { Text("Refresh") }
        }
        Spacer(Modifier.height(8.dp))
        Text(status, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))

        if (summaries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No calls yet.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(summaries) { s -> NumberCard(s) }
            }
        }
    }
}

@Composable
private fun NumberCard(s: NumberSummary) {
    var expanded by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    ElevatedCard(
        Modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                s.contactName ?: s.number,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (s.contactName != null) {
                Text(s.number, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            Text("${s.callCount} call(s) · last ${fmt.format(Date(s.lastDateMillis))}")
            Text("Total talk time: ${formatDuration(s.totalDurationSec)}")
        }
    }
}

private fun formatDuration(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return buildString {
        if (h > 0) append("${h}h ")
        if (h > 0 || m > 0) append("${m}m ")
        append("${s}s")
    }
}
