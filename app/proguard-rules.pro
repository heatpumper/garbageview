# MapLibre nutzt JNI und Reflection.
-keep class org.maplibre.** { *; }
-dontwarn org.maplibre.**

# Optionale Abhängigkeiten, die zur Laufzeit nicht gebraucht werden.
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
