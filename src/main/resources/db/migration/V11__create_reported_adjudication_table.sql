drop table if exists reported_adjudications;

create table reported_adjudications
(
    id                          serial primary key,
    prisoner_number             varchar(7)  not null,
    booking_id                  bigint      not null,
    report_number               bigint      not null,
    agency_id                   varchar(6)  not null,
    date_time_of_incident       timestamp   not null,
    handover_deadline           timestamp   not null,
    location_id                 bigint      not null,
    statement                   varchar     not null,
    create_user_id              varchar(32) not null,
    create_datetime             timestamp   not null,
    modify_user_id              varchar(32),
    modify_datetime             timestamp,
    constraint unique_report_number UNIQUE (report_number)
);
