-- Data sensitivity classification for the adjudications data dictionary.
--
-- V130 gave every table and column a description. This migration adds a sensitivity tag to each
-- column comment and fills the gaps V130 deliberately left: the BaseEntity audit columns, which were
-- described once in V130's header rather than per column, and punishment.has_child_under_18, added
-- later by V131.
--
-- V130 is not edited - it is already applied in every environment. COMMENT ON replaces a comment in
-- place, so re-issuing the same descriptions here with a tag appended is safe and idempotent.
--
-- Every column comment now ends with a sensitivity classification:
--
--   [Sensitivity: NONE]                - not personal data in itself (keys, timestamps, process flags)
--   [Sensitivity: PERSONAL]            - personal data about a prisoner: identifies or locates them
--   [Sensitivity: STAFF]               - personal data about a member of staff, typically the username
--                                        that performed an action
--   [Sensitivity: SPECIAL-CATEGORY]    - UK GDPR Article 9 data (health, sexuality, religion, race,
--                                        gender reassignment) or criminal offence data under Article 10
--   [Sensitivity: OFFICIAL-SENSITIVE]  - not personal data, but damaging if disclosed
--
-- STAFF is still personal data and still in scope for a staff member's own subject access request. It
-- is separated from PERSONAL so an extract about prisoners can be reasoned about without staff columns
-- inflating the count, and so staff data can be dropped or pseudonymised independently.
--
-- Four things to understand before using these classifications:
--
--   1. They describe the column's own content, not the row's. Every row in the reported tables
--      concerns a prisoner charged with an offence, so the whole record is personal data about them
--      whatever an individual column is marked - that is what matters for a subject access request.
--   2. This schema records alleged offending in custody. DPA 2018 s.11(2) extends Article 10 to
--      alleged as well as proven offences, so the offence codes, the incident role codes, the charge
--      status and the plea are criminal offence data whichever value they hold - not only once a
--      charge is proved.
--   3. Every free-text column should be assumed to contain more than its label asks. statement,
--      details and comment are written by officers describing an incident in their own words, and in
--      practice name third parties and describe violence, health and offending.
--   4. Protected characteristics are recorded here deliberately - as the motivation for an offence -
--      so protected_characteristics.characteristic and its draft twin are Article 9 data by design,
--      not by accident.
--
-- Two judgements worth challenging in review rather than taking as settled:
--   - hearing_outcome.adjourn_reason and outcome.not_proceed_reason are tagged SPECIAL-CATEGORY
--     because both carry an UNFIT value, which permits an inference about the prisoner's health. The
--     other values in those lists are purely procedural.
--   - punishment.has_child_under_18 is tagged PERSONAL rather than SPECIAL-CATEGORY. It concerns
--     family circumstances, which are not an Article 9 category, though they are clearly sensitive.
--
-- SchemaCommentsTest fails the build if a table or column has no comment, or if a column comment does
-- not end in a valid tag. Keep both up to date when the schema changes.

COMMENT ON TABLE draft_adjudications IS 'A report being written by a reporting officer, before it is submitted for review. Rows are deleted once the draft is submitted and copied into reported_adjudications, so this table holds work in progress only and is not a historic record.';
COMMENT ON COLUMN draft_adjudications.prisoner_number IS 'NOMIS offender number (noms id) of the prisoner the charge is against. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN draft_adjudications.offender_booking_id IS 'NOMIS OFFENDER_BOOK_ID for the prisoner''s booking at the time of the incident. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN draft_adjudications.gender IS 'Prisoner gender, used to select the correct wording of offence paragraphs. One of MALE, FEMALE. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN draft_adjudications.charge_number IS 'Charge number, populated only once the draft has been submitted and a reported adjudication exists. Format is <agency><6 digit sequence>, optionally suffixed -<n> for additional charges on the same incident. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN draft_adjudications.report_by_user_id IS 'DPS username of the officer who submitted the report. Null while the draft is unsubmitted. [Sensitivity: STAFF]';
COMMENT ON COLUMN draft_adjudications.is_youth_offender IS 'True when the YOI rule set applies rather than the adult rule set. Determines which offence paragraphs and hearing types are valid. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN draft_adjudications.originating_agency_id IS 'Agency (prison) code where the incident was reported. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN draft_adjudications.override_agency_id IS 'Agency code the prisoner has since transferred to. Set when the report needs to be actioned by the receiving prison. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN draft_adjudications.incident_details_id IS 'Foreign key to incident_details. [Sensitivity: NONE]';
COMMENT ON COLUMN draft_adjudications.incident_role_id IS 'Foreign key to incident_role. [Sensitivity: NONE]';
COMMENT ON COLUMN draft_adjudications.incident_statement_id IS 'Foreign key to incident_statement. [Sensitivity: NONE]';
COMMENT ON COLUMN draft_adjudications.damages_saved IS 'True once the reporter has completed the damages page, including confirming there were none. Distinguishes "no damages" from "not yet asked". [Sensitivity: NONE]';
COMMENT ON COLUMN draft_adjudications.evidence_saved IS 'True once the reporter has completed the evidence page, including confirming there was none. [Sensitivity: NONE]';
COMMENT ON COLUMN draft_adjudications.witnesses_saved IS 'True once the reporter has completed the witnesses page, including confirming there were none. [Sensitivity: NONE]';
COMMENT ON COLUMN draft_adjudications.created_on_behalf_of_officer IS 'Name of the officer the report was created on behalf of, when someone else entered it for them. [Sensitivity: STAFF]';
COMMENT ON COLUMN draft_adjudications.created_on_behalf_of_reason IS 'Reason the report was created on behalf of another officer. [Sensitivity: SPECIAL-CATEGORY]';

