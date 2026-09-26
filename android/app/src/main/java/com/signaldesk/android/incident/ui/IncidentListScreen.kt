package com.signaldesk.android.incident.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.Severity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentListScreen(
    incidents: List<Incident>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    error: String?,
    selectedStatus: IncidentStatus?,
    selectedSeverity: Severity?,
    onStatusFilterChange: (IncidentStatus?) -> Unit,
    onSeverityFilterChange: (Severity?) -> Unit,
    onIncidentClick: (Incident) -> Unit,
    onRefresh: () -> Unit,
    onCreateIncidentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SignalDesk")
                        Text(
                            text = "Incident monitoring and response",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing
                    ) {
                        Text("Refresh")
                    }

                    TextButton(
                        onClick = onCreateIncidentClick
                    ) {
                        Text("Create")
                    }
                }
            )
        }
    ) { innerPadding ->

        when {
            isLoading -> {
                LoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            error != null -> {
                ErrorContent(
                    error = error,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isRefreshing) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("incident_refresh_indicator")
                            )
                        }
                    }

                    item {
                        IncidentFilters(
                            selectedStatus = selectedStatus,
                            selectedSeverity = selectedSeverity,
                            onStatusFilterChange = onStatusFilterChange,
                            onSeverityFilterChange = onSeverityFilterChange
                        )
                    }

                    if (incidents.isEmpty()) {
                        item {
                            FilteredEmptyContent()
                        }
                    } else {
                        items(
                            items = incidents,
                            key = { incident -> incident.id }
                        ) { incident ->
                            IncidentCard(
                                incident = incident,
                                onClick = {
                                    onIncidentClick(incident)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentFilters(
    selectedStatus: IncidentStatus?,
    selectedSeverity: Severity?,
    onStatusFilterChange: (IncidentStatus?) -> Unit,
    onSeverityFilterChange: (Severity?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Filters",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusFilter(
                selectedStatus = selectedStatus,
                onStatusFilterChange = onStatusFilterChange,
                modifier = Modifier.weight(1f)
            )

            SeverityFilter(
                selectedSeverity = selectedSeverity,
                onSeverityFilterChange = onSeverityFilterChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatusFilter(
    selectedStatus: IncidentStatus?,
    onStatusFilterChange: (IncidentStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
    ) {
        Text(
            text = "Status",
            style = MaterialTheme.typography.labelMedium
        )

        OutlinedButton(
            onClick = {
                expanded = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedStatus?.name ?: "ALL"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            DropdownMenuItem(
                text = {
                    Text("ALL")
                },
                onClick = {
                    expanded = false
                    onStatusFilterChange(null)
                }
            )

            IncidentStatus.values().forEach { status ->
                DropdownMenuItem(
                    text = {
                        Text(status.name)
                    },
                    onClick = {
                        expanded = false
                        onStatusFilterChange(status)
                    }
                )
            }
        }
    }
}

@Composable
private fun SeverityFilter(
    selectedSeverity: Severity?,
    onSeverityFilterChange: (Severity?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
    ) {
        Text(
            text = "Severity",
            style = MaterialTheme.typography.labelMedium
        )

        OutlinedButton(
            onClick = {
                expanded = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedSeverity?.name ?: "ALL"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            DropdownMenuItem(
                text = {
                    Text("ALL")
                },
                onClick = {
                    expanded = false
                    onSeverityFilterChange(null)
                }
            )

            Severity.values().forEach { severity ->
                DropdownMenuItem(
                    text = {
                        Text(severity.name)
                    },
                    onClick = {
                        expanded = false
                        onSeverityFilterChange(severity)
                    }
                )
            }
        }
    }
}

@Composable
private fun FilteredEmptyContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "No incidents match these filters",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Change the status or severity filter to see other incidents.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Loading incidents...",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
private fun ErrorContent(
    error: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Unable to load incidents",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = error,
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Check that the SignalDesk backend is running.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun EmptyContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No incidents found",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "The backend returned an empty incident list.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun IncidentCard(
    incident: Incident,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = incident.severity.name,
                    style = MaterialTheme.typography.labelMedium
                )

                Text(
                    text = incident.status.name,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Text(
                text = incident.title,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = incident.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Incident #${incident.id}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}