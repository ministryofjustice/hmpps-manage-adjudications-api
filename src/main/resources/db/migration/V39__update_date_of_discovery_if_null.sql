update incident_details set date_time_of_discovery = date_time_of_incident where date_time_of_discovery is null;
update reported_adjudications set date_time_of_discovery = date_time_of_incident where date_time_of_discovery is null;
