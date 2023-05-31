ALTER TABLE reported_adjudications RENAME COLUMN agency_id TO originating_agency_id;
ALTER TABLE reported_adjudications ADD COLUMN override_agency_id varchar(6);
CREATE INDEX IF NOT EXISTS prisoner_and_status_idx ON reported_adjudications(prisoner_number, status);