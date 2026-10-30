package de.garbageview.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.garbageview.R
import de.garbageview.core.WastePoi
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiDetailsSheet(poi: WastePoi, onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(poi.category, size = 28.dp)
                Spacer(Modifier.size(8.dp))
                Text(poi.category.label, style = MaterialTheme.typography.labelLarge)
            }
            Text(
                poi.name ?: poi.category.label,
                style = MaterialTheme.typography.headlineSmall,
            )
            val others = poi.categories - poi.category
            if (others.isNotEmpty()) {
                Text("Auch: " + others.joinToString(", ") { it.label })
            }
            if (poi.acceptedMaterials.isNotEmpty()) {
                Detail("Annahme", poi.acceptedMaterials.joinToString(", "))
            }
            poi.openingHours?.let { Detail("Öffnungszeiten", it) }
            poi.operator?.let { Detail("Betreiber", it) }
            poi.description?.let { Detail("Hinweis", it) }

            Spacer(Modifier.size(8.dp))
            Button(onClick = { context.openNavigation(poi) }, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.ic_navigation), contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Navigation starten")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { context.openUrl(poi.osmUrl) }, modifier = Modifier.weight(1f)) {
                    Icon(painterResource(R.drawable.ic_map), contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("In OSM")
                }
                OutlinedButton(
                    onClick = {
                        context.openUrl(
                            String.format(
                                Locale.ROOT,
                                "https://www.openstreetmap.org/note/new#map=19/%.6f/%.6f",
                                poi.lat, poi.lon,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(painterResource(R.drawable.ic_flag), contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Fehler melden")
                }
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun Context.openNavigation(poi: WastePoi) {
    val label = Uri.encode(poi.name ?: poi.category.label)
    val uri = Uri.parse(String.format(Locale.ROOT, "geo:%.6f,%.6f?q=%.6f,%.6f(%s)", poi.lat, poi.lon, poi.lat, poi.lon, label))
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: ActivityNotFoundException) {
        openUrl(String.format(Locale.ROOT, "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=18/%.6f/%.6f", poi.lat, poi.lon, poi.lat, poi.lon))
    }
}

fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        // Kein Browser installiert – nichts zu tun.
    }
}
