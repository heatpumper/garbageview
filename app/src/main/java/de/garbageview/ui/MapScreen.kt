package de.garbageview.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.garbageview.R
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
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

/** Kostenloser Vektor-Kartenstil ohne API-Key, siehe docs/DATENQUELLEN.md. */
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val SOURCE_ID = "pois"
private const val LAYER_ID = "pois-symbols"

/** Startausschnitt; ohne Angabe wird ganz Deutschland gezeigt. */
data class InitialCamera(val lat: Double, val lon: Double, val zoom: Double)

/** Hält die MapLibre-Objekte über Recompositions hinweg. */
private class MapHolder {
    var map: MapLibreMap? = null

    /** Als Compose-State, damit die Daten nach dem Laden des Stils gesetzt werden. */
    var style: Style? by mutableStateOf(null)
}

@Composable
fun MapScreen(viewModel: MapViewModel, initialCamera: InitialCamera? = null) {
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
            initialCamera = initialCamera,
            onReady = {
                if (hasLocationPermission) holder.enableLocation(context, moveCamera = initialCamera == null)
            },
            onViewport = viewModel::onViewportChanged,
            onFeatureClick = viewModel::select,
        )

        // Daten in die Kartenquelle schreiben, sobald sie sich ändern.
        LaunchedEffect(state.pois, holder.style) {
            holder.style?.getSourceAs<GeoJsonSource>(SOURCE_ID)
                ?.setGeoJson(GeoJson.featureCollection(state.pois))
        }

        CategoryChips(
            enabled = state.enabled,
            counts = state.counts.takeIf { state.status == SearchStatus.Done },
            onToggle = viewModel::toggle,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding(),
        )

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
                Icon(painterResource(R.drawable.ic_info), contentDescription = "Info und Quellen")
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
                Icon(painterResource(R.drawable.ic_my_location), contentDescription = "Mein Standort")
            }
            StatusCard(state, onRetry = viewModel::retry)
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
    initialCamera: InitialCamera?,
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
                val start = initialCamera ?: InitialCamera(51.16, 10.45, 5.5)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(start.lat, start.lon), start.zoom))
                map.setStyle(Style.Builder().fromUri(STYLE_URL)) { style ->
                    WasteCategory.entries.forEach { style.addImage(it.name, markerBitmap(context, it)) }
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

/** Marker mit Kategoriesymbol; die Bilder heißen wie die Kategorie (siehe addImage). */
private fun poiLayer(): SymbolLayer =
    SymbolLayer(LAYER_ID, SOURCE_ID).withProperties(
        PropertyFactory.iconImage(Expression.get(GeoJson.PROP_CATEGORY)),
        PropertyFactory.iconAllowOverlap(true),
        PropertyFactory.iconIgnorePlacement(true),
        PropertyFactory.iconSize(
            Expression.interpolate(
                Expression.linear(), Expression.zoom(),
                Expression.stop(13, 0.55f), Expression.stop(16, 0.8f), Expression.stop(19, 1.1f),
            ),
        ),
    )

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

/**
 * Filter je Kategorie. Aktive Kategorien zeigen das farbige Kartensymbol, inaktive ein graues.
 * Nach einer Suche steht die Anzahl im sichtbaren Ausschnitt dahinter.
 */
@Composable
private fun CategoryChips(
    enabled: Set<WasteCategory>,
    counts: Map<WasteCategory, Int>?,
    onToggle: (WasteCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(WasteCategory.entries) { category ->
            val selected = category in enabled
            val count = counts?.get(category)
            FilterChip(
                selected = selected,
                onClick = { onToggle(category) },
                label = { Text(if (count != null) "${category.label} · $count" else category.label) },
                leadingIcon = { CategoryBadge(category, size = 20.dp, enabled = selected) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    }
}

/** Zeigt, ob und wonach gerade gesucht wird und was gefunden wurde. */
@Composable
private fun StatusCard(state: MapUiState, onRetry: () -> Unit) {
    val active = WasteCategory.entries.filter { it in state.enabled }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (val status = state.status) {
                    SearchStatus.ZoomTooLow -> {
                        Icon(painterResource(R.drawable.ic_search_off), contentDescription = null)
                        Spacer(Modifier.size(10.dp))
                        Text("Zum Suchen näher heranzoomen", style = MaterialTheme.typography.bodyMedium)
                    }
                    SearchStatus.Searching -> {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Suche in diesem Ausschnitt nach:", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.size(4.dp))
                            BadgeRow(active)
                        }
                    }
                    SearchStatus.Done -> {
                        val found = active.filter { (state.counts[it] ?: 0) > 0 }
                        Column(Modifier.weight(1f)) {
                            if (found.isEmpty()) {
                                Text(
                                    if (active.isEmpty()) "Keine Kategorie ausgewählt"
                                    else "Hier nichts gefunden",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            } else {
                                Text(
                                    "${state.pois.size} Standorte gefunden",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(Modifier.size(4.dp))
                                BadgeRow(found, state.counts)
                            }
                        }
                    }
                    is SearchStatus.Failed -> {
                        Column(Modifier.weight(1f)) {
                            Text("Suche fehlgeschlagen", style = MaterialTheme.typography.bodyMedium)
                            Text(status.message, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = onRetry) {
                            Icon(painterResource(R.drawable.ic_refresh), contentDescription = null)
                            Spacer(Modifier.size(4.dp))
                            Text("Erneut")
                        }
                    }
                }
            }
            Spacer(Modifier.size(6.dp))
            Text(
                "Daten © OpenStreetMap-Mitwirkende (ODbL)",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BadgeRow(categories: List<WasteCategory>, counts: Map<WasteCategory, Int>? = null) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(categories) { category ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(category, size = 18.dp)
                counts?.get(category)?.let {
                    Spacer(Modifier.size(4.dp))
                    Text("$it", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
