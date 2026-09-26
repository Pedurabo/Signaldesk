package com.signaldesk.android.incident.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.signaldesk.android.SignalDeskApplication
import com.signaldesk.android.incident.data.PendingMutationSyncResult

internal fun pendingMutationSyncResultToWorkResult(
    result: PendingMutationSyncResult
): ListenableWorker.Result =
    when (result) {
        PendingMutationSyncResult.COMPLETED ->
            ListenableWorker.Result.success()

        PendingMutationSyncResult.RETRY_NEEDED ->
            ListenableWorker.Result.retry()
    }

class IncidentSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val application =
            applicationContext as SignalDeskApplication

        return pendingMutationSyncResultToWorkResult(
            application.incidentRepository
                .syncPendingMutations()
        )
    }
}
