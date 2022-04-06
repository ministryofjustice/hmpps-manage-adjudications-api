alter table reported_adjudications add column status varchar(32) default 'AWAITING_REVIEW' not null;
alter table reported_adjudications add column statusReason varchar(128);
alter table reported_adjudications add column statusDetail varchar(4000);
