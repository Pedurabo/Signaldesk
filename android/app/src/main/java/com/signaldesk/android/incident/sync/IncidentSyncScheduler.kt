package com.signaldesk.android.incident.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class IncidentSyncScheduler(
    private val context: Context
) : IncidentSyncScheduling {

    override fun schedule() {
        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val request =
            OneTimeWorkRequestBuilder<IncidentSyncWorker>()
                .setConstraints(constraints)
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
    }

    companion object {
        const val WORK_NAME =
            "incident-mutation-sync"
    }
}