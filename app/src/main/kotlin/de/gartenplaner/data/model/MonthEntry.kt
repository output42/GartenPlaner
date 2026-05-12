package de.gartenplaner.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "month_entries",
    foreignKeys = [
        ForeignKey(
            entity        = Plant::class,
            parentColumns = ["id"],
            childColumns  = ["plant_id"],
            onDelete      = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("plant_id")],
)
data class MonthEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "plant_id")
    val plantId: Int,

    /** 0 = Januar … 11 = Dezember */
    val month: Int,

    val type: ActivityType,

    /** Angezeigter Text im Chip/Zelle, z. B. "Voranz." oder "Ernte ↑" */
    val label: String,
)
