package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateAssociate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigratePrisoner
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateVictim
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.NomisGender
import java.time.LocalDateTime

class MigrationEntityBuilder {

  fun createAdjudication(
    agencyIncidentId: Long = 1,
    oicIncidentId: Long = 1,
    offenceSequence: Long = 1,
    prisoner: MigratePrisoner = createPrisoner(),
    offence: MigrateOffence = createOffence(),
    victim: MigrateVictim? = null,
    associate: MigrateAssociate? = null,
  ): AdjudicationMigrateDto =
    AdjudicationMigrateDto(
      agencyIncidentId = agencyIncidentId,
      oicIncidentId = oicIncidentId,
      offenceSequence = offenceSequence,
      bookingId = 1,
      agencyId = "MDI",
      incidentDateTime = LocalDateTime.now(),
      locationId = 1,
      statement = "some details",
      prisoner = prisoner,
      offence = offence,
      victim = victim,
      associate = associate,
    )

  fun createPrisoner(prisonerNumber: String = "AE12345", currentAgencyId: String? = null, gender: NomisGender = NomisGender.M): MigratePrisoner =
    MigratePrisoner(
      prisonerNumber = prisonerNumber,
      currentAgencyId = currentAgencyId,
      gender = gender,
    )

  fun createOffence(offenceCode: String = "51:17"): MigrateOffence = MigrateOffence(offenceCode = offenceCode)

  fun createVictim(victimIdentifier: String = "SWATSON_GEN", isStaff: Boolean = true): MigrateVictim = MigrateVictim(victimIdentifier = victimIdentifier, isStaff = isStaff)

  fun createAssociate(): MigrateAssociate = MigrateAssociate(associatedPrisoner = "ZY12345")
}
