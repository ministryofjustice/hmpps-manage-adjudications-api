drop table if exists punishment_comments;

create table punishment_comments
(
    id                             serial primary key,
    reported_adjudication_fk_id    bigint references reported_adjudications (id),
    comment                        varchar(4000) not null,
    create_user_id                 varchar(32)   not null,
    create_datetime                timestamp     not null,
    modify_user_id                 varchar(32),
    modify_datetime                timestamp
);

create index punishment_reported_adjudication_fk_idx on punishment_comments(reported_adjudication_fk_id);
