drop table if exists reported_adjudication_status;

create table reported_adjudication_status
(
    id                             serial primary key,
    reported_adjudication_fk_id    bigint references reported_adjudications (id),
    status                         varchar(32)  not null,
    status_reason                  varchar(128),
    status_details                 varchar(4000),
    snapshot                       varchar(10000),
    create_user_id                 varchar(32)   not null,
    create_datetime                timestamp     not null,
    modify_user_id                 varchar(32),
    modify_datetime                timestamp
);

create index reported_adjudication_status_fx_idx on reported_adjudication_status(reported_adjudication_fk_id);

INSERT INTO reported_adjudication_status
SELECT  nextval('reported_adjudication_status_id_seq'), id, status, status_reason, status_details, null,
        create_user_id, create_datetime, null, null FROM reported_adjudications;

ALTER TABLE reported_adjudications DROP COLUMN status_reason;
ALTER TABLE reported_adjudications DROP COLUMN status_details;
ALTER TABLE reported_adjudications ADD COLUMN draft_created_at timestamp;
UPDATE reported_adjudications SET draft_created_at = create_datetime

