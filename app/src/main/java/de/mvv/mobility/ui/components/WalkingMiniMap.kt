package de.mvv.mobility.ui.components

import android.location.Location
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import de.mvv.mobility.data.WalkingRoute
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Small OSM walking map: violet route, blue location arrow.
 * The position is passed only when the parent has a usable GPS fix.
 * Tile loading requires network access; the route overlay remains visible
 * when already-cached map tiles are available.
 */
@Composable
fun WalkingMiniMap(route: WalkingRoute, position: Location?, walkedTrack: List<Pair<Double, Double>> = emptyList(), modifier: Modifier = Modifier, walkedSegments: List<List<Pair<Double, Double>>>? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lastHeading = remember { mutableFloatStateOf(Float.NaN) }
    val map = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            setBuiltInZoomControls(false)
            controller.setZoom(17.7)
            minZoomLevel = 13.0
            maxZoomLevel = 20.0
        }
    }
    DisposableEffect(map) {
        map.onResume()
        onDispose {
            map.onPause()
            map.onDetach()
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
    AndroidView(factory = { map }, modifier = Modifier.fillMaxSize(), update = { view ->
        view.overlays.clear()
        val routePoints = route.path.map { GeoPoint(it.first, it.second) }
        if (routePoints.size >= 2) {
            // Draw the untraveled route first. Use a nearest-segment projection
            // rather than snapping to a vertex, so both strokes meet seamlessly.
            val projection = position?.takeIf {
                !it.hasAccuracy() || it.accuracy <= 35f
            }?.let { de.mvv.mobility.data.RouteProgress.project(route.path, it.latitude, it.longitude) }
                ?.takeIf { it.lateralMeters <= 35.0 }
            val split = projection?.let { p ->
                val i = p.segmentIndex
                val a = route.path[i]
                val b = route.path[i + 1]
                // Use the same projection as the turn engine; do not recompute
                // a second fraction that can disagree at knots and loops.
                i to GeoPoint(
                    a.first + (b.first - a.first) * p.fraction,
                    a.second + (b.second - a.second) * p.fraction
                )
            }
            fun drawLine(points: List<GeoPoint>, color: Int, width: Float) {
                if (points.size < 2) return
                view.overlays.add(Polyline().apply {
                    setPoints(points)
                    outlinePaint.color = color
                    outlinePaint.strokeWidth = width
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    outlinePaint.isAntiAlias = true
                })
            }
            if (split == null) {
                drawLine(routePoints, Color.rgb(197, 174, 241), 11f)
            } else {
                val (index, point) = split
                drawLine(listOf(point) + routePoints.drop(index + 1),
                    Color.rgb(210, 192, 246), 11f)
                drawLine(routePoints.take(index + 1) + point,
                    Color.rgb(91, 52, 175), 12f)
            }
        }
        // Actual walked path is independent of the current planned route.
        // It remains visible after rerouting or a temporary GPS outage.
        // Keep each accepted GPS segment separate. Never bridge a GPS outage
        // with an invented straight line. Legacy callers still pass walkedTrack
        // until the navigation session owns WalkedTrace.
        fun drawWalkedDot(point: Pair<Double, Double>) {
            // A single accepted fix after a GPS outage is real history. Show it
            // as a dot instead of silently dropping it or connecting a gap.
            val bitmap = Bitmap.createBitmap(14, 14, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            dotPaint.color = Color.rgb(33, 135, 128)
            canvas.drawCircle(7f, 7f, 6f, dotPaint)
            view.overlays.add(Marker(view).apply {
                this.position = GeoPoint(point.first, point.second)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = BitmapDrawable(context.resources, bitmap)
            })
        }
        for (segment in (walkedSegments ?: listOf(walkedTrack))) {
            if (segment.size < 2) {
                segment.firstOrNull()?.let(::drawWalkedDot)
                continue
            }
            view.overlays.add(Polyline().apply {
                setPoints(segment.map { GeoPoint(it.first, it.second) })
                outlinePaint.color = Color.rgb(33, 135, 128)
                outlinePaint.strokeWidth = 7f
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
                outlinePaint.isAntiAlias = true
            })
        }
        // Route-forward fallback avoids noisy GPS direction at walking speed.
        val origin = position?.let { GeoPoint(it.latitude, it.longitude) }
            ?: routePoints.firstOrNull()
        val routeHeading = origin?.let {
            de.mvv.mobility.data.RouteProgress.heading(route.path, it.latitude, it.longitude)
        }
        val gpsHeading = position?.takeIf {
            it.hasBearing() && it.hasSpeed() && it.speed >= 1.2f &&
                (!it.hasAccuracy() || it.accuracy <= 25f)
        }?.bearing
        if (position == null) lastHeading.floatValue = Float.NaN
        val proposed = gpsHeading ?: routeHeading
        if (proposed != null) {
            val previous = lastHeading.floatValue
            val delta = if (previous.isNaN()) 180f else
                kotlin.math.abs(((proposed - previous + 540f) % 360f) - 180f)
            if (previous.isNaN() || delta > 12f) lastHeading.floatValue = proposed
            view.setMapOrientation(-lastHeading.floatValue)
        }
        // Shift camera 70m forward so the current position sits below center.
        val cameraPoint = origin?.let { point ->
            // Without a trustworthy direction do not pretend the camera is
            // forward-looking; centre on the known position instead.
            val heading = lastHeading.floatValue.takeUnless { it.isNaN() } ?: return@let null
            val radians = Math.toRadians(heading.toDouble())
            val latOffset = 70.0 * kotlin.math.cos(radians) / 111_320.0
            val lonOffset = 70.0 * kotlin.math.sin(radians) /
                (111_320.0 * kotlin.math.cos(Math.toRadians(point.latitude)).coerceAtLeast(0.1))
            GeoPoint(point.latitude + latOffset, point.longitude + lonOffset)
        }
        val center = position?.let { GeoPoint(it.latitude, it.longitude) }
            ?: routePoints.firstOrNull()
            ?: route.turns.firstOrNull()?.let { GeoPoint(it.latitude, it.longitude) }
        if (position != null && center != null) {
            val arrow = Marker(view).apply {
                this.position = center
                title = "Aktuelle Position"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                // Branded violet position indicator with a white halo. The
                // small directional chevron is only shown with reliable bearing.
                val moving = position.hasBearing() && position.hasSpeed() &&
                    position.speed > 0.4f && (!position.hasAccuracy() || position.accuracy <= 25f)
                val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.color = Color.argb(65, 68, 39, 130)
                canvas.drawCircle(40f, 40f, 34f, paint)
                paint.color = Color.WHITE
                canvas.drawCircle(40f, 40f, 25f, paint)
                paint.color = Color.rgb(91, 52, 175)
                canvas.drawCircle(40f, 40f, 19f, paint)
                if (moving) {
                    val chevron = Path().apply {
                        moveTo(40f, 8f)
                        lineTo(31f, 31f)
                        lineTo(40f, 26f)
                        lineTo(49f, 31f)
                        close()
                    }
                    paint.color = Color.rgb(91, 52, 175)
                    canvas.drawPath(chevron, paint)
                    rotation = position.bearing - view.mapOrientation
                } else {
                    paint.color = Color.WHITE
                    canvas.drawCircle(40f, 40f, 6f, paint)
                }
                icon = BitmapDrawable(context.resources, bitmap)
            }
            view.overlays.add(arrow)
        }
        route.destination?.let { stop ->
            val target = Marker(view).apply {
                this.position = GeoPoint(stop.latitude, stop.longitude)
                title = stop.platform?.let { "MVV-Einstieg · Steig $it" }
                    ?: "MVV-Einstieg · Steig nicht eindeutig"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                val bitmap = Bitmap.createBitmap(76, 76, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.color = Color.WHITE
                canvas.drawCircle(38f, 38f, 30f, paint)
                paint.color = Color.rgb(91, 52, 175)
                canvas.drawCircle(38f, 38f, 25f, paint)
                paint.color = Color.WHITE
                paint.textSize = 34f
                paint.textAlign = Paint.Align.CENTER
                paint.isFakeBoldText = true
                canvas.drawText("H", 38f, 50f, paint)
                icon = BitmapDrawable(context.resources, bitmap)
            }
            view.overlays.add(target)
        }
        if (cameraPoint != null) view.controller.setCenter(cameraPoint)
        view.invalidate()
    })
        // TileSourceFactory.MAPNIK uses OpenStreetMap standard tiles. Their usage
        // policy requires a visible credit; keep it independent of map rotation,
        // tile cache state and route overlay redraws.
        Text(
            text = "© OpenStreetMap contributors",
            color = ComposeColor(0xFF333333),
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(ComposeColor(0xD9FFFFFF))
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag("osm_attribution")
        )
    }
}
