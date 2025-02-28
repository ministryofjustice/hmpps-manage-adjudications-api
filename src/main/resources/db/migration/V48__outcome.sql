drop table if exists outcome;

create table outcome
(
    id              serial primary key,
    code            varchar(32) not null,
    reason          varchar(32),
    details         varchar(4000),
    create_user_id  varchar(32) not null,
    create_datetime timestamp   not null,
    modify_user_id  varchar(32),
    modify_datetime timestamp
);

alter table reported_adjudications
    add column outcome_id integer null;
alter table reported_adjudications
    add constraint fk_adjudication_outcome foreign key (outcome_id) REFERENCES outcome (id);
create index reported_adjudications_outcome_idx on reported_adjudications (outcome_id);








