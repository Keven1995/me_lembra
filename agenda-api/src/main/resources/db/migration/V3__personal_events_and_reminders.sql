ALTER TABLE appointments RENAME COLUMN patient_name TO title;
ALTER TABLE appointments ADD COLUMN category VARCHAR(32) NOT NULL DEFAULT 'OTHER';
ALTER TABLE appointments ADD COLUMN location VARCHAR(240);
ALTER TABLE appointments ADD COLUMN notes VARCHAR(1000);
ALTER TABLE appointments DROP COLUMN notification_sent_at;

CREATE TABLE reminder_jobs (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    kind VARCHAR(32) NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (appointment_id, kind)
);
CREATE INDEX idx_reminder_jobs_due ON reminder_jobs(due_at) WHERE sent_at IS NULL;
