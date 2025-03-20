drop table if exists reported_offence;

create table reported_offence
(
    id                          serial primary key,
    reported_adjudication_fk_id bigint references reported_adjudications (id),
    offence_code                integer       not null,
    victim_prisoners_number     varchar(7)    null,
    create_user_id              varchar(32)   not null,
    create_datetime             timestamp     not null,
    modify_user_id              varchar(32),
    modify_datetime             timestamp
);

create index offence_reported_adjudication_fk_idx on reported_offence(reported_adjudication_fk_id);