COMMENT ON COLUMN draft_adjudications.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN draft_adjudications.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN draft_adjudications.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN draft_adjudications.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN draft_adjudications.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE incident_details IS 'When and where the incident happened, for a draft adjudication. One row per draft.';
COMMENT ON COLUMN incident_details.date_time_of_incident IS 'When the incident occurred. [Sensitivity: NONE]';
COMMENT ON COLUMN incident_details.date_time_of_discovery IS 'When the incident was discovered. Equal to the incident date unless it was found later; the 48 hour notice clock runs from here. [Sensitivity: NONE]';
COMMENT ON COLUMN incident_details.handover_deadline IS 'Deadline for issuing the notice of report to the prisoner, calculated as 48 hours after discovery. [Sensitivity: NONE]';
COMMENT ON COLUMN incident_details.location_id IS 'Legacy NOMIS internal location id (AGENCY_INTERNAL_LOCATIONS.INTERNAL_LOCATION_ID). Superseded by location_uuid. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN incident_details.location_uuid IS 'Location identifier in the locations-inside-prison service. This is the current location reference. [Sensitivity: PERSONAL]';

COMMENT ON COLUMN incident_details.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN incident_details.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN incident_details.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN incident_details.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN incident_details.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE incident_role IS 'The prisoner''s role in the incident for a draft adjudication - whether they committed the offence themselves or attempted, incited or assisted another. One row per draft.';
COMMENT ON COLUMN incident_role.role_code IS 'Paragraph 25 role code. Null means the prisoner committed the offence alone. 25a = attempts to commit, 25b = incites another to commit, 25c = assists another to commit. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN incident_role.associated_prisoners_number IS 'NOMIS offender number of the other prisoner involved, where the role code requires one. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN incident_role.associated_prisoners_name IS 'Name of the other prisoner involved, held where they are not in the current establishment and cannot be looked up. [Sensitivity: PERSONAL]';

COMMENT ON COLUMN incident_role.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN incident_role.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN incident_role.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN incident_role.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN incident_role.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE incident_statement IS 'The reporting officer''s free text account of the incident, for a draft adjudication. One row per draft.';
COMMENT ON COLUMN incident_statement.statement IS 'Free text description of the incident, up to 4000 characters. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN incident_statement.completed IS 'True once the reporter has marked the statement as finished. [Sensitivity: NONE]';

COMMENT ON COLUMN incident_statement.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN incident_statement.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN incident_statement.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN incident_statement.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN incident_statement.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE offence IS 'The offence selected for a draft adjudication. In practice there is one row per draft; multiple offences are raised as separate charges.';
COMMENT ON COLUMN offence.offence_code IS 'Internal DPS offence code (an integer, not the Prison Rules paragraph). Resolved in application code by OffenceCodeLookupService against the OffenceCodes enum - there is no reference table in this database. See the reference-data export for the full code list. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN offence.victim_prisoners_number IS 'NOMIS offender number of the prisoner victim, for offences that have one. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN offence.victim_staff_username IS 'DPS username of the staff victim, for offences that have one. [Sensitivity: STAFF]';
COMMENT ON COLUMN offence.victim_other_persons_name IS 'Name of a victim who is neither a prisoner nor staff (for example a visitor). [Sensitivity: PERSONAL]';
COMMENT ON COLUMN offence.draft_adjudication_fk_id IS 'Foreign key to draft_adjudications. [Sensitivity: NONE]';

