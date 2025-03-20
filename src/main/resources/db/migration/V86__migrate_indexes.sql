create index migrate_1 on reported_adjudications(originating_agency_id);
create index migrate_2 on reported_adjudications(originating_agency_id, migrated);
