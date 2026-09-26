package com.signaldesk.android.incident.sync

fun interface IncidentSyncScheduling {
    fun schedule()
}

object NoOpIncidentSyncScheduler :
    IncidentSyncScheduling {

    override fun schedule() = Unit
}