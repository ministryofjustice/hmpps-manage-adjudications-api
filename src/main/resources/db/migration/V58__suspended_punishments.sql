ALTER TABLE punishment
    ADD COLUMN activated_from bigint;
ALTER TABLE punishment
    ADD COLUMN activated_by bigint;
ALTER TABLE punishment
    ADD COLUMN suspended_until date;

CREATE index prisoner_suspended_punishment_idx ON punishment (suspended_until);
CREATE index prisoner_reported_adjudication_idx ON reported_adjudications (prisoner_number);




