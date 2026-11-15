package de.mvv.mobility.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import de.mvv.mobility.data.RouteFilterMode

@Composable
fun RouteFilterControl(
    mode: RouteFilterMode,
    query: String,
    onModeChange: (RouteFilterMode) -> Unit,
    onQueryChange: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.Dp(8f))) {
        FilterChip(selected = mode == RouteFilterMode.ANY,
            onClick = { onModeChange(RouteFilterMode.ANY) },
            label = { Text("Alle") }, modifier = Modifier.testTag("route_filter_mode_ANY"))
        FilterChip(selected = mode == RouteFilterMode.ONLY_LINE_WITH_WALK,
            onClick = { onModeChange(RouteFilterMode.ONLY_LINE_WITH_WALK) },
            label = { Text("Nur Linie") }, modifier = Modifier.testTag("route_filter_mode_ONLY_LINE_WITH_WALK"))
        FilterChip(selected = mode == RouteFilterMode.CONTAINS,
            onClick = { onModeChange(RouteFilterMode.CONTAINS) },
            label = { Text("Mit Linie") }, modifier = Modifier.testTag("route_filter_mode_CONTAINS"))
    }
    if (mode != RouteFilterMode.ANY) {
        OutlinedTextField(value = query, onValueChange = onQueryChange,
            label = { Text("Linie, z. B. 134 oder U3") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("route_filter_query"))
    }
}
