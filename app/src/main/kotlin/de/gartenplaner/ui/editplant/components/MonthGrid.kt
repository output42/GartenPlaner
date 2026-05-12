package de.gartenplaner.ui.editplant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.gartenplaner.data.model.MonthEntry

private val MONTH_NAMES = listOf(
    "Jan","Feb","Mär","Apr","Mai","Jun",
    "Jul","Aug","Sep","Okt","Nov","Dez",
)

@Composable
fun MonthGrid(
    months      : List<MonthEntry?>,
    onMonthClick: (month: Int) -> Unit,
    modifier    : Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        for (row in 0..1) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (col in 0..5) {
                    val month = row * 6 + col
                    MonthButton(
                        modifier     = Modifier.weight(1f),
                        name         = MONTH_NAMES[month],
                        entry        = months.getOrNull(month),
                        onClick      = { onMonthClick(month) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthButton(
    modifier: Modifier,
    name    : String,
    entry   : MonthEntry?,
    onClick : () -> Unit,
) {
    val bgColor   = if (entry != null) Color(entry.type.colorArgb)
                    else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (entry != null) Color(entry.type.textArgb)
                    else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier         = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = textColor)
            if (entry != null) {
                Text(
                    text       = entry.label.take(8),
                    fontSize   = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color      = textColor.copy(alpha = 0.85f),
                    modifier   = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}
