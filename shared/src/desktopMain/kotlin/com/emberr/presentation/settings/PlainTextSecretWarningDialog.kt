// One time warning shown when secrets had to be saved as readable text.
package com.emberr.presentation.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emberr.core.security.secrets.DesktopSecretStore
import com.emberr.core.security.secrets.SecretStorageState
import com.emberr.presentation.shared.components.EmberrAlertDialog
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import org.koin.compose.koinInject

@Composable
fun PlainTextSecretWarningDialog() {
    val secretStore = koinInject<DesktopSecretStore>()
    val isWarningPending by secretStore.plainTextWarningPending.collectAsState()
    val storageState by secretStore.storageState.collectAsState()

    if (!isWarningPending) return
    val plainTextState = storageState as? SecretStorageState.StoredInPlainText ?: return

    EmberrAlertDialog(
        onDismissRequest = { secretStore.rememberPlainTextWarningWasShown() },
        title = "Your keys are not protected"
    ) {
        Text(
            text = "This system has no credential manager Emberr can use, so your AI API keys " +
                "and sync keys are saved as readable text in ~/.emberr/secrets.properties. " +
                "The file is readable only by your user account, but it is not encrypted.\n\n" +
                "${plainTextState.reason}\n\n${plainTextState.remedy}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(20.dp))
        EmberrButtonPrimary(
            text = "I understand",
            onClick = { secretStore.rememberPlainTextWarningWasShown() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
