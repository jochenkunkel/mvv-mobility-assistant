package de.mvv.mobility.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import de.mvv.mobility.data.RouteFilterMode

@Composable
fun RouteFilterControl(
    mode: RouteFilterMode,
    query: String,
    onModeChange: (RouteFilterMode) -> Unit,
    onQueryChange: (String) -> Unit
) {
    // Radio selection is mutually exclusive: unlike checkboxes, only one mode can be active.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            Triple(RouteFilterMode.ANY, "Alle", "route_filter_mode_ANY"),
            Triple(RouteFilterMode.ONLY_LINE_WITH_WALK, "Nur Linie", "route_filter_mode_ONLY_LINE_WITH_WALK"),
            Triple(RouteFilterMode.CONTAINS, "Mit Linie", "route_filter_mode_CONTAINS")
        ).forEach { (option, label, tag) ->
            Row(
                modifier = Modifier.testTag(tag).clickable { onModeChange(option) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                RadioButton(
                    selected = mode == option,
                    onClick = { onModeChange(option) },
                    modifier = Modifier.size(36.dp)
                )
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    if (mode != RouteFilterMode.ANY) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Linie") },
            placeholder = { Text("z. B. 134 oder U3") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("route_filter_query")
        )
    }
}
