package com.signaldesk.android.incident.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.Severity
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
    val selectedStatus: IncidentStatus? = null,
    val selectedSeverity: Severity? = null,
    val isCreatingIncident: Boolean = false,
    val createIncidentError: String? = null,
    val selectedIncident: Incident? = null,
    val isIncidentLoading: Boolean = false,
    val incidentError: String? = null,
    val createdIncidentId: Long? = null,
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
        val status = _uiState.value.selectedStatus
        val severity = _uiState.value.selectedSeverity

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val incidents = withContext(Dispatchers.IO) {
                    repository.getIncidents(
                        status = status,
                        severity = severity
                    )
                }

                _uiState.value = _uiState.value.copy(
                    incidents = incidents,
                    isLoading = false,
                    error = null
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    incidents = emptyList(),
                    isLoading = false,
                    error = exception.message
                        ?: "Unable to load incidents"
                )
            }
        }
    }

    fun refreshIncidents() {
        loadIncidents()
    }

    fun setStatusFilter(status: IncidentStatus?) {
        if (_uiState.value.selectedStatus == status) {
            return
        }

        _uiState.value = _uiState.value.copy(
            selectedStatus = status
        )

        loadIncidents()
    }

    fun setSeverityFilter(severity: Severity?) {
        if (_uiState.value.selectedSeverity == severity) {
            return
        }

        _uiState.value = _uiState.value.copy(
            selectedSeverity = severity
        )

        loadIncidents()
    }
    fun createIncident(
        title: String,
        description: String,
        severity: Severity
    ) {
        val trimmedTitle = title.trim()
        val trimmedDescription = description.trim()

        if (trimmedTitle.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                createIncidentError = "Title cannot be empty"
            )
            return
        }

        if (trimmedDescription.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                createIncidentError = "Description cannot be empty"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCreatingIncident = true,
                createIncidentError = null,
                createdIncidentId = null
            )

            try {
                val createdIncident = withContext(Dispatchers.IO) {
                    repository.createIncident(
                        title = trimmedTitle,
                        description = trimmedDescription,
                        severity = severity
                    )
                }

                _uiState.value = _uiState.value.copy(
                    incidents = listOf(createdIncident) +
                        _uiState.value.incidents.filterNot {
                            it.id == createdIncident.id
                        },
                    isCreatingIncident = false,
                    createIncidentError = null,
                    createdIncidentId = createdIncident.id
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCreatingIncident = false,
                    createIncidentError = exception.message
                        ?: "Unable to create incident",
                    createdIncidentId = null
                )
            }
        }
    }

    fun clearCreatedIncident() {
        _uiState.value = _uiState.value.copy(
            createdIncidentId = null
        )
    }

    fun loadIncident(
        incidentId: Long
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedIncident = null,
                isIncidentLoading = true,
                incidentError = null
            )

            try {
                val incident = withContext(Dispatchers.IO) {
                    repository.getIncident(
                        incidentId = incidentId
                    )
                }

                _uiState.value = _uiState.value.copy(
                    selectedIncident = incident,
                    isIncidentLoading = false,
                    incidentError = null
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    selectedIncident = null,
                    isIncidentLoading = false,
                    incidentError = exception.message
                        ?: "Unable to load incident"
                )
            }
        }
    }

    fun clearIncidentDetail() {
        _uiState.value = _uiState.value.copy(
            selectedIncident = null,
            isIncidentLoading = false,
            incidentError = null,
            timeline = emptyList(),
            isTimelineLoading = false,
            timelineError = null,
            isAddingNote = false,
            noteError = null
        )
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
                    selectedIncident =
                        if (_uiState.value.selectedIncident?.id == updatedIncident.id) {
                            updatedIncident
                        } else {
                            _uiState.value.selectedIncident
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