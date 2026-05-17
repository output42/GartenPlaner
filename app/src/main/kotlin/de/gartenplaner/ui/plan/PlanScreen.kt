package de.gartenplaner.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.rememberTextMeasurer
import de.gartenplaner.ui.plan.components.LocalPlanTextMeasurer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import de.gartenplaner.MainActivity
import de.gartenplaner.R
import de.gartenplaner.data.db.GardenDatabase
import de.gartenplaner.export.HtmlExporter
import de.gartenplaner.data.model.Section
import de.gartenplaner.data.model.SectionWithPlants
import de.gartenplaner.data.repository.PlanRepository
import de.gartenplaner.navigation.Screen
import de.gartenplaner.ui.plan.components.MonthChipRow
import de.gartenplaner.ui.plan.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(navController: NavController, planId: Int) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sharedTextMeasurer = rememberTextMeasurer(cacheSize = 48)
    val vm: PlanViewModel = viewModel(
        factory = remember { PlanViewModel.Factory(PlanRepository(GardenDatabase.getInstance(context)), planId) }
    )
    val uiState  by vm.uiState.collectAsStateWithLifecycle()
    val editMode by vm.editMode.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val dragElevationPx = with(density) { 8.dp.toPx() }
    val localSections   = remember { mutableStateListOf<SectionWithPlants>() }
    var draggingPlantId by rememberSaveable { mutableStateOf<Int?>(null) }
    var dragOffsetY     by rememberSaveable { mutableStateOf(0f) }
    val itemHeights     = remember { mutableStateMapOf<Int, Int>() }

    val dbSections = (uiState as? PlanUiState.Success)?.sections
    LaunchedEffect(dbSections) {
        if (draggingPlantId == null && dbSections != null) {
            localSections.clear()
            localSections.addAll(dbSections)
        }
    }

    var showAddMenu         by remember { mutableStateOf(false) }
    var deleteSectionTarget by remember { mutableStateOf<Section?>(null) }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is PlanEvent.NavigateTo   -> navController.navigate(event.route)
                is PlanEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message     = event.message,
                        actionLabel = event.actionLabel,
                        duration    = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) vm.undoDeletePlant()
                }
                is PlanEvent.StartPrint -> {
                    val html = HtmlExporter.buildHtml(event.plan, event.sections, event.monthEntries)
                    (context as? MainActivity)?.startPrint(html, event.plan.title)
                }
            }
        }
    }

    deleteSectionTarget?.let { section ->
        AlertDialog(
            onDismissRequest = { deleteSectionTarget = null },
            text = { Text(stringResource(R.string.plan_delete_section_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteSection(section)
                    deleteSectionTarget = null
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteSectionTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showAddMenu) {
        ModalBottomSheet(onDismissRequest = { showAddMenu = false }) {
            Column(Modifier.padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.plan_add_from_library)) },
                    leadingContent  = { Text("🌿", style = MaterialTheme.typography.titleMedium) },
                    modifier = Modifier.clickable {
                        showAddMenu = false
                        navController.navigate(Screen.PlantPicker.route(planId))
                    },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.plan_add_manual)) },
                    leadingContent  = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showAddMenu = false
                        navController.navigate(Screen.EditPlant.route(planId))
                    },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.plan_add_section)) },
                    leadingContent  = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showAddMenu = false
                        navController.navigate(Screen.EditSection.route(planId))
                    },
                )
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
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
                    containerColor         = MaterialTheme.colorScheme.primary,
                    titleContentColor      = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = { vm.toggleEditMode() }) {
                        Icon(
                            imageVector        = if (editMode) Icons.Outlined.Check else Icons.Outlined.Edit,
                            contentDescription = stringResource(
                                if (editMode) R.string.plan_done else R.string.plan_edit_mode
                            ),
                        )
                    }
                    if (!editMode) {
                        IconButton(onClick = { vm.triggerPrint() }) {
                            Icon(
                                painter            = painterResource(R.drawable.ic_print),
                                contentDescription = stringResource(R.string.plan_print),
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (uiState is PlanUiState.Success) {
                FloatingActionButton(
                    onClick = {
                        if (editMode) navController.navigate(Screen.EditSection.route(planId))
                        else showAddMenu = true
                    }
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val state = uiState) {
            PlanUiState.Loading -> {
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    repeat(5) {
                        item {
                            PlantRowSkeleton()
                            HorizontalDivider()
                        }
                    }
                }
            }
            PlanUiState.Empty -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.plan_empty_sections_title)) }
            }
            is PlanUiState.Success -> {
                CompositionLocalProvider(LocalPlanTextMeasurer provides sharedTextMeasurer) {
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    state.unsectionedPlants.forEach { plant ->
                        item(key = "unsec_${plant.id}") {
                            val entries = state.monthEntries[plant.id] ?: emptyList()
                            MonthChipRow(
                                plantName    = plant.name,
                                subtitle     = plant.subtitle,
                                entries      = entries,
                                editMode     = editMode,
                                isDragging   = false,
                                onPlantClick = { vm.navigateToEditPlant(plant.id) },
                                onMonthClick = { vm.navigateToEditPlant(plant.id) },
                                onDelete     = { vm.deletePlant(plant) },
                                onDragStart  = {},
                                onDrag       = {},
                                onDragEnd    = {},
                            )
                        }
                    }

                    (if (draggingPlantId != null) localSections else state.sections).forEach { sw ->
                        item(key = "sec_${sw.section.id}") {
                            SectionHeader(
                                title    = sw.section.title,
                                editMode = editMode,
                                onEdit   = { navController.navigate(Screen.EditSection.route(planId, sw.section.id)) },
                                onDelete = { deleteSectionTarget = sw.section },
                            )
                        }

                        sw.plants.forEach { plant ->
                            item(key = "plant_${plant.id}") {
                                val isDragging = draggingPlantId == plant.id
                                val entries    = state.monthEntries[plant.id] ?: emptyList()

                                // Drag-Modifier nur in editMode aktiv:
                                // graphicsLayer{} erzeugt sonst für jedes Item einen eigenen GPU-RenderNode
                                // (auch mit Identity-Werten), was bei Animation 2 Screens × N Items das
                                // GPU-Layer-Limit sprengen und Software-Fallback auslösen kann.
                                val itemMod = when {
                                    isDragging -> Modifier
                                        .animateItem(fadeInSpec = null, fadeOutSpec = null)
                                        .onGloballyPositioned { coords ->
                                            if (!isDragging) itemHeights[plant.id] = coords.size.height
                                        }
                                        .zIndex(1f)
                                        .graphicsLayer(
                                            translationY    = dragOffsetY,
                                            shadowElevation = dragElevationPx,
                                            scaleX          = 1.03f,
                                            scaleY          = 1.03f,
                                        )
                                    editMode -> Modifier
                                        .animateItem(fadeInSpec = null, fadeOutSpec = null)
                                        .onGloballyPositioned { coords ->
                                            itemHeights[plant.id] = coords.size.height
                                        }
                                    else -> Modifier
                                }

                                Box(itemMod) {
                                    MonthChipRow(
                                        plantName    = plant.name,
                                        subtitle     = plant.subtitle,
                                        entries      = entries,
                                        editMode     = editMode,
                                        isDragging   = isDragging,
                                        onPlantClick = { vm.navigateToEditPlant(plant.id) },
                                        onMonthClick = { vm.navigateToEditPlant(plant.id) },
                                        onDelete = { vm.deletePlant(plant) },
                                        onDragStart = {
                                            draggingPlantId = plant.id
                                            dragOffsetY     = 0f
                                        },
                                        onDrag = { deltaY ->
                                            dragOffsetY += deltaY
                                            swapIfNeeded(
                                                plantId       = plant.id,
                                                dragOffsetY   = dragOffsetY,
                                                localSections = localSections,
                                                itemHeights   = itemHeights,
                                                onOffsetAdj   = { adj -> dragOffsetY += adj },
                                            )
                                        },
                                        onDragEnd = {
                                            val sec = localSections.find { s ->
                                                s.plants.any { it.id == plant.id }
                                            }
                                            sec?.let { vm.reorderPlants(it.plants) }
                                            draggingPlantId = null
                                            dragOffsetY     = 0f
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                } // CompositionLocalProvider
            }
        }
    }
}

@Composable
private fun PlantRowSkeleton() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.45f)
                .height(14.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            content = {},
        )
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(12) {
                Surface(
                    modifier = Modifier.size(22.dp),
                    shape    = MaterialTheme.shapes.extraSmall,
                    color    = MaterialTheme.colorScheme.surfaceVariant,
                    content  = {},
                )
            }
        }
    }
}

