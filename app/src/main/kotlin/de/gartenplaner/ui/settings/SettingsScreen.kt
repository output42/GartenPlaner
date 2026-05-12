package de.gartenplaner.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import de.gartenplaner.R
import de.gartenplaner.data.db.GardenDatabase
import de.gartenplaner.data.repository.PlanRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, planId: Int) {
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            PlanRepository(GardenDatabase.getInstance(context)),
            planId,
        )
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val plan = uiState.plan

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = {
            // TODO Session 4: PlanBottomNavigation
        }
    ) { padding ->
        if (plan == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            // ── Gruppe: Dieser Plan ──────────────────────────────────────────
            item {
                SettingsGroupHeader(stringResource(R.string.settings_group_plan))
                SettingsRow(
                    label    = stringResource(R.string.settings_plan_title),
                    value    = plan.title,
                    onClick  = { /* TODO Session 9: Dialog */ },
                )
                SettingsRow(
                    label    = stringResource(R.string.settings_plan_year),
                    value    = plan.year.toString(),
                    onClick  = { /* TODO Session 9: Dialog */ },
                )
            }

            // ── Gruppe: Klima & Standort ─────────────────────────────────────
            item {
                SettingsGroupHeader(stringResource(R.string.settings_group_climate))
                SettingsRow(
                    label    = stringResource(R.string.settings_frost_last),
                    value    = plan.frostInfoLast.ifBlank { "—" },
                    onClick  = { /* TODO Session 9 */ },
                )
                SettingsRow(
                    label    = stringResource(R.string.settings_frost_first),
                    value    = plan.frostInfoFirst.ifBlank { "—" },
                    onClick  = { /* TODO Session 9 */ },
                )
                SettingsRow(
                    label    = stringResource(R.string.settings_climate_zone),
                    value    = plan.climateZone.ifBlank { "—" },
                    onClick  = { /* TODO Session 9 */ },
                )
            }

            // ── Gruppe: Planverwaltung ───────────────────────────────────────
            item {
                SettingsGroupHeader(stringResource(R.string.settings_group_management))
                SettingsRow(
                    label   = stringResource(R.string.settings_copy_plan),
                    value   = "",
                    onClick = { /* TODO Session 9: Dialog mit Jahresfeld */ },
                )
                SettingsRow(
                    label   = stringResource(R.string.settings_delete_plan),
                    value   = "",
                    onClick = { /* TODO Session 9: AlertDialog */ },
                    tint    = MaterialTheme.colorScheme.error,
                )
            }

            // ── Gruppe: Datensicherung ───────────────────────────────────────
            item {
                SettingsGroupHeader(stringResource(R.string.settings_group_backup))
                SettingsRow(
                    label   = stringResource(R.string.settings_backup_export),
                    value   = "",
                    onClick = { /* TODO Session 11: SAF Export */ },
                )
                SettingsRow(
                    label   = stringResource(R.string.settings_backup_import),
                    value   = "",
                    onClick = { /* TODO Session 11: SAF Import */ },
                )
            }

            // ── Gruppe: App ──────────────────────────────────────────────────
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
}

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
        headlineContent   = { Text(label, color = tint) },
        trailingContent   = if (value.isNotBlank()) ({
            Text(value, style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
        }) else null,
        modifier          = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
}
