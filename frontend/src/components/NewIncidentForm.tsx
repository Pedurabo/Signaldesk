import { useState, type FormEvent } from 'react'
import type { CreateIncidentRequest } from '../api/incidents'
import type { Severity } from '../types/incident'

type NewIncidentFormProps = {
  onCreate: (request: CreateIncidentRequest) => Promise<void>
}

export function NewIncidentForm({ onCreate }: NewIncidentFormProps) {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [severity, setSeverity] = useState<Severity>('MEDIUM')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    setSubmitting(true)
    setError(null)

    try {
      await onCreate({
        title,
        description,
        severity,
      })

      setTitle('')
      setDescription('')
      setSeverity('MEDIUM')
    } catch (error) {
      setError(
        error instanceof Error
          ? error.message
          : 'Failed to create incident'
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="incident-form" onSubmit={handleSubmit}>
      <div className="form-heading">
        <div>
          <p className="eyebrow">REPORT</p>
          <h2>New incident</h2>
        </div>
      </div>

      <label>
        Title
        <input
          type="text"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="e.g. Payment API unavailable"
          required
        />
      </label>

      <label>
        Description
        <textarea
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          placeholder="Describe what is happening..."
          required
          rows={4}
        />
      </label>

      <label>
        Severity
        <select
          value={severity}
          onChange={(event) =>
            setSeverity(event.target.value as Severity)
          }
        >
          <option value="LOW">Low</option>
          <option value="MEDIUM">Medium</option>
          <option value="HIGH">High</option>
          <option value="CRITICAL">Critical</option>
        </select>
      </label>

      {error && <p className="form-error">{error}</p>}

      <button type="submit" disabled={submitting}>
        {submitting ? 'Creating...' : 'Create incident'}
      </button>
    </form>
  )
}
