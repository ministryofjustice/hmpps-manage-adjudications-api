ALTER TABLE reported_adjudications ADD COLUMN offender_booking_id bigint null;

ALTER TABLE reported_adjudications DROP COLUMN draft_created_on;
