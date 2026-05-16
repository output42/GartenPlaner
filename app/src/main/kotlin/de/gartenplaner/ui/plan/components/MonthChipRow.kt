package de.gartenplaner.ui.plan.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import de.gartenplaner.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.gartenplaner.data.model.MonthEntry

// Einzel-Instanz wird von PlanScreen via CompositionLocalProvider bereitgestellt,
// damit alle MonthChipCanvas-Rows denselben Mess-Cache nutzen.
val LocalPlanTextMeasurer = compositionLocalOf<TextMeasurer?> { null }

private val MONTH_SHORT = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
private val MONTH_ABBR  = listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez")

@Composable
fun MonthChipRow(
    plantName   : String,
    subtitle    : String,
    entries     : List<MonthEntry>,
    editMode    : Boolean,
    isDragging  : Boolean = false,
    onPlantClick: () -> Unit,
    onMonthClick: (month: Int) -> Unit,
    onDelete    : () -> Unit,
    onDragStart : (() -> Unit)? = null,
    onDrag      : ((deltaY: Float) -> Unit)? = null,
    onDragEnd   : (() -> Unit)? = null,
    modifier    : Modifier = Modifier,
) {
    val byMonth = entries.associateBy { it.month }
    var showZoom by remember { mutableStateOf(false) }

    if (showZoom) {
        MonthZoomDialog(
            plantName = plantName,
            subtitle  = subtitle,
            byMonth   = byMonth,
            onDismiss = { showZoom = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isDragging) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                else Color.Transparent
            )
            .clickable { if (editMode) onPlantClick() else showZoom = true }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 7.dp),
        ) {
            if (editMode) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { onDragStart?.invoke() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDrag?.invoke(dragAmount.y)
                                },
                                onDragEnd    = { onDragEnd?.invoke() },
                                onDragCancel = { onDragEnd?.invoke() },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_drag_handle),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isDragging)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                    )
                }
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
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        if (editMode) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(12) { month ->
                    val entry = byMonth[month]
                    MonthChip(
                        modifier = Modifier.weight(1f),
                        label    = MONTH_SHORT[month],
                        entry    = entry,
                        editMode = editMode,
                        onClick  = { onMonthClick(month) },
                    )
                }
            }
        } else {
            MonthChipCanvas(byMonth = byMonth, modifier = Modifier.fillMaxWidth())
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}

@Composable
private fun MonthChip(
    modifier : Modifier,
    label    : String,
    entry    : MonthEntry?,
    editMode : Boolean,
    onClick  : () -> Unit,
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
        modifier         = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(enabled = editMode, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = label,
                fontSize   = 7.5.sp,
                color      = textColor,
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

@Composable
private fun MonthChipCanvas(
    byMonth  : Map<Int, MonthEntry>,
    modifier : Modifier = Modifier,
) {
    val textMeasurer     = LocalPlanTextMeasurer.current ?: rememberTextMeasurer()
    val surfaceVariant   = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Pre-measure during composition — draw callback is allocation-free (no per-frame work)
    val specs = remember(byMonth, surfaceVariant, onSurfaceVariant) {
        (0 until 12).map { month ->
            val entry    = byMonth[month]
            val txtColor = if (entry != null) Color(entry.type.textArgb)  else onSurfaceVariant
            val bgColor  = if (entry != null) Color(entry.type.colorArgb) else surfaceVariant
            Triple(
                bgColor,
                textMeasurer.measure(MONTH_SHORT[month], TextStyle(fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = txtColor)),
                entry?.let { textMeasurer.measure(it.label.take(6), TextStyle(fontSize = 5.5.sp, color = txtColor.copy(alpha = 0.8f))) },
            )
        }
    }

    Canvas(modifier = modifier.height(28.dp)) {
        val gap    = 3.dp.toPx()
        val chipW  = (size.width - gap * 11f) / 12f
        val chipH  = size.height
        val corner = CornerRadius(6.dp.toPx())

        specs.forEachIndexed { month, (bgColor, header, sub) ->
            val x = month * (chipW + gap)
            drawRoundRect(color = bgColor, topLeft = Offset(x, 0f), size = Size(chipW, chipH), cornerRadius = corner)
            val headerY = if (sub != null) chipH / 4f - header.size.height / 2f
                          else              chipH / 2f - header.size.height / 2f
            drawText(header, topLeft = Offset(x + (chipW - header.size.width) / 2f, headerY))
            sub?.let { drawText(it, topLeft = Offset(x + (chipW - it.size.width) / 2f, chipH * 3f / 4f - it.size.height / 2f)) }
        }
    }
}

@Composable
private fun MonthZoomDialog(
    plantName : String,
    subtitle  : String,
    byMonth   : Map<Int, MonthEntry>,
    onDismiss : () -> Unit,
) {
    val dialogShape = RoundedCornerShape(16.dp)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape  = dialogShape,
            color  = MaterialTheme.colorScheme.surface,
            modifier = Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, dialogShape),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(plantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(6) { i ->
                        LargeMonthChip(
                            modifier = Modifier.weight(1f),
                            abbr     = MONTH_ABBR[i],
                            entry    = byMonth[i],
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(6) { i ->
                        LargeMonthChip(
                            modifier = Modifier.weight(1f),
                            abbr     = MONTH_ABBR[6 + i],
                            entry    = byMonth[6 + i],
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick  = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text  = "Schließen",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun LargeMonthChip(
    modifier : Modifier,
    abbr     : String,
    entry    : MonthEntry?,
) {
    val bgColor   = if (entry != null) Color(entry.type.colorArgb) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (entry != null) Color(entry.type.textArgb)  else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier         = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(horizontal = 1.dp),
        ) {
            Text(abbr, fontSize = 10.sp, color = textColor, fontWeight = FontWeight.Bold, maxLines = 1)
            if (entry != null) {
                Text(
                    text     = entry.label.take(7),
                    fontSize = 7.sp,
                    color    = textColor.copy(alpha = 0.85f),
                    maxLines = 1,
                )
            }
        }
    }
}
