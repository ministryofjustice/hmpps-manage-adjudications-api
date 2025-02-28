ALTER TABLE reported_adjudications ALTER COLUMN report_number TYPE VARCHAR(16);
ALTER TABLE reported_adjudications DROP CONSTRAINT unique_report_number;
ALTER TABLE reported_adjudications RENAME COLUMN report_number TO charge_number;
ALTER TABLE reported_adjudications
    ADD CONSTRAINT unique_report_number UNIQUE (charge_number);
ALTER TABLE punishment ALTER COLUMN consecutive_report_number TYPE VARCHAR(16);
ALTER TABLE punishment RENAME COLUMN consecutive_report_number TO consecutive_charge_number;
ALTER TABLE punishment ALTER COLUMN activated_from TYPE VARCHAR(16);
ALTER TABLE punishment RENAME COLUMN activated_from TO activated_from_charge_number;
ALTER TABLE punishment ALTER COLUMN activated_by TYPE VARCHAR(16);
ALTER TABLE punishment RENAME COLUMN activated_by TO activated_by_charge_number;
ALTER TABLE hearing ALTER COLUMN report_number TYPE VARCHAR(16);
ALTER TABLE hearing RENAME COLUMN report_number TO charge_number;
ALTER TABLE draft_adjudications ALTER COLUMN report_number TYPE VARCHAR(16);
ALTER TABLE draft_adjudications RENAME COLUMN report_number TO charge_number;
DROP INDEX IF EXISTS consecutive_report_number_and_type_idx;
CREATE INDEX IF NOT EXISTS consecutive_report_number_and_type_idx ON punishment(consecutive_charge_number, type);