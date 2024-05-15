alter table punishment_schedule rename column days to duration;
alter table punishment_schedule add column measurement varchar(16) not null default 'DAYS';
alter table punishment add column payback_notes varchar(4000) null;
