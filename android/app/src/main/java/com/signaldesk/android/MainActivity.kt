package com.signaldesk.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.signaldesk.android.incident.data.NetworkIncidentRepository
import com.signaldesk.android.incident.ui.CreateIncidentScreen
import com.signaldesk.android.incident.ui.IncidentDetailScreen
import com.signaldesk.android.incident.ui.IncidentDetailStateScreen
import com.signaldesk.android.incident.ui.IncidentDetailViewModel
import com.signaldesk.android.incident.ui.IncidentDetailViewModelFactory
import com.signaldesk.android.incident.ui.IncidentListScreen
import com.signaldesk.android.incident.ui.IncidentListViewModel
import com.signaldesk.android.incident.ui.IncidentListViewModelFactory
import com.signaldesk.android.ui.theme.SignalDeskTheme

private const val INCIDENT_LIST_ROUTE = "incidents"
private const val CREATE_INCIDENT_ROUTE = "incidents/create"
private const val INCIDENT_DETAIL_ROUTE = "incidents/{incidentId}"

class MainActivity : ComponentActivity() {

    private val repository = NetworkIncidentRepository()

    private val listViewModel: IncidentListViewModel by viewModels {
        IncidentListViewModelFactory(
            repository
        )
    }

    private val detailViewModel: IncidentDetailViewModel by viewModels {
        IncidentDetailViewModelFactory(
            repository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            SignalDeskTheme {
                val uiState by listViewModel.uiState.collectAsStateWithLifecycle()
                val detailUiState by detailViewModel.uiState.collectAsStateWithLifecycle()
                val navController = rememberNavController()

                LaunchedEffect(uiState.createdIncidentId) {
                    val createdIncidentId = uiState.createdIncidentId

                    if (createdIncidentId != null) {
                        listViewModel.clearCreatedIncident()

                        navController.navigate(
                            "incidents/$createdIncidentId"
                        ) {
                            popUpTo(CREATE_INCIDENT_ROUTE) {
                                inclusive = true
                            }
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = INCIDENT_LIST_ROUTE
                ) {
                    composable(
                        route = INCIDENT_LIST_ROUTE
                    ) {
                        IncidentListScreen(
                            incidents = uiState.incidents,
                            isLoading = uiState.isLoading,
                            error = uiState.error,
                            selectedStatus = uiState.selectedStatus,
                            selectedSeverity = uiState.selectedSeverity,
                            onStatusFilterChange = listViewModel::setStatusFilter,
                            onSeverityFilterChange = listViewModel::setSeverityFilter,
                            onIncidentClick = { clickedIncident ->
                                navController.navigate(
                                    "incidents/${clickedIncident.id}"
                                )
                            },
                            onCreateIncidentClick = {
                                navController.navigate(
                                    CREATE_INCIDENT_ROUTE
                                )
                            }
                        )
                    }

                    composable(
                        route = CREATE_INCIDENT_ROUTE
                    ) {
                        CreateIncidentScreen(
                            isCreating = uiState.isCreatingIncident,
                            error = uiState.createIncidentError,
                            onCreateIncident = { title, description, severity ->
                                listViewModel.createIncident(
                                    title = title,
                                    description = description,
                                    severity = severity
                                )
                            },
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(
                        route = INCIDENT_DETAIL_ROUTE,
                        arguments = listOf(
                            navArgument("incidentId") {
                                type = NavType.LongType
                            }
                        )
                    ) { backStackEntry ->
                        val incidentId =
                            backStackEntry.arguments?.getLong("incidentId")
                                ?: return@composable

                        LaunchedEffect(incidentId) {
                            detailViewModel.loadIncident(
                                incidentId = incidentId
                            )

                            detailViewModel.loadIncidentTimeline(
                                incidentId = incidentId
                            )
                        }

                        val incident = detailUiState.incident

                        when {
                            detailUiState.isLoading -> {
                                IncidentDetailStateScreen(
                                    isLoading = true,
                                    error = null,
                                    onRetry = {}
                                )
                            }

                            detailUiState.error != null -> {
                                IncidentDetailStateScreen(
                                    isLoading = false,
                                    error = detailUiState.error,
                                    onRetry = {
                                        detailViewModel.loadIncident(
                                            incidentId = incidentId
                                        )

                                        detailViewModel.loadIncidentTimeline(
                                            incidentId = incidentId
                                        )
                                    }
                                )
                            }

                            incident != null -> {
                                IncidentDetailScreen(
                                    incident = incident,
                                    timeline = detailUiState.timeline,
                                    isTimelineLoading =
                                        detailUiState.isTimelineLoading,
                                    timelineError =
                                        detailUiState.timelineError,
                                    isAddingNote =
                                        detailUiState.isAddingNote,
                                    noteError =
                                        detailUiState.noteError,
                                    isUpdating =
                                        detailUiState.isUpdating,
                                    error = detailUiState.error,
                                    onAddNote = { message ->
                                        detailViewModel.addIncidentNote(
                                            incidentId = incident.id,
                                            message = message
                                        )
                                    },
                                    onStatusChange = { newStatus ->
                                        detailViewModel.updateIncidentStatus(
                                            incidentId = incident.id,
                                            status = newStatus
                                        )
                                    },
                                    onBack = {
                                        detailViewModel.clearIncident()
                                        listViewModel.refreshIncidents()
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
