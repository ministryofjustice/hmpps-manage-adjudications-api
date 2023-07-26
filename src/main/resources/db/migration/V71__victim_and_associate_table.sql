drop table if exists associate;

create table associate
(
    id                                        serial primary key,
    reported_adjudication_fk_id               bigint references reported_adjudications (id),
    incident_role_associated_prisoners_number varchar (7) null,
    incident_role_associated_prisoners_name   varchar(100) null
    create_user_id                            varchar(32)   not null,
    create_datetime                           timestamp     not null,
    modify_user_id                            varchar(32),
    modify_datetime                           timestamp
);

create index associate_reported_adjudication_fk_idx on associate(reported_adjudication_fk_id);





