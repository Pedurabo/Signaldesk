package com.signaldesk.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.signaldesk.android.incident.data.NetworkIncidentRepository
import com.signaldesk.android.incident.ui.IncidentDetailScreen
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

                val incident = selectedIncidentId?.let { incidentId ->
                    uiState.incidents.firstOrNull { it.id == incidentId }
                }

                if (selectedIncidentId == null) {
                    IncidentListScreen(
                        incidents = uiState.incidents,
                        isLoading = uiState.isLoading,
                        error = uiState.error,
                        onIncidentClick = { clickedIncident ->
                            selectedIncidentId = clickedIncident.id
                        }
                    )
                } else if (incident != null) {
                    IncidentDetailScreen(
                        incident = incident,
                        isUpdating = uiState.updatingIncidentId == incident.id,
                        error = uiState.error,
                        onStatusChange = { newStatus ->
                            viewModel.updateIncidentStatus(
                                incidentId = incident.id,
                                status = newStatus
                            )
                        },
                        onBack = {
                            selectedIncidentId = null
                        }
                    )
                }
            }
        }
    }
}