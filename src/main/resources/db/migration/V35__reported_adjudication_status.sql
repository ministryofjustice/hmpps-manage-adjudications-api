drop table if exists reported_adjudication_status_audit;

create table reported_adjudication_status_audit
(
    id                             serial primary key,
    reported_adjudication_fk_id    bigint references reported_adjudications (id),
    status                         varchar(32)  not null,
    status_reason                  varchar(128),
    status_details                 varchar(4000),
    create_user_id                 varchar(32)   not null,
    create_datetime                timestamp     not null,
    modify_user_id                 varchar(32),
    modify_datetime                timestamp
);

create index reported_adjudication_status_fx_idx on reported_adjudication_status_audit(reported_adjudication_fk_id);

INSERT INTO reported_adjudication_status_audit
SELECT  nextval('reported_adjudication_status_audit_id_seq'), id, status, status_reason, status_details,
        create_user_id, create_datetime, null, null FROM reported_adjudications;

ALTER TABLE reported_adjudications DROP COLUMN status_reason;
ALTER TABLE reported_adjudications DROP COLUMN status_details;
ALTER TABLE reported_adjudications ADD COLUMN draft_created_at timestamp;
UPDATE reported_adjudications SET draft_created_at = create_datetime

