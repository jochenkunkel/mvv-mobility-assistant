package de.mvv.mobility.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal

/** One-shot foreground position only. No background tracking or persistence. */
object CurrentLocationRepository {
    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun request(context: Context, onResult: (Location?) -> Unit) {
        if (!hasPermission(context)) {
            onResult(null)
            return
        }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ->
                LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            onResult(null)
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manager.getCurrentLocation(provider, CancellationSignal(), context.mainExecutor) { onResult(it) }
            } else {
                @Suppress("DEPRECATION")
                onResult(manager.getLastKnownLocation(provider)?.takeIf {
                    System.currentTimeMillis() - it.time < 2 * 60 * 1000
                })
            }
        } catch (_: SecurityException) {
            onResult(null)
        } catch (_: IllegalArgumentException) {
            onResult(null)
        }
    }
}
