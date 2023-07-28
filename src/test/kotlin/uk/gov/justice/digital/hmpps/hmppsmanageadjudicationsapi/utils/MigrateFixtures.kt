package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.NomisGender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import java.math.BigDecimal
import java.time.LocalDateTime

class MigrateFixtures {
  private val migrationEntityBuilder = MigrationEntityBuilder()

  val ADULT_SINGLE_OFFENCE = migrationEntityBuilder.createAdjudication()

  val OFFENCE_NOT_ON_FILE = migrationEntityBuilder.createAdjudication(
    offence = migrationEntityBuilder.createOffence(offenceCode = "1234"),
  )

  val WITH_ASSOCIATE = migrationEntityBuilder.createAdjudication(
    associates = listOf(migrationEntityBuilder.createAssociate()),
  )

  val WITH_STAFF_VICTIM = migrationEntityBuilder.createAdjudication(
    victims = listOf(migrationEntityBuilder.createVictim()),
  )

  val WITH_PRISONER_VICTIM = migrationEntityBuilder.createAdjudication(
    victims = listOf(migrationEntityBuilder.createVictim(victimIdentifier = "QW12345", isStaff = false)),
  )

  val YOUTH_SINGLE_OFFENCE = migrationEntityBuilder.createAdjudication(
    offence = MigrateOffence(offenceCode = "55:17"),
  )

  val NON_BINARY_GENDER = migrationEntityBuilder.createAdjudication(
    prisoner = migrationEntityBuilder.createPrisoner(gender = NomisGender.NK.name),
  )

  val UNKNOWN_GENDER = migrationEntityBuilder.createAdjudication(
    prisoner = migrationEntityBuilder.createPrisoner(gender = "?"),
  )

  val ADULT_MULITPLE_OFFENCES = listOf(
    migrationEntityBuilder.createAdjudication(),
    migrationEntityBuilder.createAdjudication(offenceSequence = 2),
    migrationEntityBuilder.createAdjudication(offenceSequence = 3),
  )

  val ADULT_TRANSFER = migrationEntityBuilder.createAdjudication(
    prisoner = migrationEntityBuilder.createPrisoner(currentAgencyId = "LEI"),
  )

  val ADDITIONAL_VICTIMS = migrationEntityBuilder.createAdjudication(
    victims = listOf(
      migrationEntityBuilder.createVictim(victimIdentifier = "OFFICER_GEN", isStaff = true),
      migrationEntityBuilder.createVictim(victimIdentifier = "FD12345", isStaff = false),
      migrationEntityBuilder.createVictim(victimIdentifier = "GH12345", isStaff = false),
    ),
  )

  val ADDITIONAL_ASSOCIATES = migrationEntityBuilder.createAdjudication(
    associates = listOf(
      migrationEntityBuilder.createAssociate(associatedPrisoner = "QWERTY12"),
      migrationEntityBuilder.createAssociate(associatedPrisoner = "QWERTY13"),
    ),
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

  val WITH_PUNISHMENT_DAMAGES_AMOUNT = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = "OTHER", amount = BigDecimal(10.50)),
    ),
  )

  val WITH_PUNISHMENT_COMMENT = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(comment = "some notes"),
    ),
  )

  val WITH_PUNISHMENT_CONSECUTIVE = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = "ADA", status = "IMMEDIATE", consecutiveChargeNumber = 12345),
    ),
  )

  val WITH_PUNISHMENT_INVALID_CODE = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(code = "?"),
    ),
  )

  val WITH_PUNISHMENT_INVALID_STATUS = migrationEntityBuilder.createAdjudication(
    punishments = listOf(
      migrationEntityBuilder.createPunishment(status = "?"),
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
    ADULT_SINGLE_OFFENCE, WITH_ASSOCIATE, WITH_STAFF_VICTIM, WITH_PRISONER_VICTIM, YOUTH_SINGLE_OFFENCE, NON_BINARY_GENDER, UNKNOWN_GENDER,
    WITH_HEARINGS_AND_RESULTS_MUDDLED, WITH_HEARINGS_AND_SOME_RESULTS, WITH_HEARINGS_AND_RESULTS, WITH_HEARING, WITH_NO_ADJUDICATOR,
    WITH_HEARINGS, WITH_HEARING_AND_RESULT, WITH_HEARINGS_AND_RESULTS_MULTIPLE_PROVED,
  )
}
