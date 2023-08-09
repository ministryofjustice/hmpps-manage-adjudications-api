package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.NomisGender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicSanctionCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class MigrateFixtures {

  private val migrationEntityBuilder = MigrationEntityBuilder()

  val ADULT_SINGLE_OFFENCE = migrationEntityBuilder.createAdjudication()

  fun ADULT_WITH_REPORTED_DATE_TIME(reportedDateTime: LocalDateTime) = migrationEntityBuilder.createAdjudication(
    reportedDateTime = reportedDateTime,
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

  val WITH_PUNISHMENT_CC = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.CC.name),
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

  val WITH_PUNISHMENT_PROSPECITVE_ADA_SUSPENDED = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = "ADA", status = Status.SUSP_PROSP.name, days = 10, effectiveDate = LocalDate.now()),
    ),
  )

  val WITH_PUNISHMENT_DAMAGES_AMOUNT = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = "OTHER", amount = BigDecimal(10.50)),
    ),
  )

  val WITH_PUNISHMENT_DAMAGES_NO_AMOUNT = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = "OTHER"),
    ),
  )

  val WITH_PUNISHMENT_STOPPAGE_PERCENTAGE = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = OicSanctionCode.STOP_PCT.name, amount = BigDecimal(10.50)),
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

  val MULTIPLE_OFFENDERS = listOf(
    migrationEntityBuilder.createAdjudication(
      oicIncidentId = 2,
      prisoner = migrationEntityBuilder.createPrisoner(
        prisonerNumber = "AB12345",
      ),
    ),
    migrationEntityBuilder.createAdjudication(
      oicIncidentId = 3,
      prisoner = migrationEntityBuilder.createPrisoner(
        prisonerNumber = "AC12345",
      ),
    ),
    migrationEntityBuilder.createAdjudication(
      oicIncidentId = 4,
      prisoner = migrationEntityBuilder.createPrisoner(
        prisonerNumber = "AD12345",
      ),
    ),
  )

  val MULTIPLE_OFFENDERS_AND_OFFENCES = listOf(
    migrationEntityBuilder.createAdjudication(
      oicIncidentId = 2,
      prisoner = migrationEntityBuilder.createPrisoner(
        prisonerNumber = "AB12345",
      ),
    ),
    migrationEntityBuilder.createAdjudication(
      oicIncidentId = 2,
      offenceSequence = 2,
      prisoner = migrationEntityBuilder.createPrisoner(
        prisonerNumber = "AB12345",
      ),
    ),
    migrationEntityBuilder.createAdjudication(
      oicIncidentId = 3,
      prisoner = migrationEntityBuilder.createPrisoner(
        prisonerNumber = "AC12345",
      ),
    ),
    migrationEntityBuilder.createAdjudication(
      oicIncidentId = 3,
      offenceSequence = 2,
      prisoner = migrationEntityBuilder.createPrisoner(
        prisonerNumber = "AC12345",
      ),
    ),
    migrationEntityBuilder.createAdjudication(
      oicIncidentId = 4,
      prisoner = migrationEntityBuilder.createPrisoner(
        prisonerNumber = "AD12345",
      ),
    ),
    migrationEntityBuilder.createAdjudication(
      oicIncidentId = 4,
      offenceSequence = 2,
      prisoner = migrationEntityBuilder.createPrisoner(
        prisonerNumber = "AD12345",
      ),
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
        hearingResult = migrationEntityBuilder.createHearingResult(),
      ),
    ),
  )

  val WITH_HEARING_AND_PROSCUTION = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROSECUTED.name),
      ),
    ),
  )

  val POLICE_REFERRAL_NEW_HEARING = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.REF_POLICE.name),
      ),
      migrationEntityBuilder.createHearing(),
    ),
  )

  val MULITPLE_POLICE_REEER_TO_PROSECUTION = migrationEntityBuilder.createAdjudication(
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

  val WITH_HEARINGS_AND_RESULTS_MUDDLED = migrationEntityBuilder.createAdjudication(
    hearings = listOf(
      migrationEntityBuilder.createHearing(
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().plusDays(1),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().plusDays(2),
        hearingResult = migrationEntityBuilder.createHearingResult(finding = Finding.PROVED.name, createdDateTime = LocalDateTime.now().plusDays(1)),
      ),
      migrationEntityBuilder.createHearing(
        hearingDateTime = LocalDateTime.now().plusDays(3),
      ),
    ),
  )
  fun getAll(): List<AdjudicationMigrateDto> = listOf(
    ADULT_SINGLE_OFFENCE, YOUTH_SINGLE_OFFENCE, NON_BINARY_GENDER, UNKNOWN_GENDER,
    WITH_HEARINGS_AND_RESULTS_MUDDLED, WITH_HEARINGS_AND_SOME_RESULTS, WITH_HEARINGS_AND_RESULTS, WITH_HEARING, WITH_NO_ADJUDICATOR,
    WITH_HEARINGS, WITH_HEARING_AND_RESULT, WITH_HEARINGS_AND_RESULTS_MULTIPLE_PROVED, FEMALE,
    FEMALE, WITH_PUNISHMENT, WITH_PUNISHMENT_DAMAGES_AMOUNT, WITH_PUNISHMENT_ADA, WITH_PUNISHMENT_DAMAGES_AMOUNT, WITH_PUNISHMENT_COMMENT,
    WITH_PUNISHMENT_STOPPAGE_PERCENTAGE, WITH_PUNISHMENT_CONSECUTIVE, WITH_PUNISHMENT_SUSPENDED, WITH_PUNISHMENT_START_DATE,
  )
}
