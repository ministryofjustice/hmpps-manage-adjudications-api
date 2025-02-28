alter table submitted_adjudication_history
    add column agency_id varchar(6) DEFAULT 'MDI' not null;
