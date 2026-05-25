-- Run this SQL in your Supabase SQL Editor (Dashboard -> SQL Editor -> New query)

-- Workers
CREATE TABLE IF NOT EXISTS workers (
    id          TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name        TEXT NOT NULL,
    dept        TEXT NOT NULL DEFAULT '',
    shift       TEXT NOT NULL DEFAULT 'General',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Shifts (seed defaults after creation)
CREATE TABLE IF NOT EXISTS shifts (
    id                       TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name                     TEXT NOT NULL UNIQUE,
    start_time               TEXT NOT NULL DEFAULT '08:30',
    end_time                 TEXT NOT NULL DEFAULT '17:30',
    late_threshold_minutes   INT NOT NULL DEFAULT 15,
    enabled                  BOOLEAN NOT NULL DEFAULT TRUE,
    always_on                BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO shifts (name, start_time, end_time, late_threshold_minutes, enabled, always_on) VALUES
    ('General', '08:30', '17:30', 15, TRUE,  TRUE),
    ('A',       '08:30', '17:30', 15, TRUE,  FALSE),
    ('B',       '14:00', '23:00', 15, FALSE, FALSE),
    ('C',       '22:00', '07:00', 15, FALSE, FALSE),
    ('D',       '05:00', '14:00', 15, FALSE, FALSE)
ON CONFLICT (name) DO NOTHING;

-- Attendance records
CREATE TABLE IF NOT EXISTS attendance_records (
    id                TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    worker_id         TEXT NOT NULL REFERENCES workers(id) ON DELETE CASCADE,
    date              DATE NOT NULL,
    status            TEXT NOT NULL DEFAULT 'UNMARKED'
                        CHECK (status IN ('PRESENT','ABSENT','LATE','OFF','UNMARKED')),
    time_in           TEXT,
    time_out          TEXT,
    check_in_method   TEXT NOT NULL DEFAULT 'MANUAL'
                        CHECK (check_in_method IN ('MANUAL','GEO_FENCE')),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(worker_id, date)
);

CREATE INDEX IF NOT EXISTS idx_attendance_date   ON attendance_records(date);
CREATE INDEX IF NOT EXISTS idx_attendance_worker ON attendance_records(worker_id);

-- Auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_attendance_updated_at ON attendance_records;
CREATE TRIGGER trg_attendance_updated_at
    BEFORE UPDATE ON attendance_records
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Geo config (single-row table)
CREATE TABLE IF NOT EXISTS geo_config (
    id              TEXT PRIMARY KEY DEFAULT 'default',
    factory_lat     DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    factory_lng     DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    radius_meters   INT NOT NULL DEFAULT 200
);
INSERT INTO geo_config (id) VALUES ('default') ON CONFLICT (id) DO NOTHING;

-- Row Level Security (enable for production)
-- ALTER TABLE workers ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE attendance_records ENABLE ROW LEVEL SECURITY;
-- CREATE POLICY "allow_all" ON workers USING (true) WITH CHECK (true);
-- CREATE POLICY "allow_all" ON attendance_records USING (true) WITH CHECK (true);

-- Enable realtime for live updates
ALTER TABLE attendance_records REPLICA IDENTITY FULL;
ALTER TABLE workers REPLICA IDENTITY FULL;
