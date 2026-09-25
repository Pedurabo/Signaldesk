CREATE INDEX IF NOT EXISTS idx_incident_events_incident_id
    ON incident_events (incident_id);
