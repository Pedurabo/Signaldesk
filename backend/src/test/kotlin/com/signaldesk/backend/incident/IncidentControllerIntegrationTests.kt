package com.signaldesk.backend.incident

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentControllerIntegrationTests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var incidentEventRepository: IncidentEventRepository

    @Test
    fun `creating incident returns created incident with open status`() {

        val requestBody = """
            {
                "title": "Automated test incident",
                "description": "Created by integration test",
                "severity": "HIGH"
            }
        """.trimIndent()

        mockMvc.post("/api/incidents") {
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.id") { exists() }
                jsonPath("$.title") { value("Automated test incident") }
                jsonPath("$.description") { value("Created by integration test") }
                jsonPath("$.severity") { value("HIGH") }
                jsonPath("$.status") { value("OPEN") }
                jsonPath("$.createdAt") { exists() }
            }
    }

    @Test
    fun `creating incident also creates created timeline event`() {

        val requestBody = """
            {
                "title": "Timeline test incident",
                "description": "Testing automatic timeline creation",
                "severity": "MEDIUM"
            }
        """.trimIndent()

        val result = mockMvc.post("/api/incidents") {
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
        }
            .andExpect {
                status { isCreated() }
            }
            .andReturn()

        val responseBody = result.response.contentAsString

        val id = Regex(""""id"\s*:\s*(\d+)""")
            .find(responseBody)
            ?.groupValues
            ?.get(1)
            ?.toLong()
            ?: error("Incident ID was not found in response")

        mockMvc.get("/api/incidents/$id/timeline")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0].type") { value("CREATED") }
                jsonPath("$[0].message") { value("Incident created") }
                jsonPath("$[0].createdAt") { exists() }
            }
    }
    @Test
    fun `changing incident status creates status changed timeline event`() {

        val createBody = """
            {
                "title": "Status transition test",
                "description": "Testing timeline status auditing",
                "severity": "HIGH"
            }
        """.trimIndent()

        val createResult = mockMvc.post("/api/incidents") {
            contentType = MediaType.APPLICATION_JSON
            content = createBody
        }
            .andExpect {
                status { isCreated() }
            }
            .andReturn()

        val incidentId = Regex(""""id"\s*:\s*(\d+)""")
            .find(createResult.response.contentAsString)
            ?.groupValues
            ?.get(1)
            ?.toLong()
            ?: error("Incident ID was not found in response")

        val statusBody = """
            {
                "status": "INVESTIGATING"
            }
        """.trimIndent()

        mockMvc.patch("/api/incidents/$incidentId/status") {
            contentType = MediaType.APPLICATION_JSON
            content = statusBody
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("INVESTIGATING") }
            }

        mockMvc.get("/api/incidents/$incidentId/timeline")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }

                jsonPath("$[0].type") {
                    value("CREATED")
                }

                jsonPath("$[1].type") {
                    value("STATUS_CHANGED")
                }

                jsonPath("$[1].message") {
                    value("Status changed from OPEN to INVESTIGATING")
                }
            }
    }
    @Test
    fun `setting same status twice does not create duplicate timeline event`() {

        val createBody = """
            {
                "title": "Duplicate status test",
                "description": "Testing duplicate status protection",
                "severity": "HIGH"
            }
        """.trimIndent()

        val createResult = mockMvc.post("/api/incidents") {
            contentType = MediaType.APPLICATION_JSON
            content = createBody
        }
            .andExpect {
                status { isCreated() }
            }
            .andReturn()

        val incidentId = Regex(""""id"\s*:\s*(\d+)""")
            .find(createResult.response.contentAsString)
            ?.groupValues
            ?.get(1)
            ?.toLong()
            ?: error("Incident ID was not found in response")

        val statusBody = """
            {
                "status": "INVESTIGATING"
            }
        """.trimIndent()

        // First status change: OPEN -> INVESTIGATING
        mockMvc.patch("/api/incidents/$incidentId/status") {
            contentType = MediaType.APPLICATION_JSON
            content = statusBody
        }
            .andExpect {
                status { isOk() }
            }

        // Same status again: should NOT create another event
        mockMvc.patch("/api/incidents/$incidentId/status") {
            contentType = MediaType.APPLICATION_JSON
            content = statusBody
        }
            .andExpect {
                status { isOk() }
            }

        mockMvc.get("/api/incidents/$incidentId/timeline")
            .andExpect {
                status { isOk() }

                // CREATED + one STATUS_CHANGED = exactly 2
                jsonPath("$.length()") {
                    value(2)
                }

                jsonPath("$[0].type") {
                    value("CREATED")
                }

                jsonPath("$[1].type") {
                    value("STATUS_CHANGED")
                }

                jsonPath("$[1].message") {
                    value("Status changed from OPEN to INVESTIGATING")
                }
            }
    }
    @Test
    fun `adding note creates note added timeline event`() {

        val createBody = """
            {
                "title": "Investigation note test",
                "description": "Testing investigation notes",
                "severity": "CRITICAL"
            }
        """.trimIndent()

        val createResult = mockMvc.post("/api/incidents") {
            contentType = MediaType.APPLICATION_JSON
            content = createBody
        }
            .andExpect {
                status { isCreated() }
            }
            .andReturn()

        val incidentId = Regex(""""id"\s*:\s*(\d+)""")
            .find(createResult.response.contentAsString)
            ?.groupValues
            ?.get(1)
            ?.toLong()
            ?: error("Incident ID was not found in response")

        val noteBody = """
            {
                "message": "Database latency confirmed"
            }
        """.trimIndent()

        mockMvc.post("/api/incidents/$incidentId/notes") {
            contentType = MediaType.APPLICATION_JSON
            content = noteBody
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.type") {
                    value("NOTE_ADDED")
                }
                jsonPath("$.message") {
                    value("Database latency confirmed")
                }
                jsonPath("$.createdAt") {
                    exists()
                }
            }

        mockMvc.get("/api/incidents/$incidentId/timeline")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") {
                    value(2)
                }
                jsonPath("$[0].type") {
                    value("CREATED")
                }
                jsonPath("$[1].type") {
                    value("NOTE_ADDED")
                }
                jsonPath("$[1].message") {
                    value("Database latency confirmed")
                }
            }
    }
    @Test
    fun `blank note returns validation error`() {

        val createBody = """
            {
                "title": "Note validation test",
                "description": "Testing blank note validation",
                "severity": "MEDIUM"
            }
        """.trimIndent()

        val createResult = mockMvc.post("/api/incidents") {
            contentType = MediaType.APPLICATION_JSON
            content = createBody
        }
            .andExpect {
                status { isCreated() }
            }
            .andReturn()

        val incidentId = Regex(""""id"\s*:\s*(\d+)""")
            .find(createResult.response.contentAsString)
            ?.groupValues
            ?.get(1)
            ?.toLong()
            ?: error("Incident ID was not found in response")

        val noteBody = """
            {
                "message": ""
            }
        """.trimIndent()

        mockMvc.post("/api/incidents/$incidentId/notes") {
            contentType = MediaType.APPLICATION_JSON
            content = noteBody
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.status") {
                    value(400)
                }
                jsonPath("$.message") {
                    value("Validation failed")
                }
                jsonPath("$.errors.message") {
                    value("Note must not be blank")
                }
            }

        // Failed validation must not add a timeline event.
        mockMvc.get("/api/incidents/$incidentId/timeline")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") {
                    value(1)
                }
                jsonPath("$[0].type") {
                    value("CREATED")
                }
            }
    }

    @Test
    fun `adding note to nonexistent incident returns not found`() {

        val noteBody = """
            {
                "message": "This should never be stored"
            }
        """.trimIndent()

        mockMvc.post("/api/incidents/999999/notes") {
            contentType = MediaType.APPLICATION_JSON
            content = noteBody
        }
            .andExpect {
                status { isNotFound() }
            }
    }
    @Test
    fun `deleting incident also deletes its timeline events`() {

        val createBody = """
            {
                "title": "Delete cleanup test",
                "description": "Testing timeline cleanup",
                "severity": "HIGH"
            }
        """.trimIndent()

        val createResult = mockMvc.post("/api/incidents") {
            contentType = MediaType.APPLICATION_JSON
            content = createBody
        }
            .andExpect {
                status { isCreated() }
            }
            .andReturn()

        val incidentId = Regex(""""id"\s*:\s*(\d+)""")
            .find(createResult.response.contentAsString)
            ?.groupValues
            ?.get(1)
            ?.toLong()
            ?: error("Incident ID was not found in response")

        // Prove the CREATED event exists before deletion.
        check(
            incidentEventRepository
                .findByIncidentIdOrderByCreatedAtAsc(incidentId)
                .size == 1
        )

        mockMvc.delete("/api/incidents/$incidentId")
            .andExpect {
                status { isNoContent() }
            }

        // The timeline rows must actually be gone from the database.
        check(
            incidentEventRepository
                .findByIncidentIdOrderByCreatedAtAsc(incidentId)
                .isEmpty()
        )

        // The incident itself must also be gone from the API.
        mockMvc.get("/api/incidents/$incidentId")
            .andExpect {
                status { isNotFound() }
            }
    }
    @Test
    fun `filtering incidents by severity returns matching incidents`() {

        val criticalBody = """
            {
                "title": "Critical filter test",
                "description": "Should appear in CRITICAL results",
                "severity": "CRITICAL"
            }
        """.trimIndent()

        val lowBody = """
            {
                "title": "Low filter test",
                "description": "Should not appear in CRITICAL results",
                "severity": "LOW"
            }
        """.trimIndent()

        mockMvc.post("/api/incidents") {
            contentType = MediaType.APPLICATION_JSON
            content = criticalBody
        }
            .andExpect {
                status { isCreated() }
            }

        mockMvc.post("/api/incidents") {
            contentType = MediaType.APPLICATION_JSON
            content = lowBody
        }
            .andExpect {
                status { isCreated() }
            }

        mockMvc.get("/api/incidents") {
            param("severity", "CRITICAL")
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") {
                    value(1)
                }
                jsonPath("$[0].title") {
                    value("Critical filter test")
                }
                jsonPath("$[0].severity") {
                    value("CRITICAL")
                }
            }
    }

    @Test
    fun `filtering incidents by status returns matching incidents`() {

        val firstBody = """
            {
                "title": "Investigating filter test",
                "description": "Will be moved to INVESTIGATING",
                "severity": "HIGH"
            }
        """.trimIndent()

        val secondBody = """
            {
                "title": "Open filter test",
                "description": "Should remain OPEN",
                "severity": "LOW"
            }
        """.trimIndent()

        val createResult = mockMvc.post("/api/incidents") {
            contentType = MediaType.APPLICATION_JSON
            content = firstBody
        }
            .andExpect {
                status { isCreated() }
            }
            .andReturn()

        mockMvc.post("/api/incidents") {
            contentType = MediaType.APPLICATION_JSON
            content = secondBody
        }
            .andExpect {
                status { isCreated() }
            }

        val incidentId = Regex(""""id"\s*:\s*(\d+)""")
            .find(createResult.response.contentAsString)
            ?.groupValues
            ?.get(1)
            ?.toLong()
            ?: error("Incident ID was not found in response")

        mockMvc.patch("/api/incidents/$incidentId/status") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"status":"INVESTIGATING"}"""
        }
            .andExpect {
                status { isOk() }
            }

        mockMvc.get("/api/incidents") {
            param("status", "INVESTIGATING")
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") {
                    value(1)
                }
                jsonPath("$[0].title") {
                    value("Investigating filter test")
                }
                jsonPath("$[0].status") {
                    value("INVESTIGATING")
                }
            }
    }

    @Test
    fun `filtering incidents by status and severity returns matching incidents`() {

        val criticalBody = """
            {
                "title": "Combined filter match",
                "description": "Matches both filters",
                "severity": "CRITICAL"
            }
        """.trimIndent()

        val highBody = """
            {
                "title": "Combined filter nonmatch",
                "description": "Matches status but not severity",
                "severity": "HIGH"
            }
        """.trimIndent()

        val criticalResult = mockMvc.post("/api/incidents") {
            contentType = MediaType.APPLICATION_JSON
            content = criticalBody
        }
            .andExpect {
                status { isCreated() }
            }
            .andReturn()

        val highResult = mockMvc.post("/api/incidents") {
            contentType = MediaType.APPLICATION_JSON
            content = highBody
        }
            .andExpect {
                status { isCreated() }
            }
            .andReturn()

        val criticalId = Regex(""""id"\s*:\s*(\d+)""")
            .find(criticalResult.response.contentAsString)
            ?.groupValues
            ?.get(1)
            ?.toLong()
            ?: error("Critical incident ID was not found")

        val highId = Regex(""""id"\s*:\s*(\d+)""")
            .find(highResult.response.contentAsString)
            ?.groupValues
            ?.get(1)
            ?.toLong()
            ?: error("High incident ID was not found")

        val statusBody = """{"status":"INVESTIGATING"}"""

        mockMvc.patch("/api/incidents/$criticalId/status") {
            contentType = MediaType.APPLICATION_JSON
            content = statusBody
        }.andExpect {
            status { isOk() }
        }

        mockMvc.patch("/api/incidents/$highId/status") {
            contentType = MediaType.APPLICATION_JSON
            content = statusBody
        }.andExpect {
            status { isOk() }
        }

        mockMvc.get("/api/incidents") {
            param("status", "INVESTIGATING")
            param("severity", "CRITICAL")
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") {
                    value(1)
                }
                jsonPath("$[0].title") {
                    value("Combined filter match")
                }
                jsonPath("$[0].status") {
                    value("INVESTIGATING")
                }
                jsonPath("$[0].severity") {
                    value("CRITICAL")
                }
            }
    }

    @Test
    fun `invalid status filter returns structured bad request`() {

        mockMvc.get("/api/incidents") {
            param("status", "BANANA")
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.status") {
                    value(400)
                }
                jsonPath("$.message") {
                    value("Invalid request parameter")
                }
            }
    }

    @Test
    fun `malformed incident request returns structured bad request`() {

        val malformedBody = """
            {
                "title": "Broken JSON",
                "description": "Missing closing structure",
                "severity": "HIGH"
        """.trimIndent()

        mockMvc.post("/api/incidents") {
            contentType = MediaType.APPLICATION_JSON
            content = malformedBody
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.status") {
                    value(400)
                }
                jsonPath("$.message") {
                    value("Malformed request body")
                }
            }
    }

    @Test
    fun `blank incident fields return structured validation error`() {

        val invalidBody = """
            {
                "title": "",
                "description": "",
                "severity": "HIGH"
            }
        """.trimIndent()

        mockMvc.post("/api/incidents") {
            contentType = MediaType.APPLICATION_JSON
            content = invalidBody
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.status") {
                    value(400)
                }
                jsonPath("$.message") {
                    value("Validation failed")
                }
                jsonPath("$.errors.title") {
                    value("Title must not be blank")
                }
                jsonPath("$.errors.description") {
                    value("Description must not be blank")
                }
            }
    }
}













