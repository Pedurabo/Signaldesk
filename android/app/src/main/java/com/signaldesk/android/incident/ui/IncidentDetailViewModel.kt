package com.signaldesk.android.incident.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

data class IncidentDetailUiState(
    val incident: Incident? = null,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val error: String? = null,
    val timeline: List<IncidentTimelineEvent> = emptyList(),
    val isTimelineLoading: Boolean = false,
    val timelineError: String? = null,
    val isAddingNote: Boolean = false,
    val noteError: String? = null
)

class IncidentDetailViewModel(
    private val repository: IncidentRepository = NetworkIncidentRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        IncidentDetailUiState()
    )

    val uiState: StateFlow<IncidentDetailUiState> =
        _uiState.asStateFlow()

    fun loadIncident(
        incidentId: Long
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                incident = null,
                isLoading = true,
                error = null
            )

            try {
                val incident = withContext(Dispatchers.IO) {
                    repository.getIncident(
                        incidentId = incidentId
                    )
                }

                _uiState.value = _uiState.value.copy(
                    incident = incident,
                    isLoading = false,
                    error = null
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    incident = null,
                    isLoading = false,
                    error = exception.message
                        ?: "Unable to load incident"
                )
            }
        }
    }

    fun updateIncidentStatus(
        incidentId: Long,
        status: IncidentStatus
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdating = true,
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
                    incident = updatedIncident,
                    isUpdating = false,
                    error = null
                )

                loadIncidentTimeline(
                    incidentId = incidentId
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = exception.message
                        ?: "Unable to update incident status"
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
    fun clearIncident() {
        _uiState.value = IncidentDetailUiState()
    }
}

class IncidentDetailViewModelFactory(
    private val repository: IncidentRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(
                IncidentDetailViewModel::class.java
            )
        ) {
            return IncidentDetailViewModel(
                repository = repository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
