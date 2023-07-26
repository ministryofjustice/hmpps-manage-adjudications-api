drop table if exists additional_associate;

create table additional_associate
(
    id                                        serial primary key,
    reported_adjudication_fk_id               bigint references reported_adjudications (id),
    incident_role_associated_prisoners_number varchar (7) null,
    incident_role_associated_prisoners_name   varchar(100) null,
    create_user_id                            varchar(32)   not null,
    create_datetime                           timestamp     not null,
    modify_user_id                            varchar(32),
    modify_datetime                           timestamp
);

create index associate_reported_adjudication_fk_idx on additional_associate(reported_adjudication_fk_id);

drop table if exists additional_victim;

create table additional_victim
(
    id                                        serial primary key,
    reported_offence_fk_id                    bigint references reported_offence (id),
    victim_prisoners_number                   varchar(7)    null,
    victim_other_persons_name                 varchar(100) null,
    victim_staff_username                     varchar(30) null,
    create_user_id                            varchar(32)   not null,
    create_datetime                           timestamp     not null,
    modify_user_id                            varchar(32),
    modify_datetime                           timestamp
);

create index victim_reported_offence_fk_idx on additional_victim(reported_offence_fk_id);





