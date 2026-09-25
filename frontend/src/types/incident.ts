export type Severity =
  | 'LOW'
  | 'MEDIUM'
  | 'HIGH'
  | 'CRITICAL'

export type IncidentStatus =
  | 'OPEN'
  | 'INVESTIGATING'
  | 'RESOLVED'

export interface Incident {
  id: number
  title: string
  description: string
  severity: Severity
  status: IncidentStatus
  createdAt: string
}
