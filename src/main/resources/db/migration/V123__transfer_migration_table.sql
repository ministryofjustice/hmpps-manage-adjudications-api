
create table transfer_migration_charges
(
    id                 serial primary key,
    charge_number      varchar(255),
    status             varchar(255)
);
