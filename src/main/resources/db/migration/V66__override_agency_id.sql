ALTER TABLE draft_adjudications ADD COLUMN override_agency_id varchar(6);
ALTER TABLE draft_adjudications RENAME COLUMN agency_id TO originating_agency_id;