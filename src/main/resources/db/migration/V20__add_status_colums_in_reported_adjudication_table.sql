alter table reported_adjudications add column status varchar(32) default 'AWAITING_REVIEW' not null;
alter table reported_adjudications add column status_reason varchar(128);
alter table reported_adjudications add column status_details varchar(4000);
