package com.last.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.last.app.infrastructure.ConnectionMonitorService
import com.last.app.infrastructure.platform.PermissionHelper
import com.last.app.presentation.layout.ScreenLayout
import com.last.app.presentation.layout.ScreenLayoutDefaults
import com.last.app.presentation.settings.SettingsPermissionCallbacks
import com.last.app.presentation.theme.LastIconBackground
import com.last.app.presentation.theme.LastPrimary
import com.last.app.presentation.theme.LastTextPrimary
import com.last.app.presentation.theme.LastTextSecondary

@Composable
fun SettingsView(
    viewModel: SettingsViewModel,
    permissionCallbacks: SettingsPermissionCallbacks,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    fun applySettings() {
        ConnectionMonitorService.applySettings(context)
    }

    fun enableLocationSetting() {
        viewModel.setLocationTrackingEnabled(true)
        applySettings()
    }

    fun enableBluetoothSetting() {
        viewModel.setDeviceScanEnabled(true)
        applySettings()
    }

    fun onLocationToggle(enabled: Boolean) {
        if (!enabled) {
            viewModel.setLocationTrackingEnabled(false)
            applySettings()
            return
        }
        permissionCallbacks.requestLocationPermission(::enableLocationSetting)
    }

    fun onBluetoothToggle(enabled: Boolean) {
        if (!enabled) {
            viewModel.setDeviceScanEnabled(false)
            applySettings()
            return
        }
        permissionCallbacks.requestBluetoothPermission(::enableBluetoothSetting)
    }

    Box(modifier = modifier.fillMaxSize()) {
        ScreenLayout(
            title = stringResource(R.string.settings_title),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LastPrimary)
                }
            } else {
                Column(
                    Modifier.padding(horizontal = ScreenLayoutDefaults.CONTENT_HORIZONTAL_PADDING_DP.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val settings = uiState.settings
                    val locationChecked = settings.locationTrackingEnabled &&
                        PermissionHelper.hasLocationPermission(context)
                    val bluetoothChecked = settings.deviceScanEnabled &&
                        PermissionHelper.hasBluetoothPermissions(context)

                    SettingsToggleCard(
                        icon = Icons.Outlined.LocationOn,
                        title = stringResource(R.string.settings_location),
                        checked = locationChecked,
                        onCheckedChange = ::onLocationToggle,
                    )
                    SettingsToggleCard(
                        icon = Icons.Outlined.Bluetooth,
                        title = stringResource(R.string.settings_bluetooth),
                        checked = bluetoothChecked,
                        onCheckedChange = ::onBluetoothToggle,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleCard(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(LastIconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, title, Modifier.size(22.dp), LastTextSecondary)
            }
            Text(
                title,
                Modifier.weight(1f).padding(horizontal = 16.dp),
                color = LastTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = LastPrimary),
            )
        }
    }
}
