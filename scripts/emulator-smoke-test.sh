#!/usr/bin/env bash
set -euo pipefail

PACKAGE=de.mvv.mobility
APK=app/build/outputs/apk/debug/app-debug.apk
mkdir -p test-results
adb wait-for-device
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb install -r "$APK"
adb shell pm grant "$PACKAGE" android.permission.ACCESS_FINE_LOCATION
adb shell pm grant "$PACKAGE" android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant "$PACKAGE" android.permission.READ_CALENDAR
adb emu geo fix 11.576124 48.137154
adb shell am start -W -n "$PACKAGE/.MainActivity"
sleep 5
adb shell uiautomator dump /sdcard/window.xml >/dev/null
adb pull /sdcard/window.xml test-results/start.xml >/dev/null
grep -q 'Mein Weg' test-results/start.xml || { echo "ERROR: app title missing"; exit 1; }
adb shell dumpsys package "$PACKAGE" > test-results/package.txt
grep -q 'android.permission.INTERNET' test-results/package.txt || { echo "ERROR: INTERNET permission missing"; exit 1; }
# Swipe up and verify that controls below the fold are reachable.
adb shell input swipe 500 1450 500 380 450
sleep 2
adb shell uiautomator dump /sdcard/window.xml >/dev/null
adb pull /sdcard/window.xml test-results/scrolled.xml >/dev/null
grep -q 'Gehgeschwindigkeit' test-results/scrolled.xml || { echo "ERROR: speed control missing after scroll"; exit 1; }
grep -q '3,0 km/h' test-results/scrolled.xml || { echo "ERROR: default speed not 3.0 km/h"; exit 1; }
grep -q 'MVV-Verbindungen direkt anzeigen' test-results/scrolled.xml || { echo "ERROR: scroll did not reveal MVV action"; exit 1; }
adb shell screencap -p /sdcard/mein-weg.png
adb pull /sdcard/mein-weg.png test-results/screenshot.png >/dev/null
echo "PASS: startup, internet permission, default walking speed, scrolling and MVV button"
