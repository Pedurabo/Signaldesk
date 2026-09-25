package com.signaldesk.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.signaldesk.android.incident.data.NetworkIncidentRepository
import com.signaldesk.android.incident.ui.CreateIncidentScreen
import com.signaldesk.android.incident.ui.IncidentDetailScreen
import com.signaldesk.android.incident.ui.IncidentDetailStateScreen
import com.signaldesk.android.incident.ui.IncidentListScreen
import com.signaldesk.android.incident.ui.IncidentListViewModel
import com.signaldesk.android.incident.ui.IncidentListViewModelFactory
import com.signaldesk.android.ui.theme.SignalDeskTheme

class MainActivity : ComponentActivity() {

    private val viewModel: IncidentListViewModel by viewModels {
        IncidentListViewModelFactory(
            NetworkIncidentRepository()
        )
    }

    private var selectedIncidentId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            SignalDeskTheme {

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                var isCreatingIncident by remember {
                    mutableStateOf(false)
                }

                LaunchedEffect(uiState.createdIncidentId) {
                    val createdIncidentId = uiState.createdIncidentId

                    if (createdIncidentId != null) {
                        isCreatingIncident = false
                        selectedIncidentId = createdIncidentId

                        viewModel.loadIncident(
                            incidentId = createdIncidentId
                        )

                        viewModel.loadIncidentTimeline(
                            incidentId = createdIncidentId
                        )

                        viewModel.clearCreatedIncident()
                    }
                }


                if (isCreatingIncident) {
                    CreateIncidentScreen(
                        isCreating = uiState.isCreatingIncident,
                        error = uiState.createIncidentError,
                        onCreateIncident = { title, description, severity ->
                            viewModel.createIncident(
                                title = title,
                                description = description,
                                severity = severity
                            )
                        },
                        onBack = {
                            isCreatingIncident = false
                        }
                    )
                } else if (selectedIncidentId == null) {
                    IncidentListScreen(
                        incidents = uiState.incidents,
                        isLoading = uiState.isLoading,
                        error = uiState.error,
                        selectedStatus = uiState.selectedStatus,
                        selectedSeverity = uiState.selectedSeverity,
                        onStatusFilterChange = viewModel::setStatusFilter,
                        onSeverityFilterChange = viewModel::setSeverityFilter,
                        onIncidentClick = { clickedIncident ->
                            selectedIncidentId = clickedIncident.id

                            viewModel.loadIncident(
                                incidentId = clickedIncident.id
                            )

                            viewModel.loadIncidentTimeline(
                                incidentId = clickedIncident.id
                            )
                        },
                        onCreateIncidentClick = {
                            isCreatingIncident = true
                        }
                    )
                } else {
                    val incident = uiState.selectedIncident
                    val incidentId = selectedIncidentId

                    when {
                        uiState.isIncidentLoading -> {
                            IncidentDetailStateScreen(
                                isLoading = true,
                                error = null,
                                onRetry = {}
                            )
                        }

                        uiState.incidentError != null && incidentId != null -> {
                            IncidentDetailStateScreen(
                                isLoading = false,
                                error = uiState.incidentError,
                                onRetry = {
                                    viewModel.loadIncident(
                                        incidentId = incidentId
                                    )
                                }
                            )
                        }

                        incident != null -> {
                            IncidentDetailScreen(
                                incident = incident,
                                timeline = uiState.timeline,
                                isTimelineLoading = uiState.isTimelineLoading,
                                timelineError = uiState.timelineError,
                                isAddingNote = uiState.isAddingNote,
                                noteError = uiState.noteError,
                                isUpdating =
                                    uiState.updatingIncidentId == incident.id,
                                error = uiState.error,
                                onAddNote = { message ->
                                    viewModel.addIncidentNote(
                                        incidentId = incident.id,
                                        message = message
                                    )
                                },
                                onStatusChange = { newStatus ->
                                    viewModel.updateIncidentStatus(
                                        incidentId = incident.id,
                                        status = newStatus
                                    )
                                },
                                onBack = {
                                    selectedIncidentId = null
                                    viewModel.clearIncidentDetail()
                                    viewModel.refreshIncidents()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}