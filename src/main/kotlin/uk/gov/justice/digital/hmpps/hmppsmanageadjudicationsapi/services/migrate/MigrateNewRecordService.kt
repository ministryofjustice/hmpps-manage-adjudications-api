package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.ChargeNumberMapping
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.MigrateResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateAssociate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateVictim
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.NomisGender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.AdditionalAssociate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.AdditionalVictim
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftAdjudicationService

@Transactional
@Service
class MigrateNewRecordService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {
  fun accept(adjudicationMigrateDto: AdjudicationMigrateDto): MigrateResponse {
    val chargeNumber = adjudicationMigrateDto.getChargeNumber()
    val associates = adjudicationMigrateDto.associates.toAssociates()
    val firstAssociate = associates.removeFirstOrNull()

    val reportedAdjudication = ReportedAdjudication(
      chargeNumber = chargeNumber,
      agencyIncidentId = adjudicationMigrateDto.agencyIncidentId,
      prisonerNumber = adjudicationMigrateDto.prisoner.prisonerNumber,
      offenderBookingId = adjudicationMigrateDto.bookingId,
      originatingAgencyId = adjudicationMigrateDto.agencyId,
      overrideAgencyId = adjudicationMigrateDto.getOverrideAgencyId(),
      dateTimeOfDiscovery = adjudicationMigrateDto.incidentDateTime,
      dateTimeOfIncident = adjudicationMigrateDto.incidentDateTime,
      incidentRoleCode = adjudicationMigrateDto.getIncidentRole(),
      incidentRoleAssociatedPrisonersNumber = firstAssociate?.incidentRoleAssociatedPrisonersNumber,
      incidentRoleAssociatedPrisonersName = null,
      additionalAssociates = associates,
      damages = adjudicationMigrateDto.damages.toDamages(),
      evidence = adjudicationMigrateDto.evidence.toEvidence(),
      witnesses = adjudicationMigrateDto.witnesses.toWitnesses(),
      disIssueHistory = mutableListOf(),
      draftCreatedOn = adjudicationMigrateDto.incidentDateTime,
      status = ReportedAdjudicationStatus.UNSCHEDULED,
      handoverDeadline = DraftAdjudicationService.daysToActionFromIncident(adjudicationMigrateDto.incidentDateTime),
      gender = adjudicationMigrateDto.getGender(),
      hearings = mutableListOf(),
      punishments = mutableListOf(),
      punishmentComments = mutableListOf(),
      isYouthOffender = adjudicationMigrateDto.offence.getIsYouthOffender(),
      locationId = adjudicationMigrateDto.locationId,
      outcomes = mutableListOf(),
      statement = adjudicationMigrateDto.statement,
      offenceDetails = mutableListOf(adjudicationMigrateDto.getOffenceDetails()),
      migrated = true,
    )

    reportedAdjudicationRepository.save(reportedAdjudication)

    return MigrateResponse(
      chargeNumberMapping = ChargeNumberMapping(
        oicIncidentId = adjudicationMigrateDto.oicIncidentId,
        chargeNumber = chargeNumber,
        offenceSequence = adjudicationMigrateDto.offenceSequence,
      ),
    )
  }

  companion object {

    fun AdjudicationMigrateDto.getChargeNumber(): String = "${this.oicIncidentId}-${this.offenceSequence}"

    fun MigrateOffence.getIsYouthOffender(): Boolean = this.offenceCode.startsWith("55:")

    fun AdjudicationMigrateDto.getIncidentRole(): String? = null

    fun AdjudicationMigrateDto.getGender(): Gender =
      when (this.prisoner.gender) {
        NomisGender.F.name -> Gender.FEMALE
        else -> Gender.MALE
      }

    fun AdjudicationMigrateDto.getOverrideAgencyId(): String? {
      this.prisoner.currentAgencyId ?: return null

      return if (this.agencyId != this.prisoner.currentAgencyId) this.prisoner.currentAgencyId else null
    }

    fun List<MigrateAssociate>.toAssociates(): MutableList<AdditionalAssociate> =
      this.map { AdditionalAssociate(incidentRoleAssociatedPrisonersNumber = it.associatedPrisoner) }.toMutableList()

    fun List<MigrateDamage>.toDamages(): MutableList<ReportedDamage> =
      this.map {
        ReportedDamage(
          code = it.damageType,
          details = it.details ?: "No recorded details",
          reporter = it.createdBy,
        )
      }.toMutableList()

    fun List<MigrateEvidence>.toEvidence(): MutableList<ReportedEvidence> =
      this.map {
        ReportedEvidence(
          code = it.evidenceCode,
          details = it.details,
          reporter = it.reporter,
        )
      }.toMutableList()

    fun List<MigrateWitness>.toWitnesses(): MutableList<ReportedWitness> =
      this.map {
        ReportedWitness(
          firstName = it.firstName,
          lastName = it.lastName,
          reporter = it.createdBy,
          code = it.witnessType,
        )
      }.toMutableList()

    fun AdjudicationMigrateDto.getOffenceDetails(): ReportedOffence {
      val victims = this.victims.toVictims()
      val firstVictim = victims.removeFirstOrNull()
      return ReportedOffence(
        offenceCode = 0,
        victimPrisonersNumber = firstVictim?.victimPrisonersNumber,
        victimStaffUsername = firstVictim?.victimStaffUsername,
        additionalVictims = victims,
      )
    }

    private fun List<MigrateVictim>.toVictims(): MutableList<AdditionalVictim> =
      this.map {
        AdditionalVictim(
          victimStaffUsername = if (it.isStaff) it.victimIdentifier else null,
          victimPrisonersNumber = if (!it.isStaff) it.victimIdentifier else null,
        )
      }.toMutableList()
  }
}
