package de.gartenplaner.data.db

import androidx.room.TypeConverter
import de.gartenplaner.data.model.ActivityType

class Converters {

    @TypeConverter
    fun fromActivityType(type: ActivityType): String = type.name

    @TypeConverter
    fun toActivityType(name: String): ActivityType = ActivityType.valueOf(name)
}
