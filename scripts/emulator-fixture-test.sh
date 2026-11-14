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
# Find the actual clickable MVV button; do not tap off-screen coordinates.
python3 - <<'PY'
import re, subprocess, time, xml.etree.ElementTree as ET

def adb(*args):
    return subprocess.check_output(['adb', *args], text=True, stderr=subprocess.STDOUT)

def dump(name):
    adb('shell','uiautomator','dump','/sdcard/fixture.xml')
    subprocess.run(['adb','pull','/sdcard/fixture.xml','test-results/'+name+'.xml'],check=True,stdout=subprocess.DEVNULL)
    return ET.parse('test-results/'+name+'.xml').getroot()

def foreground():
    return adb('shell','dumpsys','activity','activities')

def ensure_app(stage):
    state=foreground()
    if 'de.mvv.mobility/.MainActivity' not in state and 'de.mvv.mobility/de.mvv.mobility.MainActivity' not in state:
        print('ERROR: app left foreground at',stage)
        print('\\n'.join(line for line in state.splitlines() if 'Resumed' in line or 'mTop' in line)[:2000])
        subprocess.run(['adb','shell','screencap','-p','/sdcard/fixture-failure.png'])
        subprocess.run(['adb','pull','/sdcard/fixture-failure.png','test-results/fixture-failure.png'])
        raise SystemExit(1)

ensure_app('after launch')
for attempt in range(7):
    root=dump('fixture-scroll')
    matches=[n for n in root.iter('node') if 'MVV-Verbindungen direkt anzeigen' in n.attrib.get('text','')]
    if matches:
        n=matches[0]
        x1,y1,x2,y2=map(int,re.findall(r'\d+',n.attrib['bounds']))
        screen=adb('shell','wm','size')
        height=int(re.search(r'(\d+)x(\d+)',screen).group(2))
        # Ensure the center of the button is well inside the display.
        if 80 < (y1+y2)//2 < height-120 and n.attrib.get('enabled')=='true':
            subprocess.run(['adb','shell','input','tap',str((x1+x2)//2),str((y1+y2)//2)],check=True)
            time.sleep(2)
            ensure_app('after MVV tap')
            break
    subprocess.run(['adb','shell','input','swipe','500','1350','500','750','350'],check=True)
    time.sleep(1)
    ensure_app('after scroll')
else:
    raise SystemExit('ERROR: MVV button not found enabled and onscreen')

seen_u3 = False
seen_status = False
for attempt in range(5):
    root=dump('fixture-result')
    texts=[n.attrib.get('text','') for n in root.iter('node') if n.attrib.get('text')]
    seen_u3 |= any('U3' in t for t in texts)
    seen_status |= any('Testverbindung geladen' in t for t in texts)
    if seen_u3 and seen_status:
        print('PASS: MVV fixture result and U3 visible')
        break
    print('Result viewport',attempt,':',' | '.join(texts)[:1200])
    subprocess.run(['adb','shell','input','swipe','500','1350','500','800','350'],check=True)
    time.sleep(1)
    ensure_app('during result scroll')
else:
    print('ERROR: MVV fixture result not visible')
    subprocess.run(['adb','shell','screencap','-p','/sdcard/fixture-failure.png'])
    subprocess.run(['adb','pull','/sdcard/fixture-failure.png','test-results/fixture-failure.png'])
    raise SystemExit(1)
PY
adb shell screencap -p /sdcard/fixture.png
adb pull /sdcard/fixture.png test-results/fixture-screenshot.png >/dev/null
echo "PASS: debug fixture calendar, GPS, route search, MVV rendering"
