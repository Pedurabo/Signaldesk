import type { Incident, Severity } from '../types/incident'

const API_URL = 'http://localhost:8082/api/incidents'

export type CreateIncidentRequest = {
  title: string
  description: string
  severity: Severity
}

export async function getIncidents(): Promise<Incident[]> {
  const response = await fetch(API_URL)

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
