package com.signaldesk.android.incident.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    incident: Incident,
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

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Incident ID",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Text(
                        text = "#${incident.id}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to incidents")
            }
        }
    }
}