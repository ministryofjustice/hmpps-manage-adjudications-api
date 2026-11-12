-- Reporting reference data. Application behaviour continues to use its Kotlin enums.
-- Update this data with a new forward-only migration when the reporting catalogue changes.

CREATE TABLE constant_punishment_type (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_punishment_type IS 'Reporting labels for punishment type. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_punishment_type.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_punishment_type.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_punishment_type.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_punishment_type (code, description, sequence) VALUES
    ('PRIVILEGE', 'Loss of privileges', 0),
    ('EARNINGS', 'Stoppage of earnings', 1),
    ('CONFINEMENT', 'Cellular confinement', 2),
    ('REMOVAL_ACTIVITY', 'Removal from activity', 3),
    ('EXCLUSION_WORK', 'Exclusion from associated work', 4),
    ('EXTRA_WORK', 'Extra work', 5),
    ('REMOVAL_WING', 'Removal from wing or unit', 6),
    ('ADDITIONAL_DAYS', 'Additional days', 7),
    ('PROSPECTIVE_DAYS', 'Prospective additional days', 8),
    ('CAUTION', 'Caution', 9),
    ('DAMAGES_OWED', 'Recovery of money for damages', 10),
    ('PAYBACK', 'Payback punishment', 11),
    ('RESTRICTION_OF_SOCIAL_VISITS', 'Restriction of social visits', 12),
    ('LOSS_OF_SOCIAL_VISITS', 'Loss of social visits', 13);

CREATE TABLE constant_privilege_type (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_privilege_type IS 'Reporting labels for privilege type. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_privilege_type.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_privilege_type.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_privilege_type.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_privilege_type (code, description, sequence) VALUES
    ('CANTEEN', 'Canteen', 0),
    ('FACILITIES', 'Facilities', 1),
    ('MONEY', 'Money', 2),
    ('TV', 'TV', 3),
    ('ASSOCIATION', 'Association', 4),
    ('GYM', 'Gym', 5),
    ('OTHER', 'Other', 6);

CREATE TABLE constant_not_completed_outcome (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_not_completed_outcome IS 'Reporting labels for not completed outcome. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_not_completed_outcome.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_not_completed_outcome.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_not_completed_outcome.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_not_completed_outcome (code, description, sequence) VALUES
    ('FULL_ACTIVATE', 'Suspended punishment activated', 0),
    ('PARTIAL_ACTIVATE', 'Suspended punishment activated for a specified number of days', 1),
    ('EXT_SUSPEND', 'Suspension end date extended', 2),
    ('NO_ACTION', 'No further action', 3);

CREATE TABLE constant_measurement (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_measurement IS 'Reporting labels for measurement. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_measurement.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_measurement.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_measurement.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_measurement (code, description, sequence) VALUES
    ('DAYS', 'Days', 0),
    ('HOURS', 'Hours', 1);

CREATE TABLE constant_reason_for_change (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_reason_for_change IS 'Reporting labels for reason for change. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_reason_for_change.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_reason_for_change.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_reason_for_change.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_reason_for_change (code, description, sequence) VALUES
    ('APPEAL', 'The punishments have been changed after an appeal', 0),
    ('CORRECTION', 'To make a correction', 1),
    ('OTHER', 'Other', 2),
    ('GOV_OR_DIRECTOR', 'A Governor or Director has decided to terminate or change punishments for another reason', 3);

CREATE TABLE constant_reported_adjudication_status (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_reported_adjudication_status IS 'Reporting labels for reported adjudication status. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_reported_adjudication_status.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_reported_adjudication_status.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_reported_adjudication_status.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_reported_adjudication_status (code, description, sequence) VALUES
    ('ACCEPTED', 'Accepted', 0),
    ('REJECTED', 'Rejected', 1),
    ('AWAITING_REVIEW', 'Awaiting review', 2),
    ('RETURNED', 'Returned', 3),
    ('UNSCHEDULED', 'Unscheduled', 4),
    ('SCHEDULED', 'Scheduled', 5),
    ('REFER_POLICE', 'Referred to police', 6),
    ('REFER_INAD', 'Referred to independent adjudicator', 7),
    ('REFER_GOV', 'Referred to governor', 8),
    ('PROSECUTION', 'Police prosecution', 9),
    ('DISMISSED', 'Dismissed', 10),
    ('NOT_PROCEED', 'Not proceeded with', 11),
    ('ADJOURNED', 'Adjourned', 12),
    ('CHARGE_PROVED', 'Charge proved', 13),
    ('QUASHED', 'Quashed', 14),
    ('INVALID_OUTCOME', 'Invalid outcome', 15),
    ('INVALID_SUSPENDED', 'Invalid suspended', 16),
    ('INVALID_ADA', 'Invalid added days', 17);

CREATE TABLE constant_gender (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_gender IS 'Reporting labels for gender. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_gender.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_gender.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_gender.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_gender (code, description, sequence) VALUES
    ('MALE', 'Male', 0),
    ('FEMALE', 'Female', 1);

CREATE TABLE constant_oic_hearing_type (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_oic_hearing_type IS 'Reporting labels for oic hearing type. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_oic_hearing_type.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_oic_hearing_type.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_oic_hearing_type.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_oic_hearing_type (code, description, sequence) VALUES
    ('GOV_ADULT', 'Governor (adult)', 0),
    ('GOV_YOI', 'Governor (YOI)', 1),
    ('INAD_YOI', 'Independent adjudicator (YOI)', 2),
    ('GOV', 'Governor', 3),
    ('INAD_ADULT', 'Independent adjudicator (adult)', 4);

CREATE TABLE constant_hearing_outcome_code (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_hearing_outcome_code IS 'Reporting labels for hearing outcome code. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_hearing_outcome_code.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_hearing_outcome_code.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_hearing_outcome_code.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_hearing_outcome_code (code, description, sequence) VALUES
    ('COMPLETE', 'Hearing complete', 0),
    ('REFER_POLICE', 'Refer to police', 1),
    ('REFER_INAD', 'Refer to independent adjudicator', 2),
    ('ADJOURN', 'Adjourn hearing', 3),
    ('NOMIS', 'NOMIS outcome', 4),
    ('REFER_GOV', 'Refer to governor', 5);

CREATE TABLE constant_hearing_outcome_plea (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_hearing_outcome_plea IS 'Reporting labels for hearing outcome plea. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_hearing_outcome_plea.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_hearing_outcome_plea.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_hearing_outcome_plea.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_hearing_outcome_plea (code, description, sequence) VALUES
    ('UNFIT', 'Unfit', 0),
    ('ABSTAIN', 'Abstain', 1),
    ('GUILTY', 'Guilty', 2),
    ('NOT_GUILTY', 'Not guilty', 3),
    ('NOT_ASKED', 'Not asked', 4);

CREATE TABLE constant_hearing_outcome_adjourn_reason (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_hearing_outcome_adjourn_reason IS 'Reporting labels for hearing outcome adjourn reason. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_hearing_outcome_adjourn_reason.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_hearing_outcome_adjourn_reason.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_hearing_outcome_adjourn_reason.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_hearing_outcome_adjourn_reason (code, description, sequence) VALUES
    ('LEGAL_ADVICE', 'Request for legal advice approved', 0),
    ('LEGAL_REPRESENTATION', 'Request for legal representation approved', 1),
    ('RO_ATTEND', 'Adjourned so that reporting officer can attend', 2),
    ('HELP', 'Prisoner needs help, such as an interpreter or disability aid', 3),
    ('UNFIT', 'Prisoner unfit to proceed', 4),
    ('WITNESS', 'Request for witness approved', 5),
    ('WITNESS_SUPPORT', 'Request for witness to support mitigation', 6),
    ('MCKENZIE', 'Request for McKenzie friend approved', 7),
    ('EVIDENCE', 'Further evidence needed', 8),
    ('INVESTIGATION', 'Further investigation needed', 9),
    ('OTHER', 'Other reason', 10);

CREATE TABLE constant_outcome_code (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_outcome_code IS 'Reporting labels for outcome code. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_outcome_code.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_outcome_code.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_outcome_code.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_outcome_code (code, description, sequence) VALUES
    ('REFER_POLICE', 'Police referral', 0),
    ('REFER_INAD', 'Independent adjudicator referral', 1),
    ('REFER_GOV', 'Governor referral', 2),
    ('NOT_PROCEED', 'Not proceeded with', 3),
    ('DISMISSED', 'Dismissed', 4),
    ('PROSECUTION', 'Prosecution', 5),
    ('SCHEDULE_HEARING', 'Hearing scheduled', 6),
    ('CHARGE_PROVED', 'Charge proved', 7),
    ('QUASHED', 'Guilty finding quashed', 8);

CREATE TABLE constant_not_proceed_reason (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_not_proceed_reason IS 'Reporting labels for not proceed reason. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_not_proceed_reason.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_not_proceed_reason.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_not_proceed_reason.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_not_proceed_reason (code, description, sequence) VALUES
    ('ANOTHER_WAY', 'Resolved in another way', 0),
    ('RELEASED', 'Prisoner released', 1),
    ('WITNESS_NOT_ATTEND', 'Witness unable to attend', 2),
    ('UNFIT', 'Prisoner mentally or physically unfit to attend', 3),
    ('FLAWED', 'Flawed notice of report', 4),
    ('EXPIRED_NOTICE', 'Notice of report issued more than 48 hours after discovery', 5),
    ('EXPIRED_HEARING', 'Hearing open outside timeframe', 6),
    ('NOT_FAIR', 'Not fair to continue', 7),
    ('OTHER', 'Other', 8);

CREATE TABLE constant_quashed_reason (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_quashed_reason IS 'Reporting labels for quashed reason. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_quashed_reason.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_quashed_reason.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_quashed_reason.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_quashed_reason (code, description, sequence) VALUES
    ('FLAWED_CASE', 'Flawed case', 0),
    ('JUDICIAL_REVIEW', 'Quashed on judicial review', 1),
    ('APPEAL_UPHELD', 'Prisoner appeal upheld', 2),
    ('OTHER', 'Other', 3);

CREATE TABLE constant_refer_gov_reason (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_refer_gov_reason IS 'Reporting labels for refer gov reason. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_refer_gov_reason.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_refer_gov_reason.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_refer_gov_reason.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_refer_gov_reason (code, description, sequence) VALUES
    ('REVIEW_FOR_REFER_POLICE', 'To review for police referral', 0),
    ('GOV_INQUIRY', 'For governor inquiry', 1),
    ('OTHER', 'Other', 2);

CREATE TABLE constant_damage_code (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_damage_code IS 'Reporting labels for damage code. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_damage_code.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_damage_code.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_damage_code.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_damage_code (code, description, sequence) VALUES
    ('ELECTRICAL_REPAIR', 'Electrical', 0),
    ('PLUMBING_REPAIR', 'Plumbing', 1),
    ('FURNITURE_OR_FABRIC_REPAIR', 'Furniture or fabric', 2),
    ('LOCK_REPAIR', 'Lock', 3),
    ('REDECORATION', 'Redecoration', 4),
    ('CLEANING', 'Cleaning', 5),
    ('REPLACE_AN_ITEM', 'Replacing an item', 6);

CREATE TABLE constant_evidence_code (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_evidence_code IS 'Reporting labels for evidence code. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_evidence_code.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_evidence_code.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_evidence_code.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_evidence_code (code, description, sequence) VALUES
    ('PHOTO', 'Photo', 0),
    ('BODY_WORN_CAMERA', 'Body-worn camera', 1),
    ('CCTV', 'CCTV', 2),
    ('BAGGED_AND_TAGGED', 'Bagged and tagged', 3),
    ('OTHER', 'Other', 4);

CREATE TABLE constant_witness_code (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_witness_code IS 'Reporting labels for witness code. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_witness_code.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_witness_code.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_witness_code.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_witness_code (code, description, sequence) VALUES
    ('OFFICER', 'Prison officer', 0),
    ('STAFF', 'Member of staff who''s not a prison officer', 1),
    ('OTHER_PERSON', 'Other person', 2),
    ('VICTIM', 'Victim', 3),
    ('PRISONER', 'Prisoner', 4);

CREATE TABLE constant_characteristic (
    code text PRIMARY KEY,
    description text NOT NULL,
    sequence integer NOT NULL UNIQUE
);
COMMENT ON TABLE constant_characteristic IS 'Reporting labels for characteristic. Reporting labels can differ from SAR wording; SAR descriptions remain unchanged.';
COMMENT ON COLUMN constant_characteristic.code IS 'Stored enum code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_characteristic.description IS 'General reporting label for this code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_characteristic.sequence IS 'Display order in the reporting catalogue. [Sensitivity: NONE]';
INSERT INTO constant_characteristic (code, description, sequence) VALUES
    ('AGE', 'Age', 0),
    ('DISABILITY', 'Disability', 1),
    ('GENDER_REASSIGN', 'Gender reassignment', 2),
    ('MARRIAGE_AND_CP', 'Marriage and civil partnership', 3),
    ('PREGNANCY_AND_MAT', 'Pregnancy and maternity', 4),
    ('RACE', 'Race', 5),
    ('RELIGION', 'Religion or belief', 6),
    ('SEX', 'Sex', 7),
    ('SEX_ORIENTATION', 'Sexual orientation', 8);

CREATE TABLE constant_offence (
    offence_code integer NOT NULL,
    rule_set text NOT NULL CHECK (rule_set IN ('ADULT', 'YOI')),
    nomis_code text NOT NULL,
    nomis_code_with_others text NOT NULL,
    paragraph text NOT NULL,
    description text NOT NULL,
    retired boolean NOT NULL,
    PRIMARY KEY (offence_code, rule_set)
 );
COMMENT ON TABLE constant_offence IS 'Reporting offence reference. Join on offence_code and the adjudication rule set selected by is_youth_offender. Code 0 is a migrated sentinel: use the NOMIS code and description stored on reported_offence. Drafts with no rule set remain unresolved.';
COMMENT ON COLUMN constant_offence.offence_code IS 'Stored offence code; excludes migrated sentinel 0. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_offence.rule_set IS 'ADULT or YOI; selected using the adjudication is_youth_offender flag. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_offence.nomis_code IS 'NOMIS offence code for this rule set. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_offence.nomis_code_with_others IS 'NOMIS offence code when others are involved; defaults to the base NOMIS code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_offence.paragraph IS 'Applicable rule paragraph. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_offence.description IS 'Neutral reporting description, without HTML or pronoun placeholders. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_offence.retired IS 'True if this offence is not selectable in the current rule version; retained for history. [Sensitivity: NONE]';
INSERT INTO constant_offence (offence_code, rule_set, nomis_code, nomis_code_with_others, paragraph, description, retired) VALUES
    (1001, 'ADULT', '51:1A', '51:1A', '1(a)', 'Commits any racially aggravated assault', TRUE),
    (1003, 'ADULT', '51:1A', '51:1A', '1(a)', 'Commits any racially aggravated assault', TRUE),
    (1005, 'ADULT', '51:1A', '51:1A', '1(a)', 'Commits any racially aggravated assault', TRUE),
    (1021, 'ADULT', '51:1A', '51:1A', '1(a)', 'Commits any racially aggravated assault', TRUE),
    (1007, 'ADULT', '51:1A', '51:1A', '1(a)', 'Commits any racially aggravated assault', TRUE),
    (100124, 'ADULT', '51:1A (24)', '51:1A (24)', '1(a)', 'Commits any assault aggravated by a protected characteristic', FALSE),
    (100324, 'ADULT', '51:1A (24)', '51:1A (24)', '1(a)', 'Commits any assault aggravated by a protected characteristic', FALSE),
    (100524, 'ADULT', '51:1A (24)', '51:1A (24)', '1(a)', 'Commits any assault aggravated by a protected characteristic', FALSE),
    (102124, 'ADULT', '51:1A (24)', '51:1A (24)', '1(a)', 'Commits any assault aggravated by a protected characteristic', FALSE),
    (100724, 'ADULT', '51:1A (24)', '51:1A (24)', '1(a)', 'Commits any assault aggravated by a protected characteristic', FALSE),
    (1002, 'ADULT', '51:1B', '51:25D', '1', 'Commits any assault', FALSE),
    (1022, 'ADULT', '51:1B', '51:25D', '1', 'Commits any assault', FALSE),
    (102224, 'ADULT', '51:1B (24)', '51:1B (24)', '1(b)', 'Commits any sexual assault', FALSE),
    (102324, 'ADULT', '51:1C (24)', '51:1C (24)', '1(c)', 'Exposes themselves, or commits any other indecent or obscene act', FALSE),
    (102424, 'ADULT', '51:1D (24)', '51:1D (24)', '1(d)', 'Sexually harasses any person', FALSE),
    (1004, 'ADULT', '51:1J', '51:25F', '1', 'Commits any assault', FALSE),
    (1006, 'ADULT', '51:1N', '51:25G', '1', 'Commits any assault', FALSE),
    (1008, 'ADULT', '51:1F', '51:25E', '1', 'Commits any assault', FALSE),
    (4001, 'ADULT', '51:4', '51:4', '4', 'Fights with any person', FALSE),
    (4002, 'ADULT', '51:4', '51:4', '4', 'Fights with any person', FALSE),
    (4003, 'ADULT', '51:4', '51:4', '4', 'Fights with any person', FALSE),
    (4004, 'ADULT', '51:4', '51:4', '4', 'Fights with any person', FALSE),
    (4005, 'ADULT', '51:4', '51:4', '4', 'Fights with any person', FALSE),
    (5001, 'ADULT', '51:5', '51:25Q', '5', 'Intentionally endangers the health or personal safety of others or, by the person''s conduct, is reckless whether such health or personal safety is endangered', FALSE),
    (5002, 'ADULT', '51:5', '51:25Q', '5', 'Intentionally endangers the health or personal safety of others or, by the person''s conduct, is reckless whether such health or personal safety is endangered', FALSE),
    (5003, 'ADULT', '51:5', '51:25Q', '5', 'Intentionally endangers the health or personal safety of others or, by the person''s conduct, is reckless whether such health or personal safety is endangered', FALSE),
    (5004, 'ADULT', '51:5', '51:25Q', '5', 'Intentionally endangers the health or personal safety of others or, by the person''s conduct, is reckless whether such health or personal safety is endangered', FALSE),
    (5005, 'ADULT', '51:5', '51:25Q', '5', 'Intentionally endangers the health or personal safety of others or, by the person''s conduct, is reckless whether such health or personal safety is endangered', FALSE),
    (7001, 'ADULT', '51:7', '51:25O', '7', 'Escapes or absconds from prison or from legal custody', FALSE),
    (7002, 'ADULT', '51:7', '51:25O', '7', 'Escapes or absconds from prison or from legal custody', FALSE),
    (8001, 'ADULT', '51:8D', '51:25AI', '8', 'Fails to comply with any condition upon which the person is temporarily released under rule 9', FALSE),
    (8002, 'ADULT', '51:8E', '51:25AJ', '8', 'Fails to comply with any condition upon which the person is temporarily released under rule 9', FALSE),
    (12001, 'ADULT', '51:12', '51:25AM', '12', 'Has in the person''s possession: (a) any unauthorised article, or (b) a greater quantity of any article than the person is authorised to have', FALSE),
    (12002, 'ADULT', '51:12A', '51:25A', '12', 'Has in the person''s possession: (a) any unauthorised article, or (b) a greater quantity of any article than the person is authorised to have', FALSE),
    (14001, 'ADULT', '51:14', '51:25J', '14', 'Sells or, without permission, delivers to any person any article which the person is allowed to have only for the person''s own use', FALSE),
    (13001, 'ADULT', '51:13B', '51:25J', '13', 'Sells or delivers to any person any unauthorised article', FALSE),
    (15001, 'ADULT', '51:15', '51:25T', '15', 'Takes improperly any article belonging to another person or to a prison', FALSE),
    (24001, 'ADULT', '51:24', '51:25Y', '24', 'Receives any controlled drug, pharmacy medicine, prescription only medicine, psychoactive substance or specified substance or, without the consent of an officer, any other article, during the course of a visit (not being an interview such as is mentioned in rule 38)', FALSE),
    (24002, 'ADULT', '51:24', '51:25Y', '24', 'Receives any controlled drug, pharmacy medicine, prescription only medicine, psychoactive substance or specified substance or, without the consent of an officer, any other article, during the course of a visit (not being an interview such as is mentioned in rule 38)', FALSE),
    (23001, 'ADULT', '51:23AP', '51:25V', '23', 'Disobeys or fails to comply with any rule or regulation applying to the person', FALSE),
    (23201, 'ADULT', '51:23AP', '51:25V', '23', 'Disobeys or fails to comply with any rule or regulation applying to the person', FALSE),
    (23002, 'ADULT', '51:25Z', '51:25Z', '22', 'Disobeys any lawful order', FALSE),
    (23202, 'ADULT', '51:25Z', '51:25Z', '22', 'Disobeys any lawful order', FALSE),
    (9001, 'ADULT', '51:9', '51:25U', '9', 'Is found with any substance in the person''s urine which demonstrates that a controlled drug, pharmacy medication, prescription only medicine, psychoactive substance or specified substance has, whether in prison or while on temporary release under rule 9, been administered to the person by themselves or by another person (but subject to Rule 52)', FALSE),
    (9002, 'ADULT', '51:9', '51:25U', '9', 'Is found with any substance in the person''s urine which demonstrates that a controlled drug, pharmacy medication, prescription only medicine, psychoactive substance or specified substance has, whether in prison or while on temporary release under rule 9, been administered to the person by themselves or by another person (but subject to Rule 52)', FALSE),
    (12101, 'ADULT', '51:12AQ', '51:25X', '12', 'Has in the person''s possession: (a) any unauthorised article, or (b) a greater quantity of any article than the person is authorised to have', FALSE),
    (12102, 'ADULT', '51:12AQ', '51:25X', '12', 'Has in the person''s possession: (a) any unauthorised article, or (b) a greater quantity of any article than the person is authorised to have', FALSE),
    (10001, 'ADULT', '51:10', '51:10', '10', 'Is intoxicated as a consequence of consuming any alcoholic beverage (but subject to rule 52A)', FALSE),
    (11001, 'ADULT', '51:11', '51:11', '11', 'Consumes any alcoholic beverage whether or not provided to the person by another person (but subject to rule 52A)', FALSE),
    (16001, 'ADULT', '51:16', '51:25C', '16', 'Intentionally or recklessly sets fire to any part of a prison or any other property, whether or not the person''s own', FALSE),
    (24101, 'ADULT', '51:24A', '51:25L', '24(a)', 'Displays, attaches or draws on any part of a prison, or on any other property, threatening, abusive or insulting racist words, drawings, symbols or other material', TRUE),
    (2410124, 'ADULT', '51:24A (24)', '51:24A (24)', '24(a)', 'Displays, attaches or draws on any part of a prison, or on any other property, threatening, abusive or insulting words, drawings, symbols or other material, which demonstrate, or are motivated (wholly or partly) by, hostility to persons based on them sharing a protected characteristic', FALSE),
    (17001, 'ADULT', '51:17A', '51:17A', '17(a)', 'Causes racially aggravated damage to, or destruction of, any part of a prison or any other property, other than the person''s own', TRUE),
    (1700124, 'ADULT', '51:17A (24)', '51:17A (24)', '17(a)', 'Causes damage to, or destruction of, any part of a prison or any other property, other than the person''s own, aggravated by a protected characteristic', FALSE),
    (17002, 'ADULT', '51:17', '51:17', '17', 'Destroys or damages any part of a prison or any other property, other than the person''s own', FALSE),
    (19001, 'ADULT', '51:19B', '51:25AE', '19', 'Is disrespectful to any officer, or any person (other than a prisoner) who is at the prison for the purpose of working there, or any person visiting a prison', FALSE),
    (19002, 'ADULT', '51:19C', '51:25AF', '19', 'Is disrespectful to any officer, or any person (other than a prisoner) who is at the prison for the purpose of working there, or any person visiting a prison', FALSE),
    (19003, 'ADULT', '51:19A', '51:25AD', '19', 'Is disrespectful to any officer, or any person (other than a prisoner) who is at the prison for the purpose of working there, or any person visiting a prison', FALSE),
    (20001, 'ADULT', '51:20A', '51:20A', '20(a)', 'Uses threatening, abusive or insulting racist words or behaviour', TRUE),
    (2000124, 'ADULT', '51:20A (24)', '51:20A (24)', '20(a)', 'Uses threatening, abusive or insulting words or behaviour, which demonstrate, or are motivated (wholly or partly) by, hostility to persons based on them sharing a protected characteristic', FALSE),
    (20002, 'ADULT', '51:20', '51:25AC', '20', 'Uses threatening, abusive or insulting words or behaviour', FALSE),
    (22001, 'ADULT', '51:22', '51:25W', '22', 'Disobeys any lawful order', FALSE),
    (23101, 'ADULT', '51:23', '51:25AN', '23', 'Disobeys or fails to comply with any rule or regulation applying to the person', FALSE),
    (2001, 'ADULT', '51:2A', '51:25K', '2', 'Detains any person against the person''s will', FALSE),
    (2021, 'ADULT', '51:2A', '51:25K', '2', 'Detains any person against the person''s will', FALSE),
    (2002, 'ADULT', '51:2C', '51:25M', '2', 'Detains any person against the person''s will', FALSE),
    (2003, 'ADULT', '51:2D', '51:25N', '2', 'Detains any person against the person''s will', FALSE),
    (2004, 'ADULT', '51:2B', '51:25L', '2', 'Detains any person against the person''s will', FALSE),
    (3001, 'ADULT', '51:3', '51:25H', '3', 'Denies access to any part of the prison to any officer or any person (other than a prisoner) who is at the prison for the purpose of working there', FALSE),
    (6001, 'ADULT', '51:6', '51:25R', '6', 'Intentionally obstructs an officer in the execution of their duty, or any person (other than a prisoner) who is at the prison for the purpose of working there, in the performance of their work', FALSE),
    (18001, 'ADULT', '51:18A', '51:25B', '18', 'Absents themselves from any place the person is required to be or is present at any place where the person is not authorised to be', FALSE),
    (18002, 'ADULT', '51:18B', '51:25AO', '18', 'Absents themselves from any place the person is required to be or is present at any place where the person is not authorised to be', FALSE),
    (21001, 'ADULT', '51:21', '51:25AL', '21', 'Intentionally fails to work properly or, being required to work, refuses to do so', FALSE),
    (2600124, 'ADULT', '51:23A (24)', '51:23A (24)', '23(a)', 'Fails to comply with any payback punishment', FALSE),
    (1001, 'YOI', '55:2', '55:2', '2', 'Commits any racially aggravated assault', TRUE),
    (1003, 'YOI', '55:2', '55:2', '2', 'Commits any racially aggravated assault', TRUE),
    (1005, 'YOI', '55:2', '55:2', '2', 'Commits any racially aggravated assault', TRUE),
    (1021, 'YOI', '55:2', '55:2', '2', 'Commits any racially aggravated assault', TRUE),
    (1007, 'YOI', '55:2', '55:2', '2', 'Commits any racially aggravated assault', TRUE),
    (100124, 'YOI', '55:2 (24)', '55:2 (24)', '2', 'Commits any assault aggravated by a protected characteristic', FALSE),
    (100324, 'YOI', '55:2 (24)', '55:2 (24)', '2', 'Commits any assault aggravated by a protected characteristic', FALSE),
    (100524, 'YOI', '55:2 (24)', '55:2 (24)', '2', 'Commits any assault aggravated by a protected characteristic', FALSE),
    (102124, 'YOI', '55:2 (24)', '55:2 (24)', '2', 'Commits any assault aggravated by a protected characteristic', FALSE),
    (100724, 'YOI', '55:2 (24)', '55:2 (24)', '2', 'Commits any assault aggravated by a protected characteristic', FALSE),
    (1002, 'YOI', '55:1A', '55:29D', '1', 'Commits any assault', FALSE),
    (1022, 'YOI', '55:1A', '55:29D', '1', 'Commits any assault', FALSE),
    (102224, 'YOI', '55:2A (24)', '55:2A (24)', '2(a)', 'Commits any sexual assault', FALSE),
    (102324, 'YOI', '55:2B (24)', '55:2B (24)', '2(b)', 'Exposes themselves, or commits any other indecent or obscene act', FALSE),
    (102424, 'YOI', '55:2C (24)', '55:2C (24)', '2(c)', 'Sexually harasses any person', FALSE),
    (1004, 'YOI', '55:1L', '55:29F', '1', 'Commits any assault', FALSE),
    (1006, 'YOI', '55:1M', '55:29G', '1', 'Commits any assault', FALSE),
    (1008, 'YOI', '55:1E', '55:29E', '1', 'Commits any assault', FALSE),
    (4001, 'YOI', '55:5', '55:5', '5', 'Fights with any person', FALSE),
    (4002, 'YOI', '55:5', '55:5', '5', 'Fights with any person', FALSE),
    (4003, 'YOI', '55:5', '55:5', '5', 'Fights with any person', FALSE),
    (4004, 'YOI', '55:5', '55:5', '5', 'Fights with any person', FALSE),
    (4005, 'YOI', '55:5', '55:5', '5', 'Fights with any person', FALSE),
    (5001, 'YOI', '55:6', '55:29Q', '6', 'Intentionally endangers the health or personal safety of others or, by the person''s conduct, is reckless whether such health or personal safety is endangered', FALSE),
    (5002, 'YOI', '55:6', '55:29Q', '6', 'Intentionally endangers the health or personal safety of others or, by the person''s conduct, is reckless whether such health or personal safety is endangered', FALSE),
    (5003, 'YOI', '55:6', '55:29Q', '6', 'Intentionally endangers the health or personal safety of others or, by the person''s conduct, is reckless whether such health or personal safety is endangered', FALSE),
    (5004, 'YOI', '55:6', '55:29Q', '6', 'Intentionally endangers the health or personal safety of others or, by the person''s conduct, is reckless whether such health or personal safety is endangered', FALSE),
    (5005, 'YOI', '55:6', '55:29Q', '6', 'Intentionally endangers the health or personal safety of others or, by the person''s conduct, is reckless whether such health or personal safety is endangered', FALSE),
    (7001, 'YOI', '55:8', '55:29O', '8', 'Escapes or absconds from a young offender institution or from legal custody', FALSE),
    (7002, 'YOI', '55:8', '55:29O', '8', 'Escapes or absconds from a young offender institution or from legal custody', FALSE),
    (8001, 'YOI', '55:9D', '55:29AI', '9', 'Fails to comply with any condition upon which the person was temporarily released under rule 5 of these rules', FALSE),
    (8002, 'YOI', '55:9E', '55:29AJ', '9', 'Fails to comply with any condition upon which the person was temporarily released under rule 5 of these rules', FALSE),
    (12001, 'YOI', '55:13', '55:29AM', '13', 'Has in the person''s possession: (a) any unauthorised article, or (b) a greater quantity of any article than the person is authorised to have', FALSE),
    (12002, 'YOI', '55:13A', '55:29AH', '13', 'Has in the person''s possession: (a) any unauthorised article, or (b) a greater quantity of any article than the person is authorised to have', FALSE),
    (14001, 'YOI', '55:15', '55:29J', '15', 'Sells or, without permission, delivers to any person any article which the person is allowed to have only for the person''s own use', FALSE),
    (13001, 'YOI', '55:14B', '55:29J', '14', 'Sells or delivers to any person any unauthorised article', FALSE),
    (15001, 'YOI', '55:16', '55:29T', '16', 'Takes improperly any article belonging to another person or to a young offender institution', FALSE),
    (24001, 'YOI', '55:27', '55:29Y', '27', 'Receives any controlled drug, pharmacy medicine, prescription only medicine, psychoactive substance or specified substance or, without the consent of an officer, any other article, during the course of a visit (not being an interview such as is mentioned in rule 16)', FALSE),
    (24002, 'YOI', '55:27', '55:29Y', '27', 'Receives any controlled drug, pharmacy medicine, prescription only medicine, psychoactive substance or specified substance or, without the consent of an officer, any other article, during the course of a visit (not being an interview such as is mentioned in rule 16)', FALSE),
    (23001, 'YOI', '55:26B', '55:29V', '26', 'Disobeys or fails to comply with any rule or regulation applying to the person', FALSE),
    (23201, 'YOI', '55:26B', '55:29V', '26', 'Disobeys or fails to comply with any rule or regulation applying to the person', FALSE),
    (23002, 'YOI', '55:26C', '55:29Z', '25', 'Disobeys any lawful order', FALSE),
    (23202, 'YOI', '55:26C', '55:29Z', '25', 'Disobeys any lawful order', FALSE),
    (9001, 'YOI', '55:10', '55:29U', '10', 'Is found with any substance in the person''s urine which demonstrates that a controlled drug, pharmacy medication, prescription only medicine, psychoactive substance or specified substance has, whether in prison or while on temporary release under rule 5, been administered to the person by themselves or by another person (but subject to Rule 56)', FALSE),
    (9002, 'YOI', '55:10', '55:29U', '10', 'Is found with any substance in the person''s urine which demonstrates that a controlled drug, pharmacy medication, prescription only medicine, psychoactive substance or specified substance has, whether in prison or while on temporary release under rule 5, been administered to the person by themselves or by another person (but subject to Rule 56)', FALSE),
    (12101, 'YOI', '55:13C', '55:29X', '13', 'Has in the person''s possession: (a) any unauthorised article, or (b) a greater quantity of any article than the person is authorised to have', FALSE),
    (12102, 'YOI', '55:13B', '55:29X', '13', 'Has in the person''s possession: (a) any unauthorised article, or (b) a greater quantity of any article than the person is authorised to have', FALSE),
    (10001, 'YOI', '55:11', '55:11', '11', 'Is intoxicated as a consequence of knowingly consuming any alcoholic beverage', FALSE),
    (11001, 'YOI', '55:12', '55:12', '12', 'Knowingly consumes any alcoholic beverage, other than any provided to the person pursuant to a written order of the medical officer under rule 21(1)', FALSE),
    (16001, 'YOI', '55:17', '55:29C', '17', 'Intentionally or recklessly sets fire to any part of a young offender institution or any other property, whether or not the person''s own', FALSE),
    (24101, 'YOI', '55:28', '55:29I', '28', 'Displays, attaches or draws on any part of a young offender institution, or on any other property, threatening, abusive, or insulting racist words, drawings, symbols or other material', TRUE),
    (2410124, 'YOI', '55:28 (24)', '55:28 (24)', '28', 'Displays, attaches or draws on any part of a young offender institution, or on any other property, threatening, abusive or insulting words, drawings, symbols or other material, which demonstrate, or are motivated (wholly or partly) by, hostility to persons based on them sharing a protected characteristic', FALSE),
    (17001, 'YOI', '55:19', '55:19', '19', 'Causes racially aggravated damage to, or destruction of, any part of a young offender institution or any other property, other than the person''s own', TRUE),
    (1700124, 'YOI', '55:19 (24)', '55:19 (24)', '19', 'Causes damage to, or destruction of, any part of a young offender institution or any other property, other than the person''s own, aggravated by a protected characteristic', FALSE),
    (17002, 'YOI', '55:18', '55:18', '18', 'Destroys or damages any part of a young offender institution or any other property other than the person''s own', FALSE),
    (19001, 'YOI', '55:21B', '55:29AE', '21', 'Is disrespectful to any officer, or any person (other than an inmate) who is at the young offender institution for the purpose of working there, or any person visiting a young offender institution', FALSE),
    (19002, 'YOI', '55:21C', '55:29AF', '21', 'Is disrespectful to any officer, or any person (other than an inmate) who is at the young offender institution for the purpose of working there, or any person visiting a young offender institution', FALSE),
    (19003, 'YOI', '55:21A', '55:29AD', '21', 'Is disrespectful to any officer, or any person (other than an inmate) who is at the young offender institution for the purpose of working there, or any person visiting a young offender institution', FALSE),
    (20001, 'YOI', '55:23', '55:23', '23', 'Uses threatening, abusive or insulting racist words or behaviour', TRUE),
    (2000124, 'YOI', '55:23 (24)', '55:23 (24)', '23', 'Uses threatening, abusive or insulting words or behaviour, which demonstrate, or are motivated (wholly or partly) by, hostility to persons based on them sharing a protected characteristic', FALSE),
    (20002, 'YOI', '55:22', '55:29AC', '22', 'Uses threatening, abusive or insulting words or behaviour', FALSE),
    (22001, 'YOI', '55:25', '55:29W', '25', 'Disobeys any lawful order', FALSE),
    (23101, 'YOI', '55:26', '55:29AI', '26', 'Disobeys or fails to comply with any rule or regulation applying to the person', FALSE),
    (2001, 'YOI', '55:3A', '55:29K', '3', 'Detains any person against the person''s will', FALSE),
    (2021, 'YOI', '55:3A', '55:29K', '3', 'Detains any person against the person''s will', FALSE),
    (2002, 'YOI', '55:3C', '55:29M', '3', 'Detains any person against the person''s will', FALSE),
    (2003, 'YOI', '55:3D', '55:29N', '3', 'Detains any person against the person''s will', FALSE),
    (2004, 'YOI', '55:3B', '55:29L', '3', 'Detains any person against the person''s will', FALSE),
    (3001, 'YOI', '55:4', '55:29H', '4', 'Denies access to any part of the young offender institution to any officer or any person (other than an inmate) who is at the young offender institution for the purpose of working there', FALSE),
    (6001, 'YOI', '55:7', '55:29R', '7', 'Intentionally obstructs an officer in the execution of their duty, or any person (other than an inmate) who is at the young offender institution for the purpose of working there, in the performance of their work', FALSE),
    (18001, 'YOI', '55:20A', '55:29AB', '20', 'Absents themselves from any place where the person is required to be or is present at any place where the person is not authorised to be', FALSE),
    (18002, 'YOI', '55:20B', '55:29AO', '20', 'Absents themselves from any place where the person is required to be or is present at any place where the person is not authorised to be', FALSE),
    (21001, 'YOI', '55:24', '55:29AL', '24', 'Intentionally fails to work properly or, being required to work, refuses to do so', FALSE),
    (2600124, 'YOI', '55:26A (24)', '55:26A (24)', '26(a)', 'Fails to comply with any payback punishment', FALSE);

CREATE TABLE constant_incident_role (
    code text NOT NULL,
    rule_set text NOT NULL CHECK (rule_set IN ('ADULT', 'YOI')),
    paragraph text NOT NULL,
    description text NOT NULL,
    PRIMARY KEY (code, rule_set)
);
COMMENT ON TABLE constant_incident_role IS 'Incident role reference by adult or YOI rule set. A null role code indicates committed, not whether the person acted alone. A missing draft incident_role row means the role has not been supplied. Drafts with no rule set remain unresolved.';
COMMENT ON COLUMN constant_incident_role.code IS 'Stored incident role code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_incident_role.rule_set IS 'ADULT or YOI; selected using is_youth_offender. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_incident_role.paragraph IS 'Applicable rule paragraph. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_incident_role.description IS 'General description of the incident role. [Sensitivity: NONE]';
INSERT INTO constant_incident_role (code, rule_set, paragraph, description) VALUES
    ('25a', 'ADULT', '25(a)', 'Attempts to commit any of the foregoing offences:'),
    ('25b', 'ADULT', '25(b)', 'Incites another prisoner to commit any of the foregoing offences:'),
    ('25c', 'ADULT', '25(c)', 'Assists another prisoner to commit, or to attempt to commit, any of the foregoing offences:'),
    ('25a', 'YOI', '29(a)', 'Attempts to commit any of the foregoing offences:'),
    ('25b', 'YOI', '29(b)', 'Incites another inmate to commit any of the foregoing offences:'),
    ('25c', 'YOI', '29(c)', 'Assists another inmate to commit, or to attempt to commit, any of the foregoing offences:');

COMMENT ON COLUMN offence.offence_code IS 'Internal DPS offence code. For reporting join constant_offence by code and the parent draft rule set; an unset is_youth_offender remains unresolved. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN reported_offence.offence_code IS 'Internal DPS offence code. For reporting join constant_offence by code and the parent adjudication rule set. Migrated sentinel 0 has no reference row: use nomis_offence_code and nomis_offence_description on this record. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN incident_role.role_code IS 'Incident role code. Null in an existing role row indicates committed, not whether the person acted alone. A missing draft incident_role row means the role has not been supplied. 25a = attempts to commit, 25b = incites another to commit, 25c = assists another to commit. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN reported_adjudications.incident_role_code IS 'Incident role code. Null indicates committed, not whether the person acted alone. 25a = attempts to commit, 25b = incites another to commit, 25c = assists another to commit. [Sensitivity: SPECIAL-CATEGORY]';
