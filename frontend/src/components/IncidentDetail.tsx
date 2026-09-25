import { useEffect, useState } from 'react'
import {
  getIncident,
  getIncidentTimeline,
} from '../api/incidents'
import { IncidentNoteForm } from './IncidentNoteForm'
import { IncidentTimeline } from './IncidentTimeline'
import type {
  Incident,
  IncidentEvent,
} from '../types/incident'

type IncidentDetailProps = {
  incidentId: number
  onBack: () => void
}

export function IncidentDetail({
  incidentId,
  onBack,
}: IncidentDetailProps) {
  const [incident, setIncident] =
    useState<Incident | null>(null)

  const [timeline, setTimeline] =
    useState<IncidentEvent[]>([])

  const [loading, setLoading] = useState(true)
  const [error, setError] =
    useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    setError(null)

    Promise.all([
      getIncident(incidentId),
      getIncidentTimeline(incidentId),
    ])
      .then(([loadedIncident, loadedTimeline]) => {
        setIncident(loadedIncident)
        setTimeline(loadedTimeline)
      })
      .catch((error: Error) => {
        setError(error.message)
      })
      .finally(() => {
        setLoading(false)
      })
  }, [incidentId])

  function handleNoteAdded(event: IncidentEvent) {
    setTimeline((currentTimeline) => [
      ...currentTimeline,
      event,
    ])
  }

  if (loading) {
    return (
      <main className="dashboard">
        <p className="state-message">
          Loading incident...
        </p>
      </main>
    )
  }

  if (error || !incident) {
    return (
      <main className="dashboard">
        <button
          type="button"
          className="back-action"
          onClick={onBack}
        >
          Back to incidents
        </button>

        <p className="state-message error">
          {error ?? 'Incident not found'}
        </p>
      </main>
    )
  }

  return (
    <main className="dashboard">
      <button
        type="button"
        className="back-action"
        onClick={onBack}
      >
        Back to incidents
      </button>

      <article className="incident-detail">
        <header className="incident-detail-header">
          <div>
            <p className="eyebrow">
              INCIDENT #{incident.id}
            </p>

            <h1>{incident.title}</h1>

            <p className="incident-description">
              {incident.description}
            </p>
          </div>

          <div className="incident-detail-badges">
            <span
              className={`severity severity-${incident.severity.toLowerCase()}`}
            >
              {incident.severity}
            </span>

            <span className="status">
              {incident.status.replace('_', ' ')}
            </span>
          </div>
        </header>

        <section className="timeline incident-detail-timeline">
          <IncidentTimeline events={timeline} />

          <IncidentNoteForm
            incidentId={incident.id}
            onNoteAdded={handleNoteAdded}
            onError={setError}
          />
        </section>
      </article>
    </main>
  )
}

