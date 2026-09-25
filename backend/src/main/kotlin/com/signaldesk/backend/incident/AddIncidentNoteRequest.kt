package com.signaldesk.backend.incident

import jakarta.validation.constraints.NotBlank

data class AddIncidentNoteRequest(

    @NotBlank(message = "Note must not be blank")
    val message: String
)
