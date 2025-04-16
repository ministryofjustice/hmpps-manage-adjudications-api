--temp indexes for updating location ids
create index incident_details_location_idx on incident_details (location_id);
