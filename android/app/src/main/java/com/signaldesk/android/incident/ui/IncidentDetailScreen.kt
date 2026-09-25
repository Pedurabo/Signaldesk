package com.signaldesk.android.incident.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.IncidentTimelineEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    incident: Incident,
    timeline: List<IncidentTimelineEvent>,
    isTimelineLoading: Boolean,
    timelineError: String?,
    isUpdating: Boolean,
    error: String?,
    onStatusChange: (IncidentStatus) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TextButton(
                        onClick = onBack
                    ) {
                        Text("Back")
                    }
                },
                title = {
                    Text("Incident #${incident.id}")
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = incident.title,
                style = MaterialTheme.typography.headlineSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Severity",
                            style = MaterialTheme.typography.labelMedium
                        )

                        Text(
                            text = incident.severity.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.labelMedium
                        )

                        Text(
                            text = incident.status.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = incident.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            when (incident.status) {
                IncidentStatus.OPEN -> {
                    Button(
                        onClick = {
                            onStatusChange(
                                IncidentStatus.INVESTIGATING
                            )
                        },
                        enabled = !isUpdating,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isUpdating) {
                                "Updating..."
                            } else {
                                "Start investigating"
                            }
                        )
                    }
                }

                IncidentStatus.INVESTIGATING -> {
                    Button(
                        onClick = {
                            onStatusChange(
                                IncidentStatus.RESOLVED
                            )
                        },
                        enabled = !isUpdating,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isUpdating) {
                                "Updating..."
                            } else {
                                "Resolve incident"
                            }
                        )
                    }
                }

                IncidentStatus.RESOLVED -> {
                    Text(
                        text = "This incident is resolved.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Timeline",
                style = MaterialTheme.typography.titleLarge
            )

            when {
                isTimelineLoading -> {
                    Text(
                        text = "Loading timeline...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                timelineError != null -> {
                    Text(
                        text = timelineError,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                timeline.isEmpty() -> {
                    Text(
                        text = "No timeline events.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> {
                    timeline.forEach { event ->
                        TimelineEventCard(
                            event = event
                        )
                    }
                }
            }

            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to incidents")
            }
        }
    }
}

@Composable
private fun TimelineEventCard(
    event: IncidentTimelineEvent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = event.type.name.replace("_", " "),
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = event.message,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = event.createdAt,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}