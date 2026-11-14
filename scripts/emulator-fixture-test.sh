#!/usr/bin/env bash
set -euo pipefail
mkdir -p test-results
# Gradle connectedDebugAndroidTest uninstalls the tested APK; reinstall before UI fixture run.
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant de.mvv.mobility android.permission.READ_CALENDAR
adb shell pm grant de.mvv.mobility android.permission.ACCESS_FINE_LOCATION
adb shell am force-stop de.mvv.mobility
adb shell am start -W -n de.mvv.mobility/.MainActivity --ez mein_weg_test_fixture true
sleep 4
adb shell uiautomator dump /sdcard/fixture.xml >/dev/null
adb pull /sdcard/fixture.xml test-results/fixture-start.xml >/dev/null
grep -q 'TESTMODUS' test-results/fixture-start.xml
grep -q 'Testtermin Fußball' test-results/fixture-start.xml
grep -q 'Olympiapark München' test-results/fixture-start.xml
adb shell input swipe 500 1500 500 300 400
sleep 2
adb shell uiautomator dump /sdcard/fixture.xml >/dev/null
adb pull /sdcard/fixture.xml test-results/fixture-scroll.xml >/dev/null
python3 - <<'PY'
import re, subprocess, xml.etree.ElementTree as ET
root=ET.parse('test-results/fixture-scroll.xml').getroot()
for node in root.iter('node'):
    if 'MVV-Verbindungen direkt anzeigen' in node.attrib.get('text',''):
        b=node.attrib['bounds']
        x1,y1,x2,y2=map(int,re.findall(r'\d+',b))
        subprocess.run(['adb','shell','input','tap',str((x1+x2)//2),str((y1+y2)//2)],check=True)
        break
else:
    raise SystemExit('MVV button not visible in fixture mode')
PY
sleep 3
# The result card may appear below the viewport after tapping the button.
adb shell input swipe 500 1500 500 350 400
sleep 2
adb shell uiautomator dump /sdcard/fixture.xml >/dev/null
adb pull /sdcard/fixture.xml test-results/fixture-result.xml >/dev/null
if ! grep -q 'Testverbindung geladen' test-results/fixture-result.xml || ! grep -q 'U3' test-results/fixture-result.xml; then
    echo "ERROR: expected fixture result is not visible; UI texts:"
    python3 - <<'PY'
import xml.etree.ElementTree as ET
root=ET.parse('test-results/fixture-result.xml').getroot()
print(' | '.join(n.attrib.get('text','') for n in root.iter('node') if n.attrib.get('text')))
PY
    adb shell screencap -p /sdcard/fixture-failure.png
    adb pull /sdcard/fixture-failure.png test-results/fixture-failure.png >/dev/null
    exit 1
fi
adb shell screencap -p /sdcard/fixture.png
adb pull /sdcard/fixture.png test-results/fixture-screenshot.png >/dev/null
echo "PASS: debug fixture calendar, GPS, route search, MVV rendering"
