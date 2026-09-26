package com.signaldesk.android.incident.sync

import androidx.work.ListenableWorker
import com.signaldesk.android.incident.data.PendingMutationSyncResult
import org.junit.Assert.assertEquals
import org.junit.Test

class IncidentSyncWorkerTest {

    @Test
    fun completedSyncProducesSuccess() {
        val result =
            pendingMutationSyncResultToWorkResult(
                PendingMutationSyncResult.COMPLETED
            )

        assertEquals(
            ListenableWorker.Result.success(),
            result
        )
    }

    @Test
    fun retryNeededProducesRetry() {
        val result =
            pendingMutationSyncResultToWorkResult(
                PendingMutationSyncResult.RETRY_NEEDED
            )

        assertEquals(
            ListenableWorker.Result.retry(),
            result
        )
    }
}
