drop table if exists protected_characteristics;

create table protected_characteristics
(
    id                     serial primary key,
    reported_offence_fk_id bigint references reported_offence (id),
    characteristic         varchar(20) not null,
    create_user_id         varchar(32) not null,
    create_datetime        timestamp   not null,
    modify_user_id         varchar(32),
    modify_datetime        timestamp
);

create index protected_characteristics_reported_offence_fk_idx on protected_characteristics (reported_offence_fk_id);


drop table if exists draft_protected_characteristics;

create table draft_protected_characteristics
(
    id              serial primary key,
    offence_fk_id   bigint references offence (id),
    characteristic  varchar(20) not null,
    create_user_id  varchar(32) not null,
    create_datetime timestamp   not null,
    modify_user_id  varchar(32),
    modify_datetime timestamp
);

create index protected_characteristics_offence_fk_idx on draft_protected_characteristics (offence_fk_id);
