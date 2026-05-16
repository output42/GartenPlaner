package de.gartenplaner

import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.gartenplaner.data.db.GardenDatabase
import de.gartenplaner.data.repository.PlanRepository
import de.gartenplaner.navigation.Screen
import de.gartenplaner.ui.editsection.EditSectionScreen
import de.gartenplaner.ui.editplant.EditPlantScreen
import de.gartenplaner.ui.plan.PlanScreen
import de.gartenplaner.ui.plan.PlanUiState
import de.gartenplaner.ui.plan.PlanViewModel
import de.gartenplaner.ui.planlist.PlanListScreen
import de.gartenplaner.ui.plantpicker.PlantPickerScreen
import de.gartenplaner.ui.settings.SettingsScreen
import de.gartenplaner.ui.theme.GartenPlanerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /**
     * WebView für PDF-Export — einmalig hier gehalten, nie im View-Tree.
     * Zugriff aus dem Export-Flow via (context as MainActivity).printWebView.
     */
    lateinit var printWebView: WebView
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        printWebView = WebView(this).apply {
            settings.javaScriptEnabled = false
        }

        prewarmPlanCache()

        setContent {
            GartenPlanerTheme {
                val primaryColor = MaterialTheme.colorScheme.primary
                val view = LocalView.current
                // LaunchedEffect(Unit) läuft nur einmal statt auf jedem Rekompositions-Frame
                LaunchedEffect(Unit) {
                    val win = (view.context as ComponentActivity).window
                    @Suppress("DEPRECATION")
                    win.statusBarColor = primaryColor.toArgb()
                    WindowInsetsControllerCompat(win, view).isAppearanceLightStatusBars = false
                }

                val navController = rememberNavController()

                // Scaffold ohne bottomBar — jeder Screen bringt seine eigene BottomBar mit.
                // Dadurch ändert sich die NavHost-Höhe nie während einer Animation.
                Scaffold(contentWindowInsets = WindowInsets(0)) { innerPadding ->
                    NavHost(
                        navController    = navController,
                        startDestination = Screen.PlanList.route,
                        modifier         = Modifier.fillMaxSize().padding(innerPadding),
                        // Vorwärts: neuer Screen gleitet von rechts ein, alter nach links raus
                        // Tabs richtungsabhängig, hierarchisch immer rechts→links.
                        // Parallax: neuer Screen gleitet voll rein, alter nur ¼ raus.
                        enterTransition = {
                            val from = tabIndex(initialState.destination.route)
                            val to   = tabIndex(targetState.destination.route)
                            slideInHorizontally(tween(600)) {
                                if (from >= 0 && to >= 0 && to < from) -it else it
                            }
                        },
                        exitTransition = {
                            val from = tabIndex(initialState.destination.route)
                            val to   = tabIndex(targetState.destination.route)
                            slideOutHorizontally(tween(600)) {
                                if (from >= 0 && to >= 0 && to < from) it / 4 else -it / 4
                            }
                        },
                        popEnterTransition = {
                            slideInHorizontally(tween(600)) { -it / 4 }
                        },
                        popExitTransition = {
                            slideOutHorizontally(tween(600)) { it }
                        },
                    ) {
                        // PlanList (Start-Destination) braucht explizites None: NavHost-Default
                        // popEnterTransition wird für die Start-Destination ignoriert und fällt
                        // auf enterTransition zurück — was falsch wäre.
                        composable(
                            Screen.PlanList.route,
                            popEnterTransition = { EnterTransition.None },
                        ) {
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
                            val planId     = back.arguments?.getString(Screen.EditPlant.ARG_PLAN_ID)?.toIntOrNull() ?: return@composable
                            val plantId    = back.arguments?.getString(Screen.EditPlant.ARG_PLANT_ID)?.toIntOrNull()
                            val templateId = back.arguments?.getString(Screen.EditPlant.ARG_TEMPLATE_ID)?.toIntOrNull()
                            EditPlantScreen(navController, planId, plantId, templateId)
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

    private var isPrinting = false

    fun startPrint(html: String, jobName: String) {
        if (isPrinting) return
        isPrinting = true
        printWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                isPrinting = false
                val adapter = view.createPrintDocumentAdapter(jobName)
                val pm = getSystemService(PrintManager::class.java)
                pm.print(jobName, adapter, PrintAttributes.Builder().build())
            }
        }
        printWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun prewarmPlanCache() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val repo = PlanRepository(GardenDatabase.getInstance(this@MainActivity))
                repo.getAllPlans().first().forEach { plan ->
                    val sections    = repo.getSectionsWithPlants(plan.id).first()
                    val entries     = repo.getMonthEntriesForPlan(plan.id).first()
                    val unsectioned = repo.getUnsectionedPlants(plan.id).first()
                    PlanViewModel.setCache(
                        plan.id,
                        PlanUiState.Success(
                            plan              = plan,
                            sections          = sections.map { sw ->
                                sw.copy(plants = sw.plants.sortedBy { it.order })
                            },
                            unsectionedPlants = unsectioned.sortedBy { it.order },
                            monthEntries      = entries.groupBy { it.plantId },
                        )
                    )
                }
            } catch (_: Exception) { }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        printWebView.destroy()
    }
}

// Tab-Reihenfolge: Plan=0, Library=1, Settings=2
// Wird in den NavHost-Transitions genutzt, um die Slide-Richtung zwischen Tabs zu bestimmen.
private fun tabIndex(route: String?): Int = when {
    route?.startsWith("plan/") == true    -> 0
    route?.startsWith("library/") == true -> 1
    route?.startsWith("settings/") == true -> 2
    else -> -1
}
