import type { IncidentEvent } from '../types/incident'

type IncidentTimelineProps = {
  events: IncidentEvent[]
}

export function IncidentTimeline({
  events,
}: IncidentTimelineProps) {
  return (
    <>
      <h4>Timeline</h4>

      {events.length === 0 ? (
        <p className="timeline-empty">
          No timeline events.
        </p>
      ) : (
        <ol className="timeline-list">
          {events.map((event) => (
            <li key={event.id}>
              <div className="timeline-event-header">
                <strong>
                  {event.type.replaceAll('_', ' ')}
                </strong>

                <time dateTime={event.createdAt}>
                  {new Date(
                    event.createdAt
                  ).toLocaleString()}
                </time>
              </div>

              <p>{event.message}</p>
            </li>
          ))}
        </ol>
      )}
    </>
  )
}
