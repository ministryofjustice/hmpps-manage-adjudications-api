ALTER TABLE outcome DROP COLUMN amount;
ALTER TABLE outcome DROP COLUMN caution;

ALTER TABLE punishment ADD COLUMN sanction_seq bigint;
ALTER TABLE punishment ADD COLUMN amount decimal(10,2);