drop table if exists damages;

create table damages
(
    id                       serial primary key,
    draft_adjudication_fk_id bigint references draft_adjudications (id),
    code                     varchar(32)   not null,
    details                  varchar(4000) not null,
    reporter                 varchar(32)   not null,
    create_user_id           varchar(32)   not null,
    create_datetime          timestamp     not null,
    modify_user_id           varchar(32),
    modify_datetime          timestamp
);

create index damage_draft_adjudication_fk_idx on damages (draft_adjudication_fk_id);