COMMENT ON COLUMN offence.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN offence.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN offence.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN offence.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN offence.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE draft_protected_characteristics IS 'Protected characteristics of the victim recorded as motivating the offence, on a draft adjudication. Copied to protected_characteristics on submission.';
COMMENT ON COLUMN draft_protected_characteristics.characteristic IS 'One of AGE, DISABILITY, GENDER_REASSIGN, MARRIAGE_AND_CP, PREGNANCY_AND_MAT, RACE, RELIGION, SEX, SEX_ORIENTATION. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN draft_protected_characteristics.offence_fk_id IS 'Foreign key to offence. [Sensitivity: NONE]';

COMMENT ON COLUMN draft_protected_characteristics.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN draft_protected_characteristics.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN draft_protected_characteristics.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN draft_protected_characteristics.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN draft_protected_characteristics.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE damages IS 'Damage to prison property recorded on a draft adjudication. Copied to reported_damages on submission.';
COMMENT ON COLUMN damages.code IS 'Type of damage. One of ELECTRICAL_REPAIR, PLUMBING_REPAIR, FURNITURE_OR_FABRIC_REPAIR, LOCK_REPAIR, REDECORATION, CLEANING, REPLACE_AN_ITEM. [Sensitivity: NONE]';
COMMENT ON COLUMN damages.details IS 'Free text description of the damage. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN damages.reporter IS 'DPS username of the officer who recorded this entry. [Sensitivity: STAFF]';
COMMENT ON COLUMN damages.draft_adjudication_fk_id IS 'Foreign key to draft_adjudications. [Sensitivity: NONE]';

COMMENT ON COLUMN damages.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN damages.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN damages.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN damages.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN damages.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE evidence IS 'Evidence recorded on a draft adjudication. Copied to reported_evidence on submission.';
COMMENT ON COLUMN evidence.code IS 'Type of evidence. One of PHOTO, BODY_WORN_CAMERA, CCTV, BAGGED_AND_TAGGED, OTHER. [Sensitivity: NONE]';
COMMENT ON COLUMN evidence.identifier IS 'Reference number for the evidence, such as a body worn camera or seal number. [Sensitivity: NONE]';
COMMENT ON COLUMN evidence.details IS 'Free text description of the evidence. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN evidence.reporter IS 'DPS username of the officer who recorded this entry. [Sensitivity: STAFF]';
COMMENT ON COLUMN evidence.draft_adjudication_fk_id IS 'Foreign key to draft_adjudications. [Sensitivity: NONE]';

COMMENT ON COLUMN evidence.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN evidence.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN evidence.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN evidence.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN evidence.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE witness IS 'Witnesses recorded on a draft adjudication. Copied to reported_witness on submission.';
COMMENT ON COLUMN witness.code IS 'Type of witness. One of OFFICER, STAFF, OTHER_PERSON, VICTIM, PRISONER. [Sensitivity: NONE]';
COMMENT ON COLUMN witness.first_name IS 'Witness first name. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN witness.last_name IS 'Witness last name. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN witness.username IS 'DPS username of the witness where they are a member of staff. [Sensitivity: STAFF]';
COMMENT ON COLUMN witness.reporter IS 'DPS username of the officer who recorded this entry. [Sensitivity: STAFF]';
COMMENT ON COLUMN witness.draft_adjudication_fk_id IS 'Foreign key to draft_adjudications. [Sensitivity: NONE]';

COMMENT ON COLUMN witness.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN witness.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN witness.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN witness.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN witness.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

------------------------------------------------------------------------------------------------
-- Reported (submitted) side - the main analytical tables

------------------------------------------------------------------------------------------------

