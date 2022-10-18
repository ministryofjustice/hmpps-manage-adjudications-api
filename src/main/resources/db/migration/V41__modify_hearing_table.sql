ALTER TABLE hearing ADD COLUMN report_number bigint  not null;
ALTER TABLE hearing DROP COLUMN prisoner_number;