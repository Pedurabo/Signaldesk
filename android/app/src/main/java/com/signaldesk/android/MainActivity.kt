package com.signaldesk.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.signaldesk.android.incident.ui.IncidentDetailViewModel
import com.signaldesk.android.incident.ui.IncidentDetailViewModelFactory
import com.signaldesk.android.incident.ui.IncidentListViewModel
import com.signaldesk.android.incident.ui.IncidentListViewModelFactory
import com.signaldesk.android.ui.theme.SignalDeskTheme

class MainActivity : ComponentActivity() {
    private val repository by lazy {
        (application as SignalDeskApplication)
            .incidentRepository
    }

    private val listViewModel: IncidentListViewModel by viewModels {
        IncidentListViewModelFactory(
            repository = repository,
            observableRepository = repository
        )
    }

    private val detailViewModel: IncidentDetailViewModel by viewModels {
        IncidentDetailViewModelFactory(
            repository = repository,
            observableRepository = repository,
            observableTimelineRepository = repository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            SignalDeskTheme {
                SignalDeskApp(
                    listViewModel = listViewModel,
                    detailViewModel = detailViewModel
                )
            }
        }
    }
}
