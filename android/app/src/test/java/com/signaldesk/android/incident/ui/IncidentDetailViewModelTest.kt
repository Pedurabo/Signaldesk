package com.signaldesk.android.incident.ui
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import kotlinx.coroutines.test.advanceUntilIdle
import com.signaldesk.android.incident.IncidentTimelineEventType

import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.IncidentTimelineEvent
import com.signaldesk.android.incident.Severity
import com.signaldesk.android.incident.data.IncidentRepository
import com.signaldesk.android.incident.data.ObservableIncidentDetailRepository
import com.signaldesk.android.incident.data.ObservableIncidentTimelineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun timelineRefreshFailureKeepsObservedTimeline() =
        runTest(testDispatcher) {
            val incident = Incident(
                id = 703,
                title = "Cached timeline",
                description = "Timeline should survive refresh failure",
                severity = Severity.HIGH,
                status = IncidentStatus.INVESTIGATING
            )

            val cachedTimeline = listOf(
                IncidentTimelineEvent(
                    id = 903,
                    type = com.signaldesk.android.incident.IncidentTimelineEventType.CREATED,
                    message = "Cached event",
                    createdAt = "2026-09-26T00:20:00Z"
                )
            )

            val repository =
                TestIncidentDetailRepository(
                    incident = incident,
                    timelineLoadError =
                        IllegalStateException(
                            "Timeline unavailable"
                        )
                )

            val observableTimelineRepository =
                TestObservableIncidentTimelineRepository(
                    timeline = cachedTimeline
                )

            val viewModel = IncidentDetailViewModel(
                repository = repository,
                observableTimelineRepository =
                    observableTimelineRepository,
                ioDispatcher = testDispatcher
            )

            viewModel.loadIncidentTimeline(
                incidentId = incident.id
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                cachedTimeline,
                viewModel.uiState.value.timeline
            )

            assertFalse(
                viewModel.uiState.value.isTimelineLoading
            )

            assertEquals(
                "Timeline unavailable",
                viewModel.uiState.value.timelineError
            )
        }

    @Test
    fun observableTimelineUpdatesTimelineState() =
        runTest(testDispatcher) {
            val incident = Incident(
                id = 704,
                title = "Live timeline",
                description = "Timeline follows Room emissions",
                severity = Severity.MEDIUM,
                status = IncidentStatus.OPEN
            )

            val initialTimeline = listOf(
                IncidentTimelineEvent(
                    id = 904,
                    type = com.signaldesk.android.incident.IncidentTimelineEventType.CREATED,
                    message = "Initial event",
                    createdAt = "2026-09-26T00:30:00Z"
                )
            )

            val updatedTimeline =
                initialTimeline +
                    IncidentTimelineEvent(
                        id = 905,
                        type = com.signaldesk.android.incident.IncidentTimelineEventType.NOTE_ADDED,
                        message = "Later Room event",
                        createdAt = "2026-09-26T00:31:00Z"
                    )

            val repository =
                TestIncidentDetailRepository(
                    incident = incident,
                    timeline = initialTimeline
                )

            val observableTimelineRepository =
                TestObservableIncidentTimelineRepository(
                    timeline = initialTimeline
                )

            val viewModel = IncidentDetailViewModel(
                repository = repository,
                observableTimelineRepository =
                    observableTimelineRepository,
                ioDispatcher = testDispatcher
            )

            viewModel.loadIncidentTimeline(
                incidentId = incident.id
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                initialTimeline,
                viewModel.uiState.value.timeline
            )

            observableTimelineRepository.emit(
                updatedTimeline
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                updatedTimeline,
                viewModel.uiState.value.timeline
            )
        }

    @Test
    fun observableTimelineOwnsTimelineState() =
        runTest(testDispatcher) {
            val incident = Incident(
                id = 702,
                title = "Timeline ownership",
                description = "Testing Room timeline ownership",
                severity = Severity.HIGH,
                status = IncidentStatus.INVESTIGATING
            )

            val refreshTimeline = listOf(
                IncidentTimelineEvent(
                    id = 901,
                    type = com.signaldesk.android.incident.IncidentTimelineEventType.STATUS_CHANGED,
                    message = "Remote refresh result",
                    createdAt = "2026-09-26T00:10:00Z"
                )
            )

            val observedTimeline = listOf(
                IncidentTimelineEvent(
                    id = 902,
                    type = com.signaldesk.android.incident.IncidentTimelineEventType.NOTE_ADDED,
                    message = "Room owns this state",
                    createdAt = "2026-09-26T00:11:00Z"
                )
            )

            val repository =
                TestIncidentDetailRepository(
                    incident = incident,
                    timeline = refreshTimeline
                )

            val observableTimelineRepository =
                TestObservableIncidentTimelineRepository(
                    timeline = observedTimeline
                )

            val viewModel = IncidentDetailViewModel(
                repository = repository,
                observableTimelineRepository =
                    observableTimelineRepository,
                ioDispatcher = testDispatcher
            )

            viewModel.loadIncidentTimeline(
                incidentId = incident.id
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                observedTimeline,
                viewModel.uiState.value.timeline
            )

            assertFalse(
                viewModel.uiState.value.isTimelineLoading
            )

            assertEquals(
                null,
                viewModel.uiState.value.timelineError
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
    fun observableTimelineNoteDoesNotReloadTimeline() =
        runTest(testDispatcher) {
            val incident = Incident(
                id = 705,
                title = "Note Flow",
                description = "Room should publish the new note",
                severity = Severity.MEDIUM,
                status = IncidentStatus.INVESTIGATING
            )

            val cachedTimeline = listOf(
                IncidentTimelineEvent(
                    id = 906,
                    type = com.signaldesk.android.incident.IncidentTimelineEventType.CREATED,
                    message = "Incident created",
                    createdAt = "2026-09-26T00:40:00Z"
                )
            )

            val repository =
                TestIncidentDetailRepository(
                    incident = incident
                )

            val observableTimelineRepository =
                TestObservableIncidentTimelineRepository(
                    timeline = cachedTimeline
                )

            val viewModel = IncidentDetailViewModel(
                repository = repository,
                observableTimelineRepository =
                    observableTimelineRepository,
                ioDispatcher = testDispatcher
            )

            viewModel.loadIncidentTimeline(
                incidentId = incident.id
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                1,
                repository.timelineRequestCount
            )

            viewModel.addIncidentNote(
                incidentId = incident.id,
                message = "  Check worker logs  "
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                1,
                repository.addNoteRequestCount
            )

            assertEquals(
                "Check worker logs",
                repository.addedNoteMessage
            )

            assertEquals(
                1,
                repository.timelineRequestCount
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

    @Test
    fun statusUpdateDoesNotManuallyReplaceObservedIncident() =
        runTest(testDispatcher) {
            val observedIncident = Incident(
                id = 701,
                title = "Observed incident",
                description = "Owned by observable repository",
                severity = Severity.HIGH,
                status = IncidentStatus.OPEN
            )

            val mutationResult = observedIncident.copy(
                status = IncidentStatus.RESOLVED
            )

            val repository = TestIncidentDetailRepository(
                incident = observedIncident,
                updatedIncident = mutationResult
            )

            val observableRepository =
                TestObservableIncidentDetailRepository(
                    incident = observedIncident
                )

            val viewModel = IncidentDetailViewModel(
                repository = repository,
                observableRepository = observableRepository,
                ioDispatcher = testDispatcher
            )

            viewModel.loadIncident(
                incidentId = observedIncident.id
            )

            testScheduler.advanceUntilIdle()

            viewModel.updateIncidentStatus(
                incidentId = observedIncident.id,
                status = IncidentStatus.RESOLVED
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                observedIncident,
                viewModel.uiState.value.incident
            )

            assertEquals(
                observedIncident.id,
                repository.updatedIncidentId
            )

            assertEquals(
                IncidentStatus.RESOLVED,
                repository.requestedStatus
            )

            assertFalse(
                viewModel.uiState.value.isUpdating
            )
        }

    @Test
    fun observableIncidentUpdatesDetailState() =
        runTest(testDispatcher) {
            val initialIncident = Incident(
                id = 601,
                title = "Initial cached incident",
                description = "First Room emission",
                severity = Severity.HIGH,
                status = IncidentStatus.OPEN
            )

            val updatedIncident = initialIncident.copy(
                title = "Updated cached incident",
                status = IncidentStatus.INVESTIGATING
            )

            val repository = TestIncidentDetailRepository(
                incident = initialIncident
            )

            val observableRepository =
                TestObservableIncidentDetailRepository(
                    incident = initialIncident
                )

            val viewModel = IncidentDetailViewModel(
                repository = repository,
                observableRepository = observableRepository,
                ioDispatcher = testDispatcher
            )

            viewModel.loadIncident(
                incidentId = initialIncident.id
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                initialIncident,
                viewModel.uiState.value.incident
            )

            observableRepository.emit(
                updatedIncident
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                updatedIncident,
                viewModel.uiState.value.incident
            )
        }

    @Test
    fun refreshFailureKeepsObservedIncident() =
        runTest(testDispatcher) {
            val observedIncident = Incident(
                id = 501,
                title = "Cached incident",
                description = "Available from Room",
                severity = Severity.HIGH,
                status = IncidentStatus.OPEN
            )

            val repository = TestIncidentDetailRepository(
                incident = observedIncident,
                incidentLoadError =
                    IllegalStateException("Incident unavailable")
            )

            val observableRepository =
                TestObservableIncidentDetailRepository(
                    incident = observedIncident
                )

            val viewModel = IncidentDetailViewModel(
                repository = repository,
                observableRepository = observableRepository,
                ioDispatcher = testDispatcher
            )

            viewModel.loadIncident(
                incidentId = observedIncident.id
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                observedIncident,
                viewModel.uiState.value.incident
            )

            assertFalse(
                viewModel.uiState.value.isLoading
            )

            assertEquals(
                "Incident unavailable",
                viewModel.uiState.value.error
            )
        }

    @Test
    fun observableIncidentOwnsDetailState() =
        runTest(testDispatcher) {
            val refreshIncident = Incident(
                id = 301,
                title = "Remote refresh result",
                description = "Returned by getIncident",
                severity = Severity.HIGH,
                status = IncidentStatus.OPEN
            )

            val observedIncident = Incident(
                id = 301,
                title = "Room observed incident",
                description = "Emitted by observable repository",
                severity = Severity.CRITICAL,
                status = IncidentStatus.INVESTIGATING
            )

            val repository = TestIncidentDetailRepository(
                incident = refreshIncident
            )

            val observableRepository =
                TestObservableIncidentDetailRepository(
                    incident = observedIncident
                )

            val viewModel = IncidentDetailViewModel(
                repository = repository,
                observableRepository = observableRepository,
                ioDispatcher = testDispatcher
            )

            viewModel.loadIncident(
                incidentId = refreshIncident.id
            )

            testScheduler.advanceUntilIdle()

            assertEquals(
                observedIncident,
                viewModel.uiState.value.incident
            )
        }

    @Test
    fun clearIncidentStopsObservedStateFromReappearing() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val initialIncident = Incident(
            id = 801,
            title = "Initial incident",
            description = "Initial description",
            severity = Severity.HIGH,
            status = IncidentStatus.OPEN
        )

        val laterIncident = Incident(
            id = 801,
            title = "Should not reappear",
            description = "Collector should be cancelled",
            severity = Severity.CRITICAL,
            status = IncidentStatus.INVESTIGATING
        )

        val initialTimeline = listOf(
            IncidentTimelineEvent(
                id = 1001,
                type = IncidentTimelineEventType.CREATED,
                message = "Incident created",
                createdAt = "2026-09-26T00:00:00Z"
            )
        )

        val laterTimeline = listOf(
            IncidentTimelineEvent(
                id = 1002,
                type = IncidentTimelineEventType.NOTE_ADDED,
                message = "Should not reappear",
                createdAt = "2026-09-26T00:01:00Z"
            )
        )

        val repository = TestIncidentDetailRepository(
            incident = initialIncident,
            timeline = initialTimeline
        )

        val observableRepository =
            TestObservableIncidentDetailRepository(
                incident = initialIncident
            )

        val observableTimelineRepository =
            TestObservableIncidentTimelineRepository(
                timeline = initialTimeline
            )

        val viewModel = IncidentDetailViewModel(
            repository = repository,
            observableRepository = observableRepository,
            observableTimelineRepository =
                observableTimelineRepository,
            ioDispatcher = testDispatcher
        )

        viewModel.loadIncident(801)
        viewModel.loadIncidentTimeline(801)
        advanceUntilIdle()

        viewModel.clearIncident()

        assertNull(viewModel.uiState.value.incident)
        assertTrue(viewModel.uiState.value.timeline.isEmpty())

        observableRepository.emit(laterIncident)
        observableTimelineRepository.emit(laterTimeline)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.incident)
        assertTrue(viewModel.uiState.value.timeline.isEmpty())
    }

    @Test
    fun loadingIncidentAfterClearStartsFreshObservers() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val incidentA = Incident(
            id = 801,
            title = "Incident A",
            description = "First incident",
            severity = Severity.HIGH,
            status = IncidentStatus.OPEN
        )

        val incidentB = Incident(
            id = 802,
            title = "Incident B",
            description = "Second incident",
            severity = Severity.CRITICAL,
            status = IncidentStatus.INVESTIGATING
        )

        val updatedIncidentB = incidentB.copy(
            title = "Incident B updated",
            status = IncidentStatus.RESOLVED
        )

        val timelineA = listOf(
            IncidentTimelineEvent(
                id = 1101,
                type = IncidentTimelineEventType.CREATED,
                message = "Incident A created",
                createdAt = "2026-09-26T01:00:00Z"
            )
        )

        val timelineB = listOf(
            IncidentTimelineEvent(
                id = 1201,
                type = IncidentTimelineEventType.CREATED,
                message = "Incident B created",
                createdAt = "2026-09-26T02:00:00Z"
            )
        )

        val updatedTimelineB = timelineB + IncidentTimelineEvent(
            id = 1202,
            type = IncidentTimelineEventType.NOTE_ADDED,
            message = "Fresh observer received this",
            createdAt = "2026-09-26T02:01:00Z"
        )

        val repository = TestIncidentDetailRepository(
            incident = incidentA,
            timeline = timelineA
        )

        val observableRepository =
            TestObservableIncidentDetailRepository(
                incident = incidentA
            )

        val observableTimelineRepository =
            TestObservableIncidentTimelineRepository(
                timeline = timelineA
            )

        val viewModel = IncidentDetailViewModel(
            repository = repository,
            observableRepository = observableRepository,
            observableTimelineRepository =
                observableTimelineRepository,
            ioDispatcher = testDispatcher
        )

        viewModel.loadIncident(incidentA.id)
        viewModel.loadIncidentTimeline(incidentA.id)
        advanceUntilIdle()

        viewModel.clearIncident()

        observableRepository.emit(incidentB)
        observableTimelineRepository.emit(timelineB)

        viewModel.loadIncident(incidentB.id)
        viewModel.loadIncidentTimeline(incidentB.id)
        advanceUntilIdle()

        observableRepository.emit(updatedIncidentB)
        observableTimelineRepository.emit(updatedTimelineB)
        advanceUntilIdle()

        assertEquals(
            updatedIncidentB,
            viewModel.uiState.value.incident
        )

        assertEquals(
            updatedTimelineB,
            viewModel.uiState.value.timeline
        )
    }
}

