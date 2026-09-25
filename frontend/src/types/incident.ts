export type Severity =
  | 'LOW'
  | 'MEDIUM'
  | 'HIGH'
  | 'CRITICAL'

export type IncidentStatus =
  | 'OPEN'
  | 'INVESTIGATING'
  | 'RESOLVED'

export type IncidentEventType =
  | 'CREATED'
  | 'STATUS_CHANGED'
  | 'NOTE_ADDED'

export interface Incident {
  id: number
  title: string
  description: string
  severity: Severity
  status: IncidentStatus
  createdAt: string
}

export interface IncidentEvent {
  id: number
  type: IncidentEventType
  message: string
  createdAt: string
}
