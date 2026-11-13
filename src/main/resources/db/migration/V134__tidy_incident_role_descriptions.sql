-- Reporting labels are standalone descriptions, not prefixes for offence text.
UPDATE constant_incident_role
SET description = rtrim(description, ':')
WHERE description LIKE '%:';
