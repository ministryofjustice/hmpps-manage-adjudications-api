drop table if exists incident_details;

create table incident_details
(
    id                    serial primary key,
    date_time_of_incident timestamp   null,
    location_id           bigint      null,
    create_user_id        varchar(32) not null,
    create_datetime       timestamp   not null,
    modify_user_id        varchar(32),
    modify_datetime       timestamp
);

drop table if exists incident_statement;

create table incident_statement
(
    id                 serial primary key,
    statement          varchar(4000) null,
    create_user_id     varchar(32)   not null,
    create_datetime    timestamp     not null,
    modify_user_id     varchar(32),
    modify_datetime    timestamp
);


drop table if exists draft_adjudications;

create table draft_adjudications
(
    id                          serial primary key,
    prisoner_number             varchar(7)  not null,
    incident_details_id         integer     null,
    incident_statement_id       integer     null,
    create_user_id              varchar(32) not null,
    create_datetime             timestamp   not null,
    modify_user_id              varchar(32),
    modify_datetime             timestamp,
    constraint fk_incident_details foreign key (incident_details_id) REFERENCES incident_details (id),
    constraint fk_incident_statement foreign key (incident_statement_id) REFERENCES incident_statement (id)
);

create index incident_details_idx on draft_adjudications(incident_details_id);
create index incident_statement_idx on draft_adjudications(incident_statement_id);
