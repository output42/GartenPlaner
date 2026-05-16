package de.gartenplaner.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import de.gartenplaner.R
import de.gartenplaner.navigation.Screen

enum class PlanTab { PLAN, LIBRARY, SETTINGS }

@Composable
fun PlanBottomBar(navController: NavController, planId: Int, current: PlanTab) {
    NavigationBar {
        val itemColors = NavigationBarItemDefaults.colors(
            indicatorColor    = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
        )
        NavigationBarItem(
            selected = current == PlanTab.PLAN,
            onClick  = {
                if (current != PlanTab.PLAN)
                    navController.popBackStack(Screen.Plan.route(planId), inclusive = false)
            },
            icon   = {},
            label  = { Text(stringResource(R.string.nav_plan)) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = current == PlanTab.LIBRARY,
            onClick  = {
                if (current != PlanTab.LIBRARY)
                    navController.navigate(Screen.Library.route(planId)) {
                        popUpTo(Screen.Plan.route(planId)) { saveState = true }
                        restoreState  = true
                        launchSingleTop = true
                    }
            },
            icon   = {},
            label  = { Text(stringResource(R.string.nav_library)) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = current == PlanTab.SETTINGS,
            onClick  = {
                if (current != PlanTab.SETTINGS)
                    navController.navigate(Screen.Settings.route(planId)) {
                        popUpTo(Screen.Plan.route(planId)) { saveState = true }
                        restoreState  = true
                        launchSingleTop = true
                    }
            },
            icon   = {},
            label  = { Text(stringResource(R.string.nav_settings)) },
            colors = itemColors,
        )
    }
}
