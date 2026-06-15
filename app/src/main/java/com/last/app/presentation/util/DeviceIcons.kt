package com.last.app.presentation.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.ui.graphics.vector.ImageVector

fun deviceIcon(type: String): ImageVector {
    return when (type.uppercase()) {
        "KEYBOARD" -> Icons.Outlined.Keyboard
        "MOUSE" -> Icons.Outlined.Mouse
        "HEADPHONES", "BLUETOOTH" -> Icons.Outlined.Headphones
        "USB", "USB_HUB" -> Icons.Outlined.Usb
        "POWER" -> Icons.Outlined.Power
        else -> Icons.Outlined.Bluetooth
    }
}
