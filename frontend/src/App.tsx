import { useEffect, useState } from 'react'
import {
  createIncident,
  getIncidents,
  updateIncidentStatus,
  type CreateIncidentRequest,
} from './api/incidents'
import { IncidentCard } from './components/IncidentCard'
import { NewIncidentForm } from './components/NewIncidentForm'
import type {
  Incident,
  IncidentStatus,
  Severity,
} from './types/incident'
import './App.css'

function App() {
  const [incidents, setIncidents] = useState<Incident[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [statusFilter, setStatusFilter] =
    useState<IncidentStatus | ''>('')

  const [severityFilter, setSeverityFilter] =
    useState<Severity | ''>('')

  useEffect(() => {
    setLoading(true)
    setError(null)

    getIncidents({
      status: statusFilter || undefined,
      severity: severityFilter || undefined,
    })
      .then(setIncidents)
      .catch((error: Error) => setError(error.message))
      .finally(() => setLoading(false))
  }, [statusFilter, severityFilter])

  async function handleCreateIncident(
    request: CreateIncidentRequest
  ) {
    const createdIncident = await createIncident(request)

    const matchesStatus =
      !statusFilter ||
      createdIncident.status === statusFilter

    const matchesSeverity =
      !severityFilter ||
      createdIncident.severity === severityFilter

    if (matchesStatus && matchesSeverity) {
      setIncidents((currentIncidents) => [
        createdIncident,
        ...currentIncidents,
      ])
    }
  }

  async function handleStatusChange(
    incidentId: number,
    status: IncidentStatus
  ) {
    const updatedIncident = await updateIncidentStatus(
      incidentId,
      status
    )

    const matchesStatus =
      !statusFilter ||
      updatedIncident.status === statusFilter

    const matchesSeverity =
      !severityFilter ||
      updatedIncident.severity === severityFilter

    setIncidents((currentIncidents) => {
      if (!matchesStatus || !matchesSeverity) {
        return currentIncidents.filter(
          (incident) => incident.id !== updatedIncident.id
        )
      }

      return currentIncidents.map((incident) =>
        incident.id === updatedIncident.id
          ? updatedIncident
          : incident
      )
    })
  }

  const openCount = incidents.filter(
    (incident) => incident.status === 'OPEN'
  ).length

  const investigatingCount = incidents.filter(
    (incident) => incident.status === 'INVESTIGATING'
  ).length

  const criticalCount = incidents.filter(
    (incident) => incident.severity === 'CRITICAL'
  ).length

  return (
    <main className="dashboard">
      <header className="dashboard-header">
        <div>
          <p className="eyebrow">OPERATIONS</p>
          <h1>SignalDesk</h1>
          <p className="subtitle">
            Incident monitoring and response
          </p>
        </div>
      </header>

      <section className="summary-grid">
        <div className="summary-card">
          <span>Visible incidents</span>
          <strong>{incidents.length}</strong>
        </div>

        <div className="summary-card">
          <span>Open</span>
          <strong>{openCount}</strong>
        </div>

        <div className="summary-card">
          <span>Investigating</span>
          <strong>{investigatingCount}</strong>
        </div>

        <div className="summary-card">
          <span>Critical</span>
          <strong>{criticalCount}</strong>
        </div>
      </section>

      <NewIncidentForm onCreate={handleCreateIncident} />

      <section className="incident-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">INCIDENT QUEUE</p>
            <h2>Active incidents</h2>
          </div>

          <span className="incident-count">
            {incidents.length} visible
          </span>
        </div>

        <div className="filter-bar">
          <label>
            Status

            <select
              value={statusFilter}
              onChange={(event) =>
                setStatusFilter(
                  event.target.value as IncidentStatus | ''
                )
              }
            >
              <option value="">All statuses</option>
              <option value="OPEN">Open</option>
              <option value="INVESTIGATING">
                Investigating
              </option>
              <option value="RESOLVED">Resolved</option>
            </select>
          </label>

          <label>
            Severity

            <select
              value={severityFilter}
              onChange={(event) =>
                setSeverityFilter(
                  event.target.value as Severity | ''
                )
              }
            >
              <option value="">All severities</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </label>
        </div>

        {loading && (
          <p className="state-message">
            Loading incidents...
          </p>
        )}

        {error && (
          <p className="state-message error">
            {error}
          </p>
        )}

        {!loading && !error && incidents.length === 0 && (
          <p className="state-message">
            No incidents match these filters.
          </p>
        )}

        {!loading && !error && (
          <div className="incident-grid">
            {incidents.map((incident) => (
              <IncidentCard
                key={incident.id}
                incident={incident}
                onStatusChange={handleStatusChange}
              />
            ))}
          </div>
        )}
      </section>
    </main>
  )
}

export default App
