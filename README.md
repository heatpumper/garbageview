# GarbageView

Android-App, die Mülleimer, Glas-, Papier- und Altkleidercontainer, Hundekotbeutel-Spender und
Wertstoffhöfe in Deutschland auf einer Karte zeigt.

Status: **Prototyp**. Recherche der Datenquellen: [docs/DATENQUELLEN.md](docs/DATENQUELLEN.md).

**APK herunterladen:** https://github.com/heatpumper/garbageview/releases/download/prototyp/garbageview.apk
(wird bei jedem Push neu gebaut; Screenshot aus dem Emulator-Test im [Release](https://github.com/heatpumper/garbageview/releases/tag/prototyp)).

## Funktionen

- Karte (MapLibre + OpenFreeMap, kein API-Key nötig), auf Deutschland begrenzt
- Standorte aus OpenStreetMap, live über die Overpass API, ab Zoomstufe 13
- Filter nach Kategorie, Detailansicht mit Materialien, Öffnungszeiten und Betreiber
- „Navigation starten“ (geo:-Intent), „In OSM“ und „Fehler melden“ (OSM-Notiz)
- Eigener Standort (nur während der Nutzung)
- Eine Overpass-Abfrage pro Kartenausschnitt, Kachel-Cache (Speicher + Datei, 7 Tage)
- Statusleiste zeigt, ob und wonach gerade gesucht wird und was gefunden wurde

## Aufbau

| Modul | Inhalt |
|---|---|
| `core` | Reines Kotlin: Kategorien, Overpass-Abfrage, Parser, Kachelraster, GeoJSON. Ohne Android testbar. |
| `app` | Android-App (Jetpack Compose, MapLibre, OkHttp) |

## Bauen

Voraussetzung: Android Studio bzw. Android SDK mit Platform 36, JDK 17+.

```bash
./gradlew :core:test           # Unit-Tests
./gradlew :app:assembleDebug   # Debug-APK
./gradlew -PcoreOnly :core:test   # nur core, ohne Android SDK
```

## Vor dem Play-Store-Release

- [ ] Overpass ersetzen: eigener wöchentlicher OSM-Export (Geofabrik + osmium) auf statischem Hosting
- [ ] Release-Signierung (Upload-Key) und App Bundle (`bundleRelease`)
- [ ] Endgültiges App-Icon, Screenshots, Store-Texte
- [ ] Datenschutzerklärung (URL) und Data-Safety-Formular
- [ ] Kartenstil-URL konfigurierbar machen bzw. Fallback-Anbieter

## CI

`.github/workflows/android.yml`: Unit-Tests inkl. Live-Abfrage an Overpass, verkleinerte Release-APK
(nur arm64-v8a/armeabi-v7a), Release „prototyp“, danach Start im Emulator mit Screenshot
(`scripts/smoke-test.sh`).

## Lizenzen

Symbole: Material Symbols (Apache 2.0).

Standortdaten © OpenStreetMap-Mitwirkende, ODbL. Kartenkacheln: OpenFreeMap, © OpenMapTiles, Daten von OpenStreetMap.
