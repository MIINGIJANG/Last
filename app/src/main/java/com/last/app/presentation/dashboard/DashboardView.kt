package com.last.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.last.app.R
import com.last.app.domain.model.dashboard.DashboardRecentEventItem
import com.last.app.data.entity.Device
import com.last.app.presentation.map.LastMapView
import com.last.app.presentation.components.LoadingBox
import com.last.app.domain.model.location.DeviceMapMarker
import com.last.app.presentation.layout.ScreenLayout
import com.last.app.presentation.layout.ScreenLayoutDefaults
import com.last.app.presentation.util.deviceIcon
import com.last.app.presentation.theme.LastCardBorder
import com.last.app.presentation.theme.LastIconBackground
import com.last.app.presentation.theme.LastPrimary
import com.last.app.presentation.theme.LastStatusConnected
import com.last.app.presentation.theme.LastStatusDisconnected
import com.last.app.presentation.theme.LastTextLight
import com.last.app.presentation.theme.LastTextMuted
import com.last.app.presentation.theme.LastTextPrimary

@Composable
fun DashboardView(
    viewModel: DashboardViewModel,
    isTabVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(isTabVisible) {
        viewModel.setScreenActive(isTabVisible)
    }
    val mapMarkers = uiState.mapMarkers

    ScreenLayout(
        title = stringResource(R.string.dashboard_title),
        modifier = modifier,
    ) {
        if (uiState.isLoading) {
            LoadingBox()
        } else if (uiState.devices.isEmpty()) {
            EmptyCard(
                message = stringResource(R.string.dashboard_no_devices),
                modifier = Modifier.padding(horizontal = ScreenLayoutDefaults.CONTENT_HORIZONTAL_PADDING_DP.dp),
            )
        } else {
            ConnectionStatusSection(
                devices = uiState.devices,
                selectedDevice = uiState.selectedDevice,
                onDeviceClick = viewModel::toggleDeviceSelection,
                modifier = Modifier.padding(horizontal = ScreenLayoutDefaults.CONTENT_HORIZONTAL_PADDING_DP.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))

            RecentConnectionHistorySection(
                events = uiState.recentEvents,
                modifier = Modifier.padding(horizontal = ScreenLayoutDefaults.CONTENT_HORIZONTAL_PADDING_DP.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))

            MapPreviewSection(
                markers = mapMarkers,
                isActive = isTabVisible,
                modifier = Modifier.padding(horizontal = ScreenLayoutDefaults.CONTENT_HORIZONTAL_PADDING_DP.dp),
            )
        }
    }
}

@Composable
private fun ConnectionStatusSection(
    devices: List<Device>,
    selectedDevice: Device?,
    onDeviceClick: (Device) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(R.string.dashboard_connection_status),
        icon = Icons.Outlined.Link,
        modifier = modifier,
    ) {
        devices.forEachIndexed { index, device ->
            if (index > 0) Spacer(Modifier.height(8.dp))
            DeviceStatusChip(
                label = device.deviceName,
                icon = deviceIcon(device.deviceType),
                connected = device.isConnected,
                selected = selectedDevice?.id == device.id,
                onClick = { onDeviceClick(device) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RecentConnectionHistorySection(
    events: List<DashboardRecentEventItem>,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(R.string.dashboard_recent_history),
        icon = Icons.Outlined.History,
        modifier = modifier,
    ) {
        if (events.isEmpty()) {
            Text(stringResource(R.string.dashboard_no_history), color = LastTextMuted, fontSize = 13.sp)
        } else {
            events.forEachIndexed { index, event ->
                if (index > 0) Spacer(Modifier.height(8.dp))
                RecentConnectionHistoryEventCard(event = event)
            }
        }
    }
}

@Composable
private fun RecentConnectionHistoryEventCard(event: DashboardRecentEventItem) {
    val statusColor = if (event.isDisconnect) LastStatusDisconnected else LastStatusConnected

    Card(
        Modifier
            .fillMaxWidth()
            .border(1.dp, LastCardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = event.timeLabel,
                color = LastTextLight,
                fontSize = 12.sp,
                modifier = Modifier.widthIn(min = 80.dp),
                maxLines = 1,
            )
            Icon(
                deviceIcon(event.deviceType),
                event.deviceName,
                Modifier.size(18.dp),
                LastTextMuted,
            )
            Text(
                text = event.deviceName,
                color = LastTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = event.eventStatusLabel,
                color = statusColor,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MapPreviewSection(
    markers: List<DeviceMapMarker>,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier.fillMaxWidth().aspectRatio(0.9f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        LastMapView(
            markers = markers,
            modifier = Modifier.fillMaxSize(),
            interactive = false,
            showBlankMapWhenEmpty = true,
            showMarker = markers.isNotEmpty(),
            isActive = isActive,
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(18.dp), LastPrimary)
                Text(
                    title,
                    Modifier.padding(start = 8.dp),
                    color = LastTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun EmptyCard(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Text(
            message,
            Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            color = LastTextMuted,
            fontSize = 14.sp,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun DeviceStatusChip(
    label: String,
    icon: ImageVector,
    connected: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dot = if (connected) LastStatusConnected else LastStatusDisconnected
    val borderColor = if (selected) LastPrimary else LastCardBorder
    val backgroundColor = if (selected) LastIconBackground else Color.White

    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Icon(icon, null, Modifier.size(18.dp), LastTextMuted)
        Text(label, color = LastTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
