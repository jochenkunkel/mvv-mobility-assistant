package de.mvv.mobility.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.compose.ui.unit.dp
import de.mvv.mobility.data.DeviceCalendarRepository
import de.mvv.mobility.data.UpcomingAppointment

class JourneyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val permitted = DeviceCalendarRepository.hasPermission(context)
        val appointment = if (permitted) DeviceCalendarRepository.nextAppointment(context) else null
        provideContent { WidgetContent(permitted, appointment) }
    }
}
@Composable
private fun WidgetContent(permitted: Boolean, appointment: UpcomingAppointment?) {
    Column(modifier = GlanceModifier.padding(16.dp)) {
        Text("Mein Weg · Kalender")
        when {
            !permitted -> Text("App öffnen und Kalenderzugriff erlauben")
            appointment == null -> Text("Kein anstehender Termin gefunden")
            else -> {
                Text(appointment.title)
                Text(appointment.timeLabel())
                Text(appointment.location ?: "Zielort fehlt – bitte in App prüfen")
            }
        }
    }
}
class JourneyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = JourneyWidget()
}
