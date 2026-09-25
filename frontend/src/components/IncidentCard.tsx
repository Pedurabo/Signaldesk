import { useState, type FormEvent } from 'react'
import {
  addIncidentNote,
  getIncidentTimeline,
} from '../api/incidents'
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
}

export function IncidentCard({
  incident,
  onStatusChange,
}: IncidentCardProps) {
  const [updating, setUpdating] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [timeline, setTimeline] = useState<IncidentEvent[]>([])
  const [timelineVisible, setTimelineVisible] = useState(false)
  const [timelineLoading, setTimelineLoading] = useState(false)

  const [note, setNote] = useState('')
  const [noteSubmitting, setNoteSubmitting] = useState(false)

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

  async function handleNoteSubmit(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault()

    const message = note.trim()

    if (!message) {
      return
    }

    setNoteSubmitting(true)
    setError(null)

    try {
      const createdEvent = await addIncidentNote(
        incident.id,
        message
      )

      setTimeline((currentTimeline) => [
        ...currentTimeline,
        createdEvent,
      ])

      setNote('')
    } catch (error) {
      setError(
        error instanceof Error
          ? error.message
          : 'Failed to add note'
      )
    } finally {
      setNoteSubmitting(false)
    }
  }

  return (
    <article className="incident-card">
      <div className="incident-card-header">
        <span className={`severity severity-${incident.severity.toLowerCase()}`}>
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
      </div>

      {error && <p className="form-error">{error}</p>}

      {timelineVisible && (
        <section className="timeline">
          <h4>Timeline</h4>

          {timeline.length === 0 ? (
            <p className="timeline-empty">
              No timeline events.
            </p>
          ) : (
            <ol className="timeline-list">
              {timeline.map((event) => (
                <li key={event.id}>
                  <div className="timeline-event-header">
                    <strong>
                      {event.type.replaceAll('_', ' ')}
                    </strong>

                    <time dateTime={event.createdAt}>
                      {new Date(event.createdAt).toLocaleString()}
                    </time>
                  </div>

                  <p>{event.message}</p>
                </li>
              ))}
            </ol>
          )}

          <form
            className="note-form"
            onSubmit={handleNoteSubmit}
          >
            <label>
              Operational note

              <textarea
                value={note}
                onChange={(event) =>
                  setNote(event.target.value)
                }
                placeholder="Add an update for the incident timeline..."
                rows={3}
                required
              />
            </label>

            <button
              type="submit"
              disabled={noteSubmitting}
            >
              {noteSubmitting ? 'Adding...' : 'Add note'}
            </button>
          </form>
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
