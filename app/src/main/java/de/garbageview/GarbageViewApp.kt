package de.garbageview

import android.app.Application
import de.garbageview.data.PoiRepository
import de.garbageview.data.RegionClient
import org.maplibre.android.MapLibre

class GarbageViewApp : Application() {

    val repository: PoiRepository by lazy {
        PoiRepository(RegionClient(), cacheDir = cacheDir.resolve("regions"))
    }

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}
