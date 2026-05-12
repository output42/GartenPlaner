package de.gartenplaner.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.gartenplaner.data.model.Plan
import de.gartenplaner.data.model.Section
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlanDaoTest {

    private lateinit var db     : GardenDatabase
    private lateinit var planDao: PlanDao

    @Before
    fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, GardenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        planDao = db.planDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun upsert_and_getAllPlans_returnsInsertedPlan() = runTest {
        val plan = Plan(year = 2026, title = "Test")
        planDao.upsert(plan)

        val plans = planDao.getAllPlans().first()
        assertEquals(1, plans.size)
        assertEquals("Test", plans[0].title)
        assertEquals(2026, plans[0].year)
    }

    @Test
    fun getPlanById_unknownId_returnsNull() = runTest {
        val result = planDao.getPlanById(999).first()
        assertNull(result)
    }

    @Test
    fun upsert_existingPlan_updatesTitle() = runTest {
        val id = planDao.upsert(Plan(year = 2025, title = "Alt"))
        planDao.upsert(Plan(id = id.toInt(), year = 2025, title = "Neu"))

        val updated = planDao.getPlanById(id.toInt()).first()
        assertEquals("Neu", updated?.title)
    }

    @Test
    fun delete_plan_removesFromList() = runTest {
        val id = planDao.upsert(Plan(year = 2026, title = "Del"))
        val plan = planDao.getPlanById(id.toInt()).first()!!
        planDao.delete(plan)

        val plans = planDao.getAllPlans().first()
        assertEquals(0, plans.size)
    }

    @Test
    fun delete_plan_cascadestoSections() = runTest {
        val planId = planDao.upsert(Plan(year = 2026, title = "P")).toInt()
        val sectionDao = db.sectionDao()
        sectionDao.upsert(Section(planId = planId, title = "S1", order = 0))

        val plan = planDao.getPlanById(planId).first()!!
        planDao.delete(plan)

        val sections = sectionDao.getSectionsForPlan(planId).first()
        assertEquals(0, sections.size)
    }

    @Test
    fun getAllPlans_orderedByYearDescending() = runTest {
        planDao.upsert(Plan(year = 2024, title = "Old"))
        planDao.upsert(Plan(year = 2026, title = "New"))
        planDao.upsert(Plan(year = 2025, title = "Mid"))

        val plans = planDao.getAllPlans().first()
        assertEquals(listOf(2026, 2025, 2024), plans.map { it.year })
    }
}
