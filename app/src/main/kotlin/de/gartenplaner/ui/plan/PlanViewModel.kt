package de.gartenplaner.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.gartenplaner.data.model.MonthEntry
import de.gartenplaner.data.model.Plan
import de.gartenplaner.data.model.Plant
import de.gartenplaner.data.model.SectionWithPlants
import de.gartenplaner.data.repository.PlanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface PlanUiState {
    data object Loading : PlanUiState
    data object Empty   : PlanUiState
    data class  Success(
        val plan          : Plan,
        val sections      : List<SectionWithPlants>,
        /** plantId → sortierte Monatsliste */
        val monthEntries  : Map<Int, List<MonthEntry>>,
        val editMode      : Boolean = false,
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

    private val _events = MutableSharedFlow<PlanEvent>()
    val events: SharedFlow<PlanEvent> = _events.asSharedFlow()

    val uiState: StateFlow<PlanUiState> = repo.getSectionsWithPlants(planId)
        .combine(repo.getPlanById(planId)) { sections, plan ->
            if (plan == null) return@combine PlanUiState.Empty
            // MonthEntries werden pro Pflanze separat geladen — TODO Session 5
            PlanUiState.Success(plan = plan, sections = sections, monthEntries = emptyMap())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState.Loading)

    fun toggleEditMode() {
        val current = (uiState.value as? PlanUiState.Success) ?: return
        // TODO Session 7: editMode toggling
    }

    fun deletePlant(plant: Plant) {
        viewModelScope.launch {
            repo.deletePlant(plant)
            _events.emit(PlanEvent.ShowSnackbar(
                message     = "Pflanze gelöscht",
                actionLabel = "Rückgängig",
            ))
        }
    }

    fun reorderPlants(plants: List<Plant>) {
        viewModelScope.launch { repo.reorderPlants(plants) }
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
}
