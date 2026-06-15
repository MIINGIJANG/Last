package com.last.app.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.last.app.R
import com.last.app.domain.model.history.HistoryEventDisplay
import com.last.app.domain.model.history.HistoryTimelineSection
import com.last.app.presentation.components.DeviceFilterChips
import com.last.app.presentation.components.LoadingBox
import com.last.app.presentation.layout.ScreenLayout
import com.last.app.presentation.layout.ScreenLayoutDefaults
import com.last.app.presentation.util.deviceIcon
import com.last.app.presentation.theme.LastCardBorder
import com.last.app.presentation.theme.LastDivider
import com.last.app.presentation.theme.LastIconBackground
import com.last.app.presentation.theme.LastPrimary
import com.last.app.presentation.theme.LastStatusConnected
import com.last.app.presentation.theme.LastStatusDisconnected
import com.last.app.presentation.theme.LastTextLight
import com.last.app.presentation.theme.LastTextPrimary
import com.last.app.presentation.theme.LastTextSecondary

@Composable
fun HistoryView(
    viewModel: HistoryViewModel,
    isTabVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(isTabVisible) {
        viewModel.setScreenActive(isTabVisible)
    }

    ScreenLayout(
        title = stringResource(R.string.history_title),
        modifier = modifier,
        scrollable = false,
    ) {
        if (!uiState.isLoading && uiState.devices.isNotEmpty()) {
            DeviceFilterChips(
                devices = uiState.devices,
                selectedDevice = uiState.selectedDevice,
                onSelect = viewModel::selectDevice,
                modifier = Modifier.padding(horizontal = ScreenLayoutDefaults.CONTENT_HORIZONTAL_PADDING_DP.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            uiState.isLoading -> LoadingBox(
                modifier = Modifier.weight(1f),
                height = 300.dp,
            )
            uiState.isResolvingLocations && uiState.sections.isEmpty() -> LoadingBox(
                modifier = Modifier.weight(1f),
                height = 300.dp,
            )
            else -> HistoryTimelineList(
                modifier = Modifier.weight(1f),
                sections = uiState.sections,
            )
        }
    }
}

@Composable
private fun HistoryTimelineList(
    sections: List<HistoryTimelineSection>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenLayoutDefaults.CONTENT_HORIZONTAL_PADDING_DP.dp),
    ) {
        sections.forEachIndexed { sectionIndex, section ->
            HistoryTimelineSectionRow(section = section)
            if (sectionIndex < sections.lastIndex) {
                Spacer(Modifier.height(16.dp))
            }
        }
        Spacer(Modifier.height(ScreenLayoutDefaults.SCROLL_CONTENT_BOTTOM_SPACING))
    }
}

@Composable
private fun HistoryTimelineSectionRow(
    section: HistoryTimelineSection,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (section.items.isEmpty()) {
                    Modifier.height(48.dp)
                } else {
                    Modifier.height(IntrinsicSize.Min)
                },
            ),
    ) {
        Box(Modifier.width(24.dp)) {
            Box(
                Modifier
                    .size(10.dp)
                    .align(Alignment.TopCenter)
                    .clip(CircleShape)
                    .background(LastPrimary),
            )
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .width(2.dp)
                    .then(
                        if (section.items.isEmpty()) {
                            Modifier.height(32.dp)
                        } else {
                            Modifier.fillMaxHeight()
                        },
                    )
                    .background(LastDivider),
            )
        }

        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = section.dateLabel,
                color = LastPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            if (section.items.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                section.items.forEachIndexed { index, item ->
                    HistoryLogCard(item)
                    if (index < section.items.lastIndex) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryLogCard(item: HistoryEventDisplay) {
    val statusColor = if (item.isDisconnect) LastStatusDisconnected else LastStatusConnected

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, LastCardBorder, RoundedCornerShape(10.dp))
                    .background(LastIconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    deviceIcon(item.deviceType),
                    item.deviceName,
                    Modifier.size(20.dp),
                    LastTextSecondary,
                )
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    item.deviceName,
                    color = LastTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    item.eventStatusLabel,
                    color = statusColor,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(item.timeLabel, color = LastTextLight, fontSize = 12.sp)
                Text(
                    item.locationLabel,
                    Modifier.padding(top = 4.dp),
                    color = LastTextLight,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
