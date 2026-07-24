-- Data dictionary for the adjudications schema.
--
-- These comments are read by SchemaSpy (published to GitHub Pages) and by anything else that
-- reads pg_description, including the CSV export consumed by the MOJ Data Catalogue / Glue.
-- Keep them updated when columns are added or their meaning changes.
--
-- Audit columns common to every table (mapped from BaseEntity.kt) are not commented individually:
--   create_user_id / create_datetime  - the DPS username and timestamp of the row's creation
--   modify_user_id / modify_datetime  - the DPS username and timestamp of the last update
-- Note that for records migrated from NOMIS these reflect the migration run, not the original
-- prison event. Tables that hold the real NOMIS timestamp expose it as actual_created_date.

------------------------------------------------------------------------------------------------
-- Draft (pre-submission) side
------------------------------------------------------------------------------------------------

COMMENT ON TABLE draft_adjudications IS 'A report being written by a reporting officer, before it is submitted for review. Rows are deleted once the draft is submitted and copied into reported_adjudications, so this table holds work in progress only and is not a historic record.';
COMMENT ON COLUMN draft_adjudications.prisoner_number IS 'NOMIS offender number (noms id) of the prisoner the charge is against.';
COMMENT ON COLUMN draft_adjudications.offender_booking_id IS 'NOMIS OFFENDER_BOOK_ID for the prisoner''s booking at the time of the incident.';
COMMENT ON COLUMN draft_adjudications.gender IS 'Prisoner gender, used to select the correct wording of offence paragraphs. One of MALE, FEMALE.';
COMMENT ON COLUMN draft_adjudications.charge_number IS 'Charge number, populated only once the draft has been submitted and a reported adjudication exists. Format is <agency><6 digit sequence>, optionally suffixed -<n> for additional charges on the same incident.';
COMMENT ON COLUMN draft_adjudications.report_by_user_id IS 'DPS username of the officer who submitted the report. Null while the draft is unsubmitted.';
COMMENT ON COLUMN draft_adjudications.is_youth_offender IS 'True when the YOI rule set applies rather than the adult rule set. Determines which offence paragraphs and hearing types are valid.';
COMMENT ON COLUMN draft_adjudications.originating_agency_id IS 'Agency (prison) code where the incident was reported.';
COMMENT ON COLUMN draft_adjudications.override_agency_id IS 'Agency code the prisoner has since transferred to. Set when the report needs to be actioned by the receiving prison.';
COMMENT ON COLUMN draft_adjudications.incident_details_id IS 'Foreign key to incident_details.';
COMMENT ON COLUMN draft_adjudications.incident_role_id IS 'Foreign key to incident_role.';
COMMENT ON COLUMN draft_adjudications.incident_statement_id IS 'Foreign key to incident_statement.';
COMMENT ON COLUMN draft_adjudications.damages_saved IS 'True once the reporter has completed the damages page, including confirming there were none. Distinguishes "no damages" from "not yet asked".';
COMMENT ON COLUMN draft_adjudications.evidence_saved IS 'True once the reporter has completed the evidence page, including confirming there was none.';
COMMENT ON COLUMN draft_adjudications.witnesses_saved IS 'True once the reporter has completed the witnesses page, including confirming there were none.';
COMMENT ON COLUMN draft_adjudications.created_on_behalf_of_officer IS 'Name of the officer the report was created on behalf of, when someone else entered it for them.';
COMMENT ON COLUMN draft_adjudications.created_on_behalf_of_reason IS 'Reason the report was created on behalf of another officer.';

COMMENT ON TABLE incident_details IS 'When and where the incident happened, for a draft adjudication. One row per draft.';
COMMENT ON COLUMN incident_details.date_time_of_incident IS 'When the incident occurred.';
COMMENT ON COLUMN incident_details.date_time_of_discovery IS 'When the incident was discovered. Equal to the incident date unless it was found later; the 48 hour notice clock runs from here.';
COMMENT ON COLUMN incident_details.handover_deadline IS 'Deadline for issuing the notice of report to the prisoner, calculated as 48 hours after discovery.';
COMMENT ON COLUMN incident_details.location_id IS 'Legacy NOMIS internal location id (AGENCY_INTERNAL_LOCATIONS.INTERNAL_LOCATION_ID). Superseded by location_uuid.';
COMMENT ON COLUMN incident_details.location_uuid IS 'Location identifier in the locations-inside-prison service. This is the current location reference.';

