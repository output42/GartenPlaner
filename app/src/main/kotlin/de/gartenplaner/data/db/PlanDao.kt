package de.gartenplaner.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import de.gartenplaner.data.model.Plan
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Query("SELECT * FROM plans ORDER BY year DESC")
    fun getAllPlans(): Flow<List<Plan>>

    @Query("SELECT * FROM plans WHERE id = :planId")
    fun getPlanById(planId: Int): Flow<Plan?>

    /** Gibt die generierte ID zurück */
    @Upsert
    suspend fun upsert(plan: Plan): Long

    @Delete
    suspend fun delete(plan: Plan)
}
