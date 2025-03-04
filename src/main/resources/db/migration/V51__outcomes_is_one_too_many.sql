alter table reported_adjudications drop constraint fk_adjudication_outcome;
alter table reported_adjudications drop column outcome_id;
drop index if exists reported_adjudications_outcome_idx;
alter table outcome
    ADD COLUMN reported_adjudication_fk_id bigint references reported_adjudications (id);
create index outcomes_adjudication_fk_idx on outcome (reported_adjudication_fk_id);

