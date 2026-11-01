# GarbageView

Android-App, die Mülleimer, Glas-, Papier- und Altkleidercontainer, Hundekotbeutel-Spender und
Wertstoffhöfe in Deutschland auf einer Karte zeigt.

Status: **Prototyp**. Recherche der Datenquellen: [docs/DATENQUELLEN.md](docs/DATENQUELLEN.md).

**APK herunterladen:** https://github.com/heatpumper/garbageview/releases/download/prototyp/garbageview.apk
(wird bei jedem Push neu gebaut; Screenshot aus dem Emulator-Test im [Release](https://github.com/heatpumper/garbageview/releases/tag/prototyp)).

## Funktionen

- Karte (MapLibre + OpenFreeMap, kein API-Key nötig), auf Deutschland begrenzt
- Standorte aus OpenStreetMap, wöchentlich als Regionsdateien exportiert (siehe unten), ab Zoomstufe 13
- Filter nach Kategorie, Detailansicht mit Materialien, Öffnungszeiten und Betreiber
- „Navigation starten“ (geo:-Intent), „In OSM“ und „Fehler melden“ (OSM-Notiz)
- Eigener Standort (nur während der Nutzung)
- Je Region (0,25°, ca. 28 × 17 km) eine gepackte Datei, Cache auf dem Gerät für 3 Tage
- Statusleiste zeigt, ob und wonach gerade gesucht wird und was gefunden wurde

## Aufbau

| Modul | Inhalt |
|---|---|
| `core` | Reines Kotlin: Kategorien, Parser, Regionsraster, GeoJSON. Ohne Android testbar. |
| `app` | Android-App (Jetpack Compose, MapLibre, OkHttp) |

## Bauen

Voraussetzung: Android Studio bzw. Android SDK mit Platform 36, JDK 17+.

```bash
./gradlew :core:test           # Unit-Tests
./gradlew :app:assembleDebug   # Debug-APK
./gradlew -PcoreOnly :core:test   # nur core, ohne Android SDK
```

## Vor dem Play-Store-Release

- [x] Overpass ersetzt: eigener wöchentlicher OSM-Export (Geofabrik + osmium)
- [ ] Release-Signierung (Upload-Key) und App Bundle (`bundleRelease`)
- [ ] Endgültiges App-Icon, Screenshots, Store-Texte
- [ ] Datenschutzerklärung (URL) und Data-Safety-Formular
- [ ] Kartenstil-URL konfigurierbar machen bzw. Fallback-Anbieter

## CI

`.github/workflows/data.yml`: lädt wöchentlich den Deutschland-Extrakt von Geofabrik, filtert die
Abfall-Standorte (`osmium`), erzeugt Regionsdateien (`scripts/build_data.py`) und legt sie im
Release „daten“ ab. Die App lädt daraus nur die Regionen des sichtbaren Ausschnitts.
**Derzeit ausgesetzt:** läuft nur bei manuellem Start (Actions → Standortdaten → Run workflow).

`.github/workflows/android.yml`: Unit-Tests, verkleinerte Release-APK
(nur arm64-v8a/armeabi-v7a), Release „prototyp“, danach Start im Emulator mit Screenshot
(`scripts/smoke-test.sh`). Läuft nicht bei reinen Doku- oder Datenänderungen. Build-Artefakte
gibt es nur bei Pull Requests (3 Tage), sonst liegt die APK im Release.

## Lizenzen

Symbole: Material Symbols (Apache 2.0).

Standortdaten © OpenStreetMap-Mitwirkende, ODbL. Kartenkacheln: OpenFreeMap, © OpenMapTiles, Daten von OpenStreetMap.