COMMENT ON TABLE reported_adjudications IS 'A submitted adjudication charge - the root record for everything that follows (hearings, outcomes and punishments). One row per charge, not per incident: an incident involving several prisoners, or several offences, produces several rows. Includes records migrated from NOMIS as well as those raised in DPS.';
COMMENT ON COLUMN reported_adjudications.charge_number IS 'Business key for the charge, unique across the service. Format is <agency><6 digit sequence> for DPS-raised charges (for example MDI001234); migrated NOMIS charges use the NOMIS charge number, suffixed -<n> where one NOMIS incident split into several DPS charges. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN reported_adjudications.prisoner_number IS 'NOMIS offender number (noms id) of the prisoner charged. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN reported_adjudications.offender_booking_id IS 'NOMIS OFFENDER_BOOK_ID for the booking the charge belongs to. Use this to join to NOMIS sentence and sanction data. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN reported_adjudications.gender IS 'Prisoner gender, used to select the correct wording of offence paragraphs. One of MALE, FEMALE. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN reported_adjudications.status IS 'Current state of the charge. One of AWAITING_REVIEW, RETURNED, REJECTED, ACCEPTED (deprecated), UNSCHEDULED, SCHEDULED, ADJOURNED, REFER_POLICE, REFER_INAD, REFER_GOV, PROSECUTION, DISMISSED, NOT_PROCEED, CHARGE_PROVED, QUASHED, INVALID_OUTCOME, INVALID_SUSPENDED, INVALID_ADA. Derived from the latest outcome by ReportedAdjudication.calculateStatus(). The three INVALID_* values flag data inconsistencies inherited from the NOMIS migration rather than real states. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN reported_adjudications.status_reason IS 'Reason code recorded when the reviewer rejected or returned the report. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_adjudications.status_details IS 'Free text the reviewer gave alongside status_reason. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN reported_adjudications.status_before_migration IS 'Status the charge held in NOMIS before migration. Populated on migrated records only. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN reported_adjudications.originating_agency_id IS 'Agency (prison) code where the incident was reported. Together with override_agency_id this controls which caseload may see and action the record. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN reported_adjudications.override_agency_id IS 'Agency code the prisoner transferred to after the charge was raised, set by TransferService from prisoner movement events. Null when there has been no transfer. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN reported_adjudications.last_modified_agency_id IS 'Agency code of the establishment that last changed the record. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN reported_adjudications.date_time_of_incident IS 'When the incident occurred. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_adjudications.date_time_of_discovery IS 'When the incident was discovered. Equal to the incident date unless it was found later; the 48 hour notice clock runs from here. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_adjudications.handover_deadline IS 'Deadline for issuing the notice of report to the prisoner, calculated as 48 hours after discovery. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_adjudications.location_id IS 'Legacy NOMIS internal location id where the incident occurred. Superseded by location_uuid. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN reported_adjudications.location_uuid IS 'Location identifier in the locations-inside-prison service. This is the current location reference. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN reported_adjudications.is_youth_offender IS 'True when the YOI rule set applies rather than the adult rule set. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN reported_adjudications.incident_role_code IS 'Paragraph 25 role code. Null means the prisoner committed the offence alone. 25a = attempts to commit, 25b = incites another to commit, 25c = assists another to commit. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN reported_adjudications.incident_role_associated_prisoners_number IS 'NOMIS offender number of the other prisoner involved, where the role code requires one. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN reported_adjudications.incident_role_associated_prisoners_name IS 'Name of the other prisoner involved, held where they cannot be looked up. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN reported_adjudications.statement IS 'The reporting officer''s free text account of the incident. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN reported_adjudications.review_user_id IS 'DPS username of the reviewer who accepted, rejected or returned the report. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_adjudications.issuing_officer IS 'DPS username of the officer who most recently issued the notice of report (DIS1/2) to the prisoner. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_adjudications.date_time_of_issue IS 'When the notice of report was most recently issued. Full history is in dis_issue_history. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_adjudications.date_time_of_first_hearing IS 'Denormalised date of the earliest hearing, maintained for reporting and sorting. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_adjudications.date_time_resubmitted IS 'When a returned report was resubmitted for review. Where set, this is treated as the report''s created date in the API. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_adjudications.agency_incident_id IS 'NOMIS AGENCY_INCIDENT_ID the charge came from. Populated on migrated records and on records synchronised back to NOMIS. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_adjudications.migrated IS 'True when the record was migrated from NOMIS rather than raised in DPS. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_adjudications.migrated_inactive_prisoner IS 'True when the record was migrated for a prisoner who was no longer active. These records are excluded from the INVALID_* status checks because their data cannot be corrected. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_adjudications.migrated_split_record IS 'True when a single NOMIS charge was split into multiple DPS charges during migration. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_adjudications.created_on_behalf_of_officer IS 'Name of the officer the report was created on behalf of, when someone else entered it for them. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_adjudications.created_on_behalf_of_reason IS 'Reason the report was created on behalf of another officer. [Sensitivity: SPECIAL-CATEGORY]';

COMMENT ON COLUMN reported_adjudications.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_adjudications.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_adjudications.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_adjudications.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_adjudications.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE reported_offence IS 'The offence charged on a submitted adjudication. One row per reported adjudication.';
COMMENT ON COLUMN reported_offence.offence_code IS 'Internal DPS offence code (an integer, not the Prison Rules paragraph). Resolved in application code by OffenceCodeLookupService against the OffenceCodes enum - there is no reference table in this database. Migrated NOMIS charges that could not be mapped use the MIGRATED_OFFENCE code and carry the original text in nomis_offence_code / nomis_offence_description. See the reference-data export for the full code list. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN reported_offence.actual_offence_code IS 'The offence code originally chosen, retained when the stored offence_code was subsequently adjusted. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN reported_offence.nomis_offence_code IS 'NOMIS OIC offence code, for example 51:1A. Populated on migrated records. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN reported_offence.nomis_offence_description IS 'NOMIS offence description text. Populated on migrated records, and used in place of the DPS paragraph description where the offence could not be mapped. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN reported_offence.victim_prisoners_number IS 'NOMIS offender number of the prisoner victim, for offences that have one. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN reported_offence.victim_staff_username IS 'DPS username of the staff victim, for offences that have one. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_offence.victim_other_persons_name IS 'Name of a victim who is neither a prisoner nor staff (for example a visitor). [Sensitivity: PERSONAL]';
COMMENT ON COLUMN reported_offence.migrated IS 'True when the offence row was created by the NOMIS migration. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_offence.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications. [Sensitivity: NONE]';

