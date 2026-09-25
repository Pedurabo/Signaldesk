package com.signaldesk.android.incident.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.IncidentTimelineEvent
import com.signaldesk.android.incident.data.IncidentRepository
import com.signaldesk.android.incident.data.NetworkIncidentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class IncidentListUiState(
    val incidents: List<Incident> = emptyList(),
    val isLoading: Boolean = false,
    val updatingIncidentId: Long? = null,
    val timeline: List<IncidentTimelineEvent> = emptyList(),
    val isTimelineLoading: Boolean = false,
    val timelineError: String? = null,
    val isAddingNote: Boolean = false,
    val noteError: String? = null,
    val error: String? = null
)

class IncidentListViewModel(
    private val repository: IncidentRepository = NetworkIncidentRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        IncidentListUiState(isLoading = true)
    )

    val uiState: StateFlow<IncidentListUiState> =
        _uiState.asStateFlow()

    init {
        loadIncidents()
    }

    private fun loadIncidents() {
        viewModelScope.launch {
            _uiState.value = IncidentListUiState(
                isLoading = true
            )

            try {
                val incidents = withContext(Dispatchers.IO) {
                    repository.getIncidents()
                }

                _uiState.value = IncidentListUiState(
                    incidents = incidents,
                    isLoading = false
                )
            } catch (exception: Exception) {
                _uiState.value = IncidentListUiState(
                    incidents = emptyList(),
                    isLoading = false,
                    error = exception.message ?: "Unable to load incidents"
                )
            }
        }
    }

    fun loadIncidentTimeline(
        incidentId: Long
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                timeline = emptyList(),
                isTimelineLoading = true,
                timelineError = null
            )

            try {
                val timeline = withContext(Dispatchers.IO) {
                    repository.getIncidentTimeline(
                        incidentId = incidentId
                    )
                }

                _uiState.value = _uiState.value.copy(
                    timeline = timeline,
                    isTimelineLoading = false,
                    timelineError = null
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    timeline = emptyList(),
                    isTimelineLoading = false,
                    timelineError = exception.message
                        ?: "Unable to load incident timeline"
                )
            }
        }
    }

    fun addIncidentNote(
        incidentId: Long,
        message: String
    ) {
        val trimmedMessage = message.trim()

        if (trimmedMessage.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                noteError = "Note cannot be empty"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAddingNote = true,
                noteError = null
            )

            try {
                withContext(Dispatchers.IO) {
                    repository.addIncidentNote(
                        incidentId = incidentId,
                        message = trimmedMessage
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isAddingNote = false,
                    noteError = null
                )

                loadIncidentTimeline(
                    incidentId = incidentId
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAddingNote = false,
                    noteError = exception.message
                        ?: "Unable to add incident note"
                )
            }
        }
    }

    fun updateIncidentStatus(
        incidentId: Long,
        status: IncidentStatus
    ) {
        viewModelScope.launch {
            val currentState = _uiState.value

            _uiState.value = currentState.copy(
                updatingIncidentId = incidentId,
                error = null
            )

            try {
                val updatedIncident = withContext(Dispatchers.IO) {
                    repository.updateIncidentStatus(
                        incidentId = incidentId,
                        status = status
                    )
                }

                _uiState.value = _uiState.value.copy(
                    incidents = _uiState.value.incidents.map { incident ->
                        if (incident.id == updatedIncident.id) {
                            updatedIncident
                        } else {
                            incident
                        }
                    },
                    updatingIncidentId = null
                )

                loadIncidentTimeline(
                    incidentId = incidentId
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    updatingIncidentId = null,
                    error = exception.message
                        ?: "Unable to update incident status"
                )
            }
        }
    }
}