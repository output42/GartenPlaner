package de.gartenplaner.ui.editplant.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.gartenplaner.R
import de.gartenplaner.data.model.ActivityType
import de.gartenplaner.data.model.MonthEntry

private val MONTH_NAMES_LONG = listOf(
    "Januar","Februar","März","April","Mai","Juni",
    "Juli","August","September","Oktober","November","Dezember",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTypeSheet(
    month       : Int,
    currentEntry: MonthEntry?,
    onDismiss   : () -> Unit,
    onConfirm   : (type: ActivityType?, label: String) -> Unit,
) {
    var selectedType  by remember { mutableStateOf(currentEntry?.type) }
    var label         by remember { mutableStateOf(currentEntry?.label ?: "") }

    // Label auf Default setzen wenn Typ gewechselt wird
    LaunchedEffect(selectedType) {
        if (selectedType != null && label.isBlank()) {
            label = selectedType!!.defaultLabel
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                text     = MONTH_NAMES_LONG[month],
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            ActivityType.entries.forEach { type ->
                ActivityTypeRow(
                    type       = type,
                    isSelected = selectedType == type,
                    onSelect   = {
                        selectedType = type
                        label = type.defaultLabel
                    },
                )
            }

            // "Leer / nichts"-Option
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clickable { selectedType = null; label = "" }
                    .padding(horizontal = 20.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selectedType == null, onClick = { selectedType = null; label = "" })
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(Color.Gray.copy(alpha = 0.4f)))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.month_sheet_empty_option), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            // Optionales Notiz-/Label-Feld (immer sichtbar wenn Typ ausgewählt)
            AnimatedVisibility(visible = selectedType != null) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Text(
                        text  = if (selectedType == ActivityType.PFLEGE)
                                    stringResource(R.string.month_sheet_note_label)
                                else "Kürzel",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    OutlinedTextField(
                        value         = label,
                        onValueChange = { label = it },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        placeholder   = {
                            if (selectedType == ActivityType.PFLEGE)
                                Text(stringResource(R.string.month_sheet_note_hint))
                        },
                    )
                }
            }

            Button(
                onClick  = { onConfirm(selectedType, label.ifBlank { selectedType?.defaultLabel ?: "" }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(stringResource(R.string.month_sheet_confirm))
            }
        }
    }
}

@Composable
private fun ActivityTypeRow(
    type      : ActivityType,
    isSelected: Boolean,
    onSelect  : () -> Unit,
) {
    val chipColor = Color(type.colorArgb)
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = onSelect)
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(chipColor)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text       = type.defaultLabel,
            fontSize   = 14.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
        )
    }
}
