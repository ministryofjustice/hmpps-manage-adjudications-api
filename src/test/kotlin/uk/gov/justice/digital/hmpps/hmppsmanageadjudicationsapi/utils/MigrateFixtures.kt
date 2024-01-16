package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DisIssued
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.NomisGender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicSanctionCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class MigrateFixtures {

  private val migrationEntityBuilder = MigrationEntityBuilder()

  val ADULT_SINGLE_OFFENCE = migrationEntityBuilder.createAdjudication(
    disIssued = listOf(
      DisIssued(issuingOfficer = "officer", dateTimeOfIssue = LocalDateTime.now()),
      DisIssued(issuingOfficer = "officer2", dateTimeOfIssue = LocalDateTime.now().plusDays(1)),
    ),
  )

  fun ADULT_WITH_REPORTED_DATE_TIME(reportedDateTime: LocalDateTime) = migrationEntityBuilder.createAdjudication(
    reportedDateTime = reportedDateTime,
  )

  val PROSECUTED_WITH_PUNISHMENTS = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(
          finding = Finding.PROSECUTED.name,
        ),
      ),
    ),
    punishments = listOf(
      migrationEntityBuilder.createPunishment(),
    ),
  )

  val YOUTH_SINGLE_OFFENCE = migrationEntityBuilder.createAdjudication(
    offence = migrationEntityBuilder.createOffence(offenceCode = "55:12"),
  )

  val NON_BINARY_GENDER = migrationEntityBuilder.createAdjudication(
    prisoner = migrationEntityBuilder.createPrisoner(gender = NomisGender.NK.name),
  )

  val UNKNOWN_GENDER = migrationEntityBuilder.createAdjudication(
    prisoner = migrationEntityBuilder.createPrisoner(gender = "?"),
  )

  val FEMALE = migrationEntityBuilder.createAdjudication(
    prisoner = migrationEntityBuilder.createPrisoner(gender = NomisGender.F.name),
  )
  val ADULT_MULITPLE_OFFENCES = listOf(
    migrationEntityBuilder.createAdjudication(),
    migrationEntityBuilder.createAdjudication(offenceSequence = 2),
    migrationEntityBuilder.createAdjudication(offenceSequence = 3),
  )

  val ADULT_TRANSFER = migrationEntityBuilder.createAdjudication(
    prisoner = migrationEntityBuilder.createPrisoner(currentAgencyId = "LEI"),
  )

  val WITH_WITNESSES = migrationEntityBuilder.createAdjudication(
    witnesses = listOf(
      migrationEntityBuilder.createWitness(),
    ),
  )

  val WITH_DAMAGES = migrationEntityBuilder.createAdjudication(
    damages = listOf(
      migrationEntityBuilder.createDamage(),
    ),
  )

  val WITH_DAMAGES_AND_NO_DETAIL = migrationEntityBuilder.createAdjudication(
    damages = listOf(
      migrationEntityBuilder.createDamage(details = null),
    ),
  )

  val WITH_EVIDENCE = migrationEntityBuilder.createAdjudication(
    evidence = listOf(
      migrationEntityBuilder.createEvidence(),
    ),
  )

  val WITH_PUNISHMENT = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(),
    ),
  )

  val WITH_CAUTION = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.CAUTION.name, days = 0),
    ),
  )

  val WITH_PUNISHMENT_EXTRA_WORK = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.EXTW.name),
    ),
  )

  val WITH_PUNISHMENT_EXCLUSION_WORK = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.EXTRA_WORK.name),
    ),
  )

  val WITH_PUNISHMENT_REMOVAL_WING = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.REMWIN.name),
    ),
  )

  val WITH_PUNISHMENT_REMOVAL_ACTIVITY = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.REMACT.name),
    ),
  )

  val WITH_QUASHED_ADA_AND_NO_QUASHED_OUTCOME = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult()),
    ),
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.ADA.name, status = Status.QUASHED.name),
    ),
  )

  val WITH_PUNISHMENT_PRIVILEGES = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.FORFEIT.name),
    ),
  )

  val WITH_PUNISHMENT_SUSPENDED = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.FORFEIT.name, days = 10, effectiveDate = LocalDate.now(), status = Status.SUSPENDED.name),
    ),
  )

  val WITH_PUNISHMENT_ADA = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = "ADA", status = "IMMEDIATE", days = 10),
    ),
  )

  val WITH_PUNISHMENT_PROSPECITVE_ADA = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = "ADA", status = "PROSPECTIVE", days = 10),
    ),
  )

  val WITH_PUNISHMENT_PADA = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = "PADA", status = "PROSPECTIVE", days = 10),
    ),
  )

  val WITH_PUNISHMENT_PROSPECITVE_ADA_SUSPENDED = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = "ADA", status = Status.SUSP_PROSP.name, days = 10, effectiveDate = LocalDate.now()),
    ),
  )

  val WITH_PUNISHMENT_DAMAGES_AMOUNT = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = "OTHER", amount = BigDecimal(10.50), days = 0),
    ),
  )

  val WITH_PUNISHMENT_STOPPAGE_PERCENTAGE = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.STOP_PCT.name, amount = BigDecimal(10.50)),
    ),
  )

  val WITH_PUNISHMENT_STOPPAGE_EARNINGS = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.STOP_EARN.name, amount = BigDecimal(10.50)),
    ),
  )

  val WITH_PUNISHMENT_EARNINGS_NO_STOPPAGE_PERCENTAGE = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.STOP_PCT.name),
    ),
  )

  val WITH_PUNISHMENT_COMMENT = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(comment = "some notes"),
    ),
  )

  val WITH_PUNISHMENT_CONSECUTIVE = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = "ADA", status = "IMMEDIATE", consecutiveChargeNumber = "12345"),
    ),
  )

  val WITH_PUNISHMENT_UNKNOWN_CODE = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = "BONUS_PNTS"),
    ),
  )

  val WITH_PUNISHMENT_START_DATE = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(status = OicSanctionCode.CC.name),
    ),
  )

  val WITH_PUNISHMENT_SUSPENDED_AND_STATUS_DATE_GREATER = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(),
      ),
    ),
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.ADA.name, effectiveDate = LocalDate.now().plusDays(1), statusDate = LocalDate.now().plusDays(2), status = Status.SUSPENDED.name),
    ),
  )

  val WITH_PUNISHMENT_SUSPENDED_AND_EFFECITVE_DATE_GREATER = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(),
      ),
    ),
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.ADA.name, effectiveDate = LocalDate.now().plusDays(2), statusDate = LocalDate.now().plusDays(1), status = Status.SUSPENDED.name),
    ),
  )

  val WITH_PUNISHMENT_SUSPENDED_CORRUPTED = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(),
      ),
    ),
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.ADA.name, effectiveDate = LocalDate.now(), statusDate = LocalDate.now(), status = Status.SUSPENDED.name),
    ),
  )

  val WITH_PUNISHMENT_SUSPENDED_CORRUPTED_2 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(),
      ),
    ),
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.ADA.name, effectiveDate = LocalDate.now(), statusDate = null, status = Status.SUSPENDED.name),
    ),
  )

  val WITH_PUNISHMENT_SUSPENDED_CORRUPTED_INACTIVE = migrationEntityBuilder.createAdjudication(
    prisoner = migrationEntityBuilder.createPrisoner(currentAgencyId = null),
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(),
      ),
    ),
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.ADA.name, effectiveDate = LocalDate.now(), statusDate = null, status = Status.SUSPENDED.name),
    ),
  )

  val WITH_PUNISHMENT_SUSPENDED_CORRUPTED_3 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(),
      ),
    ),
    punishments = listOf(
      migrationEntityBuilder.createPunishment(
        createdDateTime = LocalDateTime.now().minusMonths(7),
        code = OicSanctionCode.ADA.name,
        effectiveDate = LocalDate.now().minusMonths(7),
        statusDate = null,
        status = Status.SUSPENDED.name,
      ),
    ),
  )

  val WITH_PUNISHMENT_CORRUPTED_ADA = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(
          finding = Finding.PROSECUTED.name,
        ),
      ),
    ),
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.ADA.name),
    ),
  )

  val WITH_PUNISHMENT_CORRUPTED_ADA_INACTIVE = migrationEntityBuilder.createAdjudication(
    prisoner = migrationEntityBuilder.createPrisoner(currentAgencyId = null),
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(
          finding = Finding.PROSECUTED.name,
        ),
      ),
    ),
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.ADA.name),
    ),
  )

  val COMPLETE_CHARGE_PROVED = migrationEntityBuilder.createAdjudication(
    disIssued = listOf(DisIssued(issuingOfficer = "officer", dateTimeOfIssue = LocalDateTime.now())),
    damages = listOf(migrationEntityBuilder.createDamage()),
    evidence = listOf(migrationEntityBuilder.createEvidence()),
    witnesses = listOf(migrationEntityBuilder.createWitness()),
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(),
      ),
    ),
    punishments = listOf(
      migrationEntityBuilder.createPunishment(comment = "something"),
    ),
  )

  val PHASE2_HEARINGS_NO_RESULTS = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(oicHearingId = 1, hearingDateTime = LocalDate.now().atStartOfDay()),
      migrationEntityBuilder.createHearing(oicHearingId = 2, hearingDateTime = LocalDate.now().atStartOfDay().plusDays(1)),
      migrationEntityBuilder.createHearing(
        oicHearingId = 3,
        hearingDateTime = LocalDate.now().atStartOfDay().plusDays(2),
        hearingResult = migrationEntityBuilder.createHearingResult(),
      ),
    ),
  )

  fun PHASE2_HEARINGS_BAD_STRUCTURE(finding: Finding, withSanctions: Boolean = true, hasReducedSanctions: Boolean = false) = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        oicHearingId = 1,
        hearingDateTime = LocalDate.now().atStartOfDay(),
        hearingResult = migrationEntityBuilder.createHearingResult(),
      ),
      migrationEntityBuilder.createHearing(oicHearingId = 2, hearingDateTime = LocalDate.now().atStartOfDay().plusDays(1)),
      migrationEntityBuilder.createHearing(
        oicHearingId = 3,
        hearingDateTime = LocalDate.now().atStartOfDay().plusDays(2),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = finding.name),
      ),
    ),
    punishments = if (withSanctions) {
      listOf(migrationEntityBuilder.createPunishment())
    } else if (hasReducedSanctions) {
      listOf(
        migrationEntityBuilder.createPunishment(status = Status.AWARD_RED.name),
      )
    } else {
      emptyList()
    },
  )

  fun PHASE2_HEARINGS_AND_NOMIS(finding: Finding) = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        oicHearingId = 1,
        hearingDateTime = LocalDate.now().atStartOfDay(),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = finding.name),
      ),
    ),
  )

  val WITH_HEARING_GOV_TO_YOI = migrationEntityBuilder.createAdjudication(
    offence = migrationEntityBuilder.createOffence(offenceCode = "55:12"),
    hearings = listOf(
      migrationEntityBuilder.createHearing(oicHearingType = OicHearingType.GOV),
    ),
  )

  val WITH_HEARING_GOV_TO_ADULT = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(oicHearingType = OicHearingType.GOV),
    ),
  )

  val WITH_HEARING = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(),
    ),
  )

  val WITH_NO_ADJUDICATOR = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(adjudicator = null),
    ),
  )

  val WITH_HEARINGS = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(),
      migrationEntityBuilder.createHearing(hearingDateTime = LocalDateTime.now().plusDays(1)),
    ),
  )

  val WITH_HEARING_AND_RESULT = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        comment = "some notes",
        hearingResult = migrationEntityBuilder.createHearingResult(),
      ),
    ),
  )

  val WITH_HEARING_AND_RESULT_REF_POLICE = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.REF_POLICE.name),
      ),
      migrationEntityBuilder.createHearing(
        oicHearingId = 101,
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name),
      ),
    ),
  )

  val WITH_HEARING_AND_REFERRAL_RESULT = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.NOT_PROCEED.name),
      ),
    ),
  )

  val HEARING_WITH_PROSECUTION = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROSECUTED.name),
      ),
    ),
  )

  val HEARING_WITH_NOT_PROCEED_DUPLICATE = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.NOT_PROCEED.name),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().plusDays(1),
      ),
      migrationEntityBuilder.createHearing(
        oicHearingId = 2,
        hearingDateTime = LocalDateTime.now().plusDays(2),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.NOT_PROCEED.name),
      ),
    ),
  )

  val HEARING_WITH_CHARGE_PROVED = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        comment = "entered in error",
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.NOT_PROCEED.name),
      ),
      migrationEntityBuilder.createHearing(
        oicHearingId = 2,
        hearingDateTime = LocalDateTime.now().plusDays(2),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name),
      ),
    ),
    punishments = listOf(migrationEntityBuilder.createPunishment()),
  )

  val POLICE_REF_NOT_PROCEED = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.REF_POLICE.name),
      ),
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.NOT_PROCEED.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
    ),
  )

  val QUASHED_FIRST_HEARING = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.QUASHED.name),
      ),
    ),
  )

  // confirm this with SQL
  val QUASHED_SECOND_HEARING = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name),
      ),
      migrationEntityBuilder.createHearing(
        oicHearingId = 2L,
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.QUASHED.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
    ),
  )

  val POLICE_REFERRAL_NEW_HEARING = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.REF_POLICE.name),
      ),
      migrationEntityBuilder.createHearing(oicHearingId = 2L),
    ),
  )

  val MULITPLE_POLICE_REFER_TO_PROSECUTION = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.REF_POLICE.name),
      ),
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.REF_POLICE.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROSECUTED.name, createdDateTime = LocalDateTime.now().plusDays(2)),
      ),
    ),
  )

  val WITH_HEARING_AND_REFER_POLICE = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.REF_POLICE.name),
      ),
    ),
  )

  val WITH_HEARING_AND_DISMISSED = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.D.name),
      ),
    ),
  )

  val WITH_HEARING_AND_NOT_PROCEED = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.NOT_PROCEED.name),
      ),
    ),
  )

  val WITH_HEARINGS_AND_RESULTS = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.REF_POLICE.name),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().plusDays(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
    ),
  )

  val WITH_HEARINGS_AND_RESULTS_MULTIPLE_PROVED = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().plusDays(2),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name, createdDateTime = LocalDateTime.now().plusDays(2)),
      ),
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().plusDays(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
    ),
  )

  val WITH_HEARINGS_AND_SOME_RESULTS = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.REF_POLICE.name),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().plusDays(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.REF_POLICE.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().plusDays(2),
      ),
    ),
  )

  val EXCEPTION_CASE = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.D.name),
      ),
      migrationEntityBuilder.createHearing(
        oicHearingId = 2,
        hearingDateTime = LocalDateTime.now().plusDays(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
    ),
  )

  val EXCEPTION_CASE_5675 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().minusYears(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name),
      ),
      migrationEntityBuilder.createHearing(
        oicHearingId = 2,
        hearingDateTime = LocalDateTime.now().minusYears(1).plusDays(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().minusYears(1).plusDays(2),
      ),
    ),
    punishments = listOf(
      migrationEntityBuilder.createPunishment(),
    ),
  )
  val EXCEPTION_CASE_5675_1 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().minusYears(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.NOT_PROCEED.name),
      ),
      migrationEntityBuilder.createHearing(
        oicHearingId = 2,
        hearingDateTime = LocalDateTime.now().minusYears(1).plusDays(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().minusYears(1).plusDays(2),
      ),
    ),
    punishments = listOf(
      migrationEntityBuilder.createPunishment(),
    ),
  )

  val EXCEPTION_CASE_2 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name),
      ),
      migrationEntityBuilder.createHearing(
        oicHearingId = 2,
        hearingDateTime = LocalDateTime.now().plusDays(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.D.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
    ),
  )

  val EXCEPTION_CASE_3 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.NOT_PROCEED.name),
      ),
      migrationEntityBuilder.createHearing(
        oicHearingId = 2,
        hearingDateTime = LocalDateTime.now().plusDays(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.D.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
    ),
  )

  val EXCEPTION_CASE_4 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().plusDays(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.D.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
    ),
    punishments = listOf(migrationEntityBuilder.createPunishment()),
  )

  val EXCEPTION_CASE_4_INACTIVE = migrationEntityBuilder.createAdjudication(
    prisoner = migrationEntityBuilder.createPrisoner(currentAgencyId = null),
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().plusDays(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.D.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
    ),
    punishments = listOf(migrationEntityBuilder.createPunishment()),
  )

  val EXCEPTION_CASE_5 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().plusDays(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.D.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
    ),
    punishments = listOf(migrationEntityBuilder.createPunishment(code = OicSanctionCode.CC.name)),
  )

  val EXCEPTION_CASE_6 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.QUASHED.name),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().plusDays(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.D.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
    ),
    punishments = listOf(migrationEntityBuilder.createPunishment(code = OicSanctionCode.CC.name)),
  )

  val EXCEPTION_CASE_7 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.QUASHED.name),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().plusDays(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.D.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
    ),
  )

  val EXCEPTION_CASE_8 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.QUASHED.name),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().plusDays(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
    ),
  )

  val EXCEPTION_CASE_9 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.APPEAL.name),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().plusDays(1),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
    ),
  )

  val WITH_FINDING_DISMISSED = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(comment = "comment", hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.DISMISSED.name)),
    ),
  )

  val WITH_FINDING_S = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.S.name)),
    ),
  )

  val WITH_FINDING_NOT_PROVEN = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.NOT_PROVEN.name)),
    ),
  )

  val WITH_FINDING_GUILTY = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.GUILTY.name)),
    ),
  )

  val WITH_FINDING_NOT_GUILTY = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.NOT_GUILTY.name)),
    ),
  )

  val WITH_FINDING_UNFIT = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.UNFIT.name)),
    ),
  )

  val WITH_FINDING_REFUSED = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.REFUSED.name)),
    ),
  )

  fun WITH_FINDING_APPEAL(reducedSanctions: Boolean) = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name)),
      migrationEntityBuilder.createHearing(hearingDateTime = LocalDateTime.now().plusDays(1), hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.APPEAL.name)),
    ),
    punishments = if (reducedSanctions) listOf(migrationEntityBuilder.createPunishment(code = OicSanctionCode.CC.name, status = Status.REDAPP.name)) else emptyList(),
  )

  val WTIH_ADDITIONAL_HEARINGS_AFTER_OUTCOME_NOT_PROCEED = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.NOT_PROCEED.name)),
      migrationEntityBuilder.createHearing(hearingDateTime = LocalDateTime.now().plusDays(1)),
    ),
  )

  val WTIH_ADDITIONAL_HEARINGS_AFTER_OUTCOME_DISMISSED = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.D.name)),
      migrationEntityBuilder.createHearing(hearingDateTime = LocalDateTime.now().plusDays(1)),
    ),
  )

  val WTIH_ADDITIONAL_HEARINGS_IN_PAST_AFTER_OUTCOME_PROVED = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingDateTime = LocalDateTime.now().minusDays(2), hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name)),
      migrationEntityBuilder.createHearing(hearingDateTime = LocalDateTime.now().minusDays(1)),
    ),
  )

  val WTIH_ADDITIONAL_HEARINGS_IN_PAST_AFTER_OUTCOME_GUILTY = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingDateTime = LocalDateTime.now().minusDays(2), hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.GUILTY.name)),
      migrationEntityBuilder.createHearing(hearingDateTime = LocalDateTime.now().minusDays(1)),
    ),
  )

  val PLEA_NOT_MAPPED_SAME_AS_FINDING = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(
          plea = Finding.QUASHED.name,
          finding = Finding.QUASHED.name,
        ),
      ),
    ),
  )

  val PLEA_NOT_MAPPED_DOUBLE_NEGATIVE = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(
          plea = Finding.DISMISSED.name,
          finding = Finding.NOT_PROCEED.name,
        ),
      ),
    ),
  )

  val PLEA_NOT_MAPPED_DOUBLE_NEGATIVE2 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(
          plea = Finding.NOT_PROVEN.name,
          finding = Finding.NOT_PROCEED.name,
        ),
      ),
    ),
  )

  fun HEARING_BEFORE_LATEST_WITH_RESULT_EXCEPTION(finding: Finding, withResult: Boolean = true) = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        oicHearingId = 100,
        hearingDateTime = LocalDateTime.now().minusDays(1),
        hearingResult =
        migrationEntityBuilder.createHearingResult(finding = finding.name),
      ),
      migrationEntityBuilder.createHearing(
        hearingResult = if (withResult) migrationEntityBuilder.createHearingResult(finding = finding.name) else null,
      ),
    ),
  )

  val HEARING_BEFORE_LATEST_WITH_RESULT = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        oicHearingId = 100,
        hearingDateTime = LocalDateTime.now().minusDays(1),
        hearingResult =
        migrationEntityBuilder.createHearingResult(),
      ),
      migrationEntityBuilder.createHearing(),
    ),
  )

  val HEARING_BEFORE_LATEST_WITH_RESULT_QUASHED = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        oicHearingId = 100,
        hearingDateTime = LocalDateTime.now().minusDays(1),
        hearingResult =
        migrationEntityBuilder.createHearingResult(finding = Finding.QUASHED.name),
      ),
      migrationEntityBuilder.createHearing(),
    ),
  )

  val HEARING_BEFORE_LATEST = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(oicHearingId = 100, hearingDateTime = LocalDateTime.now().minusDays(1)),
      migrationEntityBuilder.createHearing(),
    ),
  )

  val NEW_HEARING_AFTER_COMPLETED = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingDateTime = LocalDateTime.now().minusDays(4), hearingResult = migrationEntityBuilder.createHearingResult()),
      migrationEntityBuilder.createHearing(oicHearingId = 100, hearingDateTime = LocalDateTime.now().plusDays(1)),
    ),
  )

  val NOT_PROCEED = migrationEntityBuilder.createAdjudication()

  val NOT_PROCEED_REPLACE_WITH_NOMIS = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingDateTime = LocalDateTime.now().minusDays(4), hearingResult = migrationEntityBuilder.createHearingResult()),
    ),
  )

  val NOT_PROCEED_CHARGE_PROVED_REPLACE_WITH_NOMIS = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingDateTime = LocalDateTime.now().minusDays(4), hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.NOT_PROCEED.name)),
      migrationEntityBuilder.createHearing(oicHearingId = 2, hearingDateTime = LocalDateTime.now().minusDays(3), hearingResult = migrationEntityBuilder.createHearingResult()),
    ),
    punishments = listOf(migrationEntityBuilder.createPunishment()),
  )

  val CHARGE_3879400 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(oicHearingId = 1, hearingDateTime = LocalDateTime.now().minusDays(3), hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.D.name)),
      migrationEntityBuilder.createHearing(oicHearingId = 2, hearingDateTime = LocalDateTime.now().minusDays(3), hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.D.name)),
    ),
  )

  val CHARGE_3990011 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(oicHearingId = 1, hearingDateTime = LocalDateTime.now().minusDays(3), hearingResult = migrationEntityBuilder.createHearingResult()),
    ),
    punishments = listOf(migrationEntityBuilder.createPunishment()),
  )

  val CHARGE_3851533 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(oicHearingId = 1, hearingDateTime = LocalDateTime.now().minusDays(3), hearingResult = migrationEntityBuilder.createHearingResult()),
      migrationEntityBuilder.createHearing(oicHearingId = 2, hearingDateTime = LocalDateTime.now().minusDays(2), hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.QUASHED.name)),
      migrationEntityBuilder.createHearing(oicHearingId = 3, hearingDateTime = LocalDateTime.now().minusDays(1), hearingResult = migrationEntityBuilder.createHearingResult()),
      migrationEntityBuilder.createHearing(oicHearingId = 4, hearingDateTime = LocalDateTime.now(), hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.QUASHED.name)),
    ),
    punishments = listOf(migrationEntityBuilder.createPunishment()),
  )

  val REFER_INAD_GIVEN_NOMIS_OUTCOME = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(oicHearingId = 1, hearingDateTime = LocalDateTime.now().minusDays(3), hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.NOT_PROCEED.name)),
    ),
  )

  val PLEA_ISSUE_1 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult(plea = Finding.QUASHED.name, finding = Finding.NOT_PROCEED.name)),
    ),
  )

  val PLEA_ISSUE_2 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult(plea = Finding.QUASHED.name, finding = Finding.PROVED.name)),
    ),
  )

  val PLEA_ISSUE_3 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult(plea = Finding.REF_POLICE.name, finding = Finding.NOT_PROCEED.name)),
    ),
  )

  val PLEA_ISSUE_4 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult(plea = Finding.QUASHED.name, finding = Finding.APPEAL.name)),
    ),
  )

  val PLEA_ISSUE_6 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult()),
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult(plea = Finding.APPEAL.name, finding = Finding.QUASHED.name)),
    ),
  )

  val PLEA_ISSUE_7 = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(hearingResult = migrationEntityBuilder.createHearingResult(plea = Finding.PROSECUTED.name, finding = Finding.REF_POLICE.name)),
    ),
  )

  fun getSelection(): List<AdjudicationMigrateDto> = listOf(
    ADULT_SINGLE_OFFENCE, YOUTH_SINGLE_OFFENCE, NON_BINARY_GENDER, UNKNOWN_GENDER,
    WITH_HEARINGS_AND_SOME_RESULTS, WITH_HEARINGS_AND_RESULTS, WITH_HEARING, WITH_NO_ADJUDICATOR,
    WITH_HEARINGS, WITH_HEARING_AND_RESULT, WITH_HEARINGS_AND_RESULTS_MULTIPLE_PROVED, FEMALE,
    FEMALE, WITH_PUNISHMENT, WITH_PUNISHMENT_DAMAGES_AMOUNT, WITH_PUNISHMENT_ADA, WITH_PUNISHMENT_DAMAGES_AMOUNT, WITH_PUNISHMENT_COMMENT,
    WITH_PUNISHMENT_STOPPAGE_PERCENTAGE, WITH_PUNISHMENT_CONSECUTIVE, WITH_PUNISHMENT_SUSPENDED, WITH_PUNISHMENT_START_DATE,
  )
}
