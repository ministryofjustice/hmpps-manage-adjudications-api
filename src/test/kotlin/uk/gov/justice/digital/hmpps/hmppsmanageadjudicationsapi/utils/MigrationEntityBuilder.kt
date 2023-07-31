package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateAssociate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateHearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateHearingResult
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigratePrisoner
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigratePunishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateVictim
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.NomisGender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportingOfficer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicSanctionCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Plea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Status
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.random.Random

class MigrationEntityBuilder {

  fun createAdjudication(
    agencyIncidentId: Long = 1,
    oicIncidentId: Long = Random.nextLong(10000, 999999),
    offenceSequence: Long = 1,
    prisoner: MigratePrisoner = createPrisoner(),
    offence: MigrateOffence = createOffence(),
    victims: List<MigrateVictim> = emptyList(),
    associates: List<MigrateAssociate> = emptyList(),
    witnesses: List<MigrateWitness> = emptyList(),
    damages: List<MigrateDamage> = emptyList(),
    evidence: List<MigrateEvidence> = emptyList(),
    punishments: List<MigratePunishment> = emptyList(),
    hearings: List<MigrateHearing> = emptyList(),
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
      punishments = punishments,
      hearings = hearings,
    )

  fun createPrisoner(
    prisonerNumber: String = "AE12345",
    currentAgencyId: String? = null,
    gender: String = NomisGender.M.name,
  ): MigratePrisoner =
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

  fun createPunishment(
    code: String = OicSanctionCode.CAUTION.name,
    status: String = Status.IMMEDIATE.name,
    comment: String? = null,
    amount: BigDecimal? = null,
    days: Int? = 1,
    effectiveDate: LocalDateTime = LocalDateTime.now(),
    consecutiveChargeNumber: Long? = null,
  ): MigratePunishment =
    MigratePunishment(
      sanctionCode = code,
      sanctionStatus = status,
      sanctionSeq = 1,
      comment = comment,
      compensationAmount = amount,
      consecutiveChargeNumber = consecutiveChargeNumber,
      days = days,
      effectiveDate = effectiveDate,
    )

  fun createHearing(
    comment: String? = null,
    adjudicator: String? = "ALO_GEN",
    hearingDateTime: LocalDateTime = LocalDateTime.now(),
    hearingResult: MigrateHearingResult? = null,
  ): MigrateHearing = MigrateHearing(
    oicHearingId = 1,
    oicHearingType = OicHearingType.GOV_ADULT,
    hearingDateTime = hearingDateTime,
    commentText = comment,
    locationId = 1,
    adjudicator = adjudicator,
    hearingResult = hearingResult,
  )

  fun createHearingResult(
    plea: String = Plea.NOT_GUILTY.name,
    finding: String = Finding.PROVED.name,
    createdDateTime: LocalDateTime = LocalDateTime.now(),
  ): MigrateHearingResult = MigrateHearingResult(
    plea = plea,
    finding = finding,
    createdDateTime = createdDateTime,
    createdBy = "ALO_GEN",
  )
}