COMMENT ON COLUMN reported_offence.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_offence.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_offence.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_offence.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_offence.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE protected_characteristics IS 'Protected characteristics of the victim recorded as motivating the offence, on a submitted adjudication.';
COMMENT ON COLUMN protected_characteristics.characteristic IS 'One of AGE, DISABILITY, GENDER_REASSIGN, MARRIAGE_AND_CP, PREGNANCY_AND_MAT, RACE, RELIGION, SEX, SEX_ORIENTATION. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN protected_characteristics.reported_offence_fk_id IS 'Foreign key to reported_offence. [Sensitivity: NONE]';

COMMENT ON COLUMN protected_characteristics.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN protected_characteristics.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN protected_characteristics.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN protected_characteristics.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN protected_characteristics.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE reported_damages IS 'Damage to prison property recorded on a submitted adjudication.';
COMMENT ON COLUMN reported_damages.code IS 'Type of damage. One of ELECTRICAL_REPAIR, PLUMBING_REPAIR, FURNITURE_OR_FABRIC_REPAIR, LOCK_REPAIR, REDECORATION, CLEANING, REPLACE_AN_ITEM. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_damages.details IS 'Free text description of the damage. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN reported_damages.repair_cost IS 'Cost of repair in pounds, where recorded. Separate from the DAMAGES_OWED punishment, which is the amount the prisoner was ordered to pay. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_damages.reporter IS 'DPS username of the officer who recorded this entry. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_damages.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications. [Sensitivity: NONE]';

COMMENT ON COLUMN reported_damages.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_damages.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_damages.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_damages.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_damages.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE reported_evidence IS 'Evidence recorded on a submitted adjudication.';
COMMENT ON COLUMN reported_evidence.code IS 'Type of evidence. One of PHOTO, BODY_WORN_CAMERA, CCTV, BAGGED_AND_TAGGED, OTHER. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_evidence.identifier IS 'Reference number for the evidence, such as a body worn camera or seal number. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_evidence.details IS 'Free text description of the evidence. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN reported_evidence.date_added IS 'When the evidence was added to the report, where it was added after submission. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_evidence.reporter IS 'DPS username of the officer who recorded this entry. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_evidence.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications. [Sensitivity: NONE]';

COMMENT ON COLUMN reported_evidence.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_evidence.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_evidence.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_evidence.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_evidence.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE reported_witness IS 'Witnesses recorded on a submitted adjudication.';
COMMENT ON COLUMN reported_witness.code IS 'Type of witness. One of OFFICER, STAFF, OTHER_PERSON, VICTIM, PRISONER. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_witness.first_name IS 'Witness first name. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN reported_witness.last_name IS 'Witness last name. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN reported_witness.username IS 'DPS username of the witness where they are a member of staff. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_witness.comment IS 'Free text note about the witness or their evidence. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN reported_witness.date_added IS 'When the witness was added to the report, where they were added after submission. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_witness.reporter IS 'DPS username of the officer who recorded this entry. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_witness.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications. [Sensitivity: NONE]';

COMMENT ON COLUMN reported_witness.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_witness.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_witness.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN reported_witness.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN reported_witness.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE dis_issue_history IS 'Full history of issuing the notice of being placed on report (DIS1/2) to the prisoner. The most recent issue is also denormalised onto reported_adjudications.';
COMMENT ON COLUMN dis_issue_history.issuing_officer IS 'DPS username of the officer who issued the form. [Sensitivity: STAFF]';
COMMENT ON COLUMN dis_issue_history.date_time_of_issue IS 'When the form was issued to the prisoner. [Sensitivity: NONE]';
COMMENT ON COLUMN dis_issue_history.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications. [Sensitivity: NONE]';

COMMENT ON COLUMN dis_issue_history.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN dis_issue_history.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN dis_issue_history.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN dis_issue_history.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN dis_issue_history.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

------------------------------------------------------------------------------------------------
-- Hearings and outcomes

------------------------------------------------------------------------------------------------

