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
                    LocationFreshness.isFresh(it.time, System.currentTimeMillis())
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
        // Listen to GPS and network concurrently. GPS-only updates can pause for
        // tens of seconds outdoors/near buildings even when network fixes exist.
        val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val providers = listOfNotNull(
            LocationManager.GPS_PROVIDER.takeIf { fine && manager.isProviderEnabled(it) },
            LocationManager.NETWORK_PROVIDER.takeIf { manager.isProviderEnabled(it) }
        )
        if (providers.isEmpty()) {
            onResult(null)
            return {}
        }
        val listener = LocationListener { fix -> onResult(fix) }
        val registered = mutableListOf<String>()
        for (provider in providers) {
            try {
                manager.requestLocationUpdates(provider, 5000L, 3f, listener, Looper.getMainLooper())
                registered.add(provider)
            } catch (_: SecurityException) {
                // A coarse-only permission must not break the other provider.
            } catch (_: IllegalArgumentException) {
                // Provider may have become unavailable between checks.
            }
        }
        if (registered.isEmpty()) {
            onResult(null)
            return {}
        }
        // Prefer the newest sufficiently accurate cached fix across providers.
        val cached = registered.mapNotNull { provider ->
            runCatching {
                @Suppress("DEPRECATION")
                manager.getLastKnownLocation(provider)
            }.getOrNull()
        }.filter { LocationFreshness.isFresh(it.time, System.currentTimeMillis()) &&
            System.currentTimeMillis() - it.time < 30_000L
        }.maxByOrNull { it.time }
        if (cached != null) onResult(cached)
        val stop: () -> Unit = { manager.removeUpdates(listener) }
        return stop
    }

}
