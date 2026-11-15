package de.mvv.mobility.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Compact, accessible entry point for a progressive journey-planning step. */
@Composable
fun SetupStepHeader(
    number: Int,
    title: String,
    summary: String,
    expanded: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(tag).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) Color(0xFFECE6FF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (expanded) "●" else "✓",
                color = Color(0xFF5635B1),
                style = MaterialTheme.typography.titleMedium
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "$number · $title",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF302451),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (expanded) "Jetzt auswählen" else summary,
                    color = Color(0xFF6B637B),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            Text(if (expanded) "−" else "Ändern", color = Color(0xFF5635B1),
                style = MaterialTheme.typography.labelMedium)
        }
    }
}
