import type {
  Incident,
  IncidentEvent,
  IncidentStatus,
  Severity,
} from '../types/incident'

const API_URL = 'http://localhost:8082/api/incidents'

export type CreateIncidentRequest = {
  title: string
  description: string
  severity: Severity
}

export type IncidentFilters = {
  status?: IncidentStatus
  severity?: Severity
}

export async function getIncidents(
  filters: IncidentFilters = {}
): Promise<Incident[]> {
  const params = new URLSearchParams()

  if (filters.status) {
    params.set('status', filters.status)
  }

  if (filters.severity) {
    params.set('severity', filters.severity)
  }

  const query = params.toString()

  const url = query
    ? `${API_URL}?${query}`
    : API_URL

  const response = await fetch(url)

  if (!response.ok) {
    throw new Error(`Failed to load incidents: HTTP ${response.status}`)
  }

  return response.json()
}

export async function createIncident(
  request: CreateIncidentRequest
): Promise<Incident> {
  const response = await fetch(API_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (!response.ok) {
    throw new Error(`Failed to create incident: HTTP ${response.status}`)
  }

  return response.json()
}

export async function updateIncidentStatus(
  incidentId: number,
  status: IncidentStatus
): Promise<Incident> {
  const response = await fetch(
    `${API_URL}/${incidentId}/status`,
    {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ status }),
    }
  )

  if (!response.ok) {
    throw new Error(
      `Failed to update incident status: HTTP ${response.status}`
    )
  }

  return response.json()
}

export async function getIncidentTimeline(
  incidentId: number
): Promise<IncidentEvent[]> {
  const response = await fetch(
    `${API_URL}/${incidentId}/timeline`
  )

  if (!response.ok) {
    throw new Error(
      `Failed to load incident timeline: HTTP ${response.status}`
    )
  }

  return response.json()
}


export async function addIncidentNote(
  incidentId: number,
  message: string
): Promise<IncidentEvent> {
  const response = await fetch(
    `${API_URL}/${incidentId}/notes`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ message }),
    }
  )

  if (!response.ok) {
    throw new Error(
      `Failed to add incident note: HTTP ${response.status}`
    )
  }

  return response.json()
}

