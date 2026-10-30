#!/usr/bin/env bash
# Startet die App im Emulator auf Berlin-Mitte, wartet auf geladene Standorte (mehr als 0) und macht
# einen Screenshot. Wird von .github/workflows/android.yml aufgerufen.
set -uo pipefail

APK=app/build/outputs/apk/debug/app-debug.apk
adb install -r "$APK"
adb logcat -c
adb shell am start -n de.garbageview/.ui.MainActivity --ed lat 52.5175 --ed lon 13.3950 --ed zoom 16

ok=0
for _ in $(seq 1 90); do
  if adb logcat -d -s GarbageView:I | grep -qE ": [1-9][0-9]* Standorte in"; then ok=1; break; fi
  if adb logcat -d | grep -q "FATAL EXCEPTION"; then break; fi
  sleep 2
done
# Zeit zum Zeichnen der Marker.
sleep 10

adb exec-out screencap -p > smoke-screenshot.png
adb logcat -d -s GarbageView:* AndroidRuntime:E > smoke-logcat.txt || true
tail -n 100 smoke-logcat.txt

if [ "$ok" != 1 ]; then
  echo "::error::Die App hat im Emulator keine Standorte geladen."
  exit 1
fi
