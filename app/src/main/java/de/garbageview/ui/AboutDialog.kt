package de.garbageview.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.garbageview.BuildConfig

/** Quellen- und Lizenzhinweise (Pflicht für ODbL und OpenFreeMap). */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } },
        title = { Text("GarbageView ${BuildConfig.VERSION_NAME}") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Zeigt Mülleimer, Glas-, Papier- und Altkleidercontainer sowie Wertstoffhöfe in Deutschland.")
                Section(
                    "Standortdaten",
                    "© OpenStreetMap-Mitwirkende, verfügbar unter der Open Database License (ODbL). " +
                        "Einmal pro Woche aus dem OpenStreetMap-Auszug von Geofabrik erzeugt.",
                )
                TextButton(onClick = { context.openUrl("https://www.openstreetmap.org/copyright") }) {
                    Text("openstreetmap.org/copyright")
                }
                Section("Kartendarstellung", "OpenFreeMap, © OpenMapTiles, Daten von OpenStreetMap.")
                TextButton(onClick = { context.openUrl("https://openfreemap.org") }) {
                    Text("openfreemap.org")
                }
                Section(
                    "Fehlt etwas?",
                    "Die Daten pflegt die OpenStreetMap-Community. Über „Fehler melden“ in der Detailansicht " +
                        "kannst du einen Hinweis hinterlassen, oder du trägst Standorte direkt in OpenStreetMap ein.",
                )
                Section(
                    "Datenschutz",
                    "Dein Standort wird nur auf dem Gerät verwendet. Die App lädt Kartenkacheln von " +
                        "OpenFreeMap und Standortdaten für Regionen von etwa 28 × 17 km von GitHub. " +
                        "Beide Dienste sehen dabei technisch bedingt deine IP-Adresse.",
                )
            }
        },
    )
}

@Composable
private fun Section(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}
