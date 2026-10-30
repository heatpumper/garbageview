#!/usr/bin/env python3
"""Erzeugt die Standortdaten der App aus einem OSM-Extrakt (z. B. germany-latest.osm.pbf).

Aufruf: build_data.py <eingabe.osm.pbf> <ausgabeverzeichnis>

Schreibt je Region (REGION_SIZE Grad) eine Datei region_<x>_<y>.json.gz im Format, das
de.garbageview.core.CacheFormat liest: {"elements": [{"type", "id", "lat", "lon", "tags"}]}.
Dazu index.json mit Zeitstempel und Anzahl je Region.

Daten © OpenStreetMap-Mitwirkende, ODbL.
"""
import gzip
import json
import math
import os
import sys
import time
from collections import defaultdict

import osmium

# Muss zu de.garbageview.core.RegionGrid passen.
REGION_SIZE = 0.25

KEEP_KEYS = {
    "amenity", "recycling_type", "waste", "vending", "name", "operator",
    "opening_hours", "description", "note", "access", "fee",
}


def is_relevant(tags):
    amenity = tags.get("amenity")
    return amenity in ("waste_basket", "recycling", "waste_disposal") or tags.get("vending") == "excrement_bags"


def slim(tags):
    return {k: v for k, v in tags if k in KEEP_KEYS or k.startswith("recycling:")}


class Collector(osmium.SimpleHandler):
    def __init__(self):
        super().__init__()
        self.regions = defaultdict(list)
        self.count = 0

    def add(self, osm_type, osm_id, lat, lon, tags):
        x = math.floor(lon / REGION_SIZE)
        y = math.floor(lat / REGION_SIZE)
        self.regions[(x, y)].append({
            "type": osm_type, "id": osm_id,
            "lat": round(lat, 7), "lon": round(lon, 7),
            "tags": tags,
        })
        self.count += 1

    def node(self, n):
        if is_relevant(n.tags) and n.location.valid():
            self.add("node", n.id, n.location.lat, n.location.lon, slim(n.tags))

    def way(self, w):
        if not is_relevant(w.tags):
            return
        points = [(nd.lat, nd.lon) for nd in w.nodes if nd.location.valid()]
        if not points:
            return
        # Mittelpunkt der Bounding-Box, wie Overpass "out center".
        lats = [p[0] for p in points]
        lons = [p[1] for p in points]
        self.add("way", w.id, (min(lats) + max(lats)) / 2, (min(lons) + max(lons)) / 2, slim(w.tags))


def main(src, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    collector = Collector()
    collector.apply_file(src, locations=True)

    index = {"generated": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
             "regionSize": REGION_SIZE, "regions": {}}
    for (x, y), elements in sorted(collector.regions.items()):
        elements.sort(key=lambda e: (e["type"], e["id"]))
        name = f"region_{x}_{y}.json.gz"
        payload = json.dumps({"elements": elements}, ensure_ascii=False, separators=(",", ":"))
        with gzip.open(os.path.join(out_dir, name), "wt", encoding="utf-8", compresslevel=9) as f:
            f.write(payload)
        index["regions"][f"{x}_{y}"] = len(elements)
    with open(os.path.join(out_dir, "index.json"), "w", encoding="utf-8") as f:
        json.dump(index, f, separators=(",", ":"))
    print(f"{collector.count} Standorte in {len(collector.regions)} Regionen")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
