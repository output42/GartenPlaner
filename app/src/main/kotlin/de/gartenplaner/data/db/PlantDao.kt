package de.gartenplaner.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import de.gartenplaner.data.model.Plant
import kotlinx.coroutines.flow.Flow

data class PlanPlantCount(
    @ColumnInfo(name = "plan_id")    val planId: Int,
    @ColumnInfo(name = "plant_count") val plantCount: Int,
)

@Dao
interface PlantDao {

    @Query("SELECT * FROM plants WHERE section_id = :sectionId ORDER BY `order`")
    fun getPlantsForSection(sectionId: Int): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE plan_id = :planId AND section_id IS NULL ORDER BY `order`")
    fun getUnsectionedPlants(planId: Int): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE id = :plantId")
    suspend fun getPlantById(plantId: Int): Plant?

    @Query("SELECT plan_id, COUNT(id) AS plant_count FROM plants GROUP BY plan_id")
    fun getPlanPlantCounts(): Flow<List<PlanPlantCount>>

    @Upsert
    suspend fun upsert(plant: Plant): Long

    @Upsert
    suspend fun upsertAll(plants: List<Plant>)

    @Delete
    suspend fun delete(plant: Plant)
}
