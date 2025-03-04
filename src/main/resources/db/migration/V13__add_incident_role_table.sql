drop table if exists incident_role;

create table incident_role
(
    id                          serial primary key,
    role_code                   varchar(5) null,
    associated_prisoners_number varchar(7) null,
    create_user_id              varchar(32) not null,
    create_datetime             timestamp   not null,
    modify_user_id              varchar(32),
    modify_datetime             timestamp
);

alter table draft_adjudications
    add column incident_role_id integer null;
alter table draft_adjudications
    add constraint fk_incident_role foreign key (incident_role_id) REFERENCES incident_role (id);
create index incident_role_idx on draft_adjudications (incident_role_id);


