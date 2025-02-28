ALTER TABLE draft_adjudications
    ADD COLUMN created_on_behalf_of_officer varchar(32);
ALTER TABLE draft_adjudications
    ADD COLUMN created_on_behalf_of_reason varchar(4000);

ALTER TABLE reported_adjudications
    ADD COLUMN created_on_behalf_of_officer varchar(32);
ALTER TABLE reported_adjudications
    ADD COLUMN created_on_behalf_of_reason varchar(4000);
