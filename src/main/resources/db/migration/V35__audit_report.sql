drop table if exists reported_adjudication_status_audit;

create table reported_adjudication_status_audit
(
    id                             serial primary key,
    reported_adjudication_fk_id    bigint references reported_adjudications (id),
    status                         varchar(32)  not null,
    status_reason                  varchar(128),
    offence_codes                  varchar(4000) not null,
    status_details                 varchar(4000),
    create_user_id                 varchar(32)   not null,
    create_datetime                timestamp     not null,
    modify_user_id                 varchar(32),
    modify_datetime                timestamp
);

create index status_audit_reported_adjudication_fk_idx on reported_adjudication_status_audit(reported_adjudication_fk_id);







