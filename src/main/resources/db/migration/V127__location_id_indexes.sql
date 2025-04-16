--temp indexes for updating location ids
create index incident_details_location_idx on incident_details (location_id);
create index reported_adjudications_location_idx on reported_adjudications (location_id);
create index hearing_location_idx on hearing (location_id);
