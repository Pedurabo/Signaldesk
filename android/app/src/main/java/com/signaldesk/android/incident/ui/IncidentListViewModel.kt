package com.signaldesk.android.incident.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.Severity
import com.signaldesk.android.incident.data.IncidentRepository
import com.signaldesk.android.incident.data.NetworkIncidentRepository
import com.signaldesk.android.incident.data.ObservableIncidentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class IncidentListUiState(
    val incidents: List<Incident> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedStatus: IncidentStatus? = null,
    val selectedSeverity: Severity? = null,
    val isCreatingIncident: Boolean = false,
    val createIncidentError: String? = null,
    val createdIncidentId: Long? = null,
    val error: String? = null
)

class IncidentListViewModel(
    private val repository: IncidentRepository = NetworkIncidentRepository(),
    private val observableRepository: ObservableIncidentRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        IncidentListUiState(isLoading = true)
    )

    val uiState: StateFlow<IncidentListUiState> =
        _uiState.asStateFlow()

    private var loadIncidentsJob: Job? = null
    private var observeIncidentsJob: Job? = null

    init {
        observeIncidents()
        loadIncidents()
    }

    private fun observeIncidents() {
        val observableRepository =
            observableRepository ?: return

        observeIncidentsJob?.cancel()

        val status = _uiState.value.selectedStatus
        val severity = _uiState.value.selectedSeverity

        observeIncidentsJob = viewModelScope.launch {
            observableRepository.observeIncidents(
                status = status,
                severity = severity
            ).collect { incidents ->
                _uiState.value = _uiState.value.copy(
                    incidents = incidents
                )
            }
        }
    }

    private fun loadIncidents() {
        val status = _uiState.value.selectedStatus
        val severity = _uiState.value.selectedSeverity

        loadIncidentsJob?.cancel()

        loadIncidentsJob = viewModelScope.launch {
            val hasObservedIncidents =
                observableRepository != null &&
                    _uiState.value.incidents.isNotEmpty()

            _uiState.value = _uiState.value.copy(
                isLoading = !hasObservedIncidents,
                isRefreshing = hasObservedIncidents,
                error = null
            )

            try {
                val incidents = withContext(ioDispatcher) {
                    repository.getIncidents(
                        status = status,
                        severity = severity
                    )
                }

                _uiState.value =
                    if (observableRepository == null) {
                        _uiState.value.copy(
                            incidents = incidents,
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                    } else {
                        _uiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                    }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value =
                    if (observableRepository == null) {
                        _uiState.value.copy(
                            incidents = emptyList(),
                            isLoading = false,
                            isRefreshing = false,
                            error = exception.message
                                ?: "Unable to load incidents"
                        )
                    } else {
                        _uiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = exception.message
                                ?: "Unable to load incidents"
                        )
                    }
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

        observeIncidents()
        loadIncidents()
    }

    fun setSeverityFilter(severity: Severity?) {
        if (_uiState.value.selectedSeverity == severity) {
            return
        }

        _uiState.value = _uiState.value.copy(
            selectedSeverity = severity
        )

        observeIncidents()
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
                val createdIncident = withContext(ioDispatcher) {
                    repository.createIncident(
                        title = trimmedTitle,
                        description = trimmedDescription,
                        severity = severity
                    )
                }

                _uiState.value =
                    if (observableRepository == null) {
                        _uiState.value.copy(
                            incidents = listOf(createdIncident) +
                                _uiState.value.incidents.filterNot {
                                    it.id == createdIncident.id
                                },
                            isCreatingIncident = false,
                            createIncidentError = null,
                            createdIncidentId = createdIncident.id
                        )
                    } else {
                        _uiState.value.copy(
                            isCreatingIncident = false,
                            createIncidentError = null,
                            createdIncidentId = createdIncident.id
                        )
                    }
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

}
