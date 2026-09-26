package com.signaldesk.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.room.Room
import com.signaldesk.android.incident.data.CachedIncidentRepository
import com.signaldesk.android.incident.data.NetworkIncidentRepository
import com.signaldesk.android.incident.data.local.SignalDeskDatabase
import com.signaldesk.android.incident.ui.IncidentDetailViewModel
import com.signaldesk.android.incident.ui.IncidentDetailViewModelFactory
import com.signaldesk.android.incident.ui.IncidentListViewModel
import com.signaldesk.android.incident.ui.IncidentListViewModelFactory
import com.signaldesk.android.ui.theme.SignalDeskTheme

class MainActivity : ComponentActivity() {

    private val database: SignalDeskDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            SignalDeskDatabase::class.java,
            "signaldesk.db"
        )
            .addMigrations(
                SignalDeskDatabase.MIGRATION_1_2,
                SignalDeskDatabase.MIGRATION_2_3
            )
            .build()
    }

    private val repository by lazy {
        CachedIncidentRepository(
            remote = NetworkIncidentRepository(),
            local = database.incidentDao()
        )
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
