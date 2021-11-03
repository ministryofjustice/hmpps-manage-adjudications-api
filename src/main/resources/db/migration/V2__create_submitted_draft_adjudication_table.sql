drop table if exists submitted_draft_adjudications;

create table submitted_draft_adjudications
(
    id                  serial primary key,
    adjudication_number bigint      not null,
    date_time_sent      timestamp   not null,
    create_user_id      varchar(32) not null,
    create_datetime     timestamp   not null,
    modify_user_id      varchar(32),
    modify_datetime     timestamp
);