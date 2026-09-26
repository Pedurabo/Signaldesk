package com.signaldesk.android.incident.sync

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.signaldesk.android.SignalDeskApplication
import com.signaldesk.android.incident.data.PendingMutationSyncResult

class IncidentSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val application =
            applicationContext as SignalDeskApplication

        return when (
            application.incidentRepository
                .syncPendingMutations()
        ) {
            PendingMutationSyncResult.COMPLETED ->
                Result.success()

            PendingMutationSyncResult.RETRY_NEEDED ->
                Result.retry()
        }
    }
}