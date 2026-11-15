package de.mvv.mobility.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationListener
import android.os.Looper
import android.os.Build
import android.os.CancellationSignal

/** Foreground-only location helpers. No background tracking or persistence. */
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
    /**
     * Receives fresh positions while the caller is foreground-active.
     * Call the returned function on STOP/disposal to prevent background updates.
     */
    fun observeForeground(context: Context, onResult: (Location?) -> Unit): () -> Unit {
        if (!hasPermission(context)) {
            onResult(null)
            return {}
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
            return {}
        }
        val listener = LocationListener { location -> onResult(location) }
        return try {
            manager.requestLocationUpdates(provider, 5000L, 3f, listener, Looper.getMainLooper())
            // Show a recent cached fix while awaiting the first fresh update.
            @Suppress("DEPRECATION")
            val cached = manager.getLastKnownLocation(provider)
            if (cached != null && cached.time <= System.currentTimeMillis() &&
                System.currentTimeMillis() - cached.time < 30_000L) {
                onResult(cached)
            }
            val stop: () -> Unit = { manager.removeUpdates(listener) }
            stop
        } catch (_: SecurityException) {
            onResult(null)
            val stop: () -> Unit = {}
            stop
        } catch (_: IllegalArgumentException) {
            onResult(null)
            val stop: () -> Unit = {}
            stop
        }
    }

}
