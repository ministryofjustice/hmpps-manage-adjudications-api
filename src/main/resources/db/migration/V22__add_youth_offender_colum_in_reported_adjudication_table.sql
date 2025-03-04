alter table reported_adjudications
    add column is_youth_offender boolean default false not null;
