package de.mvv.mobility.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Stable navigation shell; the host decides which content to show for each destination. */
enum class CockpitTab(val title: String, val glyph: String) {
    START("Start", "⌂"),
    CONNECTIONS("Verbindungen", "⇄"),
    JOURNEY("Meine Fahrt", "◉")
}

@Composable
fun CockpitNavigation(selected: CockpitTab, onSelect: (CockpitTab) -> Unit) {
    NavigationBar(modifier = Modifier.testTag("cockpit_bottom_navigation")) {
        CockpitTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                icon = { Text(tab.glyph) },
                label = { Text(tab.title) },
                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
            )
        }
    }
}
