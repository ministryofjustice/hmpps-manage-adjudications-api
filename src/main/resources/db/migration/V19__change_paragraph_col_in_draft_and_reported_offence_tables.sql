alter table offence
    add column paragraph_code varchar(6) DEFAULT '1' not null;
alter table offence drop column paragraph_number;

alter table reported_offence
    add column paragraph_code varchar(6) DEFAULT '1' not null;
alter table reported_offence drop column paragraph_number;