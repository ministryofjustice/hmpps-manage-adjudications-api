drop table if exists hearing;

create table hearing
(
    id                             serial primary key,
    reported_adjudication_fk_id    bigint references reported_adjudications (id),
    agency_id                      varchar(6) not null,
    prisoner_number                varchar(7)  not null,
    location_id                    bigint     not null,
    date_time_of_hearing           timestamp   not null,
    create_user_id                 varchar(32)   not null,
    create_datetime                timestamp     not null,
    modify_user_id                 varchar(32),
    modify_datetime                timestamp
);

create index hearings_adjudication_fk_idx on hearing(reported_adjudication_fk_id);

create index hearing_agency_idx on hearing(agency_id,date_time_of_hearing);