COMMENT ON TABLE incident_role IS 'The prisoner''s role in the incident for a draft adjudication - whether they committed the offence themselves or attempted, incited or assisted another. One row per draft.';
COMMENT ON COLUMN incident_role.role_code IS 'Paragraph 25 role code. Null means the prisoner committed the offence alone. 25a = attempts to commit, 25b = incites another to commit, 25c = assists another to commit.';
COMMENT ON COLUMN incident_role.associated_prisoners_number IS 'NOMIS offender number of the other prisoner involved, where the role code requires one.';
COMMENT ON COLUMN incident_role.associated_prisoners_name IS 'Name of the other prisoner involved, held where they are not in the current establishment and cannot be looked up.';

COMMENT ON TABLE incident_statement IS 'The reporting officer''s free text account of the incident, for a draft adjudication. One row per draft.';
COMMENT ON COLUMN incident_statement.statement IS 'Free text description of the incident, up to 4000 characters.';
COMMENT ON COLUMN incident_statement.completed IS 'True once the reporter has marked the statement as finished.';

COMMENT ON TABLE offence IS 'The offence selected for a draft adjudication. In practice there is one row per draft; multiple offences are raised as separate charges.';
COMMENT ON COLUMN offence.offence_code IS 'Internal DPS offence code (an integer, not the Prison Rules paragraph). Resolved in application code by OffenceCodeLookupService against the OffenceCodes enum - there is no reference table in this database. See the reference-data export for the full code list.';
COMMENT ON COLUMN offence.victim_prisoners_number IS 'NOMIS offender number of the prisoner victim, for offences that have one.';
COMMENT ON COLUMN offence.victim_staff_username IS 'DPS username of the staff victim, for offences that have one.';
COMMENT ON COLUMN offence.victim_other_persons_name IS 'Name of a victim who is neither a prisoner nor staff (for example a visitor).';
COMMENT ON COLUMN offence.draft_adjudication_fk_id IS 'Foreign key to draft_adjudications.';

COMMENT ON TABLE draft_protected_characteristics IS 'Protected characteristics of the victim recorded as motivating the offence, on a draft adjudication. Copied to protected_characteristics on submission.';
COMMENT ON COLUMN draft_protected_characteristics.characteristic IS 'One of AGE, DISABILITY, GENDER_REASSIGN, MARRIAGE_AND_CP, PREGNANCY_AND_MAT, RACE, RELIGION, SEX, SEX_ORIENTATION.';
COMMENT ON COLUMN draft_protected_characteristics.offence_fk_id IS 'Foreign key to offence.';

COMMENT ON TABLE damages IS 'Damage to prison property recorded on a draft adjudication. Copied to reported_damages on submission.';
COMMENT ON COLUMN damages.code IS 'Type of damage. One of ELECTRICAL_REPAIR, PLUMBING_REPAIR, FURNITURE_OR_FABRIC_REPAIR, LOCK_REPAIR, REDECORATION, CLEANING, REPLACE_AN_ITEM.';
COMMENT ON COLUMN damages.details IS 'Free text description of the damage.';
COMMENT ON COLUMN damages.reporter IS 'DPS username of the officer who recorded this entry.';
COMMENT ON COLUMN damages.draft_adjudication_fk_id IS 'Foreign key to draft_adjudications.';

COMMENT ON TABLE evidence IS 'Evidence recorded on a draft adjudication. Copied to reported_evidence on submission.';
COMMENT ON COLUMN evidence.code IS 'Type of evidence. One of PHOTO, BODY_WORN_CAMERA, CCTV, BAGGED_AND_TAGGED, OTHER.';
COMMENT ON COLUMN evidence.identifier IS 'Reference number for the evidence, such as a body worn camera or seal number.';
COMMENT ON COLUMN evidence.details IS 'Free text description of the evidence.';
COMMENT ON COLUMN evidence.reporter IS 'DPS username of the officer who recorded this entry.';
COMMENT ON COLUMN evidence.draft_adjudication_fk_id IS 'Foreign key to draft_adjudications.';

