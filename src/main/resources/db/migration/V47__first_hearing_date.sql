ALTER TABLE reported_adjudications
    ADD COLUMN date_time_of_first_hearing timestamp;

UPDATE reported_adjudications ra
SET date_time_of_first_hearing = (SELECT MIN(h.date_time_of_hearing)
                                  FROM hearing h
                                  WHERE h.reported_adjudication_fk_id = ra.id);

create index reported_adjudications_agency_hearing_date_idx on reported_adjudications (agency_id, date_time_of_first_hearing, status);