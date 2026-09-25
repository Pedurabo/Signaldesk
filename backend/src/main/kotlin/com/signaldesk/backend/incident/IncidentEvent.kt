package com.signaldesk.backend.incident

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "incident_events")
class IncidentEvent(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var incidentId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: IncidentEventType,

    @Column(nullable = false)
    var message: String,

    @Column(nullable = false)
    var createdAt: Instant
)

enum class IncidentEventType {
    CREATED,
    STATUS_CHANGED,
    NOTE_ADDED
}