private fun swapIfNeeded(
    plantId       : Int,
    dragOffsetY   : Float,
    localSections : MutableList<SectionWithPlants>,
    itemHeights   : Map<Int, Int>,
    onOffsetAdj   : (Float) -> Unit,
) {
    val sIdx = localSections.indexOfFirst { sw -> sw.plants.any { it.id == plantId } }
    if (sIdx < 0) return
    val section = localSections[sIdx]
    val plants  = section.plants
    val pIdx    = plants.indexOfFirst { it.id == plantId }
    if (pIdx < 0) return

    val itemH     = (itemHeights[plantId] ?: 80).toFloat()
    val threshold = itemH / 2f

    when {
        dragOffsetY > threshold && pIdx < plants.size - 1 -> {
            val neighbor  = plants[pIdx + 1]
            val neighborH = (itemHeights[neighbor.id] ?: 80).toFloat()
            localSections[sIdx] = section.copy(
                plants = plants.toMutableList().also {
                    it[pIdx]     = neighbor
                    it[pIdx + 1] = plants[pIdx]
                }
            )
            onOffsetAdj(-neighborH)
        }
        dragOffsetY < -threshold && pIdx > 0 -> {
            val neighbor  = plants[pIdx - 1]
            val neighborH = (itemHeights[neighbor.id] ?: 80).toFloat()
            localSections[sIdx] = section.copy(
                plants = plants.toMutableList().also {
                    it[pIdx]     = neighbor
                    it[pIdx - 1] = plants[pIdx]
                }
            )
            onOffsetAdj(neighborH)
        }
    }
}
