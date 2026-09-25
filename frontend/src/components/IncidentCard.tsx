import { useState } from 'react'
import { getIncidentTimeline } from '../api/incidents'
import { IncidentNoteForm } from './IncidentNoteForm'
import { IncidentTimeline } from './IncidentTimeline'
import type {
  Incident,
  IncidentEvent,
  IncidentStatus,
} from '../types/incident'

type IncidentCardProps = {
  incident: Incident
  onStatusChange: (
    incidentId: number,
    status: IncidentStatus
  ) => Promise<void>
  onOpenDetails: (incidentId: number) => void
}

export function IncidentCard({
  incident,
  onStatusChange,
  onOpenDetails,
}: IncidentCardProps) {
  const [updating, setUpdating] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [timeline, setTimeline] = useState<IncidentEvent[]>([])
  const [timelineVisible, setTimelineVisible] = useState(false)
  const [timelineLoading, setTimelineLoading] = useState(false)

  const nextStatus: IncidentStatus | null =
    incident.status === 'OPEN'
      ? 'INVESTIGATING'
      : incident.status === 'INVESTIGATING'
        ? 'RESOLVED'
        : null

  const actionLabel =
    incident.status === 'OPEN'
      ? 'Start investigating'
      : incident.status === 'INVESTIGATING'
        ? 'Resolve incident'
        : null

  async function handleStatusChange() {
    if (!nextStatus) {
      return
    }

    setUpdating(true)
    setError(null)

    try {
      await onStatusChange(incident.id, nextStatus)

      if (timelineVisible) {
        const events = await getIncidentTimeline(incident.id)
        setTimeline(events)
      }
    } catch (error) {
      setError(
        error instanceof Error
          ? error.message
          : 'Failed to update status'
      )
    } finally {
      setUpdating(false)
    }
  }

  async function handleTimelineToggle() {
    if (timelineVisible) {
      setTimelineVisible(false)
      return
    }

    setTimelineLoading(true)
    setError(null)

    try {
      const events = await getIncidentTimeline(incident.id)

      setTimeline(events)
      setTimelineVisible(true)
    } catch (error) {
      setError(
        error instanceof Error
          ? error.message
          : 'Failed to load timeline'
      )
    } finally {
      setTimelineLoading(false)
    }
  }

  function handleNoteAdded(event: IncidentEvent) {
    setTimeline((currentTimeline) => [
      ...currentTimeline,
      event,
    ])
  }

  return (
    <article className="incident-card">
      <div className="incident-card-header">
        <span
          className={`severity severity-${incident.severity.toLowerCase()}`}
        >
          {incident.severity}
        </span>

        <span className="status">
          {incident.status.replace('_', ' ')}
        </span>
      </div>

      <h3>{incident.title}</h3>

      <p className="incident-description">
        {incident.description}
      </p>

      <div className="incident-actions">
        {actionLabel && (
          <button
            className="status-action"
            type="button"
            disabled={updating}
            onClick={handleStatusChange}
          >
            {updating ? 'Updating...' : actionLabel}
          </button>
        )}

        <button
          className="timeline-action"
          type="button"
          disabled={timelineLoading}
          onClick={handleTimelineToggle}
        >
          {timelineLoading
            ? 'Loading...'
            : timelineVisible
              ? 'Hide timeline'
              : 'View timeline'}
        </button>
        <button
          className="timeline-action"
          type="button"
          onClick={() => onOpenDetails(incident.id)}
        >
          Open details
        </button>
      </div>

      {error && (
        <p className="form-error">
          {error}
        </p>
      )}

      {timelineVisible && (
        <section className="timeline">
          <IncidentTimeline events={timeline} />

          <IncidentNoteForm
            incidentId={incident.id}
            onNoteAdded={handleNoteAdded}
            onError={setError}
          />
        </section>
      )}

      <footer className="incident-footer">
        <span>Incident #{incident.id}</span>

        <time dateTime={incident.createdAt}>
          {new Date(incident.createdAt).toLocaleString()}
        </time>
      </footer>
    </article>
  )
}

