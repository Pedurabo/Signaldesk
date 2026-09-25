package com.signaldesk.android.incident.ui

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
}

private data class IncidentRequest(

    val status: IncidentStatus?,
    val severity: Severity?
)

private class TestIncidentRepository(
    private val incidents: List<Incident>,
    private val loadError: Exception? = null
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
        error("Unexpected createIncident call")
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
