ALTER TABLE reported_adjudications
    ADD COLUMN issuing_officer varchar(32);
ALTER TABLE reported_adjudications
    ADD COLUMN date_time_of_issue timestamp;