COMMENT ON TABLE witness IS 'Witnesses recorded on a draft adjudication. Copied to reported_witness on submission.';
COMMENT ON COLUMN witness.code IS 'Type of witness. One of OFFICER, STAFF, OTHER_PERSON, VICTIM, PRISONER.';
COMMENT ON COLUMN witness.first_name IS 'Witness first name.';
COMMENT ON COLUMN witness.last_name IS 'Witness last name.';
COMMENT ON COLUMN witness.username IS 'DPS username of the witness where they are a member of staff.';
COMMENT ON COLUMN witness.reporter IS 'DPS username of the officer who recorded this entry.';
COMMENT ON COLUMN witness.draft_adjudication_fk_id IS 'Foreign key to draft_adjudications.';

------------------------------------------------------------------------------------------------
-- Reported (submitted) side - the main analytical tables
------------------------------------------------------------------------------------------------

COMMENT ON TABLE reported_adjudications IS 'A submitted adjudication charge - the root record for everything that follows (hearings, outcomes and punishments). One row per charge, not per incident: an incident involving several prisoners, or several offences, produces several rows. Includes records migrated from NOMIS as well as those raised in DPS.';
COMMENT ON COLUMN reported_adjudications.charge_number IS 'Business key for the charge, unique across the service. Format is <agency><6 digit sequence> for DPS-raised charges (for example MDI001234); migrated NOMIS charges use the NOMIS charge number, suffixed -<n> where one NOMIS incident split into several DPS charges.';
COMMENT ON COLUMN reported_adjudications.prisoner_number IS 'NOMIS offender number (noms id) of the prisoner charged.';
COMMENT ON COLUMN reported_adjudications.offender_booking_id IS 'NOMIS OFFENDER_BOOK_ID for the booking the charge belongs to. Use this to join to NOMIS sentence and sanction data.';
COMMENT ON COLUMN reported_adjudications.gender IS 'Prisoner gender, used to select the correct wording of offence paragraphs. One of MALE, FEMALE.';
COMMENT ON COLUMN reported_adjudications.status IS 'Current state of the charge. One of AWAITING_REVIEW, RETURNED, REJECTED, ACCEPTED (deprecated), UNSCHEDULED, SCHEDULED, ADJOURNED, REFER_POLICE, REFER_INAD, REFER_GOV, PROSECUTION, DISMISSED, NOT_PROCEED, CHARGE_PROVED, QUASHED, INVALID_OUTCOME, INVALID_SUSPENDED, INVALID_ADA. Derived from the latest outcome by ReportedAdjudication.calculateStatus(). The three INVALID_* values flag data inconsistencies inherited from the NOMIS migration rather than real states.';
COMMENT ON COLUMN reported_adjudications.status_reason IS 'Reason code recorded when the reviewer rejected or returned the report.';
COMMENT ON COLUMN reported_adjudications.status_details IS 'Free text the reviewer gave alongside status_reason.';
COMMENT ON COLUMN reported_adjudications.status_before_migration IS 'Status the charge held in NOMIS before migration. Populated on migrated records only.';
COMMENT ON COLUMN reported_adjudications.originating_agency_id IS 'Agency (prison) code where the incident was reported. Together with override_agency_id this controls which caseload may see and action the record.';
COMMENT ON COLUMN reported_adjudications.override_agency_id IS 'Agency code the prisoner transferred to after the charge was raised, set by TransferService from prisoner movement events. Null when there has been no transfer.';
COMMENT ON COLUMN reported_adjudications.last_modified_agency_id IS 'Agency code of the establishment that last changed the record.';
COMMENT ON COLUMN reported_adjudications.date_time_of_incident IS 'When the incident occurred.';
COMMENT ON COLUMN reported_adjudications.date_time_of_discovery IS 'When the incident was discovered. Equal to the incident date unless it was found later; the 48 hour notice clock runs from here.';
COMMENT ON COLUMN reported_adjudications.handover_deadline IS 'Deadline for issuing the notice of report to the prisoner, calculated as 48 hours after discovery.';
COMMENT ON COLUMN reported_adjudications.location_id IS 'Legacy NOMIS internal location id where the incident occurred. Superseded by location_uuid.';
COMMENT ON COLUMN reported_adjudications.location_uuid IS 'Location identifier in the locations-inside-prison service. This is the current location reference.';
COMMENT ON COLUMN reported_adjudications.is_youth_offender IS 'True when the YOI rule set applies rather than the adult rule set.';
COMMENT ON COLUMN reported_adjudications.incident_role_code IS 'Paragraph 25 role code. Null means the prisoner committed the offence alone. 25a = attempts to commit, 25b = incites another to commit, 25c = assists another to commit.';
COMMENT ON COLUMN reported_adjudications.incident_role_associated_prisoners_number IS 'NOMIS offender number of the other prisoner involved, where the role code requires one.';
COMMENT ON COLUMN reported_adjudications.incident_role_associated_prisoners_name IS 'Name of the other prisoner involved, held where they cannot be looked up.';
COMMENT ON COLUMN reported_adjudications.statement IS 'The reporting officer''s free text account of the incident.';
COMMENT ON COLUMN reported_adjudications.review_user_id IS 'DPS username of the reviewer who accepted, rejected or returned the report.';
COMMENT ON COLUMN reported_adjudications.issuing_officer IS 'DPS username of the officer who most recently issued the notice of report (DIS1/2) to the prisoner.';
COMMENT ON COLUMN reported_adjudications.date_time_of_issue IS 'When the notice of report was most recently issued. Full history is in dis_issue_history.';
COMMENT ON COLUMN reported_adjudications.date_time_of_first_hearing IS 'Denormalised date of the earliest hearing, maintained for reporting and sorting.';
COMMENT ON COLUMN reported_adjudications.date_time_resubmitted IS 'When a returned report was resubmitted for review. Where set, this is treated as the report''s created date in the API.';
COMMENT ON COLUMN reported_adjudications.agency_incident_id IS 'NOMIS AGENCY_INCIDENT_ID the charge came from. Populated on migrated records and on records synchronised back to NOMIS.';
COMMENT ON COLUMN reported_adjudications.migrated IS 'True when the record was migrated from NOMIS rather than raised in DPS.';
COMMENT ON COLUMN reported_adjudications.migrated_inactive_prisoner IS 'True when the record was migrated for a prisoner who was no longer active. These records are excluded from the INVALID_* status checks because their data cannot be corrected.';
COMMENT ON COLUMN reported_adjudications.migrated_split_record IS 'True when a single NOMIS charge was split into multiple DPS charges during migration.';
COMMENT ON COLUMN reported_adjudications.created_on_behalf_of_officer IS 'Name of the officer the report was created on behalf of, when someone else entered it for them.';
COMMENT ON COLUMN reported_adjudications.created_on_behalf_of_reason IS 'Reason the report was created on behalf of another officer.';

