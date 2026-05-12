package de.gartenplaner.ui.editsection

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.gartenplaner.R
import de.gartenplaner.data.db.GardenDatabase
import de.gartenplaner.data.model.Section
import de.gartenplaner.data.repository.PlanRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSectionScreen(
    navController: NavController,
    planId       : Int,
    sectionId    : Int?,
) {
    val context = LocalContext.current
    val repo    = remember { PlanRepository(GardenDatabase.getInstance(context)) }
    val scope   = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Lade vorhandenen Titel wenn wir umbenennen
    LaunchedEffect(sectionId) {
        if (sectionId != null) {
            repo.getSectionsWithPlants(planId)
                .collect { sections ->
                    sections.firstOrNull { it.section.id == sectionId }
                        ?.section?.title?.let { title = it }
                }
        }
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (sectionId == null) stringResource(R.string.edit_section_title_new)
                        else                   stringResource(R.string.edit_section_title_edit)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor     = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                repo.upsertSection(
                                    Section(id = sectionId ?: 0, planId = planId, title = title.trim())
                                )
                                navController.popBackStack()
                            }
                        },
                        enabled = title.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.action_save), color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(
                text     = stringResource(R.string.section_name_label),
                style    = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                placeholder   = { Text(stringResource(R.string.section_name_hint)) },
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
    }
}
