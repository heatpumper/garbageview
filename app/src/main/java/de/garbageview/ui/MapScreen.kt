package de.garbageview.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.garbageview.core.BoundingBox
import de.garbageview.core.GeoJson
import de.garbageview.core.WasteCategory
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

/** Kostenloser Vektor-Kartenstil ohne API-Key, siehe docs/DATENQUELLEN.md. */
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val SOURCE_ID = "pois"
private const val LAYER_ID = "pois-circle"

/** Hält die MapLibre-Objekte über Recompositions hinweg. */
private class MapHolder {
    var map: MapLibreMap? = null

    /** Als Compose-State, damit die Daten nach dem Laden des Stils gesetzt werden. */
    var style: Style? by mutableStateOf(null)
}

@Composable
fun MapScreen(viewModel: MapViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val holder = remember { MapHolder() }
    var showAbout by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(context.hasLocationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasLocationPermission = result.values.any { it }
        if (hasLocationPermission) holder.enableLocation(context, moveCamera = true)
    }

    Box(Modifier.fillMaxSize()) {
        MapLibreView(
            holder = holder,
            onReady = {
                if (hasLocationPermission) holder.enableLocation(context, moveCamera = true)
            },
            onViewport = viewModel::onViewportChanged,
            onFeatureClick = viewModel::select,
        )

        // Daten in die Kartenquelle schreiben, sobald sie sich ändern.
        LaunchedEffect(state.pois, holder.style) {
            holder.style?.getSourceAs<GeoJsonSource>(SOURCE_ID)
                ?.setGeoJson(GeoJson.featureCollection(state.pois))
        }

        Column(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding(),
        ) {
            CategoryChips(enabled = state.enabled, onToggle = viewModel::toggle)
            if (state.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 12.dp))
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SmallFloatingActionButton(onClick = { showAbout = true }) {
                Icon(Icons.Filled.Info, contentDescription = "Info und Quellen")
            }
            FloatingActionButton(onClick = {
                if (hasLocationPermission) {
                    holder.enableLocation(context, moveCamera = true)
                } else {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                }
            }) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Mein Standort")
            }
            StatusCard(state)
        }
    }

    state.selected?.let { poi ->
        PoiDetailsSheet(poi = poi, onDismiss = { viewModel.select(null) })
    }
    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
}

@Composable
private fun MapLibreView(
    holder: MapHolder,
    onReady: () -> Unit,
    onViewport: (BoundingBox, Double) -> Unit,
    onFeatureClick: (String?) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                holder.map = map
                map.uiSettings.setAttributionMargins(0, 0, 0, 0)
                map.setMinZoomPreference(5.0)
                map.setLatLngBoundsForCameraTarget(
                    LatLngBounds.from(
                        BoundingBox.GERMANY.north, BoundingBox.GERMANY.east,
                        BoundingBox.GERMANY.south, BoundingBox.GERMANY.west,
                    ),
                )
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(51.16, 10.45), 5.5))
                map.setStyle(Style.Builder().fromUri(STYLE_URL)) { style ->
                    style.addSource(GeoJsonSource(SOURCE_ID, GeoJson.featureCollection(emptyList())))
                    style.addLayer(poiLayer())
                    holder.style = style
                    onReady()
                }
                map.addOnCameraIdleListener {
                    val bounds = map.projection.visibleRegion.latLngBounds
                    onViewport(
                        BoundingBox(
                            south = bounds.latitudeSouth,
                            west = bounds.longitudeWest,
                            north = bounds.latitudeNorth,
                            east = bounds.longitudeEast,
                        ),
                        map.cameraPosition.zoom,
                    )
                }
                map.addOnMapClickListener { latLng ->
                    val point = map.projection.toScreenLocation(latLng)
                    val r = 16 * resources.displayMetrics.density
                    val features = map.queryRenderedFeatures(
                        RectF(point.x - r, point.y - r, point.x + r, point.y + r),
                        LAYER_ID,
                    )
                    val key = features.firstOrNull()?.getStringProperty(GeoJson.PROP_KEY)
                    if (key != null) onFeatureClick(key)
                    key != null
                }
            }
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
            holder.map = null
            holder.style = null
        }
    }

    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
}

private fun poiLayer(): CircleLayer {
    val stops = WasteCategory.entries.map {
        Expression.stop(it.name, Expression.color(android.graphics.Color.parseColor(it.colorHex)))
    }.toTypedArray()
    return CircleLayer(LAYER_ID, SOURCE_ID).withProperties(
        PropertyFactory.circleColor(
            Expression.match(Expression.get(GeoJson.PROP_CATEGORY), Expression.color(android.graphics.Color.GRAY), *stops),
        ),
        PropertyFactory.circleRadius(
            Expression.interpolate(
                Expression.linear(), Expression.zoom(),
                Expression.stop(13, 3.5f), Expression.stop(16, 6f), Expression.stop(19, 10f),
            ),
        ),
        PropertyFactory.circleStrokeColor(Expression.color(android.graphics.Color.WHITE)),
        PropertyFactory.circleStrokeWidth(1.5f),
    )
}

@SuppressLint("MissingPermission")
private fun MapHolder.enableLocation(context: Context, moveCamera: Boolean) {
    val map = map ?: return
    val style = style ?: return
    if (!context.hasLocationPermission()) return
    val component = map.locationComponent
    if (!component.isLocationComponentActivated) {
        component.activateLocationComponent(
            LocationComponentActivationOptions.builder(context, style).useDefaultLocationEngine(true).build(),
        )
    }
    component.isLocationComponentEnabled = true
    component.renderMode = RenderMode.COMPASS
    if (moveCamera) {
        val last = component.lastKnownLocation
        if (last != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(last.latitude, last.longitude), 16.0))
        } else {
            component.cameraMode = CameraMode.TRACKING
            map.animateCamera(CameraUpdateFactory.zoomTo(16.0))
        }
    }
}

private fun Context.hasLocationPermission(): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

@Composable
private fun CategoryChips(enabled: Set<WasteCategory>, onToggle: (WasteCategory) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(WasteCategory.entries) { category ->
            FilterChip(
                selected = category in enabled,
                onClick = { onToggle(category) },
                label = { Text(category.label) },
                leadingIcon = { CategoryDot(category) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    }
}

@Composable
fun CategoryDot(category: WasteCategory, size: Int = 12) {
    Box(
        Modifier
            .size(size.dp)
            .background(Color(android.graphics.Color.parseColor(category.colorHex)), CircleShape),
    )
}

@Composable
private fun StatusCard(state: MapUiState) {
    val text = when {
        state.error != null -> state.error
        state.zoomTooLow -> "Näher heranzoomen, um Standorte zu laden"
        !state.loading && state.pois.isEmpty() -> "Keine Standorte in diesem Ausschnitt"
        else -> null
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (text != null) {
                Text(text, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.size(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Daten © OpenStreetMap-Mitwirkende (ODbL)",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                )
                if (!state.zoomTooLow) {
                    Text("${state.pois.size} Standorte", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
