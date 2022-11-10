ALTER TABLE draft_adjudications ADD COLUMN gender varchar(12) default 'MALE' not null;
ALTER TABLE reported_adjudications ADD COLUMN gender varchar(12) default 'MALE' not null;