COMMENT ON TABLE reported_offence IS 'The offence charged on a submitted adjudication. One row per reported adjudication.';
COMMENT ON COLUMN reported_offence.offence_code IS 'Internal DPS offence code (an integer, not the Prison Rules paragraph). Resolved in application code by OffenceCodeLookupService against the OffenceCodes enum - there is no reference table in this database. Migrated NOMIS charges that could not be mapped use the MIGRATED_OFFENCE code and carry the original text in nomis_offence_code / nomis_offence_description. See the reference-data export for the full code list.';
COMMENT ON COLUMN reported_offence.actual_offence_code IS 'The offence code originally chosen, retained when the stored offence_code was subsequently adjusted.';
COMMENT ON COLUMN reported_offence.nomis_offence_code IS 'NOMIS OIC offence code, for example 51:1A. Populated on migrated records.';
COMMENT ON COLUMN reported_offence.nomis_offence_description IS 'NOMIS offence description text. Populated on migrated records, and used in place of the DPS paragraph description where the offence could not be mapped.';
COMMENT ON COLUMN reported_offence.victim_prisoners_number IS 'NOMIS offender number of the prisoner victim, for offences that have one.';
COMMENT ON COLUMN reported_offence.victim_staff_username IS 'DPS username of the staff victim, for offences that have one.';
COMMENT ON COLUMN reported_offence.victim_other_persons_name IS 'Name of a victim who is neither a prisoner nor staff (for example a visitor).';
COMMENT ON COLUMN reported_offence.migrated IS 'True when the offence row was created by the NOMIS migration.';
COMMENT ON COLUMN reported_offence.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications.';

