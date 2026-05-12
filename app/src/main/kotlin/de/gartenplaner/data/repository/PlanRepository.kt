package de.gartenplaner.data.repository

import de.gartenplaner.data.db.GardenDatabase
import de.gartenplaner.data.model.MonthEntry
import de.gartenplaner.data.model.Plan
import de.gartenplaner.data.model.Plant
import de.gartenplaner.data.model.Section
import de.gartenplaner.data.model.SectionWithPlants
import kotlinx.coroutines.flow.Flow

class PlanRepository(private val db: GardenDatabase) {

    // ── Reads (Flow) ──────────────────────────────────────────────────────────

    fun getAllPlans(): Flow<List<Plan>> =
        db.planDao().getAllPlans()

    fun getPlanById(planId: Int): Flow<Plan?> =
        db.planDao().getPlanById(planId)

    fun getSectionsWithPlants(planId: Int): Flow<List<SectionWithPlants>> =
        db.sectionDao().getSectionsWithPlants(planId)

    fun getMonthEntries(plantId: Int): Flow<List<MonthEntry>> =
        db.monthEntryDao().getEntriesForPlant(plantId)

    // ── Writes (suspend) ──────────────────────────────────────────────────────

    suspend fun upsertPlan(plan: Plan): Int =
        db.planDao().upsert(plan).toInt()

    suspend fun upsertSection(section: Section): Int =
        db.sectionDao().upsert(section).toInt()

    suspend fun upsertPlant(plant: Plant): Int =
        db.plantDao().upsert(plant).toInt()

    /** Ersetzt alle Monatseinträge einer Pflanze komplett (12-er Array, nulls werden ignoriert) */
    suspend fun replaceMonthEntries(plantId: Int, entries: List<MonthEntry?>) {
        db.monthEntryDao().deleteAllForPlant(plantId)
        val nonNull = entries.filterNotNull().map { it.copy(plantId = plantId, id = 0) }
        db.monthEntryDao().upsertAll(nonNull)
    }

    suspend fun deletePlan(plan: Plan) =
        db.planDao().delete(plan)

    suspend fun deleteSection(section: Section) =
        db.sectionDao().delete(section)

    suspend fun deletePlant(plant: Plant) =
        db.plantDao().delete(plant)

    /** Batch-Update der order-Felder nach Drag & Drop */
    suspend fun reorderSections(sections: List<Section>) =
        db.sectionDao().upsertAll(sections.mapIndexed { i, s -> s.copy(order = i) })

    suspend fun reorderPlants(plants: List<Plant>) =
        db.plantDao().upsertAll(plants.mapIndexed { i, p -> p.copy(order = i) })

    // ── Plan kopieren (Jahreswechsel) ─────────────────────────────────────────

    suspend fun copyPlanForYear(sourcePlanId: Int, newYear: Int): Int {
        val source = db.planDao().getPlanById(sourcePlanId)
            .let { TODO("collect once — implementiert in Session 9") }
        TODO("Session 9: Sections + Plants + MonthEntries kopieren mit neuen IDs")
    }
}
