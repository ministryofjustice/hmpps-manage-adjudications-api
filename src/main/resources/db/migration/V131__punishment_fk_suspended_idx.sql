CREATE INDEX IF NOT EXISTS punishment_fk_suspended_idx ON punishment (reported_adjudication_fk_id, suspended_until);
