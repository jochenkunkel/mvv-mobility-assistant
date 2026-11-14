package de.mvv.mobility.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import java.text.DateFormat
import java.util.Date

data class UpcomingAppointment(val title: String, val location: String?, val startMillis: Long) {
    fun timeLabel(): String = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(startMillis))
}

/** Reads the device calendar provider only; no network account or server sync. */
object DeviceCalendarRepository {
    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun nextAppointment(context: Context): UpcomingAppointment? = appointments(context, 1).firstOrNull()

    fun appointments(context: Context, limit: Int = 10): List<UpcomingAppointment> {
        if (!hasPermission(context) || limit <= 0) return emptyList()
        val now = System.currentTimeMillis()
        val end = now + 365L * 24 * 60 * 60 * 1000
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, now)
        android.content.ContentUris.appendId(builder, end)
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY
        )
        return try {
            context.contentResolver.query(
                builder.build(), projection,
                "${CalendarContract.Instances.VISIBLE} = 1",
                null, "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val titleCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val locationCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
                val beginCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val allDayCol = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val results = mutableListOf<UpcomingAppointment>()
                while (cursor.moveToNext() && results.size < limit) {
                    if (cursor.getInt(allDayCol) != 0) continue
                    val title = cursor.getString(titleCol)?.trim().orEmpty()
                    if (title.isBlank()) continue
                    results.add(UpcomingAppointment(
                        title,
                        cursor.getString(locationCol)?.trim()?.takeIf { it.isNotBlank() },
                        cursor.getLong(beginCol)
                    ))
                }
                results
            } ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }
}
