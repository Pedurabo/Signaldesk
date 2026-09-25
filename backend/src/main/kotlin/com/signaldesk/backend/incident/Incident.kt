package com.signaldesk.backend.incident

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "incidents")
class Incident(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false)
    var description: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var severity: Severity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: IncidentStatus,

    @Column(nullable = false)
    var createdAt: Instant
)

enum class Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class IncidentStatus {
    OPEN,
    INVESTIGATING,
    RESOLVED
}
