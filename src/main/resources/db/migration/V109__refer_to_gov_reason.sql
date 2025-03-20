ALTER TABLE outcome ADD COLUMN refer_gov_reason varchar(32) null;
ALTER TABLE outcome RENAME COLUMN reason TO not_proceed_reason;
ALTER TABLE hearing_outcome RENAME COLUMN reason TO adjourn_reason;