#!/usr/bin/env python3
"""Live feasibility gate: EFA must route to an actual coordinate, not a preselected stop."""
import json
import urllib.parse
import urllib.request
from datetime import datetime
from zoneinfo import ZoneInfo

now = datetime.now(ZoneInfo("Europe/Berlin"))
params = {
    "outputFormat": "JSON", "tripReductionMacro": "1",
    "coordOutputFormat": "WGS84", "locationServerActive": "1",
    "type_origin": "coord", "name_origin": "11.47657:48.09239:WGS84",
    "type_destination": "coord", "name_destination": "11.5508:48.1117:WGS84",
    "itdDate": now.strftime("%Y%m%d"), "itdTime": now.strftime("%H%M"),
    "itdTripDateTimeDepArr": "dep", "calcNumberOfTrips": "4", "useRealtime": "1",
}
url = "https://efa.mvv-muenchen.de/ng/XML_TRIP_REQUEST2?" + urllib.parse.urlencode(params)
req = urllib.request.Request(url, headers={"Accept": "application/json", "User-Agent": "MeinWeg-door-to-door-probe/1.0"})
with urllib.request.urlopen(req, timeout=25) as response:
    data = json.load(response)
print("Destination:", json.dumps(data.get("destination"), ensure_ascii=False)[:700])
trips = data.get("trips")
if not isinstance(trips, list) or not trips:
    print("Messages:", json.dumps(data.get("itdMessageList"), ensure_ascii=False)[:800])
    raise SystemExit("Coordinate-to-coordinate trip routing unavailable")
for trip in trips[:4]:
    legs = trip.get("legs") or []
    print("Trip:", len(legs), "legs; last destination:", json.dumps((legs[-1].get("destination") if legs else {}), ensure_ascii=False)[:300])
    print("Trip keys:", list(trip.keys()))
    for leg in legs[:3]:
        print("Leg sample:", json.dumps(leg, ensure_ascii=False)[:2400])
    if not legs:
        raise SystemExit("Trip without legs")
print("PASS: coordinate destination yields", len(trips), "journeys")
