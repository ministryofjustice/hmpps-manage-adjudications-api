ALTER TABLE reported_adjudications
    ADD COLUMN migrated boolean not null DEFAULT false;