COMMENT ON TABLE hearing IS 'A scheduled or held adjudication hearing. A charge can have several hearings, for example where the first is adjourned. The ordered case history shown in the UI is assembled from this table and outcome by ReportedAdjudicationService.createOutcomeHistory().';
COMMENT ON COLUMN hearing.date_time_of_hearing IS 'When the hearing is scheduled for, or was held. [Sensitivity: NONE]';
COMMENT ON COLUMN hearing.oic_hearing_type IS 'Who hears the case, and under which rule set. One of GOV_ADULT, GOV_YOI, INAD_ADULT, INAD_YOI, GOV. GOV = governor, INAD = independent adjudicator. [Sensitivity: NONE]';
COMMENT ON COLUMN hearing.agency_id IS 'Agency (prison) code where the hearing takes place. May differ from the charge''s originating agency after a transfer. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN hearing.charge_number IS 'Charge number of the parent adjudication, denormalised for lookup. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN hearing.location_id IS 'Legacy NOMIS internal location id of the hearing room. Superseded by location_uuid. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN hearing.location_uuid IS 'Location identifier in the locations-inside-prison service. This is the current location reference. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN hearing.oic_hearing_id IS 'NOMIS OIC_HEARING_ID. Populated for migrated hearings and for hearings synchronised back to NOMIS. [Sensitivity: NONE]';
COMMENT ON COLUMN hearing.representative IS 'Name of the prisoner''s representative at the hearing, where they had one. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN hearing.outcome_id IS 'Foreign key to hearing_outcome. Null while the hearing is scheduled but not yet held. [Sensitivity: NONE]';
COMMENT ON COLUMN hearing.hearing_pre_migrate_id IS 'Legacy column from the NOMIS migration. The table it referenced was dropped in V91 and it is no longer populated. [Sensitivity: NONE]';
COMMENT ON COLUMN hearing.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications. [Sensitivity: NONE]';

COMMENT ON COLUMN hearing.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN hearing.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN hearing.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN hearing.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN hearing.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE hearing_outcome IS 'What happened at a hearing. One row per hearing that has been held.';
COMMENT ON COLUMN hearing_outcome.code IS 'What the adjudicator did. One of COMPLETE (a finding was reached - see the linked outcome row for what it was), ADJOURN, REFER_POLICE, REFER_INAD, REFER_GOV, NOMIS (outcome was entered in NOMIS rather than DPS). [Sensitivity: NONE]';
COMMENT ON COLUMN hearing_outcome.adjudicator IS 'Name or username of the governor or independent adjudicator who heard the case. [Sensitivity: STAFF]';
COMMENT ON COLUMN hearing_outcome.adjourn_reason IS 'Why the hearing was adjourned. One of LEGAL_ADVICE, LEGAL_REPRESENTATION, RO_ATTEND, HELP, UNFIT, WITNESS, WITNESS_SUPPORT, MCKENZIE, EVIDENCE, INVESTIGATION, OTHER. Null unless code is ADJOURN. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN hearing_outcome.plea IS 'The prisoner''s plea. One of GUILTY, NOT_GUILTY, ABSTAIN, UNFIT, NOT_ASKED. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN hearing_outcome.details IS 'Free text notes recorded against the hearing outcome. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN hearing_outcome.nomis_outcome IS 'True when the outcome was recorded in NOMIS rather than DPS, so DPS holds only a placeholder. [Sensitivity: NONE]';
COMMENT ON COLUMN hearing_outcome.migrated IS 'True when the row was created by the NOMIS migration. [Sensitivity: NONE]';
COMMENT ON COLUMN hearing_outcome.hearing_outcome_pre_migrate_id IS 'Legacy column from the NOMIS migration. The table it referenced was dropped in V91 and it is no longer populated. [Sensitivity: NONE]';

