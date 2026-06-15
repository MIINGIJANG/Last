package com.last.app.presentation.device

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.last.app.R
import com.last.app.data.entity.Device
import com.last.app.domain.index.RegisteredDeviceOrdering
import com.last.app.infrastructure.platform.PermissionHelper
import com.last.app.presentation.layout.ScreenLayout
import com.last.app.presentation.layout.ScreenLayoutDefaults
import com.last.app.infrastructure.platform.findComponentActivity
import com.last.app.presentation.util.deviceIcon
import com.last.app.presentation.theme.LastActionBlue
import com.last.app.presentation.theme.LastActionBlueText
import com.last.app.presentation.theme.LastCardBorder
import com.last.app.presentation.theme.LastIconBackground
import com.last.app.presentation.theme.LastPrimary
import com.last.app.presentation.theme.LastStatusConnected
import com.last.app.presentation.theme.LastStatusDisconnected
import com.last.app.presentation.theme.LastTextMuted
import com.last.app.presentation.theme.LastTextPrimary
import com.last.app.presentation.theme.LastTextSecondary

@Composable
fun DeviceView(
    viewModel: DeviceViewModel,
    isTabVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(isTabVisible) {
        viewModel.setScreenActive(isTabVisible)
    }
    val activity = context.findComponentActivity()

    val successMessage = stringResource(R.string.register_success)
    val failedMessage = stringResource(R.string.register_failed)
    val scanDisabledMessage = stringResource(R.string.scan_disabled)
    val scanFailedMessage = stringResource(R.string.scan_failed)

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results.isEmpty() || results.values.all { it }
        if (granted) {
            viewModel.scanDevices()
        } else {
            Toast.makeText(context, context.getString(R.string.permission_denied_hint), Toast.LENGTH_SHORT).show()
        }
    }

    fun onScanClick() {
        val permissions = PermissionHelper.bluetoothPermissionsToRequest(context)
        when {
            permissions.isEmpty() -> viewModel.scanDevices()
            activity != null -> bluetoothLauncher.launch(permissions.toTypedArray())
            else -> Toast.makeText(context, context.getString(R.string.permission_denied_hint), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.message) {
        when (uiState.message) {
            "success" -> snackbarHostState.showSnackbar(successMessage)
            "failed" -> snackbarHostState.showSnackbar(failedMessage)
            "scan_disabled" -> snackbarHostState.showSnackbar(scanDisabledMessage)
            "scan_failed" -> snackbarHostState.showSnackbar(scanFailedMessage)
            else -> Unit
        }
        if (uiState.message != null) viewModel.clearMessage()
    }

    Box(modifier = modifier.fillMaxSize()) {
        ScreenLayout(
            title = stringResource(R.string.register_title),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LastPrimary)
                }
            } else {
                SectionHeader(
                    Icons.Outlined.Shield,
                    stringResource(R.string.registered_devices),
                    Modifier.padding(horizontal = ScreenLayoutDefaults.CONTENT_HORIZONTAL_PADDING_DP.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                val registeredGroups = remember(uiState.registered) {
                    RegisteredDeviceOrdering.groupByCategory(uiState.registered)
                }
                val hasRegistered = registeredGroups.bluetooth.isNotEmpty() ||
                    registeredGroups.usb.isNotEmpty() ||
                    registeredGroups.power.isNotEmpty()

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ScreenLayoutDefaults.CONTENT_HORIZONTAL_PADDING_DP.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!hasRegistered) {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Text(
                                stringResource(R.string.register_no_devices),
                                Modifier.padding(16.dp),
                                color = LastTextMuted,
                                fontSize = 13.sp,
                            )
                        }
                    }
                    if (registeredGroups.bluetooth.isNotEmpty()) {
                        RegisteredCategoryCard(
                            icon = Icons.Outlined.Bluetooth,
                            title = stringResource(R.string.bluetooth_devices),
                            tint = LastActionBlueText,
                            devices = registeredGroups.bluetooth,
                            onDelete = viewModel::unregisterDevice,
                        )
                    }
                    if (registeredGroups.usb.isNotEmpty()) {
                        RegisteredCategoryCard(
                            icon = Icons.Outlined.Usb,
                            title = stringResource(R.string.usb_devices),
                            tint = LastTextSecondary,
                            devices = registeredGroups.usb,
                            onDelete = viewModel::unregisterDevice,
                        )
                    }
                    if (registeredGroups.power.isNotEmpty()) {
                        RegisteredCategoryCard(
                            icon = Icons.Outlined.Power,
                            title = stringResource(R.string.power_devices),
                            tint = LastTextSecondary,
                            devices = registeredGroups.power,
                            onDelete = viewModel::unregisterDevice,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ScreenLayoutDefaults.CONTENT_HORIZONTAL_PADDING_DP.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader(Icons.Outlined.Search, stringResource(R.string.available_devices))
                    TextButton(
                        onClick = ::onScanClick,
                        enabled = !uiState.isScanning,
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(LastActionBlue),
                    ) {
                        if (uiState.isScanning) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = LastActionBlueText, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Refresh, null, Modifier.size(16.dp), LastActionBlueText)
                        }
                        Text(
                            stringResource(R.string.scan_devices),
                            Modifier.padding(start = 4.dp),
                            color = LastActionBlueText,
                            fontSize = 13.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                val availableGroups = remember(uiState.available) {
                    RegisteredDeviceOrdering.groupByCategory(uiState.available)
                }
                val hasAvailable = availableGroups.bluetooth.isNotEmpty() ||
                    availableGroups.usb.isNotEmpty() ||
                    availableGroups.power.isNotEmpty()

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ScreenLayoutDefaults.CONTENT_HORIZONTAL_PADDING_DP.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!hasAvailable) {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Text(
                                stringResource(R.string.register_no_available),
                                Modifier.padding(16.dp),
                                color = LastTextMuted,
                                fontSize = 13.sp,
                            )
                        }
                    }
                    if (availableGroups.bluetooth.isNotEmpty()) {
                        AvailableCategoryCard(
                            icon = Icons.Outlined.Bluetooth,
                            title = stringResource(R.string.bluetooth_devices),
                            tint = LastActionBlueText,
                            devices = availableGroups.bluetooth,
                            onRegister = viewModel::registerDevice,
                        )
                    }
                    if (availableGroups.usb.isNotEmpty()) {
                        AvailableCategoryCard(
                            icon = Icons.Outlined.Usb,
                            title = stringResource(R.string.usb_devices),
                            tint = LastTextSecondary,
                            devices = availableGroups.usb,
                            onRegister = viewModel::registerDevice,
                        )
                    }
                    if (availableGroups.power.isNotEmpty()) {
                        AvailableCategoryCard(
                            icon = Icons.Outlined.Power,
                            title = stringResource(R.string.power_devices),
                            tint = LastTextSecondary,
                            devices = availableGroups.power,
                            onRegister = viewModel::registerDevice,
                        )
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(18.dp), LastTextSecondary)
        Text(title, color = LastTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GroupHeader(icon: ImageVector, title: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint)
        Text(title, color = LastTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RegisteredCategoryCard(
    icon: ImageVector,
    title: String,
    tint: Color,
    devices: List<Device>,
    onDelete: (Long) -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            GroupHeader(icon, title, tint)
            Spacer(Modifier.height(8.dp))
            devices.forEach { device ->
                RegisteredDeviceRow(device) { onDelete(device.id) }
            }
        }
    }
}

@Composable
private fun RegisteredDeviceRow(device: Device, onDelete: () -> Unit) {
    val connected = device.isConnected
    val statusColor = if (connected) LastStatusConnected else LastStatusDisconnected
    val statusLabel = if (connected) "연결됨" else "연결 해제"

    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                .border(1.dp, LastCardBorder, RoundedCornerShape(10.dp))
                .background(LastIconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(deviceIcon(device.deviceType), device.deviceName, Modifier.size(20.dp), LastTextSecondary)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(device.deviceName, color = LastTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("· $statusLabel", color = statusColor, fontSize = 12.sp)
        }
        TextButton(onClick = onDelete) {
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, LastCardBorder, RoundedCornerShape(20.dp))
                    .background(LastIconBackground).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Remove, "삭제", Modifier.size(14.dp), LastTextMuted)
                Text("삭제", color = LastTextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AvailableCategoryCard(
    icon: ImageVector,
    title: String,
    tint: Color,
    devices: List<Device>,
    onRegister: (Long) -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            GroupHeader(icon, title, tint)
            Spacer(Modifier.height(8.dp))
            devices.forEach { device ->
                AvailableDeviceRow(device) { onRegister(device.id) }
            }
        }
    }
}

@Composable
private fun AvailableDeviceRow(device: Device, onRegister: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(LastIconBackground), contentAlignment = Alignment.Center) {
            Icon(deviceIcon(device.deviceType), device.deviceName, Modifier.size(20.dp), LastTextSecondary)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(device.deviceName, color = LastTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onRegister) {
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, LastCardBorder, RoundedCornerShape(20.dp))
                    .background(Color.White).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Add, "등록", Modifier.size(14.dp), LastPrimary)
                Text("등록", color = LastPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
