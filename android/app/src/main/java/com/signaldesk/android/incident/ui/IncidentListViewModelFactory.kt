package com.signaldesk.android.incident.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.signaldesk.android.incident.data.IncidentRepository
import com.signaldesk.android.incident.data.ObservableIncidentRepository

class IncidentListViewModelFactory(
    private val repository: IncidentRepository,
    private val observableRepository: ObservableIncidentRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(IncidentListViewModel::class.java)) {
            return IncidentListViewModel(
                repository = repository,
                observableRepository = observableRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}