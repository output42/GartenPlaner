package de.gartenplaner.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import de.gartenplaner.data.model.Section
import de.gartenplaner.data.model.SectionWithPlants
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {

    @Query("SELECT * FROM sections WHERE plan_id = :planId ORDER BY `order`")
    fun getSectionsForPlan(planId: Int): Flow<List<Section>>

    @Transaction
    @Query("SELECT * FROM sections WHERE plan_id = :planId ORDER BY `order`")
    fun getSectionsWithPlants(planId: Int): Flow<List<SectionWithPlants>>

    @Upsert
    suspend fun upsert(section: Section): Long

    @Upsert
    suspend fun upsertAll(sections: List<Section>)

    @Delete
    suspend fun delete(section: Section)
}
