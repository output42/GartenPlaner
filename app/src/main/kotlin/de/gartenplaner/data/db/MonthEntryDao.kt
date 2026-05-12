package de.gartenplaner.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import de.gartenplaner.data.model.MonthEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthEntryDao {

    @Query("SELECT * FROM month_entries WHERE plant_id = :plantId ORDER BY month")
    fun getEntriesForPlant(plantId: Int): Flow<List<MonthEntry>>

    @Query("SELECT * FROM month_entries WHERE plant_id = :plantId ORDER BY month")
    suspend fun getEntriesForPlantOnce(plantId: Int): List<MonthEntry>

    /** Ersetzt alle Einträge für eine Pflanze komplett */
    @Query("DELETE FROM month_entries WHERE plant_id = :plantId")
    suspend fun deleteAllForPlant(plantId: Int)

    @Upsert
    suspend fun upsert(entry: MonthEntry)

    @Upsert
    suspend fun upsertAll(entries: List<MonthEntry>)
}
