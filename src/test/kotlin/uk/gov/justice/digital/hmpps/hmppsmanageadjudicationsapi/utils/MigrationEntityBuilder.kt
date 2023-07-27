package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateAssociate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigratePrisoner
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateVictim
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.NomisGender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportingOfficer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import java.time.LocalDateTime

class MigrationEntityBuilder {

  fun createAdjudication(
    agencyIncidentId: Long = 1,
    oicIncidentId: Long = 1,
    offenceSequence: Long = 1,
    prisoner: MigratePrisoner = createPrisoner(),
    offence: MigrateOffence = createOffence(),
    victims: List<MigrateVictim> = emptyList(),
    associates: List<MigrateAssociate> = emptyList(),
    witnesses: List<MigrateWitness> = emptyList(),
    damages: List<MigrateDamage> = emptyList(),
    evidence: List<MigrateEvidence> = emptyList(),
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
      victims = victims,
      associates = associates,
      reportingOfficer = ReportingOfficer(username = "dave"),
      createdByUsername = "alo",
      witnesses = witnesses,
      damages = damages,
      evidence = evidence,
    )

  fun createPrisoner(prisonerNumber: String = "AE12345", currentAgencyId: String? = null, gender: String = NomisGender.M.name): MigratePrisoner =
    MigratePrisoner(
      prisonerNumber = prisonerNumber,
      currentAgencyId = currentAgencyId,
      gender = gender,
    )

  fun createOffence(offenceCode: String = "51:17"): MigrateOffence = MigrateOffence(offenceCode = offenceCode)

  fun createVictim(victimIdentifier: String = "SWATSON_GEN", isStaff: Boolean = true): MigrateVictim = MigrateVictim(victimIdentifier = victimIdentifier, isStaff = isStaff)

  fun createAssociate(associatedPrisoner: String = "ZY12345"): MigrateAssociate = MigrateAssociate(associatedPrisoner = associatedPrisoner)

  fun createWitness(): MigrateWitness = MigrateWitness(firstName = "first", lastName = "last", createdBy = "OFFICER_GEN", witnessType = WitnessCode.OFFICER)

  fun createDamage(details: String? = "something"): MigrateDamage = MigrateDamage(damageType = DamageCode.CLEANING, details = details, createdBy = "OFFICER_GEN")

  fun createEvidence(): MigrateEvidence = MigrateEvidence(evidenceCode = EvidenceCode.PHOTO, details = "details", reporter = "OFFICER_GEN")
}
