package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.OffenceDetailsRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Damage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Evidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Witness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftAdjudicationBaseService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftOffenceService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.ValidationChecks
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationBaseService
import java.time.LocalDateTime

private class DraftAdjudicationServiceWrapper(
  draftAdjudicationRepository: DraftAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : DraftAdjudicationBaseService(
  draftAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {
  fun get(id: Long) =
    find(id)
  fun remove(draftAdjudication: DraftAdjudication) = delete(draftAdjudication)

  fun save(draftAdjudication: DraftAdjudication) = saveToDto(draftAdjudication)
}

private class ReportedAdjudicationServiceWrapper(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {
  fun get(chargeNumber: String) =
    findByChargeNumber(chargeNumber)

  fun save(reportedAdjudication: ReportedAdjudication) = saveToDto(reportedAdjudication)

  fun getUsername() = authenticationFacade.currentUsername

  fun getChargeNumber(agency: String): String = getNextChargeNumber(agency)
}

@Transactional
@Service
class AdjudicationWorkflowService(
  draftAdjudicationRepository: DraftAdjudicationRepository,
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
  private val draftOffenceService: DraftOffenceService,
) {

  private val draftAdjudicationService = DraftAdjudicationServiceWrapper(draftAdjudicationRepository, offenceCodeLookupService, authenticationFacade)
  private val reportedAdjudicationService = ReportedAdjudicationServiceWrapper(reportedAdjudicationRepository, offenceCodeLookupService, authenticationFacade)

  fun completeDraftAdjudication(id: Long): ReportedAdjudicationDto {
    val draftAdjudication = draftAdjudicationService.get(id)

    ValidationChecks.entries.stream()
      .forEach { it.validate(draftAdjudication) }

    val isResubmitted = draftAdjudication.chargeNumber != null
    val generatedReportedAdjudication = saveAdjudication(draftAdjudication, isResubmitted)

    draftAdjudicationService.remove(draftAdjudication)

    return generatedReportedAdjudication
  }

  fun createDraftFromReportedAdjudication(chargeNumber: String): DraftAdjudicationDto {
    val reportedAdjudication =
      reportedAdjudicationService.get(chargeNumber)

    val draftAdjudication = DraftAdjudication(
      chargeNumber = reportedAdjudication.chargeNumber,
      reportByUserId = reportedAdjudication.createdByUserId,
      prisonerNumber = reportedAdjudication.prisonerNumber,
      offenderBookingId = reportedAdjudication.offenderBookingId,
      gender = reportedAdjudication.gender,
      agencyId = reportedAdjudication.originatingAgencyId,
      overrideAgencyId = reportedAdjudication.overrideAgencyId,
      incidentDetails = IncidentDetails(
        locationId = reportedAdjudication.locationId,
        locationUuid = reportedAdjudication.locationUuid,
        dateTimeOfIncident = reportedAdjudication.dateTimeOfIncident,
        dateTimeOfDiscovery = reportedAdjudication.dateTimeOfDiscovery,
        handoverDeadline = reportedAdjudication.handoverDeadline,
      ),
      incidentRole = IncidentRole(
        roleCode = reportedAdjudication.incidentRoleCode,
        associatedPrisonersNumber = reportedAdjudication.incidentRoleAssociatedPrisonersNumber,
        associatedPrisonersName = reportedAdjudication.incidentRoleAssociatedPrisonersName,
      ),
      offenceDetails = toDraftOffence(reportedAdjudication.offenceDetails),
      incidentStatement = IncidentStatement(
        statement = reportedAdjudication.statement,
        completed = true,
      ),
      isYouthOffender = reportedAdjudication.isYouthOffender,
      damages = toDraftDamages(reportedAdjudication.damages),
      evidence = toDraftEvidence(reportedAdjudication.evidence),
      witnesses = toDraftWitnesses(reportedAdjudication.witnesses),
      damagesSaved = true,
      evidenceSaved = true,
      witnessesSaved = true,
      createdOnBehalfOfOfficer = reportedAdjudication.createdOnBehalfOfOfficer,
      createdOnBehalfOfReason = reportedAdjudication.createdOnBehalfOfReason,
    )

    return draftAdjudicationService
      .save(draftAdjudication)
  }

  fun setOffenceDetailsAndCompleteDraft(id: Long, offenceDetails: OffenceDetailsRequestItem): ReportedAdjudicationDto {
    draftOffenceService.setOffenceDetails(id = id, offenceDetails = offenceDetails)
    return completeDraftAdjudication(id)
  }

  private fun saveAdjudication(draftAdjudication: DraftAdjudication, isResubmitted: Boolean): ReportedAdjudicationDto {
    if (!isResubmitted) {
      return createReportedAdjudication(draftAdjudication)
    }
    checkStateTransition(draftAdjudication)
    return updateReportedAdjudication(draftAdjudication)
  }

  private fun checkStateTransition(draftAdjudication: DraftAdjudication) {
    val chargeNumber = draftAdjudication.chargeNumber!!
    val reportedAdjudication = reportedAdjudicationService.get(chargeNumber)
    val fromStatus = reportedAdjudication.status
    if (!ReportedAdjudicationStatus.AWAITING_REVIEW.canTransitionFrom(fromStatus)) {
      throw IllegalStateException("Unable to complete draft adjudication ${draftAdjudication.chargeNumber} as it is in the state $fromStatus")
    }
  }

  private fun createReportedAdjudication(draftAdjudication: DraftAdjudication): ReportedAdjudicationDto {
    val chargeNumber = reportedAdjudicationService.getChargeNumber(draftAdjudication.agencyId)

    return reportedAdjudicationService.save(
      ReportedAdjudication(
        chargeNumber = chargeNumber,
        offenderBookingId = draftAdjudication.offenderBookingId,
        prisonerNumber = draftAdjudication.prisonerNumber,
        gender = draftAdjudication.gender,
        originatingAgencyId = draftAdjudication.agencyId,
        overrideAgencyId = draftAdjudication.overrideAgencyId,
        locationId = draftAdjudication.incidentDetails.locationId,
        locationUuid = draftAdjudication.incidentDetails.locationUuid,
        dateTimeOfIncident = draftAdjudication.incidentDetails.dateTimeOfIncident,
        dateTimeOfDiscovery = draftAdjudication.incidentDetails.dateTimeOfDiscovery,
        handoverDeadline = draftAdjudication.incidentDetails.handoverDeadline,
        isYouthOffender = draftAdjudication.isYouthOffender!!,
        incidentRoleCode = draftAdjudication.incidentRole!!.roleCode,
        incidentRoleAssociatedPrisonersNumber = draftAdjudication.incidentRole!!.associatedPrisonersNumber,
        incidentRoleAssociatedPrisonersName = draftAdjudication.incidentRole!!.associatedPrisonersName,
        offenceDetails = toReportedOffence(draftAdjudication.offenceDetails),
        statement = draftAdjudication.incidentStatement!!.statement!!,
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
        damages = toReportedDamages(draftAdjudication.damages),
        evidence = toReportedEvidence(draftAdjudication.evidence),
        witnesses = toReportedWitnesses(draftAdjudication.witnesses),
        hearings = mutableListOf(),
        outcomes = mutableListOf(),
        disIssueHistory = mutableListOf(),
        punishments = mutableListOf(),
        punishmentComments = mutableListOf(),
        createdOnBehalfOfOfficer = draftAdjudication.createdOnBehalfOfOfficer,
        createdOnBehalfOfReason = draftAdjudication.createdOnBehalfOfReason,
      ),
    )
  }

  private fun updateReportedAdjudication(
    draftAdjudication: DraftAdjudication,
  ): ReportedAdjudicationDto {
    val reportedAdjudicationNumber = draftAdjudication.chargeNumber
      ?: throw EntityNotFoundException("No reported adjudication number set on the draft adjudication")
    val previousReportedAdjudication =
      reportedAdjudicationService.get(reportedAdjudicationNumber)
    val reporter = reportedAdjudicationService.getUsername()!!
    previousReportedAdjudication.let {
      it.chargeNumber = previousReportedAdjudication.chargeNumber
      it.prisonerNumber = draftAdjudication.prisonerNumber
      it.offenderBookingId = draftAdjudication.offenderBookingId
      it.gender = draftAdjudication.gender
      it.originatingAgencyId = draftAdjudication.agencyId
      it.overrideAgencyId = draftAdjudication.overrideAgencyId
      it.locationId = draftAdjudication.incidentDetails.locationId
      it.locationUuid = draftAdjudication.incidentDetails.locationUuid
      it.dateTimeOfIncident = draftAdjudication.incidentDetails.dateTimeOfIncident
      it.dateTimeOfDiscovery = draftAdjudication.incidentDetails.dateTimeOfDiscovery
      it.handoverDeadline = draftAdjudication.incidentDetails.handoverDeadline
      it.isYouthOffender = draftAdjudication.isYouthOffender!!
      it.incidentRoleCode = draftAdjudication.incidentRole!!.roleCode
      it.incidentRoleAssociatedPrisonersNumber = draftAdjudication.incidentRole!!.associatedPrisonersNumber
      it.incidentRoleAssociatedPrisonersName = draftAdjudication.incidentRole!!.associatedPrisonersName
      it.offenceDetails.clear()
      it.offenceDetails.addAll(toReportedOffence(draftAdjudication.offenceDetails))
      it.statement = draftAdjudication.incidentStatement!!.statement!!
      it.transition(ReportedAdjudicationStatus.AWAITING_REVIEW)
      it.dateTimeResubmitted = LocalDateTime.now()

      val damagesPreserve = it.damages.filter { damage -> damage.reporter != reporter }
      val evidencePreserve = it.evidence.filter { evidence -> evidence.reporter != reporter }
      val witnessesPreserve = it.witnesses.filter { witness -> witness.reporter != reporter }

      it.damages.clear()
      it.evidence.clear()
      it.witnesses.clear()

      it.damages.addAll(damagesPreserve)
      it.evidence.addAll(evidencePreserve)
      it.witnesses.addAll(witnessesPreserve)

      it.damages.addAll(toReportedDamages(draftAdjudication.damages.filter { d -> d.reporter == reporter }.toMutableList()))
      it.evidence.addAll(toReportedEvidence(draftAdjudication.evidence.filter { e -> e.reporter == reporter }.toMutableList()))
      it.witnesses.addAll(toReportedWitnesses(draftAdjudication.witnesses.filter { w -> w.reporter == reporter }.toMutableList()))
      it.createdOnBehalfOfOfficer = draftAdjudication.createdOnBehalfOfOfficer
      it.createdOnBehalfOfReason = draftAdjudication.createdOnBehalfOfReason

      return reportedAdjudicationService.save(it)
    }
  }

  private fun toDraftOffence(offences: MutableList<ReportedOffence>): MutableList<Offence> =
    offences.map { offence ->
      Offence(
        offenceCode = offence.offenceCode,
        victimPrisonersNumber = offence.victimPrisonersNumber,
        victimStaffUsername = offence.victimStaffUsername,
        victimOtherPersonsName = offence.victimOtherPersonsName,
        protectedCharacteristics = offence.protectedCharacteristics.map { it.toDraftProtectedCharacteristics() }.toMutableList(),
      )
    }.toMutableList()

  private fun toDraftDamages(damages: MutableList<ReportedDamage>): MutableList<Damage> =
    damages.map {
      Damage(
        code = it.code,
        details = it.details,
        reporter = it.reporter,
      )
    }.toMutableList()

  private fun toDraftEvidence(evidence: MutableList<ReportedEvidence>): MutableList<Evidence> =
    evidence.map {
      Evidence(
        code = it.code,
        details = it.details,
        reporter = it.reporter,
        identifier = it.identifier,
      )
    }.toMutableList()

  private fun toDraftWitnesses(witnesses: MutableList<ReportedWitness>): MutableList<Witness> =
    witnesses.map {
      Witness(
        code = it.code,
        firstName = it.firstName,
        lastName = it.lastName,
        reporter = it.reporter,
        username = it.username,
      )
    }.toMutableList()

  private fun toReportedOffence(draftOffences: MutableList<Offence>?): MutableList<ReportedOffence> {
    return (draftOffences ?: listOf()).map {
      ReportedOffence(
        offenceCode = it.offenceCode,
        victimPrisonersNumber = it.victimPrisonersNumber,
        victimStaffUsername = it.victimStaffUsername,
        victimOtherPersonsName = it.victimOtherPersonsName,
        protectedCharacteristics = it.protectedCharacteristics.map { pc -> pc.toProtectedCharacteristics() }.toMutableList(),
      )
    }.toMutableList()
  }

  private fun toReportedDamages(damages: MutableList<Damage>): MutableList<ReportedDamage> {
    return damages.map {
      ReportedDamage(
        code = it.code,
        details = it.details,
        reporter = it.reporter,
      )
    }.toMutableList()
  }

  private fun toReportedEvidence(evidence: MutableList<Evidence>): MutableList<ReportedEvidence> {
    return evidence.map {
      ReportedEvidence(
        code = it.code,
        identifier = it.identifier,
        details = it.details,
        reporter = it.reporter,
      )
    }.toMutableList()
  }

  private fun toReportedWitnesses(witnesses: MutableList<Witness>): MutableList<ReportedWitness> {
    return witnesses.map {
      ReportedWitness(
        code = it.code,
        firstName = it.firstName,
        lastName = it.lastName,
        reporter = it.reporter,
        username = it.username,
      )
    }.toMutableList()
  }
}
