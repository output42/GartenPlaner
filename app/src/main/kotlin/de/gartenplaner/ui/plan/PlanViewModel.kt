package de.gartenplaner.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.gartenplaner.data.model.MonthEntry
import de.gartenplaner.data.model.Plan
import de.gartenplaner.data.model.Plant
import de.gartenplaner.data.model.Section
import de.gartenplaner.data.model.SectionWithPlants
import de.gartenplaner.data.repository.PlanRepository
import de.gartenplaner.navigation.Screen
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface PlanUiState {
    data object Loading : PlanUiState
    data object Empty   : PlanUiState
    data class  Success(
        val plan              : Plan,
        val sections          : List<SectionWithPlants>,
        val unsectionedPlants : List<Plant>,
        val monthEntries      : Map<Int, List<MonthEntry>>,
    ) : PlanUiState
}

sealed interface PlanEvent {
    data class NavigateTo(val route: String) : PlanEvent
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : PlanEvent
    data class StartPrint(
        val plan        : Plan,
        val sections    : List<SectionWithPlants>,
        val monthEntries: Map<Int, List<MonthEntry>>,
    ) : PlanEvent
}

class PlanViewModel(
    private val repo  : PlanRepository,
    private val planId: Int,
) : ViewModel() {

    private val _events = MutableSharedFlow<PlanEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PlanEvent> = _events.asSharedFlow()

    private val _editMode = MutableStateFlow(false)
    val editMode: StateFlow<Boolean> = _editMode.asStateFlow()

    val uiState: StateFlow<PlanUiState> = flow {
        uiStateCache[planId]?.let { emit(it) }
        emitAll(
            combine(
                repo.getSectionsWithPlants(planId),
                repo.getPlanById(planId),
                repo.getMonthEntriesForPlan(planId),
                repo.getUnsectionedPlants(planId),
            ) { sections, plan, entries, unsectioned ->
                if (plan == null) {
                    PlanUiState.Empty
                } else {
                    PlanUiState.Success(
                        plan              = plan,
                        sections          = sections.map { sw ->
                            sw.copy(plants = sw.plants.sortedBy { it.order })
                        },
                        unsectionedPlants = unsectioned.sortedBy { it.order },
                        monthEntries      = entries.groupBy { it.plantId },
                    )
                }
            }
        )
    }
    .onEach { uiStateCache[planId] = it }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState.Loading)

    private val undoStack = ArrayDeque<Pair<Plant, List<MonthEntry>>>()

    fun toggleEditMode() {
        _editMode.value = !_editMode.value
    }

    fun deletePlant(plant: Plant) {
        val currentEntries = (uiState.value as? PlanUiState.Success)
            ?.monthEntries?.get(plant.id) ?: emptyList()
        undoStack.addLast(plant to currentEntries)
        viewModelScope.launch {
            repo.deletePlant(plant)
            _events.emit(PlanEvent.ShowSnackbar("Pflanze gelöscht", "Rückgängig"))
        }
    }

    fun undoDeletePlant() {
        val (plant, entries) = undoStack.removeLastOrNull() ?: return
        viewModelScope.launch {
            val newId = repo.upsertPlant(plant.copy(id = 0))
            repo.replaceMonthEntries(newId, entries)
        }
    }

    fun deleteSection(section: Section) {
        viewModelScope.launch {
            repo.deleteSection(section)
        }
    }

    fun reorderPlants(plants: List<Plant>) {
        viewModelScope.launch { repo.reorderPlants(plants) }
    }

    fun navigateToEditPlant(plantId: Int) {
        viewModelScope.launch {
            _events.emit(PlanEvent.NavigateTo(Screen.EditPlant.route(planId, plantId)))
        }
    }

    fun triggerPrint() {
        val state = uiState.value as? PlanUiState.Success ?: return
        viewModelScope.launch {
            _events.emit(PlanEvent.StartPrint(state.plan, state.sections, state.monthEntries))
        }
    }

    class Factory(private val repo: PlanRepository, private val planId: Int) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlanViewModel(repo, planId) as T
    }

    companion object {
        private val uiStateCache = ConcurrentHashMap<Int, PlanUiState>()

        internal fun setCache(planId: Int, state: PlanUiState) {
            uiStateCache[planId] = state
        }
    }
}
