# Datenquellen für GarbageView

Stand der Recherche: 30.10.2026

Ziel: eine Android-App, die Mülleimer, Glascontainer, Altkleidercontainer, Wertstoffhöfe
und Ähnliches auf einer Karte zeigt und später im Play Store erscheint. Die Daten müssen
deshalb **kommerziell und in einer veröffentlichten App** nutzbar sein.

## 1. OpenStreetMap (OSM) – Hauptquelle

Die einzige Quelle, die flächendeckend und international alle gewünschten Kategorien hat.

| Kategorie | OSM-Tags |
|---|---|
| Mülleimer / Papierkorb | `amenity=waste_basket` (z. T. mit `waste=*`, z. B. `dog_excrement`) |
| Hundekotbeutel-Spender | `vending=excrement_bags` |
| Glascontainer | `amenity=recycling` + `recycling_type=container` + `recycling:glass_bottles=yes` (oft fälschlich `recycling:glass=yes`, beide abfragen) |
| Altkleider | `amenity=recycling` + `recycling:clothes=yes` (auch `recycling:shoes`) |
| Altpapier | `recycling:paper=yes` |
| Wertstoffhof / Recyclinghof | `amenity=recycling` + `recycling_type=centre` |
| Restmüll-Sammelstelle (Wohnanlagen, Camping) | `amenity=waste_disposal` |
| Elektroschrott, Batterien | `recycling:electrical_appliances`, `recycling:batteries` |
| Zusatzinfos | `opening_hours`, `operator`, `recycling:glass_bottles:colour` |

**Lizenz:** ODbL 1.0. Kommerzielle Nutzung erlaubt. Pflicht: sichtbare Quellenangabe
„© OpenStreetMap-Mitwirkende“ auf/neben der Karte. Wer eine eigene Datenbank aus OSM
erstellt und verteilt (z. B. vorab exportierte Offline-Pakete), muss diese Datenbank
ebenfalls unter ODbL bereitstellen (Share-Alike). Das Wort „OSM“ nicht im App-Namen
verwenden (Markenrecht der OSMF).

**Qualität:** Glascontainer und Wertstoffhöfe sind in Deutschland gut erfasst. Mülleimer
sind sehr ungleich erfasst (in manchen Städten fast vollständig, anderswo kaum).
→ Nutzer sollten fehlende Standorte melden bzw. direkt in OSM eintragen können.

### Zugriffswege auf die OSM-Daten

| Weg | Vorteile | Nachteile |
|---|---|---|
| **Overpass API, öffentliche Instanz** (overpass-api.de) | Live, keine eigene Infrastruktur | Nicht für Produktiv-Apps gedacht. Die Last aller App-Nutzer zählt zusammen; Faustregel für regelmäßige Nutzung: < 100 Abfragen/Tag, < 10 MB. Bei HTTP 429 drosseln. User-Agent mit App-Namen setzen. Nur für Prototyp/Entwicklung geeignet. |
| **Eigene Overpass-Instanz** (AGPL-3) | Live, keine Limits | Server nötig (Deutschland-Extrakt ca. 4 GB, Planet deutlich mehr) |
| **Kommerzielle Overpass-Hosts** (Geofabrik, Tracestrack, Overspan u. a.) | Kein eigener Betrieb | Kosten |
| **Eigener Vorab-Export** (empfohlen) | Schnell, billig, offline-fähig | Daten nur so aktuell wie der letzte Export |

**Empfohlene Architektur:** Ein geplanter Job (z. B. GitHub Action, wöchentlich) lädt den
Geofabrik-Extrakt (`germany-latest.osm.pbf`, ggf. DACH), filtert mit `osmium tags-filter`
nur die Abfall-Tags und erzeugt kompakte Kacheln (z. B. GeoJSON pro Region oder ein
PMTiles/MBTiles-Overlay). Diese Dateien liegen auf einem statischen Host/CDN. Die App lädt
nur die Kacheln der sichtbaren Region und speichert sie zwischen. Der Export wird unter ODbL
mit Quellenangabe bereitgestellt.

## 2. Kommunale Open-Data-Portale – Ergänzung

Viele Kommunen veröffentlichen Standorte ihrer Container, meist unter der
**Datenlizenz Deutschland – Namensnennung 2.0 (dl-de/by-2-0)** oder **CC BY / CC0**.
Diese Lizenzen erlauben kommerzielle Nutzung mit Quellenangabe.

Gefundene Beispiele:

| Kommune | Datensatz | Format | Lizenz |
|---|---|---|---|
| Münster | Standorte der Glascontainer (ca. 300 Standplätze, Weiß-/Buntglas, Unterflur) | CSV, GeoJSON | dl-de/by-2-0 |
| Münster | Standorte der Altkleider-Container | CSV, GeoJSON | dl-de/by-2-0 |
| Grevenbroich (Rhein-Kreis Neuss) | Glascontainer-Standorte | CSV, GeoJSON, Shapefile | Open.NRW |
| Soest | Altglascontainerstandorte im Stadtgebiet | Open.NRW | – |
| Dorsten | Glascontainerstandorte | Geoportal Ruhr | dl-de/by-2-0 |
| Recklinghausen | Glascontainer | opendata.ruhr | – |
| Bonn | Altpapier-Abgabe Großkartons u. a. | Open.NRW | – |
| Rostock | Altkleidercontainer | INSPIRE | CC0 |
| Wien | Altstoffsammelstellen (u. a.) | data.wien.gv.at | CC BY 4.0 |

