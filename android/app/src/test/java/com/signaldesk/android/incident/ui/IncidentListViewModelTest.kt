package com.signaldesk.android.incident.ui

import com.signaldesk.android.incident.data.ObservableIncidentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.IncidentTimelineEvent
import com.signaldesk.android.incident.Severity
import com.signaldesk.android.incident.data.IncidentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoadShowsRepositoryIncidents() = runTest(testDispatcher) {
        val incidents = listOf(
            incident(
                id = 101,
                status = IncidentStatus.OPEN
            )
        )

        val repository = TestIncidentRepository(
            incidents = incidents
        )

        val viewModel = IncidentListViewModel(
            repository = repository,
            ioDispatcher = testDispatcher
        )

        testScheduler.advanceUntilIdle()

        assertEquals(incidents, viewModel.uiState.value.incidents)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun selectingStatusFilterLoadsMatchingIncidents() =
        runTest(testDispatcher) {
            val openIncident = incident(
                id = 101,
                status = IncidentStatus.OPEN
            )

            val resolvedIncident = incident(
                id = 102,
                status = IncidentStatus.RESOLVED
            )

            val repository = TestIncidentRepository(
                incidents = listOf(
                    openIncident,
                    resolvedIncident
                )
            )

            val viewModel = IncidentListViewModel(
                repository = repository,
                ioDispatcher = testDispatcher
            )

            testScheduler.advanceUntilIdle()

            viewModel.setStatusFilter(
                IncidentStatus.RESOLVED
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                IncidentStatus.RESOLVED,
                viewModel.uiState.value.selectedStatus
            )

            assertEquals(
                listOf(resolvedIncident),
                viewModel.uiState.value.incidents
            )

            assertEquals(
                IncidentRequest(
                    status = IncidentStatus.RESOLVED,
                    severity = null
                ),
                repository.requests.last()
            )
        }

    @Test
    fun loadFailureShowsErrorAndStopsLoading() =
        runTest(testDispatcher) {
            val repository = TestIncidentRepository(
                incidents = emptyList(),
                loadError = IllegalStateException(
                    "Backend unavailable"
                )
            )

            val viewModel = IncidentListViewModel(
                repository = repository,
                ioDispatcher = testDispatcher
            )

            testScheduler.advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.incidents.isEmpty()
            )
            assertFalse(
                viewModel.uiState.value.isLoading
            )
            assertEquals(
                "Backend unavailable",
                viewModel.uiState.value.error
            )
        }

    @Test
    fun blankTitleDoesNotCreateIncident() =
        runTest(testDispatcher) {
            val repository = TestIncidentRepository(
                incidents = emptyList()
            )

            val viewModel = IncidentListViewModel(
                repository = repository,
                ioDispatcher = testDispatcher
            )

            testScheduler.advanceUntilIdle()

            viewModel.createIncident(
                title = "   ",
                description = "Valid description",
                severity = Severity.HIGH
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                "Title cannot be empty",
                viewModel.uiState.value.createIncidentError
            )
            assertEquals(
                0,
                repository.createRequests
            )
            assertFalse(
                viewModel.uiState.value.isCreatingIncident
            )
        }

    @Test
    fun latestStatusFilterOwnsFinalIncidentState() =
        runTest(testDispatcher) {
            val openIncident = incident(
                id = 201,
                status = IncidentStatus.OPEN
            )

            val resolvedIncident = incident(
                id = 202,
                status = IncidentStatus.RESOLVED
            )

            val repository = TestIncidentRepository(
                incidents = listOf(
                    openIncident,
                    resolvedIncident
                )
            )

            val viewModel = IncidentListViewModel(
                repository = repository,
                ioDispatcher = testDispatcher
            )

            testScheduler.advanceUntilIdle()

            viewModel.setStatusFilter(
                IncidentStatus.OPEN
            )

            viewModel.setStatusFilter(
                IncidentStatus.RESOLVED
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                IncidentStatus.RESOLVED,
                viewModel.uiState.value.selectedStatus
            )

            assertEquals(
                listOf(resolvedIncident),
                viewModel.uiState.value.incidents
            )

            assertEquals(
                IncidentRequest(
                    status = IncidentStatus.RESOLVED,
                    severity = null
                ),
                repository.requests.last()
            )

            assertEquals(
                null,
                viewModel.uiState.value.error
            )
        }

    @Test
    fun cancellationIsNotReportedAsLoadError() =
        runTest(testDispatcher) {
            val repository = TestIncidentRepository(
                incidents = emptyList(),
                loadError = CancellationException(
                    "Cancelled stale load"
                )
            )

            val viewModel = IncidentListViewModel(
                repository = repository,
                ioDispatcher = testDispatcher
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                null,
                viewModel.uiState.value.error
            )
        }

    @Test
    fun observableRepositoryUpdatesIncidentState() =
        runTest(testDispatcher) {
            val initialIncident = incident(
                id = 101,
                status = IncidentStatus.OPEN
            )

            val updatedIncident = incident(
                id = 102,
                status = IncidentStatus.INVESTIGATING
            )

            val repository = TestIncidentRepository(
                incidents = listOf(initialIncident)
            )

            val observableRepository =
                TestObservableIncidentRepository(
                    incidents = listOf(initialIncident)
                )

            val viewModel = IncidentListViewModel(
                repository = repository,
                observableRepository = observableRepository,
                ioDispatcher = testDispatcher
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                listOf(initialIncident),
                viewModel.uiState.value.incidents
            )

            observableRepository.emit(
                listOf(updatedIncident)
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                listOf(updatedIncident),
                viewModel.uiState.value.incidents
            )
        }

    @Test
    fun observableStateIsNotReplacedByRefreshReturnValue() =
        runTest(testDispatcher) {
            val remoteIncident = incident(
                id = 201,
                status = IncidentStatus.OPEN
            )

            val localIncident = incident(
                id = 202,
                status = IncidentStatus.INVESTIGATING
            )

            val repository = TestIncidentRepository(
                incidents = listOf(remoteIncident)
            )

            val observableRepository =
                TestObservableIncidentRepository(
                    incidents = listOf(localIncident)
                )

            val viewModel = IncidentListViewModel(
                repository = repository,
                observableRepository = observableRepository,
                ioDispatcher = testDispatcher
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                listOf(localIncident),
                viewModel.uiState.value.incidents
            )

            assertEquals(
                1,
                repository.requests.size
            )
        }

    @Test
    fun changingStatusFilterRestartsObservableIncidents() =
        runTest(testDispatcher) {
            val openIncident = incident(
                id = 301,
                status = IncidentStatus.OPEN
            )

            val resolvedIncident = incident(
                id = 302,
                status = IncidentStatus.RESOLVED
            )

            val repository = TestIncidentRepository(
                incidents = listOf(
                    openIncident,
                    resolvedIncident
                )
            )

            val observableRepository =
                TestObservableIncidentRepository(
                    incidents = listOf(
                        openIncident,
                        resolvedIncident
                    )
                )

            val viewModel = IncidentListViewModel(
                repository = repository,
                observableRepository = observableRepository,
                ioDispatcher = testDispatcher
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                listOf(
                    openIncident,
                    resolvedIncident
                ),
                viewModel.uiState.value.incidents
            )

            viewModel.setStatusFilter(
                IncidentStatus.RESOLVED
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                IncidentStatus.RESOLVED,
                viewModel.uiState.value.selectedStatus
            )

            assertEquals(
                listOf(resolvedIncident),
                viewModel.uiState.value.incidents
            )

            assertEquals(
                IncidentRequest(
                    status = IncidentStatus.RESOLVED,
                    severity = null
                ),
                repository.requests.last()
            )
        }

    @Test
    fun refreshFailureKeepsObservedIncidents() =
        runTest(testDispatcher) {
            val cachedIncident = incident(
                id = 401,
                status = IncidentStatus.OPEN
            )

            val repository = TestIncidentRepository(
                incidents = emptyList(),
                loadError = IllegalStateException(
                    "Backend unavailable"
                )
            )

            val observableRepository =
                TestObservableIncidentRepository(
                    incidents = listOf(cachedIncident)
                )

            val viewModel = IncidentListViewModel(
                repository = repository,
                observableRepository = observableRepository,
                ioDispatcher = testDispatcher
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                listOf(cachedIncident),
                viewModel.uiState.value.incidents
            )

            assertEquals(
                false,
                viewModel.uiState.value.isLoading
            )

            assertEquals(
                "Backend unavailable",
                viewModel.uiState.value.error
            )
        }

    @Test
    fun createDoesNotManuallyReplaceObservedIncidentState() =
        runTest(testDispatcher) {
            val observedIncident = incident(
                id = 41,
                status = IncidentStatus.OPEN,
                severity = Severity.HIGH
            )

            val createdIncident = incident(
                id = 99,
                status = IncidentStatus.OPEN,
                severity = Severity.CRITICAL
            )

            val repository = TestIncidentRepository(
                incidents = emptyList(),
                createdIncident = createdIncident
            )

            val observableRepository =
                TestObservableIncidentRepository(
                    incidents = listOf(observedIncident)
                )

            val viewModel = IncidentListViewModel(
                repository = repository,
                observableRepository = observableRepository,
                ioDispatcher = testDispatcher
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                listOf(41L),
                viewModel.uiState.value.incidents.map { it.id }
            )

            viewModel.createIncident(
                title = createdIncident.title,
                description = createdIncident.description,
                severity = createdIncident.severity
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                listOf(41L),
                viewModel.uiState.value.incidents.map { it.id }
            )

            assertEquals(
                99L,
                viewModel.uiState.value.createdIncidentId
            )

            assertFalse(
                viewModel.uiState.value.isCreatingIncident
            )

            assertEquals(
                1,
                repository.createRequests
            )
        }
}

