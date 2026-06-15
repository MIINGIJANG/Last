package com.last.app.presentation.permission

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.last.app.R

enum class ConsentStep {
    INTRO,
    LOCATION,
    BACKGROUND_LOCATION,
    BLUETOOTH,
    COMPLETE,
}

@Composable
fun PermissionConsentGate(
    step: ConsentStep,
    onConfirm: (ConsentStep) -> Unit,
    onSkip: (ConsentStep) -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (step == ConsentStep.INTRO) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        text = stringResource(R.string.permission_intro_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.permission_intro_line1),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.permission_intro_line2),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.permission_intro_line3),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onConfirm(ConsentStep.INTRO) }) {
                        Text(stringResource(R.string.permission_confirm))
                    }
                },
            )
        }
    }
}

fun nextConsentStep(
    current: ConsentStep,
    missingLocation: Boolean,
    needsBackgroundLocation: Boolean,
    missingBluetooth: Boolean,
): ConsentStep {
    return when (current) {
        ConsentStep.INTRO -> when {
            missingLocation -> ConsentStep.LOCATION
            needsBackgroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> ConsentStep.BACKGROUND_LOCATION
            missingBluetooth -> ConsentStep.BLUETOOTH
            else -> ConsentStep.COMPLETE
        }
        ConsentStep.LOCATION -> when {
            needsBackgroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> ConsentStep.BACKGROUND_LOCATION
            missingBluetooth -> ConsentStep.BLUETOOTH
            else -> ConsentStep.COMPLETE
        }
        ConsentStep.BACKGROUND_LOCATION -> when {
            missingBluetooth -> ConsentStep.BLUETOOTH
            else -> ConsentStep.COMPLETE
        }
        ConsentStep.BLUETOOTH, ConsentStep.COMPLETE -> ConsentStep.COMPLETE
    }
}
