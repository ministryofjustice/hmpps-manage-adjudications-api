drop table if exists offence;

create table offence
(
    id                          serial primary key,
    draft_adjudication_fk_id    bigint references draft_adjudications (id),
    offence_code                integer       not null,
    victim_prisoners_number     varchar(7)    null,
    create_user_id              varchar(32)   not null,
    create_datetime             timestamp     not null,
    modify_user_id              varchar(32),
    modify_datetime             timestamp
);

create index offence_draft_adjudication_fk_idx on offence(draft_adjudication_fk_id);