COMMENT ON COLUMN hearing_outcome.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN hearing_outcome.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN hearing_outcome.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN hearing_outcome.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN hearing_outcome.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE outcome IS 'A finding or decision on a charge. Some outcomes follow a hearing, others are recorded without one (for example referring to the police before any hearing). Ordering is by actual_created_date where set, otherwise create_datetime.';
COMMENT ON COLUMN outcome.code IS 'The finding or decision. One of REFER_POLICE, REFER_INAD, REFER_GOV, NOT_PROCEED, DISMISSED, PROSECUTION, SCHEDULE_HEARING, CHARGE_PROVED, QUASHED. CHARGE_PROVED is the outcome that allows punishments to be awarded; QUASHED revokes them. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN outcome.details IS 'Free text supporting the outcome. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN outcome.not_proceed_reason IS 'Why the charge was not proceeded with. One of ANOTHER_WAY, RELEASED, WITNESS_NOT_ATTEND, UNFIT, FLAWED, EXPIRED_NOTICE, EXPIRED_HEARING, NOT_FAIR, OTHER. Null unless code is NOT_PROCEED. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN outcome.quashed_reason IS 'Why the charge was quashed. One of FLAWED_CASE, JUDICIAL_REVIEW, APPEAL_UPHELD, OTHER. Null unless code is QUASHED. [Sensitivity: NONE]';
COMMENT ON COLUMN outcome.refer_gov_reason IS 'Why the case was referred to the governor. One of REVIEW_FOR_REFER_POLICE, GOV_INQUIRY, OTHER. Null unless code is REFER_GOV. [Sensitivity: NONE]';
COMMENT ON COLUMN outcome.oic_hearing_id IS 'NOMIS OIC_HEARING_ID the outcome relates to, where the outcome came from or was synchronised to NOMIS. [Sensitivity: NONE]';
COMMENT ON COLUMN outcome.deleted IS 'Soft delete flag. Rows where this is true are excluded from the case history and from status calculation; queries must filter on "deleted is not true". [Sensitivity: NONE]';
COMMENT ON COLUMN outcome.actual_created_date IS 'The real date the outcome was reached, as opposed to create_datetime which for migrated records is the date of the migration run. Use this for chronology. [Sensitivity: NONE]';
COMMENT ON COLUMN outcome.migrated IS 'True when the row was created by the NOMIS migration. [Sensitivity: NONE]';
COMMENT ON COLUMN outcome.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications. [Sensitivity: NONE]';

COMMENT ON COLUMN outcome.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN outcome.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN outcome.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN outcome.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN outcome.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

------------------------------------------------------------------------------------------------
-- Punishments

------------------------------------------------------------------------------------------------

COMMENT ON TABLE punishment IS 'A punishment (NOMIS calls these awards or sanctions) given on a charge where the outcome was CHARGE_PROVED. The duration or amount is not held here but on the linked punishment_schedule rows. ADDITIONAL_DAYS and PROSPECTIVE_DAYS punishments are the added days that feed sentence calculation via the adjustments service.';
COMMENT ON COLUMN punishment.type IS 'Type of punishment. One of PRIVILEGE (loss of privileges - see privilege_type), EARNINGS (stoppage of earnings - see stoppage_percentage), CONFINEMENT (cellular confinement), REMOVAL_ACTIVITY, EXCLUSION_WORK, EXTRA_WORK, REMOVAL_WING, ADDITIONAL_DAYS (added days awarded, ADA), PROSPECTIVE_DAYS (prospective added days, PADA - awarded where the prisoner is not yet sentenced), CAUTION, DAMAGES_OWED (money owed for damage - see amount), PAYBACK (payback punishment, measured in hours). [Sensitivity: NONE]';
COMMENT ON COLUMN punishment.privilege_type IS 'Which privilege was lost. One of CANTEEN, FACILITIES, MONEY, TV, ASSOCIATION, GYM, OTHER. Null unless type is PRIVILEGE. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment.other_privilege IS 'Free text description of the privilege lost, when privilege_type is OTHER. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN punishment.stoppage_percentage IS 'Percentage of earnings stopped. Null unless type is EARNINGS. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment.amount IS 'Amount of money owed, in pounds. Null unless type is DAMAGES_OWED. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment.suspended_until IS 'Date the punishment is suspended until. Non-null means the punishment is suspended and not currently in force. Mirrors the suspended_until on the latest punishment_schedule row. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment.activated_by_charge_number IS 'Charge number of the later adjudication that activated this suspended punishment. Null while the punishment remains suspended or was never suspended. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN punishment.consecutive_to_charge_number IS 'Charge number this punishment runs consecutively to. Used for added days that follow on from an award on an earlier charge. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN punishment.deleted IS 'Soft delete flag. Rows where this is true are excluded everywhere; queries must filter on "deleted is not true". [Sensitivity: NONE]';
COMMENT ON COLUMN punishment.sanction_seq IS 'NOMIS OFFENDER_OIC_SANCTIONS.SANCTION_SEQ. With reported_adjudications.offender_booking_id this forms the composite key of the matching NOMIS sanction row. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment.nomis_status IS 'NOMIS sanction status (reference domain OIC_SANCT_ST) for records that came from or were synchronised to NOMIS. Values include IMMEDIATE, PROSPECTIVE, SUSPENDED, SUSP_PROSP, QUASHED, AWARD_RED, REDAPP, SUSPEN_RED, SUSPEN_EXT, AS_AWARDED. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment.actual_created_date IS 'The real date the punishment was awarded, as opposed to create_datetime which for migrated records is the date of the migration run. Use this for chronology. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment.payback_notes IS 'Free text description of the payback work required. Null unless type is PAYBACK. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN punishment.rehab_completed IS 'Whether the attached rehabilitative activities were completed. Null where no rehabilitative activity applies or the outcome has not yet been recorded. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment.rehab_not_completed_outcome IS 'What was decided when rehabilitative activities were not completed. One of FULL_ACTIVATE, PARTIAL_ACTIVATE, EXT_SUSPEND, NO_ACTION. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment.punishment_pre_migrate_id IS 'Legacy column from the NOMIS migration. The table it referenced was dropped in V91 and it is no longer populated. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications. [Sensitivity: NONE]';

