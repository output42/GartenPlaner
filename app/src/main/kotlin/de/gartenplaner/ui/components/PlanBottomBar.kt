package de.gartenplaner.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
        NavigationBarItem(
            selected = current == PlanTab.PLAN,
            onClick  = {
                if (current != PlanTab.PLAN)
                    navController.navigate(Screen.Plan.route(planId)) {
                        popUpTo(Screen.Plan.route(planId)) { inclusive = true }
                    }
            },
            icon  = {},
            label = { Text(stringResource(R.string.nav_plan)) },
        )
        NavigationBarItem(
            selected = current == PlanTab.LIBRARY,
            onClick  = {
                if (current != PlanTab.LIBRARY)
                    navController.navigate(Screen.Library.route(planId)) {
                        popUpTo(Screen.Plan.route(planId))
                    }
            },
            icon  = {},
            label = { Text(stringResource(R.string.nav_library)) },
        )
        NavigationBarItem(
            selected = current == PlanTab.SETTINGS,
            onClick  = {
                if (current != PlanTab.SETTINGS)
                    navController.navigate(Screen.Settings.route(planId)) {
                        popUpTo(Screen.Plan.route(planId))
                    }
            },
            icon  = {},
            label = { Text(stringResource(R.string.nav_settings)) },
        )
    }
}