COMMENT ON TABLE protected_characteristics IS 'Protected characteristics of the victim recorded as motivating the offence, on a submitted adjudication.';
COMMENT ON COLUMN protected_characteristics.characteristic IS 'One of AGE, DISABILITY, GENDER_REASSIGN, MARRIAGE_AND_CP, PREGNANCY_AND_MAT, RACE, RELIGION, SEX, SEX_ORIENTATION.';
COMMENT ON COLUMN protected_characteristics.reported_offence_fk_id IS 'Foreign key to reported_offence.';

COMMENT ON TABLE reported_damages IS 'Damage to prison property recorded on a submitted adjudication.';
COMMENT ON COLUMN reported_damages.code IS 'Type of damage. One of ELECTRICAL_REPAIR, PLUMBING_REPAIR, FURNITURE_OR_FABRIC_REPAIR, LOCK_REPAIR, REDECORATION, CLEANING, REPLACE_AN_ITEM.';
COMMENT ON COLUMN reported_damages.details IS 'Free text description of the damage.';
COMMENT ON COLUMN reported_damages.repair_cost IS 'Cost of repair in pounds, where recorded. Separate from the DAMAGES_OWED punishment, which is the amount the prisoner was ordered to pay.';
COMMENT ON COLUMN reported_damages.reporter IS 'DPS username of the officer who recorded this entry.';
COMMENT ON COLUMN reported_damages.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications.';

COMMENT ON TABLE reported_evidence IS 'Evidence recorded on a submitted adjudication.';
COMMENT ON COLUMN reported_evidence.code IS 'Type of evidence. One of PHOTO, BODY_WORN_CAMERA, CCTV, BAGGED_AND_TAGGED, OTHER.';
COMMENT ON COLUMN reported_evidence.identifier IS 'Reference number for the evidence, such as a body worn camera or seal number.';
COMMENT ON COLUMN reported_evidence.details IS 'Free text description of the evidence.';
COMMENT ON COLUMN reported_evidence.date_added IS 'When the evidence was added to the report, where it was added after submission.';
COMMENT ON COLUMN reported_evidence.reporter IS 'DPS username of the officer who recorded this entry.';
COMMENT ON COLUMN reported_evidence.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications.';

COMMENT ON TABLE reported_witness IS 'Witnesses recorded on a submitted adjudication.';
COMMENT ON COLUMN reported_witness.code IS 'Type of witness. One of OFFICER, STAFF, OTHER_PERSON, VICTIM, PRISONER.';
COMMENT ON COLUMN reported_witness.first_name IS 'Witness first name.';
COMMENT ON COLUMN reported_witness.last_name IS 'Witness last name.';
COMMENT ON COLUMN reported_witness.username IS 'DPS username of the witness where they are a member of staff.';
COMMENT ON COLUMN reported_witness.comment IS 'Free text note about the witness or their evidence.';
COMMENT ON COLUMN reported_witness.date_added IS 'When the witness was added to the report, where they were added after submission.';
COMMENT ON COLUMN reported_witness.reporter IS 'DPS username of the officer who recorded this entry.';
COMMENT ON COLUMN reported_witness.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications.';

COMMENT ON TABLE dis_issue_history IS 'Full history of issuing the notice of being placed on report (DIS1/2) to the prisoner. The most recent issue is also denormalised onto reported_adjudications.';
COMMENT ON COLUMN dis_issue_history.issuing_officer IS 'DPS username of the officer who issued the form.';
COMMENT ON COLUMN dis_issue_history.date_time_of_issue IS 'When the form was issued to the prisoner.';
COMMENT ON COLUMN dis_issue_history.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications.';

------------------------------------------------------------------------------------------------
-- Hearings and outcomes
------------------------------------------------------------------------------------------------

