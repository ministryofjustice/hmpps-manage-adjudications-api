alter table draft_adjudications
    add column agency_id varchar(6) DEFAULT 'MDI' not null;