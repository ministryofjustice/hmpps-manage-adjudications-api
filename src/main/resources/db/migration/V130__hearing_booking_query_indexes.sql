CREATE INDEX IF NOT EXISTS hearing_adjudication_datetime_idx ON hearing (reported_adjudication_fk_id, date_time_of_hearing, outcome_id);
CREATE INDEX IF NOT EXISTS punishment_schedule_fk_end_date_idx ON punishment_schedule (punishment_fk_id, end_date);
