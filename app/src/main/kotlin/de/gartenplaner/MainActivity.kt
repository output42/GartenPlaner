package de.gartenplaner

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.gartenplaner.navigation.Screen
import de.gartenplaner.ui.editsection.EditSectionScreen
import de.gartenplaner.ui.editplant.EditPlantScreen
import de.gartenplaner.ui.plan.PlanScreen
import de.gartenplaner.ui.planlist.PlanListScreen
import de.gartenplaner.ui.plantpicker.PlantPickerScreen
import de.gartenplaner.ui.settings.SettingsScreen
import de.gartenplaner.ui.theme.GartenPlanerTheme

class MainActivity : ComponentActivity() {

    /**
     * WebView für PDF-Export — einmalig hier gehalten, nie im View-Tree.
     * Zugriff aus dem Export-Flow via (context as MainActivity).printWebView.
     * Implementierung: Session 10.
     */
    lateinit var printWebView: WebView
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        printWebView = WebView(this).apply {
            settings.javaScriptEnabled = false
        }

        setContent {
            GartenPlanerTheme {
                Surface(Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(
                        navController    = navController,
                        startDestination = Screen.PlanList.route,
                    ) {
                        composable(Screen.PlanList.route) {
                            PlanListScreen(navController)
                        }

                        composable(Screen.Plan.route) { back ->
                            val planId = back.arguments?.getString(Screen.Plan.ARG_PLAN_ID)?.toIntOrNull() ?: return@composable
                            PlanScreen(navController, planId)
                        }

                        composable(Screen.Library.route) { back ->
                            val planId = back.arguments?.getString(Screen.Library.ARG_PLAN_ID)?.toIntOrNull() ?: return@composable
                            PlantPickerScreen(navController, planId, standalone = true)
                        }

                        composable(Screen.Settings.route) { back ->
                            val planId = back.arguments?.getString(Screen.Settings.ARG_PLAN_ID)?.toIntOrNull() ?: return@composable
                            SettingsScreen(navController, planId)
                        }

                        composable(Screen.EditPlant.route) { back ->
                            val planId  = back.arguments?.getString(Screen.EditPlant.ARG_PLAN_ID)?.toIntOrNull() ?: return@composable
                            val plantId = back.arguments?.getString(Screen.EditPlant.ARG_PLANT_ID)?.toIntOrNull()
                            EditPlantScreen(navController, planId, plantId)
                        }

                        composable(Screen.PlantPicker.route) { back ->
                            val planId = back.arguments?.getString(Screen.PlantPicker.ARG_PLAN_ID)?.toIntOrNull() ?: return@composable
                            PlantPickerScreen(navController, planId, standalone = false)
                        }

                        composable(Screen.EditSection.route) { back ->
                            val planId    = back.arguments?.getString(Screen.EditSection.ARG_PLAN_ID)?.toIntOrNull() ?: return@composable
                            val sectionId = back.arguments?.getString(Screen.EditSection.ARG_SECTION_ID)?.toIntOrNull()
                            EditSectionScreen(navController, planId, sectionId)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        printWebView.destroy()
    }
}
