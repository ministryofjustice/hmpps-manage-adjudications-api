package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftAdjudicationBaseService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftAdjudicationService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.ValidationChecks
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationBaseService
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

private class DraftAdjudicationServiceWrapper(
  draftAdjudicationRepository: DraftAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
) : DraftAdjudicationBaseService(
  draftAdjudicationRepository, offenceCodeLookupService
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
  reportedAdjudicationRepository, offenceCodeLookupService, authenticationFacade
) {
  fun get(adjudicationNumber: Long) =
    findByAdjudicationNumber(adjudicationNumber)

  fun save(reportedAdjudication: ReportedAdjudication) = saveToDto(reportedAdjudication)
}

@Transactional
@Service
class AdjudicationWorkflowService(
  draftAdjudicationRepository: DraftAdjudicationRepository,
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  private val prisonApiGateway: PrisonApiGateway,
  private val authenticationFacade: AuthenticationFacade,
  private val telemetryClient: TelemetryClient,
) {

  private val draftAdjudicationService = DraftAdjudicationServiceWrapper(draftAdjudicationRepository, offenceCodeLookupService)
  private val reportedAdjudicationService = ReportedAdjudicationServiceWrapper(reportedAdjudicationRepository, offenceCodeLookupService, authenticationFacade)

  fun completeDraftAdjudication(id: Long): ReportedAdjudicationDto {
    val draftAdjudication = draftAdjudicationService.get(id)

    ValidationChecks.values().toList().stream()
      .forEach { it.validate(draftAdjudication) }

    val isNew = draftAdjudication.reportNumber == null
    val generatedReportedAdjudication = saveAdjudication(draftAdjudication, isNew)
    telemetryCapture(draftAdjudication, generatedReportedAdjudication.adjudicationNumber)

    draftAdjudicationService.remove(draftAdjudication)

    return generatedReportedAdjudication
  }

  fun createDraftFromReportedAdjudication(adjudicationNumber: Long): DraftAdjudicationDto {
    val reportedAdjudication =
      reportedAdjudicationService.get(adjudicationNumber)

    val draftAdjudication = DraftAdjudication(
      reportNumber = reportedAdjudication.reportNumber,
      reportByUserId = reportedAdjudication.createdByUserId,
      prisonerNumber = reportedAdjudication.prisonerNumber,
      gender = reportedAdjudication.gender,
      agencyId = reportedAdjudication.agencyId,
      incidentDetails = IncidentDetails(
        locationId = reportedAdjudication.locationId,
        dateTimeOfIncident = reportedAdjudication.dateTimeOfIncident,
        dateTimeOfDiscovery = reportedAdjudication.dateTimeOfDiscovery,
        handoverDeadline = reportedAdjudication.handoverDeadline
      ),
      incidentRole = IncidentRole(
        roleCode = reportedAdjudication.incidentRoleCode,
        associatedPrisonersNumber = reportedAdjudication.incidentRoleAssociatedPrisonersNumber,
        associatedPrisonersName = reportedAdjudication.incidentRoleAssociatedPrisonersName,
      ),
      offenceDetails = toDraftOffence(reportedAdjudication.offenceDetails),
      incidentStatement = IncidentStatement(
        statement = reportedAdjudication.statement,
        completed = true
      ),
      isYouthOffender = reportedAdjudication.isYouthOffender,
      damages = toDraftDamages(reportedAdjudication.damages),
      evidence = toDraftEvidence(reportedAdjudication.evidence),
      witnesses = toDraftWitnesses(reportedAdjudication.witnesses),
      damagesSaved = true,
      evidenceSaved = true,
      witnessesSaved = true,
    )

    return draftAdjudicationService
      .save(draftAdjudication)
  }

  private fun saveAdjudication(draftAdjudication: DraftAdjudication, isNew: Boolean): ReportedAdjudicationDto {
    if (isNew) {
      return createReportedAdjudication(draftAdjudication)
    }
    // We need to check that the already reported adjudication is in the correct status here even though it happens
    // later when we save from the draft. This is because we do not want to call nomis only later to fail validation.
    checkStateTransition(draftAdjudication)
    return updateReportedAdjudication(draftAdjudication)
  }

  private fun checkStateTransition(draftAdjudication: DraftAdjudication) {
    val reportNumber = draftAdjudication.reportNumber!!
    val reportedAdjudication = reportedAdjudicationService.get(reportNumber)
    val fromStatus = reportedAdjudication.status
    if (!ReportedAdjudicationStatus.AWAITING_REVIEW.canTransitionFrom(fromStatus)) {
      throw IllegalStateException("Unable to complete draft adjudication ${draftAdjudication.reportNumber} as it is in the state $fromStatus")
    }
  }

  private fun createReportedAdjudication(draftAdjudication: DraftAdjudication): ReportedAdjudicationDto {
    val nomisAdjudicationCreationRequestData = prisonApiGateway.requestAdjudicationCreationData(draftAdjudication.prisonerNumber)
    return reportedAdjudicationService.save(
      ReportedAdjudication(
        bookingId = nomisAdjudicationCreationRequestData.bookingId,
        reportNumber = nomisAdjudicationCreationRequestData.adjudicationNumber,
        prisonerNumber = draftAdjudication.prisonerNumber,
        gender = draftAdjudication.gender,
        agencyId = draftAdjudication.agencyId,
        locationId = draftAdjudication.incidentDetails.locationId,
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
        draftCreatedOn = draftAdjudication.createDateTime!!,
        hearings = mutableListOf(),
        outcomes = mutableListOf(),
        disIssueHistory = mutableListOf(),
      )
    )
  }

  private fun updateReportedAdjudication(
    draftAdjudication: DraftAdjudication,
  ): ReportedAdjudicationDto {
    val reportedAdjudicationNumber = draftAdjudication.reportNumber
      ?: throw EntityNotFoundException("No reported adjudication number set on the draft adjudication")
    val previousReportedAdjudication =
      reportedAdjudicationService.get(reportedAdjudicationNumber)
    val reporter = authenticationFacade.currentUsername!!
    previousReportedAdjudication.let {
      it.bookingId = previousReportedAdjudication.bookingId
      it.reportNumber = previousReportedAdjudication.reportNumber
      it.prisonerNumber = draftAdjudication.prisonerNumber
      it.gender = draftAdjudication.gender
      it.agencyId = draftAdjudication.agencyId
      it.locationId = draftAdjudication.incidentDetails.locationId
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
      )
    }.toMutableList()

  private fun toDraftDamages(damages: MutableList<ReportedDamage>): MutableList<Damage> =
    damages.map {
      Damage(
        code = it.code,
        details = it.details,
        reporter = it.reporter
      )
    }.toMutableList()

  private fun toDraftEvidence(evidence: MutableList<ReportedEvidence>): MutableList<Evidence> =
    evidence.map {
      Evidence(
        code = it.code,
        details = it.details,
        reporter = it.reporter,
        identifier = it.identifier
      )
    }.toMutableList()

  private fun toDraftWitnesses(witnesses: MutableList<ReportedWitness>): MutableList<Witness> =
    witnesses.map {
      Witness(
        code = it.code,
        firstName = it.firstName,
        lastName = it.lastName,
        reporter = it.reporter
      )
    }.toMutableList()

  private fun toReportedOffence(draftOffences: MutableList<Offence>?): MutableList<ReportedOffence> {
    return (draftOffences ?: listOf()).map {
      ReportedOffence(
        offenceCode = it.offenceCode,
        victimPrisonersNumber = it.victimPrisonersNumber,
        victimStaffUsername = it.victimStaffUsername,
        victimOtherPersonsName = it.victimOtherPersonsName,
      )
    }.toMutableList()
  }

  private fun toReportedDamages(damages: MutableList<Damage>): MutableList<ReportedDamage> {
    return damages.map {
      ReportedDamage(
        code = it.code,
        details = it.details,
        reporter = it.reporter
      )
    }.toMutableList()
  }

  private fun toReportedEvidence(evidence: MutableList<Evidence>): MutableList<ReportedEvidence> {
    return evidence.map {
      ReportedEvidence(
        code = it.code,
        identifier = it.identifier,
        details = it.details,
        reporter = it.reporter
      )
    }.toMutableList()
  }

  private fun toReportedWitnesses(witnesses: MutableList<Witness>): MutableList<ReportedWitness> {
    return witnesses.map {
      ReportedWitness(
        code = it.code,
        firstName = it.firstName,
        lastName = it.lastName,
        reporter = it.reporter
      )
    }.toMutableList()
  }

  private fun telemetryCapture(draftAdjudication: DraftAdjudication, reportNumber: Long?) {
    telemetryClient.trackEvent(
      DraftAdjudicationService.TELEMETRY_EVENT,
      mapOf(
        "adjudicationNumber" to draftAdjudication.id.toString(),
        "agencyId" to draftAdjudication.agencyId,
        "reportNumber" to reportNumber.toString()
      ),
      null
    )
  }
}
