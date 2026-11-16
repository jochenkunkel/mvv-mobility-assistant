package de.mvv.mobility.ui.components

import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
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
fun WalkingMiniMap(route: WalkingRoute, position: Location?, modifier: Modifier = Modifier) {
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
    AndroidView(factory = { map }, modifier = modifier.fillMaxSize(), update = { view ->
        view.overlays.clear()
        val routePoints = route.path.map { GeoPoint(it.first, it.second) }
        if (routePoints.size >= 2) {
            view.overlays.add(Polyline().apply {
                setPoints(routePoints)
                outlinePaint.color = android.graphics.Color.rgb(100, 54, 195)
                outlinePaint.strokeWidth = 10f
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
        val proposed = gpsHeading ?: routeHeading ?: lastHeading.floatValue.takeUnless { it.isNaN() }
        if (proposed != null) {
            val previous = lastHeading.floatValue
            val delta = if (previous.isNaN()) 180f else
                kotlin.math.abs(((proposed - previous + 540f) % 360f) - 180f)
            if (previous.isNaN() || delta > 12f) lastHeading.floatValue = proposed
            view.setMapOrientation(-lastHeading.floatValue)
        }
        // Shift camera 70m forward so the current position sits below center.
        val cameraPoint = origin?.let { point ->
            val heading = lastHeading.floatValue.takeUnless { it.isNaN() } ?: 0f
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
                // A directional triangle when the GPS bearing is trustworthy;
                // a dot when stationary or no movement vector is available.
                val moving = position.hasBearing() && position.hasSpeed() && position.speed > 0.4f
                val bitmap = android.graphics.Bitmap.createBitmap(56, 56, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.rgb(25, 117, 218)
                    style = android.graphics.Paint.Style.FILL
                }
                if (moving) {
                    val arrowPath = android.graphics.Path().apply {
                        moveTo(28f, 5f)
                        lineTo(48f, 48f)
                        lineTo(28f, 37f)
                        lineTo(8f, 48f)
                        close()
                    }
                    paint.setShadowLayer(2f, 0f, 1f, android.graphics.Color.WHITE)
                    canvas.drawPath(arrowPath, paint)
                    rotation = position.bearing - view.mapOrientation
                } else {
                    canvas.drawCircle(28f, 28f, 16f, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                    })
                    canvas.drawCircle(28f, 28f, 11f, paint)
                }
                icon = android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
            }
            view.overlays.add(arrow)
        }
        if (cameraPoint != null) view.controller.setCenter(cameraPoint)
        view.invalidate()
    })
}
