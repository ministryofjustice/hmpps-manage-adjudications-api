ALTER TABLE reported_damages
    ADD COLUMN repair_cost decimal(10, 2) null;
ALTER TABLE reported_witness
    ADD COLUMN comment varchar(240) null;
ALTER TABLE reported_witness
    ADD COLUMN username varchar(32) null;
ALTER TABLE witness
    ADD COLUMN username varchar(32) null;
ALTER TABLE hearing
    ADD COLUMN representative varchar(240) null;
ALTER TABLE hearing
    ADD COLUMN comment varchar(240) null;
ALTER TABLE reported_witness
    ADD COLUMN date_added timestamp null;
ALTER TABLE reported_damages
    ADD COLUMN date_added timestamp null;
ALTER TABLE reported_evidence
    ADD COLUMN date_added timestamp null;
ALTER TABLE dis_issue_history
    ADD COLUMN migrated boolean not null DEFAULT false;