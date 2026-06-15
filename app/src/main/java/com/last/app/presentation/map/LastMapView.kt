package com.last.app.presentation.map

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.last.app.R
import com.last.app.domain.model.location.DeviceMapMarker
import com.last.app.presentation.theme.LastTextMuted
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker

private const val DEFAULT_LATITUDE = 36.350411
private const val DEFAULT_LONGITUDE = 127.384548
private const val DEFAULT_ZOOM = 7.0
private const val LOCATED_ZOOM = 18.0
private const val BOUNDING_BOX_PADDING_PX = 120
private const val MAP_UPDATE_DEBOUNCE_MS = 120L

@Composable
fun LastMapView(
    latitude: Double? = null,
    longitude: Double? = null,
    markers: List<DeviceMapMarker> = emptyList(),
    centerLatitude: Double? = null,
    centerLongitude: Double? = null,
    markerDeviceType: String? = null,
    modifier: Modifier = Modifier,
    zoom: Double = LOCATED_ZOOM,
    interactive: Boolean = true,
    showBlankMapWhenEmpty: Boolean = false,
    showMarker: Boolean = true,
    isActive: Boolean = true,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val emptyMessage = stringResource(R.string.dashboard_no_location)
    val effectiveMarkers = when {
        markers.isNotEmpty() -> markers
        showMarker && latitude != null && longitude != null -> {
            listOf(
                DeviceMapMarker(
                    latitude = latitude,
                    longitude = longitude,
                    deviceName = "",
                    deviceType = markerDeviceType.orEmpty(),
                ),
            )
        }
        else -> emptyList()
    }
    val hasMarkers = effectiveMarkers.isNotEmpty()

    if (!hasMarkers && !showBlankMapWhenEmpty) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = emptyMessage, color = LastTextMuted)
        }
        return
    }

    if (!isActive) {
        Box(modifier = modifier)
        return
    }

    val markerIcon = remember(appContext) { MapMarkerIconFactory.create(appContext) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var mapState by remember {
        mutableStateOf(
            AppliedMapState(
                markers = effectiveMarkers,
                centerLatitude = centerLatitude,
                centerLongitude = centerLongitude,
                zoom = zoom,
                showBlankMapWhenEmpty = showBlankMapWhenEmpty,
            ),
        )
    }

    LaunchedEffect(effectiveMarkers, centerLatitude, centerLongitude, zoom, showBlankMapWhenEmpty) {
        val nextState = AppliedMapState(
            markers = effectiveMarkers,
            centerLatitude = centerLatitude,
            centerLongitude = centerLongitude,
            zoom = zoom,
            showBlankMapWhenEmpty = showBlankMapWhenEmpty,
        )
        if (mapState != nextState) {
            delay(MAP_UPDATE_DEBOUNCE_MS)
            mapState = nextState
        }
    }

    val currentMapState = mapState

    AndroidView(
        modifier = modifier,
        factory = {
            createMapView(appContext, interactive).also { created ->
                mapViewRef = created
            }
        },
        update = { view ->
            mapViewRef = view
            updateMapView(
                mapView = view,
                markers = currentMapState.markers,
                centerLatitude = currentMapState.centerLatitude,
                centerLongitude = currentMapState.centerLongitude,
                zoom = currentMapState.zoom,
                showBlankMapWhenEmpty = currentMapState.showBlankMapWhenEmpty,
                markerIcon = markerIcon,
            )
        },
        onRelease = { view ->
            view.onPause()
            view.onDetach()
            if (mapViewRef === view) {
                mapViewRef = null
            }
        },
    )

    LaunchedEffect(isActive) {
        if (isActive) {
            mapViewRef?.onResume()
        } else {
            mapViewRef?.onPause()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onPause()
        }
    }
}

private fun createMapView(
    context: Context,
    interactive: Boolean,
): MapView {
    ensureOsmConfig(context)
    return MapView(context).apply {
        setTileSource(MapTileSources.cartoVoyager)
        setMultiTouchControls(interactive)
        isClickable = interactive
        isFocusable = interactive
        isTilesScaledToDpi = true
        setBuiltInZoomControls(false)
        setHorizontalMapRepetitionEnabled(false)
        setVerticalMapRepetitionEnabled(false)
        minZoomLevel = 5.0
        maxZoomLevel = 20.0
        overlayManager.tilesOverlay.setLoadingBackgroundColor(0xFFF8F9FA.toInt())
    }
}

private data class AppliedMapState(
    val markers: List<DeviceMapMarker>,
    val centerLatitude: Double?,
    val centerLongitude: Double?,
    val zoom: Double,
    val showBlankMapWhenEmpty: Boolean,
)

private fun updateMapView(
    mapView: MapView,
    markers: List<DeviceMapMarker>,
    centerLatitude: Double?,
    centerLongitude: Double?,
    zoom: Double,
    showBlankMapWhenEmpty: Boolean,
    markerIcon: BitmapDrawable,
) {
    val nextState = AppliedMapState(
        markers = markers,
        centerLatitude = centerLatitude,
        centerLongitude = centerLongitude,
        zoom = zoom,
        showBlankMapWhenEmpty = showBlankMapWhenEmpty,
    )
    val previousState = mapView.tag as? AppliedMapState
    if (previousState == nextState) return
    mapView.tag = nextState

    mapView.post {
        mapView.overlays.removeAll { it is Marker || it is CopyrightOverlay }

        if (markers.isEmpty()) {
            if (centerLatitude != null && centerLongitude != null) {
                mapView.controller.setZoom(zoom)
                mapView.controller.setCenter(GeoPoint(centerLatitude, centerLongitude))
            } else {
                mapView.controller.setZoom(if (showBlankMapWhenEmpty) DEFAULT_ZOOM else zoom)
                mapView.controller.setCenter(GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
            }
            mapView.overlays.add(CopyrightOverlay(mapView.context))
            mapView.invalidate()
            return@post
        }

        val geoPoints = markers.map { GeoPoint(it.latitude, it.longitude) }
        markers.forEach { marker ->
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = GeoPoint(marker.latitude, marker.longitude)
                    icon = markerIcon
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    infoWindow = null
                },
            )
        }

        when {
            markers.size == 1 -> {
                mapView.controller.setZoom(zoom)
                mapView.controller.setCenter(geoPoints.first())
            }
            else -> {
                val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                mapView.zoomToBoundingBox(boundingBox, false, BOUNDING_BOX_PADDING_PX)
            }
        }
        mapView.overlays.add(CopyrightOverlay(mapView.context))
        mapView.invalidate()
    }
}

private fun ensureOsmConfig(context: Context) {
    val configuration = Configuration.getInstance()
    if (configuration.userAgentValue.isNullOrBlank()) {
        val preferences = context.getSharedPreferences("osmdroid", MODE_PRIVATE)
        configuration.load(context, preferences)
        configuration.userAgentValue = context.packageName
    }
}
