package de.gartenplaner.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sections",
    foreignKeys = [
        ForeignKey(
            entity        = Plan::class,
            parentColumns = ["id"],
            childColumns  = ["plan_id"],
            onDelete      = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("plan_id")],
)
data class Section(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "plan_id")
    val planId: Int,

    val title: String,

    /** Reihenfolge innerhalb des Plans (0-basiert) */
    val order: Int = 0,
)
