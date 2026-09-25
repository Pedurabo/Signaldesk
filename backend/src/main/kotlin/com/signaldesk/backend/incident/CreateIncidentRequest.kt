package com.signaldesk.backend.incident

import jakarta.validation.constraints.NotBlank

data class CreateIncidentRequest(

    @NotBlank(message = "Title must not be blank")
    val title: String,

    @NotBlank(message = "Description must not be blank")
    val description: String,

    val severity: Severity
)
