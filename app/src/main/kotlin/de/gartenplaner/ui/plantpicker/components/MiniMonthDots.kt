package de.gartenplaner.ui.plantpicker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.gartenplaner.data.model.MonthEntryTemplate

@Composable
fun MiniMonthDots(months: List<MonthEntryTemplate?>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        months.forEach { entry ->
            Box(
                Modifier
                    .size(width = 12.dp, height = 5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (entry != null) Color(entry.type.colorArgb)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}
