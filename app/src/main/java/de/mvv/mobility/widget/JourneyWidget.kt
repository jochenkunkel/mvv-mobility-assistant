package de.mvv.mobility.widget

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
import de.mvv.mobility.data.DemoJourneyRepository

class JourneyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: android.content.Context, id: GlanceId) {
        provideContent { WidgetContent() }
    }
}
@Composable
private fun WidgetContent() {
    val journey = DemoJourneyRepository.nextJourney
    Column(modifier = GlanceModifier.padding(16.dp)) {
        Text("Mein Weg · DEMO")
        Text(journey.appointment)
        Text("Abfahrt in ${journey.minutesUntilDeparture} min")
        Text("Restreisezeit ${journey.remainingMinutes} min")
        Text("${journey.transfers} Umstieg · ${journey.walkingMinutes} min Fußweg")
    }
}
class JourneyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = JourneyWidget()
}
