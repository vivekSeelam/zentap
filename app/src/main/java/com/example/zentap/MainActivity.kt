package com.example.zentap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.zentap.ui.theme.ZentapTheme

class MainActivity : ComponentActivity() {

    // Compose state at the activity level — updated in onResume so the UI
    // refreshes automatically when the user returns from the Settings screens.
    private var canDrawOverlays by mutableStateOf(false)
    private var accessibilityEnabled by mutableStateOf(false)
    private var apiKeyConfigured by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZentapTheme {
                SetupScreen(
                    canDrawOverlays = canDrawOverlays,
                    accessibilityEnabled = accessibilityEnabled,
                    apiKeyConfigured = apiKeyConfigured,
                    onGrantOverlay = {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                        )
                    },
                    onGrantAccessibility = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onSaveApiKey = { key ->
                        ApiKeyStore.saveKey(this, key)
                        apiKeyConfigured = true
                    },
                    onViewLog = {
                        startActivity(Intent(this, LogActivity::class.java))
                    },
                    onManageApps = {
                        startActivity(Intent(this, GuardedAppsActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check every time the user comes back from a Settings screen
        canDrawOverlays = Settings.canDrawOverlays(this)
        accessibilityEnabled = isAccessibilityEnabled()
        apiKeyConfigured = ApiKeyStore.isConfigured(this)
    }

    private fun isAccessibilityEnabled(): Boolean {
        // The service id Android expects is "package/fully.qualified.ClassName"
        val serviceId = "$packageName/${GuardAccessibilityService::class.java.canonicalName}"
        return try {
            val globallyEnabled = Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
            ) == 1
            if (!globallyEnabled) return false

            val enabledServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            // The value is a colon-separated list of service ids
            enabledServices.split(":").any { it.equals(serviceId, ignoreCase = true) }
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
private fun SetupScreen(
    canDrawOverlays: Boolean,
    accessibilityEnabled: Boolean,
    apiKeyConfigured: Boolean,
    onGrantOverlay: () -> Unit,
    onGrantAccessibility: () -> Unit,
    onSaveApiKey: (String) -> Unit,
    onViewLog: () -> Unit,
    onManageApps: () -> Unit,
) {
    val allReady = canDrawOverlays && accessibilityEnabled && apiKeyConfigured

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Zentap Guardian", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Complete the three steps below, then open Instagram.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(40.dp))

            PermissionRow(
                label = "Draw over other apps",
                granted = canDrawOverlays,
                onGrant = onGrantOverlay,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
            PermissionRow(
                label = "Accessibility service",
                granted = accessibilityEnabled,
                onGrant = onGrantAccessibility,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
            ApiKeySection(isConfigured = apiKeyConfigured, onSave = onSaveApiKey)

            if (allReady) {
                Spacer(Modifier.height(40.dp))
                Text(
                    text = "All set! Open Instagram to see the overlay.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(32.dp))
            TextButton(onClick = onManageApps) {
                Text("Choose apps to guard →")
            }
            TextButton(onClick = onViewLog) {
                Text("View access log →")
            }
        }
    }
}

@Composable
private fun ApiKeySection(isConfigured: Boolean, onSave: (String) -> Unit) {
    var editing by remember { mutableStateOf(!isConfigured) }
    var input by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Anthropic API key",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (isConfigured && !editing) {
            Text("✓", color = Color(0xFF2E7D32), style = MaterialTheme.typography.titleLarge)
        } else if (!isConfigured) {
            Text("Required", color = Color(0xFFE65100), style = MaterialTheme.typography.bodySmall)
        }
    }

    if (editing || !isConfigured) {
        Spacer(Modifier.height(8.dp))
        // Takes them straight to the Anthropic Console key page
        OutlinedButton(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://console.anthropic.com/settings/keys"))
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Get your API key →") }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Paste key here  (sk-ant-api03-...)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (input.isNotBlank()) {
                    onSave(input)
                    editing = false
                    input = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save key") }
    } else {
        TextButton(onClick = { editing = true }) { Text("Change key") }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onGrant: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (granted) {
            Text("✓", color = Color(0xFF2E7D32), style = MaterialTheme.typography.titleLarge)
        } else {
            Button(onClick = onGrant) { Text("Grant") }
        }
    }
}