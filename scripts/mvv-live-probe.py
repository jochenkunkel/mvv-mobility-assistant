#!/usr/bin/env python3
"""Probe the live MVV EFA endpoint with fixed public Munich test coordinates."""
import json
import urllib.parse
import urllib.request
from datetime import datetime
from zoneinfo import ZoneInfo

now = datetime.now(ZoneInfo("Europe/Berlin"))
params = {
    "outputFormat": "JSON",
    "tripReductionMacro": "1",
    "coordOutputFormat": "WGS84",
    "locationServerActive": "1",
    "type_origin": "coord",
    "name_origin": "11.576124:48.137154:WGS84",
    "type_destination": "any",
    "name_destination": "Olympiapark München",
    "itdDate": now.strftime("%Y%m%d"),
    "itdTime": now.strftime("%H%M"),
    "itdTripDateTimeDepArr": "dep",
    "calcNumberOfTrips": "4",
    "useRealtime": "1",
}
url = "https://efa.mvv-muenchen.de/ng/XML_TRIP_REQUEST2?" + urllib.parse.urlencode(params)
req = urllib.request.Request(url, headers={"Accept": "application/json", "User-Agent": "MeinWeg-CI-live-probe/1.0"})
with urllib.request.urlopen(req, timeout=25) as response:
    body = response.read(250000)
    print("HTTP:", response.status, "Content-Type:", response.headers.get("Content-Type"))
print("Body length:", len(body))
try:
    data = json.loads(body)
except (ValueError, UnicodeDecodeError) as exc:
    print("Not JSON:", type(exc).__name__)
    print("Response prefix:", body[:400].decode("utf-8", errors="replace"))
    raise
if not isinstance(data, dict):
    raise SystemExit("Unexpected JSON root type: " + type(data).__name__)
print("Root keys:", sorted(data.keys()))
for key in ("error", "message", "version", "trips", "trip", "journeys", "itdTripRequest", "itdTripRequestResponse"):
    if key in data:
        value = data[key]
        if isinstance(value, dict):
            print(key, "object keys:", sorted(value.keys())[:40])
        elif isinstance(value, list):
            print(key, "array length:", len(value))
            if value and isinstance(value[0], dict):
                print(key, "first item keys:", sorted(value[0].keys())[:40])
        else:
            print(key, "value:", str(value)[:300])
trips = data.get("trips")
if not isinstance(trips, list):
    print("MVV returned trips type:", type(trips).__name__)
    for key in ("itdMessageList", "origin", "destination"):
        if key in data:
            print(key, "diagnostic:", json.dumps(data[key], ensure_ascii=False)[:1200])
    raise SystemExit("Live EFA response has no trips array; investigate request parameters or MVV messages")
print("Live EFA response contains", len(trips), "trips")
