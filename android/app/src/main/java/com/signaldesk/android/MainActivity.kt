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
import com.signaldesk.android.incident.Incident
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

    private var selectedIncident by mutableStateOf<Incident?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            SignalDeskTheme {

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                val incident = selectedIncident

                if (incident == null) {
                    IncidentListScreen(
                        incidents = uiState.incidents,
                        isLoading = uiState.isLoading,
                        error = uiState.error,
                        onIncidentClick = { clickedIncident ->
                            selectedIncident = clickedIncident
                        }
                    )
                } else {
                    IncidentDetailScreen(
                        incident = incident,
                        onBack = {
                            selectedIncident = null
                        }
                    )
                }
            }
        }
    }
}