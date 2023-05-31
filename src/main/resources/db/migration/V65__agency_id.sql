ALTER TABLE reported_adjudications RENAME COLUMN agency_id TO originating_agency_id;
ALTER TABLE reported_adjudications ADD COLUMN override_agency_id varchar(6);