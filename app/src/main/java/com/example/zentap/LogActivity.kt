package com.example.zentap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zentap.ui.theme.ZentapTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZentapTheme {
                LogScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<AccessLog>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        entries = withContext(Dispatchers.IO) { AccessLogger.readAll(context) }.reversed()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Access Log") })
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Time",     Modifier.width(60.dp),  fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("Status",   Modifier.width(76.dp),  fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("Duration", Modifier.width(56.dp),  fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("Reason",   Modifier.weight(1f),    fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            HorizontalDivider()

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sessions logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn {
                    items(entries) { entry ->
                        LogRow(entry)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LogRow(entry: AccessLog) {
    val timeFmt = remember { SimpleDateFormat("HH:mm\nMMM d", Locale.getDefault()) }
    val statusColor = if (entry.allowed) Color(0xFF4CAF50) else Color(0xFFEF5350)
    val statusText  = if (entry.allowed) "✓ Granted" else "✗ Denied"
    val durationText = if (entry.allowed && entry.durationMinutes > 0) "${entry.durationMinutes} min" else "—"

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            timeFmt.format(Date(entry.timestamp)),
            Modifier.width(60.dp),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            statusText,
            Modifier.width(76.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = statusColor
        )
        Text(
            durationText,
            Modifier.width(56.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            entry.reason,
            Modifier.weight(1f),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 3,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
