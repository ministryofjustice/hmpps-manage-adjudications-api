drop table if exists hearing_pre_migrate;

create table hearing_pre_migrate
(
    id                             serial primary key,
    location_id                    bigint     not null,
    date_time_of_hearing           timestamp   not null,
    oic_hearing_type               varchar(32) not null
);

drop table if exists hearing_outcome_pre_migrate;

create table hearing_outcome_pre_migrate
(
    id                             serial primary key,
    code                           varchar(32) not null,
    adjudicator                    varchar(32) not null,
    plea                           varchar(32)
);

drop table if exists punishment_pre_migrate;

create table punishment_pre_migrate
(
    id                             serial primary key,
    type                           varchar(32) not null,
    privilege_type                 varchar(32),
    other_privilege                varchar(32),
    stoppage_percentage            integer,
    consecutive_charge_number      bigint NULL,
    amount                         decimal(10,2) NULL
);


ALTER TABLE punishment_schedule ADD COLUMN migrated boolean not null DEFAULT false;

ALTER TABLE punishment ADD COLUMN nomis_status varchar(32) null;

alter table hearing
    add column hearing_pre_migrate_id integer null;
alter table hearing
    add constraint fk_hearing_pre_migrate foreign key (hearing_pre_migrate_id) REFERENCES hearing_pre_migrate(id);
create index hearing_pre_migrate_id_idx on hearing(hearing_pre_migrate_id);

alter table hearing_outcome
    add column hearing_outcome_pre_migrate_id integer null;
alter table hearing_outcome
    add constraint fk_hearing_outcome_pre_migrate foreign key (hearing_outcome_pre_migrate_id) REFERENCES hearing_outcome_pre_migrate(id);
create index hearing_outcome_pre_migrate_id_idx on hearing_outcome(hearing_outcome_pre_migrate_id);

alter table punishment
    add column punishment_pre_migrate_id integer null;
alter table punishment
    add constraint fk_punishment_pre_migrate foreign key (punishment_pre_migrate_id) REFERENCES punishment_pre_migrate(id);
create index punishment_pre_migrate_id_idx on punishment(punishment_pre_migrate_id);