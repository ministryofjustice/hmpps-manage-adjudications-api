ALTER TABLE hearing ADD COLUMN migrated boolean not null DEFAULT false;
ALTER TABLE punishment ADD COLUMN migrated boolean not null DEFAULT false;
ALTER TABLE punishment_comments ADD COLUMN migrated boolean not null DEFAULT false;
ALTER TABLE hearing_outcome ADD COLUMN migrated boolean not null DEFAULT false;
ALTER TABLE outcome ADD COLUMN migrated boolean not null DEFAULT false;
ALTER TABLE reported_witness ADD COLUMN migrated boolean not null DEFAULT false;
ALTER TABLE reported_damages ADD COLUMN migrated boolean not null DEFAULT false;
ALTER TABLE reported_evidence ADD COLUMN migrated boolean not null DEFAULT false;
ALTER TABLE reported_offence ADD COLUMN migrated boolean not null DEFAULT false;
ALTER TABLE reported_offence ADD COLUMN actual_offence_code integer null;
ALTER TABLE reported_adjudications ADD COLUMN status_before_migration varchar(32);

