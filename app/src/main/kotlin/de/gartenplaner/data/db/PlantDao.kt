package de.gartenplaner.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import de.gartenplaner.data.model.Plant
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {

    @Query("SELECT * FROM plants WHERE section_id = :sectionId ORDER BY `order`")
    fun getPlantsForSection(sectionId: Int): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE id = :plantId")
    suspend fun getPlantById(plantId: Int): Plant?

    @Upsert
    suspend fun upsert(plant: Plant): Long

    @Upsert
    suspend fun upsertAll(plants: List<Plant>)

    @Delete
    suspend fun delete(plant: Plant)
}
