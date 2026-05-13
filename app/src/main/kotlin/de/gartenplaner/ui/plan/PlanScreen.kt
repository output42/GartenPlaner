package de.gartenplaner.ui.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import de.gartenplaner.R
import de.gartenplaner.data.db.GardenDatabase
import de.gartenplaner.data.repository.PlanRepository
import de.gartenplaner.navigation.Screen
import de.gartenplaner.ui.components.PlanBottomBar
import de.gartenplaner.ui.components.PlanTab
import de.gartenplaner.ui.plan.components.MonthChipRow
import de.gartenplaner.ui.plan.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(navController: NavController, planId: Int) {
    val context = LocalContext.current
    val vm: PlanViewModel = viewModel(
        factory = PlanViewModel.Factory(
            PlanRepository(GardenDatabase.getInstance(context)),
            planId,
        )
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Events aus ViewModel empfangen (Navigation, Snackbar, Print)
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is PlanEvent.NavigateTo      -> navController.navigate(event.route)
                is PlanEvent.ShowSnackbar    -> {
                    val result = snackbarHostState.showSnackbar(event.message, event.actionLabel)
                    // TODO Session 7: Undo-Handling
                }
                is PlanEvent.StartPrint      -> {
                    // TODO Session 10: WebView + PrintManager
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (val s = uiState) {
                        is PlanUiState.Success -> Column {
                            Text(s.plan.title, style = MaterialTheme.typography.titleLarge)
                            Text("${s.plan.year}", style = MaterialTheme.typography.bodySmall)
                        }
                        else -> Text(stringResource(R.string.nav_plan))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = { vm.toggleEditMode() }) {
                        Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.plan_edit_mode))
                    }
                    IconButton(onClick = {
                        navController.navigate(Screen.EditSection.route(planId))
                    }) {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.section_add))
                    }
                    IconButton(onClick = { vm.triggerPrint() }) {
                        Icon(painterResource(R.drawable.ic_print), contentDescription = stringResource(R.string.plan_print))
                    }
                },
                navigationIcon = {
                    // Bottom-Nav übernimmt — kein Back-Button hier
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // TODO Session 7: FAB-Menü (BottomSheet mit Optionen)
                navController.navigate(Screen.EditPlant.route(planId))
            }) {
                Icon(Icons.Outlined.Add, contentDescription = null)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            PlanBottomBar(navController, planId, PlanTab.PLAN)
        },
    ) { padding ->
        when (val state = uiState) {
            PlanUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            PlanUiState.Empty -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.plan_empty_sections_title))
                }
            }
            is PlanUiState.Success -> {
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    state.sections.forEach { sw ->
                        item(key = "sec_${sw.section.id}") {
                            SectionHeader(
                                title    = sw.section.title,
                                editMode = state.editMode,
                                onDelete = { /* TODO Session 7 */ },
                            )
                        }
                        sw.plants.forEach { plant ->
                            item(key = "plant_${plant.id}") {
                                val entries = state.monthEntries[plant.id] ?: emptyList()
                                MonthChipRow(
                                    plantName  = plant.name,
                                    subtitle   = plant.subtitle,
                                    entries    = entries,
                                    editMode   = state.editMode,
                                    onPlantClick = {
                                        navController.navigate(Screen.EditPlant.route(planId, plant.id))
                                    },
                                    onMonthClick = { month ->
                                        navController.navigate(Screen.EditPlant.route(planId, plant.id))
                                    },
                                    onDelete = { vm.deletePlant(plant) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

