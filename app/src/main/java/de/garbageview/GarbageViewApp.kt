package de.garbageview

import android.app.Application
import de.garbageview.data.OverpassClient
import de.garbageview.data.PoiRepository
import org.maplibre.android.MapLibre

class GarbageViewApp : Application() {

    val repository: PoiRepository by lazy {
        PoiRepository(OverpassClient(), cacheDir = cacheDir.resolve("overpass"))
    }

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}