Suchportale: [GovData](https://www.govdata.de), [Open.NRW](https://open.nrw),
[opendata.ruhr](https://opendata.ruhr), [data.gv.at](https://www.data.gv.at),
[opendata.swiss](https://opendata.swiss), [data.europa.eu](https://data.europa.eu).

**Einschränkungen:**
- Jede Kommune hat eigenes Format und eigene Felder → pro Quelle ein Adapter nötig.
- Teils veraltete Stände; regelmäßig prüfen.
- Große Städte fehlen oft: **Berlin** (Glascontainer nur auf der Website von Berlin Recycling
  und in der BSR-App, kein offener Datensatz gefunden), **Hamburg**, **München** (AWM
  Wertstoffinseln) – kein offener Datensatz gefunden.
- Für Papierkörbe gibt es fast keine kommunalen Datensätze.
- Vor dem Mischen mit OSM: Quellen getrennt halten. Daten aus offenen Portalen dürfen nur
  in OSM importiert werden, wenn die Lizenz kompatibel ist und die OSM-Importrichtlinien
  eingehalten werden.

## 3. Nicht geeignete Quellen

- **Websites/Apps von Entsorgern** (Berlin Recycling, BSR, AWM, Duales System, „Mülltrennung
  wirkt“-Containersuche): keine offene Lizenz. Scraping verstößt in der Regel gegen
  Nutzungsbedingungen bzw. Datenbankschutzrecht (§ 87a ff. UrhG). Nur mit schriftlicher
  Erlaubnis/Kooperation nutzen.
- **Google Places API**: kennt Mülleimer/Container praktisch nicht. Die Nutzungsbedingungen
  verbieten außerdem dauerhaftes Speichern.

## 4. Kartengrundlage (Hintergrundkarte)

| Anbieter | Kosten | Hinweise |
|---|---|---|
| **OpenFreeMap** (MapLibre-Style, z. B. `https://tiles.openfreemap.org/styles/liberty`) | kostenlos, kein API-Key, kommerziell erlaubt | Kein SLA, spendenfinanziert; Attribution wird von MapLibre automatisch angezeigt |
| **MapTiler / Stadia Maps / Thunderforest** | Free-Tier, danach kostenpflichtig | API-Key, SLA |
| **Selbst gehostete PMTiles** (Protomaps) | nur Hosting | Volle Kontrolle, offline-fähig |
| Google Maps SDK for Android | Kartendarstellung im SDK kostenlos | API-Key, Google-Bedingungen; schlecht mit OSM-Overlay kombinierbar |
| tile.openstreetmap.org | – | **Nicht verwenden**: Die Tile Usage Policy verbietet Offline-Download/Bulk und ist nicht für Apps mit vielen Nutzern gedacht |

**Empfehlung:** MapLibre Native Android + OpenFreeMap als Start, mit konfigurierbarer
Style-URL, damit später ohne App-Update auf MapTiler oder eigene PMTiles gewechselt
werden kann.

## 5. Nutzerbeiträge (später)

- Fehlende/falsche Standorte melden → entweder eigene Datenbank (eigene Lizenz klären) oder
  direkt als OSM-Notiz (`POST /api/0.6/notes`, ohne Login möglich) bzw. Bearbeitung per
  OSM-OAuth2. OSM-Notizen sind am einfachsten und verbessern die Daten für alle.

## 6. Konsequenzen für den Play Store

- Impressum/Quellenseite in der App: OSM (ODbL), OpenFreeMap/OpenMapTiles, jede
  kommunale Quelle mit Lizenzname und Link.
- Standortberechtigung nur „ungefährer/genauer Standort während der Nutzung“
  (kein Hintergrund-Standort) → einfache Data-Safety-Angaben.
- Datenschutzerklärung nötig (Standort wird nicht an eigenen Server geschickt, wenn nur
  Kacheln nach Bounding-Box geladen werden).

## Fazit / vorgeschlagener nächster Schritt

1. **OSM als einzige Pflichtquelle** für Version 1 (alle Kategorien, ganz DACH bzw. weltweit).
2. Prototyp: Kotlin + Jetpack Compose + MapLibre + OpenFreeMap, Daten zunächst über
   Overpass (nur Entwicklung, mit Cache und Drosselung).
3. Vor dem Release: Umstellung auf eigenen wöchentlichen OSM-Export auf statischem Hosting.
4. Kommunale Open-Data-Quellen als optionale Zusatzebene, beginnend mit Städten, in denen
   OSM-Daten lückenhaft sind.
