alter table incident_details
    add column date_time_of_discovery timestamp;
alter table reported_adjudications
    add column date_time_of_discovery timestamp;