alter table draft_adjudications
    alter column is_youth_offender drop not null;
alter table draft_adjudications
    alter column is_youth_offender drop default;