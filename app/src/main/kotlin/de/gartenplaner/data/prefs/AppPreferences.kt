package de.gartenplaner.data.prefs

import android.content.Context
import androidx.core.content.edit

/** Leichtgewichtiger SharedPreferences-Wrapper für App-weiten UI-State */
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("gartenplaner_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_PLAN_ID = "active_plan_id"
    }

    var activePlanId: Int?
        get() = prefs.getInt(KEY_ACTIVE_PLAN_ID, -1).takeIf { it != -1 }
        set(value) = prefs.edit { putInt(KEY_ACTIVE_PLAN_ID, value ?: -1) }
}
