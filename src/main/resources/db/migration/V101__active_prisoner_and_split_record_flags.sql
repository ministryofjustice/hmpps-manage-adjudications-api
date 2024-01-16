ALTER TABLE reported_adjudications ADD COLUMN migrated_inactive_prisoner boolean not null DEFAULT false;
ALTER TABLE reported_adjudications ADD COLUMN migrated_split_record boolean not null DEFAULT false;
