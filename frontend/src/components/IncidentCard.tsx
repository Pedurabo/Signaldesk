import type { Incident } from '../types/incident'

type IncidentCardProps = {
  incident: Incident
}

export function IncidentCard({ incident }: IncidentCardProps) {
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

      <footer className="incident-footer">
        <span>Incident #{incident.id}</span>
        <time dateTime={incident.createdAt}>
          {new Date(incident.createdAt).toLocaleString()}
        </time>
      </footer>
    </article>
  )
}
