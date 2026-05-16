package de.gartenplaner.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.gartenplaner.data.backup.PlanExporter
import de.gartenplaner.data.model.Plan
import de.gartenplaner.data.repository.PlanRepository
import de.gartenplaner.export.HtmlExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val plan      : Plan?  = null,
    val isLoading : Boolean = true,
)

class SettingsViewModel(
    private val repo  : PlanRepository,
    private val planId: Int,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = repo.getPlanById(planId)
        .map { SettingsUiState(plan = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(30_000), SettingsUiState())

    fun updateTitle(title: String) = updatePlan { it.copy(title = title.trim()) }
    fun updateYear(year: Int)      = updatePlan { it.copy(year = year) }
    fun updateFrostLast(v: String) = updatePlan { it.copy(frostInfoLast = v.trim()) }
    fun updateFrostFirst(v: String)= updatePlan { it.copy(frostInfoFirst = v.trim()) }
    fun updateClimateZone(v: String) = updatePlan { it.copy(climateZone = v.trim()) }

    fun copyPlanForYear(newYear: Int, onCreated: (Int) -> Unit) {
        viewModelScope.launch {
            val newPlanId = repo.copyPlanForYear(planId, newYear)
            onCreated(newPlanId)
        }
    }

    fun deletePlan(onDeleted: () -> Unit) {
        viewModelScope.launch {
            uiState.value.plan?.let { repo.deletePlan(it) }
            onDeleted()
        }
    }

    fun exportPlan(context: Context, uri: Uri) {
        val plan = uiState.value.plan ?: return
        viewModelScope.launch {
            val sections          = repo.getSectionsWithPlants(planId).first()
            val unsectionedPlants = repo.getUnsectionedPlants(planId).first()
            val entries           = repo.getMonthEntriesForPlan(planId).first()
            val json = PlanExporter.export(
                plan              = plan,
                sections          = sections,
                monthEntries      = entries.groupBy { it.plantId },
                unsectionedPlants = unsectionedPlants,
            )
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
        }
    }

    fun importPlan(
        context  : Context,
        uri      : Uri,
        onSuccess: (newPlanId: Int) -> Unit,
        onError  : (message: String) -> Unit,
    ) {
        viewModelScope.launch {
            val json = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                }.getOrNull()
            } ?: run { onError("Datei konnte nicht gelesen werden"); return@launch }
            val result = repo.importPlan(json)
            if (result.isSuccess) onSuccess(result.getOrThrow())
            else onError(result.exceptionOrNull()?.message ?: "Fehler beim Import")
        }
    }

    fun triggerPrint(onReady: (html: String, jobName: String) -> Unit) {
        val plan = uiState.value.plan ?: return
        viewModelScope.launch {
            val sections = repo.getSectionsWithPlants(planId).first()
            val entries  = repo.getMonthEntriesForPlan(planId).first()
            val html     = HtmlExporter.buildHtml(plan, sections, entries.groupBy { it.plantId })
            onReady(html, plan.title)
        }
    }

    private fun updatePlan(transform: (Plan) -> Plan) {
        val plan = uiState.value.plan ?: return
        viewModelScope.launch { repo.upsertPlan(transform(plan)) }
    }

    class Factory(private val repo: PlanRepository, private val planId: Int) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(repo, planId) as T
    }
}
