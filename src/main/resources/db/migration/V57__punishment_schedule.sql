drop table if exists punishment_schedule;

create table punishment_schedule
(
    id                             serial primary key,
    punishment_fk_id    bigint references punishment (id),
    days                           integer not null,
    start_date                     date,
    end_date                       date,
    suspended_until                date,
    create_user_id                 varchar(32)   not null,
    create_datetime                timestamp     not null,
    modify_user_id                 varchar(32),
    modify_datetime                timestamp
);

create index punishment_schedule_fk_idx on punishment_schedule(punishment_fk_id);








