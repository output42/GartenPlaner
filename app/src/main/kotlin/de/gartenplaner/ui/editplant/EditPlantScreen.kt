package de.gartenplaner.ui.editplant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import de.gartenplaner.ui.editplant.components.ActivityTypeSheet
import de.gartenplaner.ui.editplant.components.MonthGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlantScreen(
    navController: NavController,
    planId       : Int,
    plantId      : Int?,
) {
    val context = LocalContext.current
    val vm: EditPlantViewModel = viewModel(
        factory = EditPlantViewModel.Factory(
            PlanRepository(GardenDatabase.getInstance(context)),
            planId,
            plantId,
        )
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isNew) stringResource(R.string.edit_plant_title_new)
                        else              stringResource(R.string.edit_plant_title_edit)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor     = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(
                        onClick  = { vm.save { navController.popBackStack() } },
                        enabled  = uiState.name.isNotBlank() && uiState.sectionId != null && !uiState.isSaving,
                    ) {
                        Text(stringResource(R.string.action_save), color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            // Pflanzname
            Text(stringResource(R.string.plant_name_label),
                 style = MaterialTheme.typography.labelSmall,
                 modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            OutlinedTextField(
                value         = uiState.name,
                onValueChange = vm::updateName,
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(8.dp))

            // Untertitel
            Text(stringResource(R.string.plant_subtitle_label),
                 style = MaterialTheme.typography.labelSmall,
                 modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            OutlinedTextField(
                value         = uiState.subtitle,
                onValueChange = vm::updateSubtitle,
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(8.dp))

            // Section-Dropdown — TODO Session 6: ExposedDropdownMenu
            Text(stringResource(R.string.plant_section_label),
                 style = MaterialTheme.typography.labelSmall,
                 modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            Spacer(Modifier.height(16.dp))

            // Monats-Grid
            Text(stringResource(R.string.plant_months_label),
                 style = MaterialTheme.typography.labelSmall,
                 modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            MonthGrid(
                months       = uiState.months,
                onMonthClick = { month -> vm.openMonthSheet(month) },
                modifier     = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text     = stringResource(R.string.plant_month_hint),
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }

    // BottomSheet für Monat-Auswahl
    if (uiState.openSheetMonth >= 0) {
        ActivityTypeSheet(
            month        = uiState.openSheetMonth,
            currentEntry = uiState.months.getOrNull(uiState.openSheetMonth),
            onDismiss    = { vm.closeMonthSheet() },
            onConfirm    = { type, label ->
                vm.setMonthEntry(uiState.openSheetMonth, type, label)
                vm.closeMonthSheet()
            },
        )
    }
}
