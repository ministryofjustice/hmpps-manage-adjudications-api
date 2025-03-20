ALTER TABLE reported_adjudications ADD COLUMN location_uuid UUID;
ALTER TABLE incident_details ADD COLUMN location_uuid UUID;
ALTER TABLE hearing ADD COLUMN location_uuid UUID;