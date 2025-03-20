drop table if exists hearing_outcome;

create table hearing_outcome
(
    id                             serial primary key,
    code                           varchar(32) not null,
    adjudicator                    varchar(32) not null,
    reason                         varchar(32),
    plea                           varchar(32),
    details                        varchar(4000),
    create_user_id                 varchar(32)   not null,
    create_datetime                timestamp     not null,
    modify_user_id                 varchar(32),
    modify_datetime                timestamp
);

alter table hearing
    add column outcome_id integer null;
alter table hearing
    add constraint fk_hearing_outcome foreign key (outcome_id) REFERENCES hearing_outcome (id);
create index hearing_outcome_idx on hearing(outcome_id);