private data class IncidentRequest(

    val status: IncidentStatus?,
    val severity: Severity?
)

private class TestObservableIncidentRepository(
    incidents: List<Incident>
) : ObservableIncidentRepository {

    private val incidentState =
        MutableStateFlow(incidents)

    override fun observeIncidents(
        status: IncidentStatus?,
        severity: Severity?
    ): Flow<List<Incident>> {
        return incidentState.map { incidents ->
            incidents.filter { incident ->
                (status == null || incident.status == status) &&
                    (severity == null || incident.severity == severity)
            }
        }
    }

    fun emit(incidents: List<Incident>) {
        incidentState.value = incidents
    }
}
private class TestIncidentRepository(
    private val incidents: List<Incident>,
    private val loadError: Exception? = null,
    private val createdIncident: Incident? = null
) : IncidentRepository {

    val requests = mutableListOf<IncidentRequest>()
    var createRequests: Int = 0
        private set

    override fun getIncidents(
        status: IncidentStatus?,
        severity: Severity?
    ): List<Incident> {
        requests += IncidentRequest(
            status = status,
            severity = severity
        )

        loadError?.let { throw it }

        return incidents.filter { incident ->
            val matchesStatus =
                status == null ||
                    incident.status == status

            val matchesSeverity =
                severity == null ||
                    incident.severity == severity

            matchesStatus && matchesSeverity
        }
    }

    override fun getIncident(
        incidentId: Long
    ): Incident {
        error("Not used by this test")
    }

    override fun createIncident(
        title: String,
        description: String,
        severity: Severity
    ): Incident {
        createRequests += 1

        return createdIncident
            ?: error("Unexpected createIncident call")
    }

    override fun getIncidentTimeline(
        incidentId: Long
    ): List<IncidentTimelineEvent> {
        error("Not used by this test")
    }

    override fun addIncidentNote(
        incidentId: Long,
        message: String
    ): IncidentTimelineEvent {
        error("Not used by this test")
    }

    override fun updateIncidentStatus(
        incidentId: Long,
        status: IncidentStatus
    ): Incident {
        error("Not used by this test")
    }
}

private fun incident(
    id: Long,
    status: IncidentStatus,
    severity: Severity = Severity.HIGH
): Incident {
    return Incident(
        id = id,
        title = "Incident $id",
        description = "Description $id",
        severity = severity,
        status = status
    )
}