COMMENT ON TABLE hearing IS 'A scheduled or held adjudication hearing. A charge can have several hearings, for example where the first is adjourned. The ordered case history shown in the UI is assembled from this table and outcome by ReportedAdjudicationService.createOutcomeHistory().';
COMMENT ON COLUMN hearing.date_time_of_hearing IS 'When the hearing is scheduled for, or was held.';
COMMENT ON COLUMN hearing.oic_hearing_type IS 'Who hears the case, and under which rule set. One of GOV_ADULT, GOV_YOI, INAD_ADULT, INAD_YOI, GOV. GOV = governor, INAD = independent adjudicator.';
COMMENT ON COLUMN hearing.agency_id IS 'Agency (prison) code where the hearing takes place. May differ from the charge''s originating agency after a transfer.';
COMMENT ON COLUMN hearing.charge_number IS 'Charge number of the parent adjudication, denormalised for lookup.';
COMMENT ON COLUMN hearing.location_id IS 'Legacy NOMIS internal location id of the hearing room. Superseded by location_uuid.';
COMMENT ON COLUMN hearing.location_uuid IS 'Location identifier in the locations-inside-prison service. This is the current location reference.';
COMMENT ON COLUMN hearing.oic_hearing_id IS 'NOMIS OIC_HEARING_ID. Populated for migrated hearings and for hearings synchronised back to NOMIS.';
COMMENT ON COLUMN hearing.representative IS 'Name of the prisoner''s representative at the hearing, where they had one.';
COMMENT ON COLUMN hearing.outcome_id IS 'Foreign key to hearing_outcome. Null while the hearing is scheduled but not yet held.';
COMMENT ON COLUMN hearing.hearing_pre_migrate_id IS 'Legacy column from the NOMIS migration. The table it referenced was dropped in V91 and it is no longer populated.';
COMMENT ON COLUMN hearing.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications.';

COMMENT ON TABLE hearing_outcome IS 'What happened at a hearing. One row per hearing that has been held.';
COMMENT ON COLUMN hearing_outcome.code IS 'What the adjudicator did. One of COMPLETE (a finding was reached - see the linked outcome row for what it was), ADJOURN, REFER_POLICE, REFER_INAD, REFER_GOV, NOMIS (outcome was entered in NOMIS rather than DPS).';
COMMENT ON COLUMN hearing_outcome.adjudicator IS 'Name or username of the governor or independent adjudicator who heard the case.';
COMMENT ON COLUMN hearing_outcome.adjourn_reason IS 'Why the hearing was adjourned. One of LEGAL_ADVICE, LEGAL_REPRESENTATION, RO_ATTEND, HELP, UNFIT, WITNESS, WITNESS_SUPPORT, MCKENZIE, EVIDENCE, INVESTIGATION, OTHER. Null unless code is ADJOURN.';
COMMENT ON COLUMN hearing_outcome.plea IS 'The prisoner''s plea. One of GUILTY, NOT_GUILTY, ABSTAIN, UNFIT, NOT_ASKED.';
COMMENT ON COLUMN hearing_outcome.details IS 'Free text notes recorded against the hearing outcome.';
COMMENT ON COLUMN hearing_outcome.nomis_outcome IS 'True when the outcome was recorded in NOMIS rather than DPS, so DPS holds only a placeholder.';
COMMENT ON COLUMN hearing_outcome.migrated IS 'True when the row was created by the NOMIS migration.';
COMMENT ON COLUMN hearing_outcome.hearing_outcome_pre_migrate_id IS 'Legacy column from the NOMIS migration. The table it referenced was dropped in V91 and it is no longer populated.';

