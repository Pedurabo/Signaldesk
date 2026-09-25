package com.signaldesk.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.signaldesk.android.incident.data.NetworkIncidentRepository
import com.signaldesk.android.incident.ui.IncidentDetailViewModel
import com.signaldesk.android.incident.ui.IncidentDetailViewModelFactory
import com.signaldesk.android.incident.ui.IncidentListViewModel
import com.signaldesk.android.incident.ui.IncidentListViewModelFactory
import com.signaldesk.android.ui.theme.SignalDeskTheme

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
                SignalDeskApp(
                    listViewModel = listViewModel,
                    detailViewModel = detailViewModel
                )
            }
        }
    }
}
