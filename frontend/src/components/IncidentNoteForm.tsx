import { useState, type FormEvent } from 'react'
import { addIncidentNote } from '../api/incidents'
import type { IncidentEvent } from '../types/incident'

type IncidentNoteFormProps = {
  incidentId: number
  onNoteAdded: (event: IncidentEvent) => void
  onError: (message: string) => void
}

export function IncidentNoteForm({
  incidentId,
  onNoteAdded,
  onError,
}: IncidentNoteFormProps) {
  const [note, setNote] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault()

    const message = note.trim()

    if (!message) {
      return
    }

    setSubmitting(true)

    try {
      const createdEvent = await addIncidentNote(
        incidentId,
        message
      )

      onNoteAdded(createdEvent)
      setNote('')
    } catch (error) {
      onError(
        error instanceof Error
          ? error.message
          : 'Failed to add note'
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form
      className="note-form"
      onSubmit={handleSubmit}
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
        disabled={submitting}
      >
        {submitting ? 'Adding...' : 'Add note'}
      </button>
    </form>
  )
}