COMMENT ON TABLE outcome IS 'A finding or decision on a charge. Some outcomes follow a hearing, others are recorded without one (for example referring to the police before any hearing). Ordering is by actual_created_date where set, otherwise create_datetime.';
COMMENT ON COLUMN outcome.code IS 'The finding or decision. One of REFER_POLICE, REFER_INAD, REFER_GOV, NOT_PROCEED, DISMISSED, PROSECUTION, SCHEDULE_HEARING, CHARGE_PROVED, QUASHED. CHARGE_PROVED is the outcome that allows punishments to be awarded; QUASHED revokes them.';
COMMENT ON COLUMN outcome.details IS 'Free text supporting the outcome.';
COMMENT ON COLUMN outcome.not_proceed_reason IS 'Why the charge was not proceeded with. One of ANOTHER_WAY, RELEASED, WITNESS_NOT_ATTEND, UNFIT, FLAWED, EXPIRED_NOTICE, EXPIRED_HEARING, NOT_FAIR, OTHER. Null unless code is NOT_PROCEED.';
COMMENT ON COLUMN outcome.quashed_reason IS 'Why the charge was quashed. One of FLAWED_CASE, JUDICIAL_REVIEW, APPEAL_UPHELD, OTHER. Null unless code is QUASHED.';
COMMENT ON COLUMN outcome.refer_gov_reason IS 'Why the case was referred to the governor. One of REVIEW_FOR_REFER_POLICE, GOV_INQUIRY, OTHER. Null unless code is REFER_GOV.';
COMMENT ON COLUMN outcome.oic_hearing_id IS 'NOMIS OIC_HEARING_ID the outcome relates to, where the outcome came from or was synchronised to NOMIS.';
COMMENT ON COLUMN outcome.deleted IS 'Soft delete flag. Rows where this is true are excluded from the case history and from status calculation; queries must filter on "deleted is not true".';
COMMENT ON COLUMN outcome.actual_created_date IS 'The real date the outcome was reached, as opposed to create_datetime which for migrated records is the date of the migration run. Use this for chronology.';
COMMENT ON COLUMN outcome.migrated IS 'True when the row was created by the NOMIS migration.';
COMMENT ON COLUMN outcome.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications.';

------------------------------------------------------------------------------------------------
-- Punishments
------------------------------------------------------------------------------------------------

COMMENT ON TABLE punishment IS 'A punishment (NOMIS calls these awards or sanctions) given on a charge where the outcome was CHARGE_PROVED. The duration or amount is not held here but on the linked punishment_schedule rows. ADDITIONAL_DAYS and PROSPECTIVE_DAYS punishments are the added days that feed sentence calculation via the adjustments service.';
COMMENT ON COLUMN punishment.type IS 'Type of punishment. One of PRIVILEGE (loss of privileges - see privilege_type), EARNINGS (stoppage of earnings - see stoppage_percentage), CONFINEMENT (cellular confinement), REMOVAL_ACTIVITY, EXCLUSION_WORK, EXTRA_WORK, REMOVAL_WING, ADDITIONAL_DAYS (added days awarded, ADA), PROSPECTIVE_DAYS (prospective added days, PADA - awarded where the prisoner is not yet sentenced), CAUTION, DAMAGES_OWED (money owed for damage - see amount), PAYBACK (payback punishment, measured in hours).';
COMMENT ON COLUMN punishment.privilege_type IS 'Which privilege was lost. One of CANTEEN, FACILITIES, MONEY, TV, ASSOCIATION, GYM, OTHER. Null unless type is PRIVILEGE.';
COMMENT ON COLUMN punishment.other_privilege IS 'Free text description of the privilege lost, when privilege_type is OTHER.';
COMMENT ON COLUMN punishment.stoppage_percentage IS 'Percentage of earnings stopped. Null unless type is EARNINGS.';
COMMENT ON COLUMN punishment.amount IS 'Amount of money owed, in pounds. Null unless type is DAMAGES_OWED.';
COMMENT ON COLUMN punishment.suspended_until IS 'Date the punishment is suspended until. Non-null means the punishment is suspended and not currently in force. Mirrors the suspended_until on the latest punishment_schedule row.';
COMMENT ON COLUMN punishment.activated_by_charge_number IS 'Charge number of the later adjudication that activated this suspended punishment. Null while the punishment remains suspended or was never suspended.';
COMMENT ON COLUMN punishment.consecutive_to_charge_number IS 'Charge number this punishment runs consecutively to. Used for added days that follow on from an award on an earlier charge.';
COMMENT ON COLUMN punishment.deleted IS 'Soft delete flag. Rows where this is true are excluded everywhere; queries must filter on "deleted is not true".';
COMMENT ON COLUMN punishment.sanction_seq IS 'NOMIS OFFENDER_OIC_SANCTIONS.SANCTION_SEQ. With reported_adjudications.offender_booking_id this forms the composite key of the matching NOMIS sanction row.';
COMMENT ON COLUMN punishment.nomis_status IS 'NOMIS sanction status (reference domain OIC_SANCT_ST) for records that came from or were synchronised to NOMIS. Values include IMMEDIATE, PROSPECTIVE, SUSPENDED, SUSP_PROSP, QUASHED, AWARD_RED, REDAPP, SUSPEN_RED, SUSPEN_EXT, AS_AWARDED.';
COMMENT ON COLUMN punishment.actual_created_date IS 'The real date the punishment was awarded, as opposed to create_datetime which for migrated records is the date of the migration run. Use this for chronology.';
COMMENT ON COLUMN punishment.payback_notes IS 'Free text description of the payback work required. Null unless type is PAYBACK.';
COMMENT ON COLUMN punishment.rehab_completed IS 'Whether the attached rehabilitative activities were completed. Null where no rehabilitative activity applies or the outcome has not yet been recorded.';
COMMENT ON COLUMN punishment.rehab_not_completed_outcome IS 'What was decided when rehabilitative activities were not completed. One of FULL_ACTIVATE, PARTIAL_ACTIVATE, EXT_SUSPEND, NO_ACTION.';
COMMENT ON COLUMN punishment.punishment_pre_migrate_id IS 'Legacy column from the NOMIS migration. The table it referenced was dropped in V91 and it is no longer populated.';
COMMENT ON COLUMN punishment.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications.';

