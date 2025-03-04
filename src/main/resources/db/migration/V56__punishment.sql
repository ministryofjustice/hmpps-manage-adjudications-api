drop table if exists punishment;

create table punishment
(
    id                          serial primary key,
    reported_adjudication_fk_id bigint references reported_adjudications (id),
    type                        varchar(32) not null,
    privilege_type              varchar(32),
    other_privilege             varchar(32),
    stoppage_percentage         integer,
    create_user_id              varchar(32) not null,
    create_datetime             timestamp   not null,
    modify_user_id              varchar(32),
    modify_datetime             timestamp
);

create index punishment_reported_adjudication_fk_idx on punishment (reported_adjudication_fk_id);








