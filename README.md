# Mein Weg – MVV Mobility Assistant

Android phase-1 prototype. All appointment and transit information is demo data. No live MVV, calendar or GPS integration yet.

## Build
JDK 17, Android SDK 35, Gradle 8.13.

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

GitHub Actions builds a debug APK artifact. Target device: Samsung Galaxy S25. Route preference: comfort; walking pace: individually calibrated in a later phase.
