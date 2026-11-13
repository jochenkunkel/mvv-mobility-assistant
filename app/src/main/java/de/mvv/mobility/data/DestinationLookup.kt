package de.mvv.mobility.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import java.util.Locale

/** Uses the device's geocoding service, which may query an online provider. */
object DestinationLookup {
    fun search(context: Context, query: String, callback: (List<String>) -> Unit) {
        if (query.isBlank() || !Geocoder.isPresent()) {
            callback(emptyList())
            return
        }
        val geocoder = Geocoder(context, Locale.GERMANY)
        fun deliver(addresses: List<Address>) {
            val options = addresses.mapNotNull { address ->
                address.getAddressLine(0)?.takeIf { it.isNotBlank() }
                    ?: listOfNotNull(address.featureName, address.thoroughfare, address.locality)
                        .distinct().joinToString(", ").takeIf { it.isNotBlank() }
            }.distinct()
            context.mainExecutor.execute { callback(options) }
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                geocoder.getFromLocationName(query, 5, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = deliver(addresses)
                    override fun onError(errorMessage: String?) {
                        context.mainExecutor.execute { callback(emptyList()) }
                    }
                })
            } else {
                Thread {
                    @Suppress("DEPRECATION")
                    val results = try { geocoder.getFromLocationName(query, 5).orEmpty() }
                        catch (_: Exception) { emptyList() }
                    deliver(results)
                }.start()
            }
        } catch (_: Exception) {
            callback(emptyList())
        }
    }
}
