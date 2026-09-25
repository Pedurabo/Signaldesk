package com.signaldesk.android.incident.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.IncidentTimelineEvent
import com.signaldesk.android.incident.data.IncidentRepository
import com.signaldesk.android.incident.data.ObservableIncidentDetailRepository
import com.signaldesk.android.incident.data.ObservableIncidentTimelineRepository
import com.signaldesk.android.incident.data.NetworkIncidentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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
    private val repository: IncidentRepository = NetworkIncidentRepository(),
    private val observableRepository: ObservableIncidentDetailRepository? = null,
    private val observableTimelineRepository: ObservableIncidentTimelineRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        IncidentDetailUiState()
    )

    val uiState: StateFlow<IncidentDetailUiState> =
        _uiState.asStateFlow()

    private var observeIncidentJob: Job? = null
    private var observeTimelineJob: Job? = null

    private fun observeTimeline(
        incidentId: Long
    ) {
        val observableTimelineRepository =
            observableTimelineRepository ?: return

        observeTimelineJob?.cancel()

        observeTimelineJob = viewModelScope.launch {
            observableTimelineRepository.observeIncidentTimeline(
                incidentId = incidentId
            ).collect { timeline ->
                _uiState.value = _uiState.value.copy(
                    timeline = timeline
                )
            }
        }
    }

    private fun observeIncident(
        incidentId: Long
    ) {
        val observableRepository =
            observableRepository ?: return

        observeIncidentJob?.cancel()

        observeIncidentJob = viewModelScope.launch {
            observableRepository.observeIncident(
                incidentId = incidentId
            ).collect { incident ->
                _uiState.value = _uiState.value.copy(
                    incident = incident
                )
            }
        }
    }

    fun loadIncident(
        incidentId: Long
    ) {
        observeIncident(
            incidentId = incidentId
        )

        viewModelScope.launch {
            _uiState.value =
                if (observableRepository == null) {
                    _uiState.value.copy(
                        incident = null,
                        isLoading = true,
                        error = null
                    )
                } else {
                    _uiState.value.copy(
                        isLoading = true,
                        error = null
                    )
                }

            try {
                val incident = withContext(ioDispatcher) {
                    repository.getIncident(
                        incidentId = incidentId
                    )
                }

                _uiState.value =
                    if (observableRepository == null) {
                        _uiState.value.copy(
                            incident = incident,
                            isLoading = false,
                            error = null
                        )
                    } else {
                        _uiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                    }
            } catch (exception: Exception) {
                _uiState.value =
                    if (observableRepository == null) {
                        _uiState.value.copy(
                            incident = null,
                            isLoading = false,
                            error = exception.message
                                ?: "Unable to load incident"
                        )
                    } else {
                        _uiState.value.copy(
                            isLoading = false,
                            error = exception.message
                                ?: "Unable to load incident"
                        )
                    }
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
                val updatedIncident = withContext(ioDispatcher) {
                    repository.updateIncidentStatus(
                        incidentId = incidentId,
                        status = status
                    )
                }

                _uiState.value =
                    if (observableRepository == null) {
                        _uiState.value.copy(
                            incident = updatedIncident,
                            isUpdating = false,
                            error = null
                        )
                    } else {
                        _uiState.value.copy(
                            isUpdating = false,
                            error = null
                        )
                    }

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
        observeTimeline(
            incidentId = incidentId
        )

        viewModelScope.launch {
            _uiState.value =
                if (observableTimelineRepository == null) {
                    _uiState.value.copy(
                        timeline = emptyList(),
                        isTimelineLoading = true,
                        timelineError = null
                    )
                } else {
                    _uiState.value.copy(
                        isTimelineLoading = true,
                        timelineError = null
                    )
                }

            try {
                val timeline = withContext(ioDispatcher) {
                    repository.getIncidentTimeline(
                        incidentId = incidentId
                    )
                }

                _uiState.value =
                    if (observableTimelineRepository == null) {
                        _uiState.value.copy(
                            timeline = timeline,
                            isTimelineLoading = false,
                            timelineError = null
                        )
                    } else {
                        _uiState.value.copy(
                            isTimelineLoading = false,
                            timelineError = null
                        )
                    }
            } catch (exception: Exception) {
                _uiState.value =
                    if (observableTimelineRepository == null) {
                        _uiState.value.copy(
                            timeline = emptyList(),
                            isTimelineLoading = false,
                            timelineError = exception.message
                                ?: "Unable to load incident timeline"
                        )
                    } else {
                        _uiState.value.copy(
                            isTimelineLoading = false,
                            timelineError = exception.message
                                ?: "Unable to load incident timeline"
                        )
                    }
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
                withContext(ioDispatcher) {
                    repository.addIncidentNote(
                        incidentId = incidentId,
                        message = trimmedMessage
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isAddingNote = false,
                    noteError = null
                )

                if (observableTimelineRepository == null) {

                    loadIncidentTimeline(

                        incidentId = incidentId

                    )

                }
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
    private val repository: IncidentRepository,
    private val observableRepository: ObservableIncidentDetailRepository,
    private val observableTimelineRepository: ObservableIncidentTimelineRepository
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
                repository = repository,
                observableRepository = observableRepository,
                observableTimelineRepository =
                    observableTimelineRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
