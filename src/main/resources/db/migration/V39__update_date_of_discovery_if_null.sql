update incident_details set date_time_of_discovery = date_time_of_incident where date_time_of_discovery is null;
update reported_adjudications set date_time_of_discovery = date_time_of_incident where date_time_of_discovery is null;

alter table incident_details alter column date_time_of_discovery set not null;
alter table reported_adjudications alter column date_time_of_discovery set not null;