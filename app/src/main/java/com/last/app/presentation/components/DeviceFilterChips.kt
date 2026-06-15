package com.last.app.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.last.app.R
import com.last.app.data.entity.Device
import com.last.app.presentation.theme.LastPrimary

@Composable
fun DeviceFilterChips(
    devices: List<Device>,
    selectedDevice: Device?,
    onSelect: (Device?) -> Unit,
    modifier: Modifier = Modifier,
    showAll: Boolean = true,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showAll) {
            FilterChip(
                selected = selectedDevice == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.history_filter_all)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LastPrimary,
                    selectedLabelColor = Color.White,
                ),
            )
        }
        devices.forEach { device ->
            FilterChip(
                selected = selectedDevice?.id == device.id,
                onClick = { onSelect(device) },
                label = { Text(device.deviceName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LastPrimary,
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}
