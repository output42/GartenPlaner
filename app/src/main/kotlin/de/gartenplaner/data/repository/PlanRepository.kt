package de.gartenplaner.data.repository

import de.gartenplaner.data.backup.PlanImporter
import de.gartenplaner.data.db.GardenDatabase
import de.gartenplaner.data.db.PlanPlantCount
import de.gartenplaner.data.model.MonthEntry
import de.gartenplaner.data.model.Plan
import de.gartenplaner.data.model.Plant
import de.gartenplaner.data.model.Section
import de.gartenplaner.data.model.SectionWithPlants
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PlanRepository(private val db: GardenDatabase) {

    // ── Reads (Flow) ──────────────────────────────────────────────────────────

    fun getAllPlans(): Flow<List<Plan>> =
        db.planDao().getAllPlans()

    fun getPlanById(planId: Int): Flow<Plan?> =
        db.planDao().getPlanById(planId)

    fun getSectionsWithPlants(planId: Int): Flow<List<SectionWithPlants>> =
        db.sectionDao().getSectionsWithPlants(planId)

    fun getUnsectionedPlants(planId: Int): Flow<List<Plant>> =
        db.plantDao().getUnsectionedPlants(planId)

    fun getMonthEntries(plantId: Int): Flow<List<MonthEntry>> =
        db.monthEntryDao().getEntriesForPlant(plantId)

    fun getMonthEntriesForPlan(planId: Int): Flow<List<MonthEntry>> =
        db.monthEntryDao().getEntriesForPlan(planId)

    fun getPlanPlantCounts(): Flow<List<PlanPlantCount>> =
        db.plantDao().getPlanPlantCounts()

    suspend fun getPlantById(plantId: Int): Plant? =
        db.plantDao().getPlantById(plantId)

    suspend fun getMonthEntriesOnce(plantId: Int): List<MonthEntry> =
        db.monthEntryDao().getEntriesForPlantOnce(plantId)

    // ── Writes (suspend) ──────────────────────────────────────────────────────

    suspend fun upsertPlan(plan: Plan): Int =
        db.planDao().upsert(plan).toInt()

    suspend fun upsertSection(section: Section): Int =
        db.sectionDao().upsert(section).toInt()

    suspend fun upsertPlant(plant: Plant): Int =
        db.plantDao().upsert(plant).toInt()

    /** Ersetzt alle Monatseinträge einer Pflanze komplett (12-er Array, nulls werden ignoriert) */
    suspend fun replaceMonthEntries(plantId: Int, entries: List<MonthEntry?>) {
        val plant = db.plantDao().getPlantById(plantId) ?: return
        db.withTransaction {
            db.monthEntryDao().deleteAllForPlant(plantId)
            val nonNull = entries.filterNotNull()
                .map { it.copy(plantId = plantId, planId = plant.planId, id = 0) }
            db.monthEntryDao().upsertAll(nonNull)
        }
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
        val sourcePlan = db.planDao().getPlanById(sourcePlanId).first() ?: return -1

        return db.withTransaction {
            val newPlanId = db.planDao().upsert(
                sourcePlan.copy(id = 0, year = newYear)
            ).toInt()

            val sectionsWithPlants = db.sectionDao().getSectionsWithPlants(sourcePlanId).first()
            for (swp in sectionsWithPlants) {
                val newSectionId = db.sectionDao().upsert(
                    swp.section.copy(id = 0, planId = newPlanId)
                ).toInt()
                for (plant in swp.plants) {
                    val newPlantId = db.plantDao().upsert(
                        plant.copy(id = 0, planId = newPlanId, sectionId = newSectionId)
                    ).toInt()
                    val entries = db.monthEntryDao().getEntriesForPlantOnce(plant.id)
                    db.monthEntryDao().upsertAll(
                        entries.map { it.copy(id = 0, plantId = newPlantId, planId = newPlanId) }
                    )
                }
            }
            val unsectioned = db.plantDao().getUnsectionedPlants(sourcePlanId).first()
            for (plant in unsectioned) {
                val newPlantId = db.plantDao().upsert(
                    plant.copy(id = 0, planId = newPlanId, sectionId = null)
                ).toInt()
                val entries = db.monthEntryDao().getEntriesForPlantOnce(plant.id)
                db.monthEntryDao().upsertAll(
                    entries.map { it.copy(id = 0, plantId = newPlantId, planId = newPlanId) }
                )
            }
            newPlanId
        }
    }

    // ── JSON-Backup ───────────────────────────────────────────────────────────

    suspend fun importPlan(json: String): Result<Int> = PlanImporter.import(json, db)
}
