import { useEffect, useState } from 'react'

type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

type IncidentStatus = 'OPEN' | 'INVESTIGATING' | 'RESOLVED'

type Incident = {
  id: number
  title: string
  description: string
  severity: Severity
  status: IncidentStatus
  createdAt: string
}

function App() {
  const [incidents, setIncidents] = useState<Incident[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetch('http://localhost:8082/api/incidents')
      .then((response) => {
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`)
        }

        return response.json()
      })
      .then((data: Incident[]) => {
        setIncidents(data)
      })
      .catch((error: Error) => {
        setError(error.message)
      })
  }, [])

  return (
    <main>
      <h1>SignalDesk</h1>
      <p>Incident Management</p>

      {error && <p>Failed to load incidents: {error}</p>}

      {incidents.map((incident) => (
        <article key={incident.id}>
          <h2>{incident.title}</h2>
          <p>{incident.description}</p>
          <p>Severity: {incident.severity}</p>
          <p>Status: {incident.status}</p>
        </article>
      ))}
    </main>
  )
}

export default App
