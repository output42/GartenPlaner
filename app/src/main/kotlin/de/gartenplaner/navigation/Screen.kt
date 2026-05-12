package de.gartenplaner.navigation

sealed class Screen(val route: String) {

    /** Start-Destination — Übersicht aller Pläne */
    data object PlanList : Screen("plan_list")

    /** Bottom-Nav-Tab: Jahresplan (trägt planId) */
    data object Plan : Screen("plan/{planId}") {
        fun route(planId: Int) = "plan/$planId"
        const val ARG_PLAN_ID = "planId"
    }

    /** Bottom-Nav-Tab: Pflanzenbibliothek standalone */
    data object Library : Screen("library/{planId}") {
        fun route(planId: Int) = "library/$planId"
        const val ARG_PLAN_ID = "planId"
    }

    /** Bottom-Nav-Tab: Einstellungen des aktiven Plans */
    data object Settings : Screen("settings/{planId}") {
        fun route(planId: Int) = "settings/$planId"
        const val ARG_PLAN_ID = "planId"
    }

    /** Pushed: Pflanze anlegen/bearbeiten */
    data object EditPlant : Screen("edit_plant/{planId}?plantId={plantId}") {
        fun route(planId: Int, plantId: Int? = null) =
            if (plantId != null) "edit_plant/$planId?plantId=$plantId"
            else "edit_plant/$planId"
        const val ARG_PLAN_ID  = "planId"
        const val ARG_PLANT_ID = "plantId"
    }

    /** Pushed: Bibliothek als Sub-Screen von PlanScreen */
    data object PlantPicker : Screen("plant_picker/{planId}") {
        fun route(planId: Int) = "plant_picker/$planId"
        const val ARG_PLAN_ID = "planId"
    }

    /** Pushed: Section anlegen/umbenennen */
    data object EditSection : Screen("edit_section/{planId}?sectionId={sectionId}") {
        fun route(planId: Int, sectionId: Int? = null) =
            if (sectionId != null) "edit_section/$planId?sectionId=$sectionId"
            else "edit_section/$planId"
        const val ARG_PLAN_ID    = "planId"
        const val ARG_SECTION_ID = "sectionId"
    }
}
