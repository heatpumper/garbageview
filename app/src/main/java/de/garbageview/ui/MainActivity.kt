package de.garbageview.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {

    private val viewModel: MapViewModel by viewModels { MapViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Optionaler Startausschnitt, z. B. für automatische Tests:
        // adb shell am start -n de.garbageview/.ui.MainActivity --ed lat 52.52 --ed lon 13.40 --ed zoom 15
        val initialCamera = intent.extras
            ?.takeIf { it.containsKey(EXTRA_LAT) && it.containsKey(EXTRA_LON) }
            ?.let { InitialCamera(it.getDouble(EXTRA_LAT), it.getDouble(EXTRA_LON), it.getDouble(EXTRA_ZOOM, 15.0)) }
        setContent {
            GarbageViewTheme {
                MapScreen(viewModel, initialCamera)
            }
        }
    }

    private companion object {
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
        const val EXTRA_ZOOM = "zoom"
    }
}
