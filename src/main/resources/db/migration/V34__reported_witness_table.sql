drop table if exists reported_witness;

create table reported_witness
(
    id                          serial primary key,
    draft_adjudication_fk_id    bigint references draft_adjudications (id),
    code                        varchar(32)   not null,
    first_name                  varchar(32)   not null,
    last_name                   varchar(32)   not null,
    reporter                    varchar(32)   not null,
    create_user_id              varchar(32)   not null,
    create_datetime             timestamp     not null,
    modify_user_id              varchar(32),
    modify_datetime             timestamp
);

create index reported_witness_draft_adjudication_fk_idx on reported_witness(draft_adjudication_fk_id);





