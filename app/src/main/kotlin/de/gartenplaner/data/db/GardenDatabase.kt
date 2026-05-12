package de.gartenplaner.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import de.gartenplaner.data.model.MonthEntry
import de.gartenplaner.data.model.Plan
import de.gartenplaner.data.model.Plant
import de.gartenplaner.data.model.Section
import de.gartenplaner.data.seed.DemoSeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Plan::class, Section::class, Plant::class, MonthEntry::class],
    version  = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GardenDatabase : RoomDatabase() {

    abstract fun planDao(): PlanDao
    abstract fun sectionDao(): SectionDao
    abstract fun plantDao(): PlantDao
    abstract fun monthEntryDao(): MonthEntryDao

    companion object {
        @Volatile private var INSTANCE: GardenDatabase? = null

        fun getInstance(context: Context): GardenDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): GardenDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                GardenDatabase::class.java,
                "gartenplaner.db",
            )
            .addCallback(SeedCallback())
            .build()
    }

    /** Wird genau einmal aufgerufen wenn die DB neu erstellt wird */
    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    DemoSeed.seed(database)
                }
            }
        }
    }
}
