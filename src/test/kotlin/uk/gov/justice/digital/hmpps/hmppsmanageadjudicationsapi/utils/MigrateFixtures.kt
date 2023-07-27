package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.NomisGender

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
}
