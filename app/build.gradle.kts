plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
// Build-time region controls both the launcher icon and the visible region config.
// Example: ./gradlew :app:assembleDebug -PmeinWegRegion=berlin
val meinWegRegion = providers.gradleProperty("meinWegRegion").orNull?.lowercase() ?: "munich"
require(meinWegRegion in setOf("munich", "berlin", "frankfurt")) {
    "Unsupported meinWegRegion: $meinWegRegion (munich, berlin, frankfurt)"
}
android {
    namespace = "de.mvv.mobility"
    compileSdk = 35
    defaultConfig {
        applicationId = "de.mvv.mobility"
        minSdk = 28
        targetSdk = 35
        versionCode = providers.gradleProperty("meinWegVersionCode").orNull?.toIntOrNull() ?: 1
        versionName = providers.gradleProperty("meinWegVersionName").orNull
            ?: "0.2.0_build${providers.gradleProperty("meinWegVersionCode").orNull ?: "1"}"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["launcherIcon"] = "@mipmap/ic_launcher_$meinWegRegion"
        buildConfigField("String", "MOBILITY_REGION", "\"$meinWegRegion\"")
    }
    val releaseKeystore = System.getenv("MEIN_WEG_KEYSTORE_PATH")
    val ciDebugKeystore = System.getenv("MEIN_WEG_CI_SIGNING_STORE")
    val ciDebugStorePassword = System.getenv("MEIN_WEG_CI_SIGNING_STORE_PASSWORD")
    val ciDebugKeyAlias = System.getenv("MEIN_WEG_CI_SIGNING_KEY_ALIAS")
    val ciDebugKeyPassword = System.getenv("MEIN_WEG_CI_SIGNING_KEY_PASSWORD")
    val stableCiSigningReady = listOf(ciDebugKeystore, ciDebugStorePassword,
        ciDebugKeyAlias, ciDebugKeyPassword).all { !it.isNullOrBlank() }
    signingConfigs {
        if (stableCiSigningReady) {
            create("stableCiDebug") {
                storeFile = file(ciDebugKeystore!!)
                storePassword = ciDebugStorePassword
                keyAlias = ciDebugKeyAlias
                keyPassword = ciDebugKeyPassword
            }
        }
        if (!releaseKeystore.isNullOrBlank()) {
            create("stableRelease") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("MEIN_WEG_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("MEIN_WEG_KEY_ALIAS")
                keyPassword = System.getenv("MEIN_WEG_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("debug") {
            if (stableCiSigningReady) signingConfig = signingConfigs.getByName("stableCiDebug")
            buildConfigField("boolean", "ENABLE_MVV_DEBUG_EXPORT", "true")
            buildConfigField("boolean", "ENABLE_GPS_DEBUG_EXPORT", "true")
        }
        getByName("release") {
            buildConfigField("boolean", "ENABLE_MVV_DEBUG_EXPORT", "false")
            buildConfigField("boolean", "ENABLE_GPS_DEBUG_EXPORT", "false")
            isMinifyEnabled = false
            if (!releaseKeystore.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("stableRelease")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    testImplementation("junit:junit:4.13.2")
    // Real JSON implementation for offline JVM routing tests (Android org.json is stubbed).
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
