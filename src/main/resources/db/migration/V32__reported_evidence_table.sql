drop table if exists reported_evidence;

create table reported_evidence
(
    id                             serial primary key,
    reported_adjudication_fk_id    bigint references reported_adjudications (id),
    code                           varchar(32)   not null,
    identifier                     varchar(32),
    details                        varchar(4000) not null,
    reporter                       varchar(32)   not null,
    create_user_id                 varchar(32)   not null,
    create_datetime                timestamp     not null,
    modify_user_id                 varchar(32),
    modify_datetime                timestamp
);

create index evidence_reported_adjudication_fk_idx on reported_evidence(reported_adjudication_fk_id);





