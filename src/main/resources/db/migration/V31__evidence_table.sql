drop table if exists evidence;

create table evidence
(
    id                          serial primary key,
    draft_adjudication_fk_id    bigint references draft_adjudications (id),
    code                        varchar(32)   not null,
    identifier                  varchar(32),
    details                     varchar(4000) not null,
    reporter                    varchar(32)   not null,
    create_user_id              varchar(32)   not null,
    create_datetime             timestamp     not null,
    modify_user_id              varchar(32),
    modify_datetime             timestamp
);

create index evidence_draft_adjudication_fk_idx on evidence(draft_adjudication_fk_id);