private class TestObservableIncidentDetailRepository(

    incident: Incident?
) : ObservableIncidentDetailRepository {

    private val incidentState =
        MutableStateFlow(incident)

    override fun observeIncident(
        incidentId: Long
    ): Flow<Incident?> {
        return incidentState
    }

    fun emit(
        incident: Incident?
    ) {
        incidentState.value = incident
    }
}
private class TestObservableIncidentTimelineRepository(
    timeline: List<IncidentTimelineEvent>
) : ObservableIncidentTimelineRepository {

    private val timelineState =
        MutableStateFlow(timeline)

    override fun observeIncidentTimeline(
        incidentId: Long
    ): Flow<List<IncidentTimelineEvent>> {
        return timelineState
    }

    fun emit(
        timeline: List<IncidentTimelineEvent>
    ) {
        timelineState.value = timeline
    }
}

private class TestIncidentDetailRepository(
    private val incident: Incident,
    private val incidentLoadError: Exception? = null,
    private val timeline: List<IncidentTimelineEvent> = emptyList(),
    private val timelineLoadError: Exception? = null,
    private val updatedIncident: Incident = incident
) : IncidentRepository {

    var requestedIncidentId: Long? = null
        private set

    var requestedTimelineIncidentId: Long? = null
        private set

    var timelineRequestCount: Int = 0
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
        timelineRequestCount += 1
        requestedTimelineIncidentId = incidentId
        timelineLoadError?.let { throw it }
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
