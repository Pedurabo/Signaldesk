package com.signaldesk.android.incident.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {

    @Query(
        """
        SELECT * FROM incidents
        WHERE (:status IS NULL OR status = :status)
          AND (:severity IS NULL OR severity = :severity)
        ORDER BY id DESC
        """
    )
    fun getIncidents(
        status: String? = null,
        severity: String? = null
    ): List<IncidentEntity>

    @Query(
        """
        SELECT * FROM incidents
        WHERE (:status IS NULL OR status = :status)
          AND (:severity IS NULL OR severity = :severity)
        ORDER BY id DESC
        """
    )
    fun observeIncidents(
        status: String? = null,
        severity: String? = null
    ): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE id = :incidentId")
    fun getIncident(
        incidentId: Long
    ): IncidentEntity?

    @Query("SELECT * FROM incidents WHERE id = :incidentId")
    fun observeIncident(
        incidentId: Long
    ): Flow<IncidentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertIncidents(
        incidents: List<IncidentEntity>
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertIncident(
        incident: IncidentEntity
    )

    @Query("DELETE FROM incidents")
    fun deleteAllIncidents()

    @Transaction
    fun replaceIncidents(
        incidents: List<IncidentEntity>
    ) {
        deleteAllIncidents()
        upsertIncidents(incidents)
    }

    @Query(
        """
        SELECT * FROM incident_timeline_events
        WHERE incidentId = :incidentId
        ORDER BY id ASC
        """
    )
    fun getTimeline(
        incidentId: Long
    ): List<IncidentTimelineEventEntity>

    @Query(
        """
        SELECT * FROM incident_timeline_events
        WHERE incidentId = :incidentId
        ORDER BY id ASC
        """
    )
    fun observeTimeline(
        incidentId: Long
    ): Flow<List<IncidentTimelineEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertTimelineEvents(
        events: List<IncidentTimelineEventEntity>
    )

    @Query(
        """
        DELETE FROM incident_timeline_events
        WHERE incidentId = :incidentId
        """
    )
    fun deleteTimelineForIncident(
        incidentId: Long
    )

    @Transaction
    fun replaceTimelineForIncident(
        incidentId: Long,
        events: List<IncidentTimelineEventEntity>
    ) {
        deleteTimelineForIncident(incidentId)
        upsertTimelineEvents(events)
    }

    @Transaction
    fun queueStatusMutation(
        incident: IncidentEntity,
        mutation: PendingIncidentMutationEntity
    ) {
        upsertIncident(incident)
        insertPendingMutation(mutation)
    }

    @Insert
    fun insertPendingMutation(
        mutation: PendingIncidentMutationEntity
    ): Long

    @Query(
        """
        SELECT * FROM pending_incident_mutations
        ORDER BY id ASC
        """
    )
    fun getPendingMutations(): List<PendingIncidentMutationEntity>

    @Query(
        "DELETE FROM pending_incident_mutations WHERE id = :mutationId"
    )
    fun deletePendingMutation(
        mutationId: Long
    )
}