COMMENT ON COLUMN punishment.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN punishment.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN punishment.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE punishment_schedule IS 'The duration and dates of a punishment. A punishment gains a new schedule row each time it is amended, so this table is a history: take the row with the greatest create_datetime for a given punishment_fk_id to get the current position.';
COMMENT ON COLUMN punishment_schedule.duration IS 'Length of the punishment, in the unit given by measurement. This is the added days count for ADDITIONAL_DAYS and PROSPECTIVE_DAYS punishments. Renamed from "days" in V115 when payback punishments introduced hours. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment_schedule.measurement IS 'Unit of duration. One of DAYS, HOURS. HOURS is used only by PAYBACK punishments. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment_schedule.start_date IS 'Date the punishment starts. Not set for added days, which take effect through a sentence adjustment rather than a date range. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment_schedule.end_date IS 'Date the punishment ends. Not set for added days. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment_schedule.suspended_until IS 'Date the punishment is suspended until. Non-null means the punishment was suspended as at this schedule row. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment_schedule.punishment_fk_id IS 'Foreign key to punishment. [Sensitivity: NONE]';

COMMENT ON COLUMN punishment_schedule.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment_schedule.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN punishment_schedule.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment_schedule.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN punishment_schedule.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE punishment_comments IS 'Free text notes recorded against the punishments on a charge, including the reason for any change made on appeal or correction.';
COMMENT ON COLUMN punishment_comments.comment IS 'The note text. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN punishment_comments.reason_for_change IS 'Why the punishments were changed. One of APPEAL, CORRECTION, OTHER, GOV_OR_DIRECTOR. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment_comments.nomis_created_by IS 'Username of the NOMIS user who created the comment, for migrated records. Takes precedence over create_user_id when displaying the author. [Sensitivity: STAFF]';
COMMENT ON COLUMN punishment_comments.actual_created_date IS 'The real date the comment was made, as opposed to create_datetime which for migrated records is the date of the migration run. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment_comments.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications. [Sensitivity: NONE]';

COMMENT ON COLUMN punishment_comments.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment_comments.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN punishment_comments.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN punishment_comments.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN punishment_comments.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

COMMENT ON TABLE rehabilitative_activity IS 'Rehabilitative activities attached to a suspended punishment as a condition of the suspension. Not available for added days, cautions, damages owed or payback punishments.';
COMMENT ON COLUMN rehabilitative_activity.details IS 'Description of the activity the prisoner must complete. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN rehabilitative_activity.monitor IS 'Name of the person responsible for monitoring the activity. [Sensitivity: STAFF]';
COMMENT ON COLUMN rehabilitative_activity.end_date IS 'Date the activity must be completed by. [Sensitivity: NONE]';
COMMENT ON COLUMN rehabilitative_activity.total_sessions IS 'Number of sessions the prisoner must attend. [Sensitivity: NONE]';
COMMENT ON COLUMN rehabilitative_activity.completed IS 'Whether this individual activity was completed. The overall decision is held on punishment.rehab_completed. [Sensitivity: NONE]';
COMMENT ON COLUMN rehabilitative_activity.punishment_fk_id IS 'Foreign key to punishment. [Sensitivity: NONE]';

COMMENT ON COLUMN rehabilitative_activity.id IS 'Surrogate primary key. Sequence value with no business meaning; it is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN rehabilitative_activity.create_user_id IS 'DPS username of the person who created the row. For records migrated from NOMIS this is the migration process rather than the officer who recorded the original prison event. [Sensitivity: STAFF]';
COMMENT ON COLUMN rehabilitative_activity.create_datetime IS 'When the row was created. For migrated records this is the date of the migration run, not the prison event - use actual_created_date where the table has one. [Sensitivity: NONE]';
COMMENT ON COLUMN rehabilitative_activity.modify_user_id IS 'DPS username of the person who last updated the row. Null if it has never been updated. [Sensitivity: STAFF]';
COMMENT ON COLUMN rehabilitative_activity.modify_datetime IS 'When the row was last updated. Null if it has never been updated. [Sensitivity: NONE]';

-- Added by V131, after V130 was written.
COMMENT ON COLUMN punishment.has_child_under_18 IS 'Whether the prisoner has a child under 18. Recorded against payback punishments so the work ordered does not conflict with childcare responsibilities. Added by V131. [Sensitivity: PERSONAL]';