COMMENT ON TABLE punishment_schedule IS 'The duration and dates of a punishment. A punishment gains a new schedule row each time it is amended, so this table is a history: take the row with the greatest create_datetime for a given punishment_fk_id to get the current position.';
COMMENT ON COLUMN punishment_schedule.duration IS 'Length of the punishment, in the unit given by measurement. This is the added days count for ADDITIONAL_DAYS and PROSPECTIVE_DAYS punishments. Renamed from "days" in V115 when payback punishments introduced hours.';
COMMENT ON COLUMN punishment_schedule.measurement IS 'Unit of duration. One of DAYS, HOURS. HOURS is used only by PAYBACK punishments.';
COMMENT ON COLUMN punishment_schedule.start_date IS 'Date the punishment starts. Not set for added days, which take effect through a sentence adjustment rather than a date range.';
COMMENT ON COLUMN punishment_schedule.end_date IS 'Date the punishment ends. Not set for added days.';
COMMENT ON COLUMN punishment_schedule.suspended_until IS 'Date the punishment is suspended until. Non-null means the punishment was suspended as at this schedule row.';
COMMENT ON COLUMN punishment_schedule.punishment_fk_id IS 'Foreign key to punishment.';

COMMENT ON TABLE punishment_comments IS 'Free text notes recorded against the punishments on a charge, including the reason for any change made on appeal or correction.';
COMMENT ON COLUMN punishment_comments.comment IS 'The note text.';
COMMENT ON COLUMN punishment_comments.reason_for_change IS 'Why the punishments were changed. One of APPEAL, CORRECTION, OTHER, GOV_OR_DIRECTOR.';
COMMENT ON COLUMN punishment_comments.nomis_created_by IS 'Username of the NOMIS user who created the comment, for migrated records. Takes precedence over create_user_id when displaying the author.';
COMMENT ON COLUMN punishment_comments.actual_created_date IS 'The real date the comment was made, as opposed to create_datetime which for migrated records is the date of the migration run.';
COMMENT ON COLUMN punishment_comments.reported_adjudication_fk_id IS 'Foreign key to reported_adjudications.';

COMMENT ON TABLE rehabilitative_activity IS 'Rehabilitative activities attached to a suspended punishment as a condition of the suspension. Not available for added days, cautions, damages owed or payback punishments.';
COMMENT ON COLUMN rehabilitative_activity.details IS 'Description of the activity the prisoner must complete.';
COMMENT ON COLUMN rehabilitative_activity.monitor IS 'Name of the person responsible for monitoring the activity.';
COMMENT ON COLUMN rehabilitative_activity.end_date IS 'Date the activity must be completed by.';
COMMENT ON COLUMN rehabilitative_activity.total_sessions IS 'Number of sessions the prisoner must attend.';
COMMENT ON COLUMN rehabilitative_activity.completed IS 'Whether this individual activity was completed. The overall decision is held on punishment.rehab_completed.';
COMMENT ON COLUMN rehabilitative_activity.punishment_fk_id IS 'Foreign key to punishment.';
