package com.emberr.presentation.shared.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.ui.theme.LocalAppIsDark

@Composable
fun EmberrTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    onSubmit: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }),
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
            unfocusedContainerColor = when {
                !isDesktopPlatform -> MaterialTheme.colorScheme.surfaceVariant
                LocalAppIsDark.current -> MaterialTheme.colorScheme.surfaceVariant
                else -> Color(0xFFD8D8D8)
            },
            focusedContainerColor = when {
                !isDesktopPlatform -> MaterialTheme.colorScheme.surfaceVariant
                LocalAppIsDark.current -> MaterialTheme.colorScheme.surfaceVariant
                else -> Color(0xFFD8D8D8)
            },
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier.onPreviewKeyEvent { event ->
            val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
            if (onSubmit != null && singleLine && isEnter && event.type == KeyEventType.KeyDown) {
                onSubmit()
                true
            } else {
                false
            }
        }
    )
}