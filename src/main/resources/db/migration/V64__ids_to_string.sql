alter table reported_adjudications
    ALTER COLUMN report_number TYPE VARCHAR(36);

alter table draft_adjudications
    ALTER COLUMN report_number TYPE VARCHAR(36);

alter table hearing
    ALTER COLUMN report_number TYPE VARCHAR(36);

alter table hearing
    ALTER COLUMN oic_hearing_id TYPE VARCHAR(36);

alter table outcome
    ALTER COLUMN oic_hearing_id TYPE VARCHAR(36);

alter table punishment
    ALTER COLUMN activated_by TYPE VARCHAR(36);

alter table punishment
    ALTER COLUMN activated_from TYPE VARCHAR(36);
