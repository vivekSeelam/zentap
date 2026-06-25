package com.example.zentap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.zentap.ui.theme.ZentapTheme

class GuardedAppsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZentapTheme {
                GuardedAppsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardedAppsScreen() {
    val context = LocalContext.current
    val installedApps = remember { GuardedAppsStore.installedApps(context) }
    var guarded by remember { mutableStateOf(GuardedAppsStore.getGuardedPackages(context)) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Apps to Guard") }) }
    ) { padding ->
        if (installedApps.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "None of the supported apps are installed.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    Text(
                        "Guardian Mom fires whenever you open a guarded app. " +
                        "Only apps installed on this device are shown.",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                }
                items(installedApps) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${app.emoji}  ${app.displayName}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = app.packageName in guarded,
                            onCheckedChange = { on ->
                                guarded = if (on) guarded + app.packageName
                                          else     guarded - app.packageName
                                GuardedAppsStore.setGuardedPackages(context, guarded)
                            }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}
