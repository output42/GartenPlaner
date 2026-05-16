package de.gartenplaner.ui.planlist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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
        factory = remember { PlanListViewModel.Factory(PlanRepository(GardenDatabase.getInstance(context))) }
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plan_list_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.primary,
                    titleContentColor      = MaterialTheme.colorScheme.onPrimary,
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
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        stringResource(R.string.plan_list_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.plan_list_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.plan_create_new))
                    }
                }
            }
            is PlanListUiState.Success -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.plans, key = { it.id }) { plan ->
                        PlanCard(
                            plan       = plan,
                            plantCount = state.plantCounts[plan.id] ?: 0,
                            onClick    = { navController.navigate(Screen.Plan.route(plan.id)) },
                            onDelete   = { vm.deletePlan(plan) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanCard(
    plan       : Plan,
    plantCount : Int,
    onClick    : () -> Unit,
    onDelete   : () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) showDeleteDialog = true
            false
        }
    )

    LaunchedEffect(showDeleteDialog) {
        if (!showDeleteDialog) dismissState.reset()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title            = { Text(stringResource(R.string.plan_delete_confirm)) },
            confirmButton    = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    SwipeToDismissBox(
        modifier                 = Modifier.clip(MaterialTheme.shapes.medium),
        state                    = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent        = {
            val visible = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val iconScale by animateFloatAsState(if (visible) 1f else 0.75f, label = "delete_icon")
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onErrorContainer,
                    modifier           = Modifier.size((24 * iconScale).dp),
                )
            }
        },
    ) {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text     = plan.year.toString(),
                    style    = MaterialTheme.typography.displaySmall,
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 16.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(plan.title, style = MaterialTheme.typography.titleMedium)
                    if (plantCount > 0) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            pluralStringResource(R.plurals.plant_count, plantCount, plantCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePlanDialog(onDismiss: () -> Unit, onCreate: (Int, String) -> Unit) {
    var yearText  by remember { mutableStateOf("2026") }
    var titleText by remember { mutableStateOf("") }
    var yearError by remember { mutableStateOf(false) }

    val defaultTitle = stringResource(R.string.plan_title_default)

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text(stringResource(R.string.plan_create_new)) },
        text             = {
            val greenFieldColors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor  = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                focusedLabelColor    = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value          = yearText,
                    onValueChange  = { yearText = it; yearError = false },
                    label          = { Text(stringResource(R.string.plan_year_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine     = true,
                    isError        = yearError,
                    supportingText = if (yearError) {
                        { Text(stringResource(R.string.plan_year_invalid)) }
                    } else null,
                    colors         = greenFieldColors,
                    modifier       = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = titleText,
                    onValueChange = { titleText = it },
                    label         = { Text(stringResource(R.string.plan_title_label)) },
                    placeholder   = { Text(stringResource(R.string.plan_title_hint)) },
                    singleLine    = true,
                    colors        = greenFieldColors,
                    modifier      = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val year = yearText.trim().toIntOrNull()
                    if (year == null || year < 1900 || year > 2100) {
                        yearError = true
                    } else {
                        onCreate(year, titleText.trim().ifBlank { defaultTitle })
                    }
                }
            ) { Text(stringResource(R.string.action_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
