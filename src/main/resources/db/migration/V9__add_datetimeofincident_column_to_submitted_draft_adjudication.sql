alter table submitted_adjudication_history
    add column date_time_of_incident timestamp DEFAULT CURRENT_TIMESTAMP not null;