package de.gartenplaner.ui.planlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import de.gartenplaner.R
import de.gartenplaner.data.db.GardenDatabase
import de.gartenplaner.data.model.Plan
import de.gartenplaner.data.repository.PlanRepository
import de.gartenplaner.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanListScreen(navController: NavController) {
    val context = LocalContext.current
    val vm: PlanListViewModel = viewModel(
        factory = PlanListViewModel.Factory(
            PlanRepository(GardenDatabase.getInstance(context))
        )
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plan_list_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.plan_create_new))
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            PlanListUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            PlanListUiState.Empty -> {
                // TODO Session 5: EmptyState-Composable
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.plan_list_empty_title))
                }
            }
            is PlanListUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.plans, key = { it.id }) { plan ->
                        PlanCard(
                            plan      = plan,
                            onClick   = { navController.navigate(Screen.Plan.route(plan.id)) },
                            onDelete  = { vm.deletePlan(plan) },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlanDialog(
            onDismiss = { showCreateDialog = false },
            onCreate  = { year, title ->
                showCreateDialog = false
                vm.createPlan(year, title) { newId ->
                    navController.navigate(Screen.Plan.route(newId))
                }
            }
        )
    }
}

@Composable
private fun PlanCard(plan: Plan, onClick: () -> Unit, onDelete: () -> Unit) {
    // TODO Session 5: vollständige PlanCard mit Jahr, Titel, Pflanzenanzahl, Swipe-to-dismiss
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(plan.year.toString(), style = MaterialTheme.typography.displaySmall)
            Text(plan.title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CreatePlanDialog(onDismiss: () -> Unit, onCreate: (Int, String) -> Unit) {
    // TODO Session 5: AlertDialog mit Jahr-Feld + Titel-Feld
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plan_create_new)) },
        text  = { Text("TODO: Jahr + Titel Felder — Session 5") },
        confirmButton = {
            TextButton(onClick = { onCreate(2026, "Mein Garten") }) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
