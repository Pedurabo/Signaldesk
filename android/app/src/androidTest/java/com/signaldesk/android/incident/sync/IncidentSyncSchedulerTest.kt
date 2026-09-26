package com.signaldesk.android.incident.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class IncidentSyncSchedulerTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context =
            ApplicationProvider.getApplicationContext()

        val configuration =
            Configuration.Builder()
                .setExecutor(SynchronousExecutor())
                .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            configuration
        )

        workManager =
            WorkManager.getInstance(context)

        workManager.cancelAllWork().result.get(
            10,
            TimeUnit.SECONDS
        )
    }

    @Test
    fun scheduleEnqueuesUniqueNetworkConstrainedWork() {
        IncidentSyncScheduler(context).schedule()

        val work = getScheduledWork()

        assertEquals(
            NetworkType.CONNECTED,
            work.constraints.requiredNetworkType
        )

        assertNotNull(work.id)

        assertEquals(
            WorkInfo.State.ENQUEUED,
            work.state
        )
    }

    @Test
    fun scheduledWorkWaitsForNetworkConstraint() {
        IncidentSyncScheduler(context).schedule()

        val work = getScheduledWork()

        assertEquals(
            WorkInfo.State.ENQUEUED,
            work.state
        )

        val testDriver =
            WorkManagerTestInitHelper.getTestDriver(
                context
            )

        requireNotNull(testDriver)

        testDriver.setAllConstraintsMet(work.id)

        val afterConstraints =
            workManager
                .getWorkInfoById(work.id)
                .get(
                    10,
                    TimeUnit.SECONDS
                )

        assertEquals(
            WorkInfo.State.SUCCEEDED,
            requireNotNull(afterConstraints).state
        )
    }

    private fun getScheduledWork(): WorkInfo =
        workManager
            .getWorkInfosForUniqueWork(
                IncidentSyncScheduler.WORK_NAME
            )
            .get(
                10,
                TimeUnit.SECONDS
            )
            .single()
}
