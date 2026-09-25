package com.signaldesk.backend.incident

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/incidents")
class IncidentController(
    private val incidentService: IncidentService
) {

    @GetMapping
    fun getIncidents(
        @RequestParam(required = false) status: IncidentStatus?,
        @RequestParam(required = false) severity: Severity?
    ): List<IncidentResponse> {
        return incidentService.getIncidents(status, severity)
    }

    @GetMapping("/{id}")
    fun getIncident(
        @PathVariable id: Long
    ): ResponseEntity<IncidentResponse> {

        val incident = incidentService.getIncident(id)

        return if (incident != null) {
            ResponseEntity.ok(incident)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createIncident(
        @Valid @RequestBody request: CreateIncidentRequest
    ): IncidentResponse {
        return incidentService.createIncident(request)
    }

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestBody request: UpdateIncidentStatusRequest
    ): ResponseEntity<IncidentResponse> {

        val incident = incidentService.updateStatus(id, request)

        return if (incident != null) {
            ResponseEntity.ok(incident)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    fun addNote(
        @PathVariable id: Long,
        @Valid @RequestBody request: AddIncidentNoteRequest
    ): ResponseEntity<IncidentEventResponse> {

        val event = incidentService.addNote(id, request)

        return if (event != null) {
            ResponseEntity.status(HttpStatus.CREATED).body(event)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    @GetMapping("/{id}/timeline")
    fun getTimeline(
        @PathVariable id: Long
    ): ResponseEntity<List<IncidentEventResponse>> {

        val timeline = incidentService.getTimeline(id)

        return if (timeline != null) {
            ResponseEntity.ok(timeline)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    @DeleteMapping("/{id}")
    fun deleteIncident(
        @PathVariable id: Long
    ): ResponseEntity<Void> {

        val deleted = incidentService.deleteIncident(id)

        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}




