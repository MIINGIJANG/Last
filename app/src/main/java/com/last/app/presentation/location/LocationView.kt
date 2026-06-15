package com.last.app.presentation.location

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.last.app.R
import com.last.app.presentation.components.DeviceFilterChips
import com.last.app.presentation.map.LastMapView
import com.last.app.presentation.components.LoadingBox
import com.last.app.presentation.components.LocationRecordCard
import com.last.app.domain.model.location.DeviceMapMarker
import com.last.app.presentation.layout.ScreenLayout
import com.last.app.presentation.layout.ScreenLayoutDefaults
import com.last.app.presentation.theme.LastCardBorder
import com.last.app.presentation.theme.LastPrimary

@Composable
fun LocationView(
    viewModel: LocationViewModel,
    isTabVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(isTabVisible) {
        viewModel.setScreenActive(isTabVisible)
    }

    val noCoordsMessage = stringResource(R.string.location_no_coords)
    val openMapsLabel = stringResource(R.string.location_open_maps)
    val recenterLabel = stringResource(R.string.location_recenter)
    val mapMarkers = uiState.markers
    val canRecenter = mapMarkers.isNotEmpty() ||
        (uiState.centerLatitude != null && uiState.centerLongitude != null)

    ScreenLayout(
        title = stringResource(R.string.location_title),
        modifier = modifier,
        scrollable = false,
    ) {
        if (uiState.isLoading) {
            LoadingBox(
                modifier = Modifier.weight(1f),
                height = 300.dp,
            )
        } else {
            if (uiState.devices.isNotEmpty()) {
                DeviceFilterChips(
                    devices = uiState.devices,
                    selectedDevice = uiState.selectedDevice,
                    onSelect = viewModel::selectDevice,
                    modifier = Modifier.padding(horizontal = ScreenLayoutDefaults.CONTENT_HORIZONTAL_PADDING_DP.dp),
                )
                Spacer(Modifier.height(16.dp))
            }

            Box(
                Modifier
                    .padding(horizontal = ScreenLayoutDefaults.CONTENT_HORIZONTAL_PADDING_DP.dp)
                    .fillMaxWidth()
                    .aspectRatio(0.9f),
            ) {
                Card(
                    Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .border(1.dp, LastCardBorder, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp)),
                    ) {
                        LastMapView(
                            markers = mapMarkers,
                            centerLatitude = uiState.centerLatitude.takeIf { mapMarkers.isEmpty() },
                            centerLongitude = uiState.centerLongitude.takeIf { mapMarkers.isEmpty() },
                            isActive = isTabVisible,
                            modifier = Modifier.fillMaxSize(),
                            interactive = isTabVisible,
                            showBlankMapWhenEmpty = true,
                            showMarker = mapMarkers.isNotEmpty(),
                        )
                    }
                }

                if (canRecenter) {
                    MapFloatingButton(
                        onClick = viewModel::recenterMap,
                        icon = { Icon(Icons.Outlined.MyLocation, recenterLabel, tint = LastPrimary) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp),
                    )
                }

                MapFloatingButton(
                    onClick = {
                        val target = when {
                            mapMarkers.size == 1 -> mapMarkers.first()
                            mapMarkers.size > 1 -> {
                                val avgLat = mapMarkers.map { it.latitude }.average()
                                val avgLng = mapMarkers.map { it.longitude }.average()
                                DeviceMapMarker(avgLat, avgLng, "", "")
                            }
                            else -> {
                                val lat = uiState.centerLatitude
                                val lng = uiState.centerLongitude
                                if (lat != null && lng != null) DeviceMapMarker(lat, lng, "", "") else null
                            }
                        }
                        if (target == null) {
                            Toast.makeText(context, noCoordsMessage, Toast.LENGTH_SHORT).show()
                            return@MapFloatingButton
                        }
                        val lat = target.latitude
                        val lng = target.longitude
                        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
                        val mapsIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        try {
                            context.startActivity(mapsIntent)
                        } catch (_: ActivityNotFoundException) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(context, noCoordsMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    icon = { Icon(Icons.Outlined.Explore, openMapsLabel, tint = LastPrimary) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp),
                )
            }

            if (mapMarkers.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    uiState.locationCards.forEachIndexed { index, card ->
                        LocationRecordCard(
                            deviceName = card.deviceName,
                            deviceType = card.deviceType,
                            statusLabel = card.eventStatusLabel,
                            isDisconnect = card.isDisconnect,
                            timeLabel = card.timeLabel,
                            locationLabel = card.locationLabel,
                            modifier = Modifier.padding(horizontal = ScreenLayoutDefaults.CONTENT_HORIZONTAL_PADDING_DP.dp),
                        )
                        if (index < uiState.locationCards.lastIndex) {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    Spacer(Modifier.height(ScreenLayoutDefaults.SCROLL_CONTENT_BOTTOM_SPACING))
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MapFloatingButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .shadow(4.dp, CircleShape)
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, LastCardBorder, CircleShape),
    ) {
        icon()
    }
}
