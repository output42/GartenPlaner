package de.gartenplaner.ui.plan.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import de.gartenplaner.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.gartenplaner.data.model.ActivityType
import de.gartenplaner.data.model.MonthEntry

private val MONTH_SHORT = listOf("J","F","M","A","M","J","J","A","S","O","N","D")

@Composable
fun MonthChipRow(
    plantName   : String,
    subtitle    : String,
    entries     : List<MonthEntry>,
    editMode    : Boolean,
    onPlantClick: () -> Unit,
    onMonthClick: (month: Int) -> Unit,
    onDelete    : () -> Unit,
    modifier    : Modifier = Modifier,
) {
    val byMonth = entries.associateBy { it.month }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !editMode, onClick = onPlantClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 7.dp),
        ) {
            if (editMode) {
                Icon(
                    painterResource(R.drawable.ic_drag_handle),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).padding(end = 4.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
            Text(
                text       = plantName,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.weight(1f),
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text     = subtitle,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            if (editMode) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(12) { month ->
                val entry = byMonth[month]
                MonthChip(
                    modifier   = Modifier.weight(1f),
                    label      = MONTH_SHORT[month],
                    entry      = entry,
                    onClick    = { onMonthClick(month) },
                )
            }
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}

@Composable
private fun MonthChip(
    modifier: Modifier,
    label   : String,
    entry   : MonthEntry?,
    onClick : () -> Unit,
) {
    val bgColor = if (entry != null)
        Color(entry.type.colorArgb)
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (entry != null)
        Color(entry.type.textArgb)
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier          = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment  = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text     = label,
                fontSize = 7.5.sp,
                color    = textColor,
                fontWeight = FontWeight.Bold,
            )
            if (entry != null) {
                Text(
                    text     = entry.label.take(6),
                    fontSize = 5.5.sp,
                    color    = textColor.copy(alpha = 0.8f),
                )
            }
        }
    }
}
