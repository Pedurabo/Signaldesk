package com.signaldesk.android.incident.ui

import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.IncidentTimelineEvent
import com.signaldesk.android.incident.Severity
import com.signaldesk.android.incident.data.IncidentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentDetailViewModelTest {

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
    fun loadIncidentShowsRepositoryIncident() =
        runTest(testDispatcher) {
            val incident = Incident(
                id = 301,
                title = "Database latency",
                description = "Queries are responding slowly",
                severity = Severity.HIGH,
                status = IncidentStatus.INVESTIGATING
            )

            val repository = TestIncidentDetailRepository(
                incident = incident
            )

            val viewModel = IncidentDetailViewModel(
                repository = repository,
                ioDispatcher = testDispatcher
            )

            viewModel.loadIncident(
                incidentId = incident.id
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                incident,
                viewModel.uiState.value.incident
            )

            assertFalse(
                viewModel.uiState.value.isLoading
            )

            assertEquals(
                null,
                viewModel.uiState.value.error
            )

            assertEquals(
                incident.id,
                repository.requestedIncidentId
            )
        }
    @Test
    fun loadIncidentFailureShowsError() =
        runTest(testDispatcher) {
            val incident = Incident(
                id = 302,
                title = "Placeholder",
                description = "Placeholder",
                severity = Severity.LOW,
                status = IncidentStatus.OPEN
            )

            val repository = TestIncidentDetailRepository(
                incident = incident,
                incidentLoadError = IllegalStateException(
                    "Incident unavailable"
                )
            )

            val viewModel = IncidentDetailViewModel(
                repository = repository,
                ioDispatcher = testDispatcher
            )

            viewModel.loadIncident(
                incidentId = incident.id
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                null,
                viewModel.uiState.value.incident
            )

            assertFalse(
                viewModel.uiState.value.isLoading
            )

            assertEquals(
                "Incident unavailable",
                viewModel.uiState.value.error
            )

            assertEquals(
                incident.id,
                repository.requestedIncidentId
            )
        }
    @Test
    fun loadTimelineShowsRepositoryEvents() =
        runTest(testDispatcher) {
            val incident = Incident(
                id = 303,
                title = "API errors",
                description = "Requests are failing",
                severity = Severity.CRITICAL,
                status = IncidentStatus.INVESTIGATING
            )

            val timeline = listOf(
                IncidentTimelineEvent(
                    id = 401,
                    type = com.signaldesk.android.incident.IncidentTimelineEventType.CREATED,
                    message = "Incident created",
                    createdAt = "2026-09-25T12:00:00Z"
                ),
                IncidentTimelineEvent(
                    id = 402,
                    type = com.signaldesk.android.incident.IncidentTimelineEventType.STATUS_CHANGED,
                    message = "Status changed to INVESTIGATING",
                    createdAt = "2026-09-25T12:05:00Z"
                )
            )

            val repository = TestIncidentDetailRepository(
                incident = incident,
                timeline = timeline
            )

            val viewModel = IncidentDetailViewModel(
                repository = repository,
                ioDispatcher = testDispatcher
            )

            viewModel.loadIncidentTimeline(
                incidentId = incident.id
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                timeline,
                viewModel.uiState.value.timeline
            )

            assertFalse(
                viewModel.uiState.value.isTimelineLoading
            )

            assertEquals(
                null,
                viewModel.uiState.value.timelineError
            )

            assertEquals(
                incident.id,
                repository.requestedTimelineIncidentId
            )
        }
    @Test
    fun statusUpdateUpdatesIncidentAndReloadsTimeline() =
        runTest(testDispatcher) {
            val originalIncident = Incident(
                id = 304,
                title = "Worker outage",
                description = "Background workers stopped",
                severity = Severity.HIGH,
                status = IncidentStatus.OPEN
            )

            val updatedIncident = originalIncident.copy(
                status = IncidentStatus.INVESTIGATING
            )

            val refreshedTimeline = listOf(
                IncidentTimelineEvent(
                    id = 403,
                    type = com.signaldesk.android.incident.IncidentTimelineEventType.STATUS_CHANGED,
                    message = "Status changed to INVESTIGATING",
                    createdAt = "2026-09-25T12:10:00Z"
                )
            )

            val repository = TestIncidentDetailRepository(
                incident = originalIncident,
                updatedIncident = updatedIncident,
                timeline = refreshedTimeline
            )

            val viewModel = IncidentDetailViewModel(
                repository = repository,
                ioDispatcher = testDispatcher
            )

            viewModel.updateIncidentStatus(
                incidentId = originalIncident.id,
                status = IncidentStatus.INVESTIGATING
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                updatedIncident,
                viewModel.uiState.value.incident
            )

            assertFalse(
                viewModel.uiState.value.isUpdating
            )

            assertEquals(
                null,
                viewModel.uiState.value.error
            )

            assertEquals(
                originalIncident.id,
                repository.updatedIncidentId
            )

            assertEquals(
                IncidentStatus.INVESTIGATING,
                repository.requestedStatus
            )

            assertEquals(
                originalIncident.id,
                repository.requestedTimelineIncidentId
            )

            assertEquals(
                refreshedTimeline,
                viewModel.uiState.value.timeline
            )
        }
    @Test
    fun blankNoteDoesNotCallRepository() =
        runTest(testDispatcher) {
            val incident = Incident(
                id = 305,
                title = "Queue delay",
                description = "Messages are delayed",
                severity = Severity.MEDIUM,
                status = IncidentStatus.OPEN
            )

            val repository = TestIncidentDetailRepository(
                incident = incident
            )

            val viewModel = IncidentDetailViewModel(
                repository = repository,
                ioDispatcher = testDispatcher
            )

            viewModel.addIncidentNote(
                incidentId = incident.id,
                message = "   "
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                "Note cannot be empty",
                viewModel.uiState.value.noteError
            )

            assertFalse(
                viewModel.uiState.value.isAddingNote
            )

            assertEquals(
                0,
                repository.addNoteRequestCount
            )

            assertEquals(
                null,
                repository.addedNoteIncidentId
            )

            assertEquals(
                null,
                repository.addedNoteMessage
            )
        }

    @Test
    fun validNoteAddsTrimmedMessageAndReloadsTimeline() =
        runTest(testDispatcher) {
            val incident = Incident(
                id = 306,
                title = "Cache misses",
                description = "Cache hit rate dropped",
                severity = Severity.MEDIUM,
                status = IncidentStatus.INVESTIGATING
            )

            val refreshedTimeline = listOf(
                IncidentTimelineEvent(
                    id = 404,
                    type = com.signaldesk.android.incident.IncidentTimelineEventType.NOTE_ADDED,
                    message = "Investigating cache nodes",
                    createdAt = "2026-09-25T12:15:00Z"
                )
            )

            val repository = TestIncidentDetailRepository(
                incident = incident,
                timeline = refreshedTimeline
            )

            val viewModel = IncidentDetailViewModel(
                repository = repository,
                ioDispatcher = testDispatcher
            )

            viewModel.addIncidentNote(
                incidentId = incident.id,
                message = "  Investigating cache nodes  "
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                1,
                repository.addNoteRequestCount
            )

            assertEquals(
                incident.id,
                repository.addedNoteIncidentId
            )

            assertEquals(
                "Investigating cache nodes",
                repository.addedNoteMessage
            )

            assertFalse(
                viewModel.uiState.value.isAddingNote
            )

            assertEquals(
                null,
                viewModel.uiState.value.noteError
            )

            assertEquals(
                incident.id,
                repository.requestedTimelineIncidentId
            )

            assertEquals(
                refreshedTimeline,
                viewModel.uiState.value.timeline
            )
        }
}

