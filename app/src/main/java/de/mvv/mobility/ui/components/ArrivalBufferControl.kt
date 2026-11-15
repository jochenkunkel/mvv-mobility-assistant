package de.mvv.mobility.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Stateless, preview-friendly component; value changes belong to the screen state owner. */
@Composable
fun ArrivalBufferControl(minutes: Int, onMinutesChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ankunftspuffer", modifier = Modifier.weight(1f))
            Text("$minutes Min. früher", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium)
        }
        Slider(value = minutes.toFloat(), onValueChange = {
            val updated = (it / 5f).roundToInt().coerceIn(1, 12) * 5
            if (updated != minutes) onMinutesChange(updated)
        }, valueRange = 5f..60f, steps = 10,
            modifier = Modifier.testTag("arrival_buffer_slider"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("5 Min.", style = MaterialTheme.typography.labelSmall)
            Text("30 Min.", style = MaterialTheme.typography.labelSmall)
            Text("60 Min.", style = MaterialTheme.typography.labelSmall)
        }
    }
}
