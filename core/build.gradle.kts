import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

tasks.test {
    // Live-Test soll Umgebungsänderungen nicht aus dem Build-Cache überspringen.
    inputs.property("overpassLive", System.getenv("OVERPASS_LIVE_TEST") ?: "")
    testLogging {
        showStandardStreams = true
        events("failed")
    }
}