private class TestIncidentDetailRepository(
    private val incident: Incident,
    private val incidentLoadError: Exception? = null,
    private val timeline: List<IncidentTimelineEvent> = emptyList(),
    private val updatedIncident: Incident = incident
) : IncidentRepository {

    var requestedIncidentId: Long? = null
        private set

    var requestedTimelineIncidentId: Long? = null
        private set

    var updatedIncidentId: Long? = null
        private set

    var requestedStatus: IncidentStatus? = null
        private set

    var addNoteRequestCount: Int = 0
        private set

    var addedNoteIncidentId: Long? = null
        private set

    var addedNoteMessage: String? = null
        private set

    override fun getIncidents(
        status: IncidentStatus?,
        severity: Severity?
    ): List<Incident> {
        error("Not used by this test")
    }

    override fun getIncident(
        incidentId: Long
    ): Incident {
        requestedIncidentId = incidentId
        incidentLoadError?.let { throw it }
        return incident
    }

    override fun createIncident(
        title: String,
        description: String,
        severity: Severity
    ): Incident {
        error("Not used by this test")
    }

    override fun getIncidentTimeline(
        incidentId: Long
    ): List<IncidentTimelineEvent> {
        requestedTimelineIncidentId = incidentId
        return timeline
    }

    override fun addIncidentNote(
        incidentId: Long,
        message: String
    ): IncidentTimelineEvent {
        addNoteRequestCount += 1
        addedNoteIncidentId = incidentId
        addedNoteMessage = message

        return IncidentTimelineEvent(
            id = 999,
            type = com.signaldesk.android.incident.IncidentTimelineEventType.NOTE_ADDED,
            message = message,
            createdAt = "2026-09-25T12:15:00Z"
        )
    }

    override fun updateIncidentStatus(
        incidentId: Long,
        status: IncidentStatus
    ): Incident {
        updatedIncidentId = incidentId
        requestedStatus = status
        return updatedIncident
    }
}
