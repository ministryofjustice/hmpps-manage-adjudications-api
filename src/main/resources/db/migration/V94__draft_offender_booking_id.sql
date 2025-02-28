ALTER TABLE draft_adjudications
    ADD COLUMN offender_booking_id bigint null;
create index offender_bookingid_idx on reported_adjudications (offender_booking_id);
create index offender_bookingid_status_idx on reported_adjudications (offender_booking_id, status);