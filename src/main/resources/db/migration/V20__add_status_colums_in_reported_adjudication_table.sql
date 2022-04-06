alter table reported_adjudication add column status varchar(32) default 'AWAITING_REVIEW' not null;
alter table reported_adjudication add column statusReason varchar(64);
alter table reported_adjudication add column statusDetail varchar(1000);
