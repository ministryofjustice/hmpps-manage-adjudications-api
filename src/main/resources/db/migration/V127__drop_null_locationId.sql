ALTER TABLE incident_details
    ALTER COLUMN location_id DROP NOT NULL;
ALTER TABLE reported_adjudications
    ALTER COLUMN location_id DROP NOT NULL;
ALTER TABLE hearing
    ALTER COLUMN location_id DROP NOT NULL;
ALTER TABLE incident_details
    ALTER COLUMN location_uuid SET NOT NULL;
ALTER TABLE reported_adjudications
    ALTER COLUMN location_uuid SET NOT NULL;
ALTER TABLE hearing
    ALTER COLUMN location_uuid SET NOT NULL;
