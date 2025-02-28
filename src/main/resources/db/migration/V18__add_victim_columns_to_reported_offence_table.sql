alter table reported_offence
    add column victim_staff_username varchar(30) null; -- As per STAFF_USER_ACCOUNTS.USERNAME
alter table reported_offence
    add column victim_other_persons_name varchar(100) null;

alter table reported_offence
    add column paragraph_number varchar(6) DEFAULT '1' not null;