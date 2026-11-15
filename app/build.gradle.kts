plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "de.mvv.mobility"
    compileSdk = 35
    defaultConfig {
        applicationId = "de.mvv.mobility"
        minSdk = 28
        targetSdk = 35
        versionCode = providers.gradleProperty("meinWegVersionCode").orNull?.toIntOrNull() ?: 1
        versionName = providers.gradleProperty("meinWegVersionName").orNull ?: "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    val releaseKeystore = System.getenv("MEIN_WEG_KEYSTORE_PATH")
    signingConfigs {
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
        getByName("release") {
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
    buildFeatures { compose = true }
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
