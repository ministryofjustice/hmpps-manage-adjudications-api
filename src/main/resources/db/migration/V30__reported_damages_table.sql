drop table if exists reported_damages;

create table reported_damages
(
    id                             serial primary key,
    reported_adjudication_fk_id    bigint references reported_adjudications (id),
    code                           varchar(32)   not null,
    details                        varchar(4000) not null,
    create_user_id                 varchar(32)   not null,
    create_datetime                timestamp     not null,
    modify_user_id                 varchar(32),
    modify_datetime                timestamp
);

create index damage_reported_adjudication_fk_idx on reported_damages(reported_adjudication_fk_id);





