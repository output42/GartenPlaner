package de.gartenplaner.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class SectionWithPlants(
    @Embedded
    val section: Section,

    @Relation(
        parentColumn = "id",
        entityColumn = "section_id",
    )
    val plants: List<Plant>,
)
