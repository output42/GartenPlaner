package de.gartenplaner.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plants",
    foreignKeys = [
        ForeignKey(
            entity        = Plan::class,
            parentColumns = ["id"],
            childColumns  = ["plan_id"],
            onDelete      = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity        = Section::class,
            parentColumns = ["id"],
            childColumns  = ["section_id"],
            onDelete      = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("plan_id"), Index("section_id")],
)
data class Plant(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "plan_id")
    val planId: Int,

    @ColumnInfo(name = "section_id")
    val sectionId: Int?,

    val name: String,

    val subtitle: String = "",

    /** Reihenfolge innerhalb der Section bzw. Plan (0-basiert) */
    val order: Int = 0,

    /** true = aus Pflanzenbibliothek übernommen, false = manuell angelegt */
    @ColumnInfo(name = "from_library")
    val fromLibrary: Boolean = false,
)
