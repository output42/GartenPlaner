package de.gartenplaner.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
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
    version  = 3,
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE month_entries ADD COLUMN plan_id INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    UPDATE month_entries
                    SET plan_id = (SELECT plan_id FROM plants WHERE plants.id = month_entries.plant_id)
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_month_entries_plan_id ON month_entries(plan_id)")
            }
        }

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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .addCallback(SeedCallback())
            .build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // plants bekommt plan_id (direkte Plan-Referenz) und section_id wird nullable.
                // SQLite unterstützt kein ALTER COLUMN → Tabelle neu anlegen.
                db.execSQL("""
                    CREATE TABLE plants_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        plan_id INTEGER NOT NULL DEFAULT 0,
                        section_id INTEGER,
                        name TEXT NOT NULL,
                        subtitle TEXT NOT NULL DEFAULT '',
                        `order` INTEGER NOT NULL DEFAULT 0,
                        from_library INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(plan_id) REFERENCES plans(id) ON DELETE CASCADE,
                        FOREIGN KEY(section_id) REFERENCES sections(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                // plan_id aus dem Section-Join ableiten
                db.execSQL("""
                    INSERT INTO plants_new (id, plan_id, section_id, name, subtitle, `order`, from_library)
                    SELECT p.id, s.plan_id, p.section_id, p.name, p.subtitle, p.`order`, p.from_library
                    FROM plants p
                    INNER JOIN sections s ON p.section_id = s.id
                """.trimIndent())
                db.execSQL("DROP TABLE plants")
                db.execSQL("ALTER TABLE plants_new RENAME TO plants")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plants_plan_id ON plants(plan_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plants_section_id ON plants(section_id)")
            }
        }
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
