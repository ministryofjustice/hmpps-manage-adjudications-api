alter table reported_adjudications add column draft_created_on timestamp;
update reported_adjudications set draft_created_on = create_datetime;
