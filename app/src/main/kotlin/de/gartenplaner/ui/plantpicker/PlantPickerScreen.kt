package de.gartenplaner.ui.plantpicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import de.gartenplaner.R
import de.gartenplaner.data.model.PlantTemplate
import de.gartenplaner.data.repository.LibraryRepository
import de.gartenplaner.navigation.Screen
import de.gartenplaner.ui.plantpicker.components.MiniMonthDots

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantPickerScreen(
    navController: NavController,
    planId       : Int,
    standalone   : Boolean,
) {
    val vm: PlantPickerViewModel = viewModel(
        factory = PlantPickerViewModel.Factory(LibraryRepository())
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    if (!standalone) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (standalone) {
                // TODO Session 4: PlanBottomNavigation
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Suchfeld
            OutlinedTextField(
                value         = uiState.query,
                onValueChange = vm::updateQuery,
                placeholder   = { Text(stringResource(R.string.library_search_hint)) },
                leadingIcon   = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Kategorie-Filter-Chips
            LazyRow(
                contentPadding       = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier             = Modifier.padding(bottom = 8.dp),
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategory == null,
                        onClick  = { vm.selectCategory(null) },
                        label    = { Text(stringResource(R.string.library_filter_all)) },
                    )
                }
                items(uiState.categories) { cat ->
                    FilterChip(
                        selected = uiState.selectedCategory == cat,
                        onClick  = { vm.selectCategory(cat) },
                        label    = { Text(cat) },
                    )
                }
            }

            // Pflanzenliste
            LazyColumn {
                if (uiState.results.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.library_empty))
                        }
                    }
                }
                items(uiState.results, key = { it.id }) { template ->
                    LibraryRow(
                        template = template,
                        onClick  = {
                            // Template-ID via savedStateHandle übergeben, EditPlantScreen öffnen
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("template_id", template.id)
                            navController.navigate(Screen.EditPlant.route(planId))
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(template: PlantTemplate, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(template.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${template.category} · ${template.subtitle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            MiniMonthDots(months = template.months)
        }
        Text("›", style = MaterialTheme.typography.titleMedium,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
