drop table if exists dis_issue_history;

CREATE TABLE dis_issue_history
(
    id                          serial primary key,
    reported_adjudication_fk_id bigint references reported_adjudications (id),
    issuing_officer             varchar(32),
    date_time_of_issue          timestamp,
    create_user_id              varchar(32) not null,
    create_datetime             timestamp   not null,
    modify_user_id              varchar(32),
    modify_datetime             timestamp
);

create index dis_issue_history_adjudication_fk_idx on dis_issue_history (reported_adjudication_fk_id);
