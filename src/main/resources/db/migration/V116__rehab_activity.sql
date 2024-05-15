drop table if exists rehabilitative_activity;

create table rehabilitative_activity
(
    id                             serial primary key,
    punishment_fk_id               bigint references punishment (id),
    details                        varchar(4000) not null,
    monitor                        varchar(32) not null,
    end_date                       timestamp     not null,
    total_sessions                 integer null,
    completed                      boolean null,
    outcome                        varchar(32)   null,
    create_user_id                 varchar(32)   not null,
    create_datetime                timestamp     not null,
    modify_user_id                 varchar(32),
    modify_datetime                timestamp
);

create index rehabilitative_activity_punishment_fk_idx on rehabilitative_activity(punishment_fk_id);
