create index associate_merge_idx on reported_adjudications (incident_role_associated_prisoners_number);
create index draft_prisoner_idx on draft_adjudications (prisoner_number);
create index draft_associated_merge_idx on incident_role (associated_prisoners_number);
create index draft_victim_idx on offence (victim_prisoners_number);
create index reported_merge_idx on reported_offence (victim_prisoners_number);
