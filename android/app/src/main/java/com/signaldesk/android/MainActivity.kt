package com.signaldesk.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.signaldesk.android.incident.Incident
import com.signaldesk.android.incident.IncidentStatus
import com.signaldesk.android.incident.Severity
import com.signaldesk.android.ui.theme.SignalDeskTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            SignalDeskTheme {
                SignalDeskApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalDeskApp() {
    val incidents = listOf(
        Incident(
            id = 39,
            title = "Payment gateway timeout",
            description = "Checkout requests are timing out for some customers",
            severity = Severity.CRITICAL,
            status = IncidentStatus.RESOLVED
        ),
        Incident(
            id = 41,
            title = "Email delivery slowdown",
            description = "Transactional emails are taking longer than normal to reach users",
            severity = Severity.MEDIUM,
            status = IncidentStatus.INVESTIGATING
        ),
        Incident(
            id = 42,
            title = "Analytics export delayed",
            description = "Analytics exports are taking longer than expected",
            severity = Severity.HIGH,
            status = IncidentStatus.INVESTIGATING
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
                }
            )
        }
    ) { innerPadding ->
        IncidentList(
            incidents = incidents,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun IncidentList(
    incidents: List<Incident>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = incidents,
            key = { incident -> incident.id }
        ) { incident ->
            IncidentCard(incident)
        }
    }
}

@Composable
fun IncidentCard(
    incident: Incident,
    modifier: Modifier = Modifier
) {
    Card(
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

