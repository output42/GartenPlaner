@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
package de.gartenplaner.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.annotation.StringRes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import de.gartenplaner.R
import de.gartenplaner.data.db.GardenDatabase
import de.gartenplaner.data.repository.PlanRepository
import de.gartenplaner.MainActivity
import de.gartenplaner.navigation.Screen
import de.gartenplaner.ui.components.PlanBottomBar
import de.gartenplaner.ui.components.PlanTab
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ── Static data ───────────────────────────────────────────────────────────────

private val climateZones = listOf("5b", "6a", "6b", "7a", "7b", "8a", "8b")

private data class SoilType(
    val name       : String,
    val description: String,
    @StringRes val infoRes: Int,
)

private val soilTypes = listOf(
    SoilType("Lehmboden",  "Mittelschwer · nährstoffreich · wasserhaltend", R.string.soil_info_lehmboden),
    SoilType("Sandboden",  "Leicht · durchlässig · trocknet schnell",       R.string.soil_info_sandboden),
    SoilType("Tonboden",   "Schwer · verdichtet leicht · nährstoffreich",   R.string.soil_info_tonboden),
    SoilType("Humusboden", "Locker · nährstoffreich · dunkel",              R.string.soil_info_humusboden),
    SoilType("Lössboden",  "Tiefgründig · fruchtbar · schluffig",           R.string.soil_info_loessboden),
    SoilType("Kalkboden",  "Alkalisch · gut drainiert · trocken",           R.string.soil_info_kalkboden),
    SoilType("Moorerde",   "Säurereich · nährstoffarm · feucht",            R.string.soil_info_moorerde),
    SoilType("Mischboden", "Ausgewogen · mittelschwer",                     R.string.soil_info_mischboden),
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(navController: NavController, planId: Int) {
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel(
        factory = remember { SettingsViewModel.Factory(PlanRepository(GardenDatabase.getInstance(context)), planId) }
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val plan = uiState.plan

    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage: String? by remember { mutableStateOf(null) }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            snackbarMessage = null
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportPlan(context, it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            vm.importPlan(
                context   = context,
                uri       = it,
                onSuccess = { newPlanId ->
                    navController.navigate(Screen.Plan.route(newPlanId)) {
                        popUpTo(Screen.PlanList.route) { inclusive = false }
                    }
                },
                onError   = { msg -> snackbarMessage = msg },
            )
        }
    }

    // rememberSaveable → Dialog bleibt nach Rotation offen (Test 7.3)
    var showTitleDialog      by rememberSaveable { mutableStateOf(false) }
    var showYearDialog       by rememberSaveable { mutableStateOf(false) }
    var showFrostLastDialog  by rememberSaveable { mutableStateOf(false) }
    var showFrostFirstDialog by rememberSaveable { mutableStateOf(false) }
    var showClimateDialog    by rememberSaveable { mutableStateOf(false) }
    var showCopyPlanDialog   by rememberSaveable { mutableStateOf(false) }
    var showDeletePlanDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        bottomBar = { PlanBottomBar(navController, planId, PlanTab.SETTINGS) },
        topBar = {
            TopAppBar(
                title  = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (plan == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            // ── Dieser Plan ──────────────────────────────────────────────────
            item {
                SettingsGroupHeader(stringResource(R.string.settings_group_plan))
                EditableField(
                    label    = stringResource(R.string.settings_plan_title),
                    value    = plan.title,
                    onClick  = { showTitleDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                EditableField(
                    label    = stringResource(R.string.settings_plan_year),
                    value    = plan.year.toString(),
                    onClick  = { showYearDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Klima & Standort ─────────────────────────────────────────────
            item {
                SettingsGroupHeader(stringResource(R.string.settings_group_climate))
                EditableField(
                    label    = stringResource(R.string.settings_frost_last),
                    value    = plan.frostInfoLast.ifBlank { "—" },
                    onClick  = { showFrostLastDialog = true },
                    icon     = Icons.Outlined.DateRange,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                EditableField(
                    label    = stringResource(R.string.settings_frost_first),
                    value    = plan.frostInfoFirst.ifBlank { "—" },
                    onClick  = { showFrostFirstDialog = true },
                    icon     = Icons.Outlined.DateRange,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                EditableField(
                    label    = stringResource(R.string.settings_climate_zone),
                    value    = plan.climateZone.ifBlank { "—" },
                    onClick  = { showClimateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Planverwaltung ───────────────────────────────────────────────
            item {
                SettingsGroupHeader(stringResource(R.string.settings_group_management))
                SettingsRow(
                    label   = stringResource(R.string.settings_copy_plan),
                    value   = "",
                    onClick = { showCopyPlanDialog = true },
                )
                SettingsRow(
                    label   = stringResource(R.string.settings_delete_plan),
                    value   = "",
                    onClick = { showDeletePlanDialog = true },
                    tint    = MaterialTheme.colorScheme.error,
                )
            }

            // ── Datensicherung ───────────────────────────────────────────────
            item {
                SettingsGroupHeader(stringResource(R.string.settings_group_backup))
                SettingsRow(
                    label   = stringResource(R.string.settings_backup_export),
                    value   = "",
                    onClick = {
                        val safeName = plan?.title
                            ?.replace('/', '_')
                            ?.replace('\\', '_')
                            ?: "Plan"
                        val fileName = context.getString(
                            R.string.backup_export_filename, safeName, plan?.year ?: 0
                        )
                        exportLauncher.launch(fileName)
                    },
                )
                SettingsRow(
                    label   = stringResource(R.string.settings_backup_import),
                    value   = "",
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                )
            }

            // ── Export ───────────────────────────────────────────────────────
            item {
                SettingsGroupHeader(stringResource(R.string.settings_group_export))
                SettingsRow(
                    label   = stringResource(R.string.settings_print),
                    value   = stringResource(R.string.settings_print_format),
                    onClick = {
                        vm.triggerPrint { html, jobName ->
                            (context as? MainActivity)?.startPrint(html, jobName)
                        }
                    },
                )
            }

            // ── App ──────────────────────────────────────────────────────────
            item {
                SettingsGroupHeader(stringResource(R.string.settings_group_app))
                SettingsRow(
                    label   = stringResource(R.string.settings_version),
                    value   = "1.0.0 · GPL-3.0",
                    onClick = {},
                )
            }
        }
    }

    // ── Dialoge ──────────────────────────────────────────────────────────────

    if (showTitleDialog && plan != null) {
        TextFieldDialog(
            title        = stringResource(R.string.settings_plan_title),
            initialValue = plan.title,
            onConfirm    = { vm.updateTitle(it); showTitleDialog = false },
            onDismiss    = { showTitleDialog = false },
        )
    }

    if (showYearDialog && plan != null) {
        YearInputDialog(
            title       = stringResource(R.string.settings_plan_year),
            initialYear = plan.year,
            onConfirm   = { vm.updateYear(it); showYearDialog = false },
            onDismiss   = { showYearDialog = false },
        )
    }

    if (showFrostLastDialog && plan != null) {
        FrostDatePickerDialog(
            title         = stringResource(R.string.settings_frost_last),
            initialMillis = parseFrostDateMillis(plan.frostInfoLast, Calendar.APRIL, plan.year),
            onConfirm     = { vm.updateFrostLast(it); showFrostLastDialog = false },
            onClear       = { vm.updateFrostLast(""); showFrostLastDialog = false },
            onDismiss     = { showFrostLastDialog = false },
        )
    }

    if (showFrostFirstDialog && plan != null) {
        FrostDatePickerDialog(
            title         = stringResource(R.string.settings_frost_first),
            initialMillis = parseFrostDateMillis(plan.frostInfoFirst, Calendar.OCTOBER, plan.year),
            onConfirm     = { vm.updateFrostFirst(it); showFrostFirstDialog = false },
            onClear       = { vm.updateFrostFirst(""); showFrostFirstDialog = false },
            onDismiss     = { showFrostFirstDialog = false },
        )
    }

    if (showClimateDialog && plan != null) {
        ClimateZoneDialog(
            currentValue = plan.climateZone,
            onConfirm    = { vm.updateClimateZone(it); showClimateDialog = false },
            onDismiss    = { showClimateDialog = false },
        )
    }

    if (showCopyPlanDialog) {
        CopyPlanDialog(
            onConfirm = { newYear ->
                showCopyPlanDialog = false
                vm.copyPlanForYear(newYear) { newPlanId ->
                    navController.navigate(Screen.Plan.route(newPlanId)) {
                        popUpTo(Screen.PlanList.route) { inclusive = false }
                    }
                }
            },
            onDismiss = { showCopyPlanDialog = false },
        )
    }

    if (showDeletePlanDialog) {
        DeletePlanDialog(
            onConfirm = {
                showDeletePlanDialog = false
                vm.deletePlan {
                    navController.popBackStack(Screen.PlanList.route, inclusive = false)
                }
            },
            onDismiss = { showDeletePlanDialog = false },
        )
    }
}

// ── EditableField ─────────────────────────────────────────────────────────────

@Composable
private fun EditableField(
    label   : String,
    value   : String,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
    icon    : ImageVector = Icons.Outlined.Edit,
) {
    Box(modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
        OutlinedTextField(
            value         = value,
            onValueChange = {},
            readOnly      = true,
            label         = { Text(label) },
            trailingIcon  = {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            colors        = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor  = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                focusedLabelColor    = MaterialTheme.colorScheme.primary,
            ),
            modifier      = Modifier.fillMaxWidth(),
        )
        // Transparenter Overlay: fängt Click ab bevor OutlinedTextField Fokus bekommt
        Box(Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

// ── TextFieldDialog ───────────────────────────────────────────────────────────

@Composable
private fun TextFieldDialog(
    title        : String,
    initialValue : String,
    onConfirm    : (String) -> Unit,
    onDismiss    : () -> Unit,
) {
    var text by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier         = Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, AlertDialogDefaults.shape),
        title            = { Text(title) },
        text             = {
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it },
                singleLine    = true,
                label         = { Text(title) },
                colors        = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor  = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    focusedLabelColor    = MaterialTheme.colorScheme.primary,
                ),
                modifier      = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

// ── YearInputDialog ───────────────────────────────────────────────────────────

@Composable
private fun YearInputDialog(
    title       : String,
    initialYear : Int,
    onConfirm   : (Int) -> Unit,
    onDismiss   : () -> Unit,
) {
    var text by rememberSaveable(initialYear) { mutableStateOf(initialYear.toString()) }
    val year    = text.toIntOrNull()
    val isValid = year != null && year in 1900..2100
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier         = Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, AlertDialogDefaults.shape),
        title = { Text(title) },
        text  = {
            OutlinedTextField(
                value           = text,
                onValueChange   = { text = it.filter(Char::isDigit) },
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError         = text.isNotEmpty() && !isValid,
                supportingText  = if (text.isNotEmpty() && !isValid) {
                    { Text(stringResource(R.string.plan_year_invalid)) }
                } else null,
                colors          = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor  = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    focusedLabelColor    = MaterialTheme.colorScheme.primary,
                ),
                modifier        = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (isValid) onConfirm(year!!) }, enabled = isValid) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

// ── FrostDatePickerDialog ─────────────────────────────────────────────────────

@Composable
private fun FrostDatePickerDialog(
    title         : String,
    initialMillis : Long,
    onConfirm     : (String) -> Unit,
    onClear       : () -> Unit,
    onDismiss     : () -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton    = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(formatFrostDate(it)) } },
                enabled = state.selectedDateMillis != null,
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.action_delete))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
    ) {
        DatePicker(
            state = state,
            title = { Text(title, Modifier.padding(start = 24.dp, top = 16.dp)) },
        )
    }
}

// ── ClimateZoneDialog ─────────────────────────────────────────────────────────

@Composable
private fun ClimateZoneDialog(
    currentValue : String,
    onConfirm    : (String) -> Unit,
    onDismiss    : () -> Unit,
) {
    val (initZone, initSoil) = parseClimateValue(currentValue)
    var selectedZone  by rememberSaveable { mutableStateOf(initZone) }
    var selectedSoil  by rememberSaveable { mutableStateOf(initSoil) }
    var soilInfoTarget by remember { mutableStateOf<SoilType?>(null) }

    val estimatedZone = remember { estimateClimateZone() }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier         = Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, AlertDialogDefaults.shape),
        title            = { Text(stringResource(R.string.settings_climate_dialog_title)) },
        text             = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── Klimazone ────────────────────────────────────────────────
                Text(
                    stringResource(R.string.settings_climate_zone_header),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    climateZones.forEach { zone ->
                        FilterChip(
                            selected = selectedZone == zone,
                            onClick  = { selectedZone = if (selectedZone == zone) "" else zone },
                            label    = { Text(zone) },
                        )
                    }
                }
                // Standort-Vorschlag aus Systemsprache
                if (estimatedZone != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(top = 2.dp),
                    ) {
                        Text(
                            stringResource(R.string.settings_climate_estimate, estimatedZone),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick          = { selectedZone = estimatedZone },
                            enabled          = selectedZone != estimatedZone,
                            contentPadding   = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text(stringResource(R.string.action_apply),
                                 style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // ── Bodenart ─────────────────────────────────────────────────
                Text(
                    stringResource(R.string.settings_soil_type_header),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                soilTypes.forEach { soil ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSoil = if (selectedSoil == soil.name) "" else soil.name
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedSoil == soil.name,
                            onClick  = {
                                selectedSoil = if (selectedSoil == soil.name) "" else soil.name
                            },
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp),
                        ) {
                            Text(soil.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                soil.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { soilInfoTarget = soil }) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = soil.name,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val result = buildString {
                    if (selectedZone.isNotBlank()) append(selectedZone)
                    if (selectedZone.isNotBlank() && selectedSoil.isNotBlank()) append(" · ")
                    if (selectedSoil.isNotBlank()) append(selectedSoil)
                }
                onConfirm(result)
            }) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    // Boden-Info-Dialog (nested, öffnet sich über dem Hauptdialog)
    soilInfoTarget?.let { soil ->
        AlertDialog(
            onDismissRequest = { soilInfoTarget = null },
            title            = { Text(soil.name) },
            text             = { Text(stringResource(soil.infoRes)) },
            confirmButton    = {
                TextButton(onClick = { soilInfoTarget = null }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
        )
    }
}

// ── CopyPlanDialog ────────────────────────────────────────────────────────────

@Composable
private fun CopyPlanDialog(
    onConfirm : (Int) -> Unit,
    onDismiss : () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val year    = text.toIntOrNull()
    val isValid = year != null && year in 1900..2100
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier         = Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, AlertDialogDefaults.shape),
        title = { Text(stringResource(R.string.settings_copy_plan)) },
        text  = {
            OutlinedTextField(
                value           = text,
                onValueChange   = { text = it.filter(Char::isDigit) },
                singleLine      = true,
                placeholder     = { Text(stringResource(R.string.settings_copy_year_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError         = text.isNotEmpty() && !isValid,
                supportingText  = if (text.isNotEmpty() && !isValid) {
                    { Text(stringResource(R.string.plan_year_invalid)) }
                } else null,
                colors          = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                ),
                modifier        = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (isValid) onConfirm(year!!) }, enabled = isValid) {
                Text(stringResource(R.string.settings_copy_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

// ── DeletePlanDialog ──────────────────────────────────────────────────────────

@Composable
private fun DeletePlanDialog(
    onConfirm : () -> Unit,
    onDismiss : () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier         = Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, AlertDialogDefaults.shape),
        title = { Text(stringResource(R.string.settings_delete_plan)) },
        text  = { Text(stringResource(R.string.plan_delete_confirm)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

// ── Layout helpers ────────────────────────────────────────────────────────────

@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsRow(
    label  : String,
    value  : String,
    onClick: () -> Unit,
    tint   : androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    ListItem(
        headlineContent = { Text(label, color = tint) },
        trailingContent = if (value.isNotBlank()) ({
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }) else null,
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
}

// ── Pure helpers ──────────────────────────────────────────────────────────────

private fun parseFrostDateMillis(value: String, fallbackMonth: Int, planYear: Int): Long {
    val cleaned = value.trimStart('~').trim()
    val tz      = TimeZone.getTimeZone("UTC")
    for (pattern in listOf("dd. MMMM", "d. MMMM")) {
        try {
            val sdf = SimpleDateFormat("$pattern yyyy", Locale.GERMAN)
            sdf.timeZone = tz
            val date = sdf.parse("$cleaned $planYear") ?: continue
            return date.time
        } catch (_: Exception) {}
    }
    return Calendar.getInstance(tz).apply {
        set(planYear, fallbackMonth, 15, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatFrostDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd. MMMM", Locale.GERMAN)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date(millis))
}

/** Grobe Schätzung der Klimazone anhand der Systemsprache/Ländereinstellung — kein Netz, keine Permissions. */
private fun estimateClimateZone(): String? = when (Locale.getDefault().country.uppercase()) {
    "DE", "AT", "CH", "DK", "HU", "SI", "HR" -> "7a"
    "NL", "BE", "LU"                           -> "8a"
    "FR"                                        -> "8a"
    "GB", "IE"                                 -> "8b"
    "SE"                                        -> "6a"
    "NO", "FI", "IS"                           -> "5b"
    "PL", "CZ", "SK"                           -> "6b"
    "IT", "GR"                                 -> "8b"
    "ES", "PT"                                 -> "9a"
    else                                        -> null
}

private fun parseClimateValue(value: String): Pair<String, String> {
    val trimmed = value.trim()
    return when {
        " · " in trimmed -> {
            val parts = trimmed.split(" · ", limit = 2)
            val zone  = parts[0].trim().takeIf { it in climateZones } ?: ""
            zone to parts[1].trim()
        }
        trimmed in climateZones -> trimmed to ""
        else                    -> "" to trimmed
    }
}
