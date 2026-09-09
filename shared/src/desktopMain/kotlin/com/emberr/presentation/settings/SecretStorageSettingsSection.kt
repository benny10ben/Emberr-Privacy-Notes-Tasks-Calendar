// Settings row naming the credential manager that holds the keys, with details on tap.
package com.emberr.presentation.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emberr.core.security.secrets.DesktopSecretStore
import com.emberr.core.security.secrets.SecretStorageState
import com.emberr.presentation.shared.components.EmberrAlertDialog
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.shield_alert
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
actual fun SecretStorageSettingsSection() {
    val secretStore = koinInject<DesktopSecretStore>()
    val storageState by secretStore.storageState.collectAsState()
    var isDetailDialogVisible by remember { mutableStateOf(false) }

    SettingsGroup(title = "Security") {
        SettingsActionRow(
            icon = painterResource(Res.drawable.shield_alert),
            title = "Credential storage",
            trailingLabel = shortSummaryFor(storageState),
            isDestructive = storageState is SecretStorageState.StoredInPlainText,
            onClick = { isDetailDialogVisible = true }
        )
    }

    if (isDetailDialogVisible) {
        SecretStorageDetailDialog(
            storageState = storageState,
            onDismiss = { isDetailDialogVisible = false }
        )
    }
}

@Composable
private fun SecretStorageDetailDialog(
    storageState: SecretStorageState,
    onDismiss: () -> Unit
) {
    EmberrAlertDialog(
        onDismissRequest = onDismiss,
        title = "Credential storage"
    ) {
        Text(
            text = detailedExplanationFor(storageState),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(20.dp))
        EmberrButtonPrimary(
            text = "Got it",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun shortSummaryFor(storageState: SecretStorageState): String = when (storageState) {
    is SecretStorageState.Starting -> "Checking"
    is SecretStorageState.ProtectedByCredentialManager -> storageState.backendDisplayName
    is SecretStorageState.StoredInPlainText -> "Plain text"
}

private fun detailedExplanationFor(storageState: SecretStorageState): String = when (storageState) {
    is SecretStorageState.Starting ->
        "Emberr is still checking which credential manager this system provides."

    is SecretStorageState.ProtectedByCredentialManager ->
        "Your AI API keys and sync keys are held by ${storageState.backendDisplayName}, " +
            "managed by your operating system. They are never written to Emberr's own files."

    is SecretStorageState.StoredInPlainText ->
        "Your AI API keys and sync keys are stored as readable text in " +
            "~/.emberr/secrets.properties. Anyone who can read your home folder can read them.\n\n" +
            "${storageState.reason}\n\n${storageState.remedy}"
}
