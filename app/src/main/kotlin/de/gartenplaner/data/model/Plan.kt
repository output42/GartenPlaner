package de.gartenplaner.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plans")
data class Plan(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val year: Int,

    val title: String,

    @ColumnInfo(name = "frost_info_last")
    val frostInfoLast: String = "",

    @ColumnInfo(name = "frost_info_first")
    val frostInfoFirst: String = "",

    @ColumnInfo(name = "climate_zone")
    val climateZone: String = "",
)
