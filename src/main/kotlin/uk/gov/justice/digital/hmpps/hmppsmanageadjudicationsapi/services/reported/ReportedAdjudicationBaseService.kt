package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedDamageDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedEvidenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedWitnessDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.IncidentRoleRuleLookup
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import javax.persistence.EntityNotFoundException

open class ReportedDtoService(
  internal val offenceCodeLookupService: OffenceCodeLookupService,
) {
  fun ReportedAdjudication.toDto(): ReportedAdjudicationDto = ReportedAdjudicationDto(
    adjudicationNumber = reportNumber,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    incidentDetails = IncidentDetailsDto(
      locationId = locationId,
      dateTimeOfIncident = dateTimeOfIncident,
      dateTimeOfDiscovery = dateTimeOfDiscovery,
      handoverDeadline = handoverDeadline
    ),
    isYouthOffender = isYouthOffender,
    incidentRole = IncidentRoleDto(
      roleCode = incidentRoleCode,
      offenceRule = IncidentRoleRuleLookup.getOffenceRuleDetails(incidentRoleCode, isYouthOffender),
      associatedPrisonersNumber = incidentRoleAssociatedPrisonersNumber,
      associatedPrisonersName = incidentRoleAssociatedPrisonersName,
    ),
    offenceDetails = toReportedOffence(offenceDetails, isYouthOffender, offenceCodeLookupService),
    incidentStatement = IncidentStatementDto(
      statement = statement,
      completed = true,
    ),
    createdByUserId = createdByUserId!!,
    createdDateTime = createDateTime!!,
    reviewedByUserId = reviewUserId,
    damages = toReportedDamages(damages),
    evidence = toReportedEvidence(evidence),
    witnesses = toReportedWitnesses(witnesses),
    status = status,
    statusReason = statusReason,
    statusDetails = statusDetails,
    hearings = toHearings(hearings),
  )

  private fun toReportedOffence(offences: MutableList<ReportedOffence>, isYouthOffender: Boolean, offenceCodeLookupService: OffenceCodeLookupService): List<OffenceDto> =
    offences.map { offence ->
      OffenceDto(
        offenceCode = offence.offenceCode,
        offenceRule = OffenceRuleDto(
          paragraphNumber = offenceCodeLookupService.getParagraphNumber(offence.offenceCode, isYouthOffender),
          paragraphDescription = offenceCodeLookupService.getParagraphDescription(offence.offenceCode, isYouthOffender),
        ),
        victimPrisonersNumber = offence.victimPrisonersNumber,
        victimStaffUsername = offence.victimStaffUsername,
        victimOtherPersonsName = offence.victimOtherPersonsName,
      )
    }.toList()

  private fun toReportedDamages(damages: MutableList<ReportedDamage>): List<ReportedDamageDto> =
    damages.map {
      ReportedDamageDto(
        code = it.code,
        details = it.details,
        reporter = it.reporter
      )
    }.toList()

  private fun toReportedEvidence(evidence: MutableList<ReportedEvidence>): List<ReportedEvidenceDto> =
    evidence.map {
      ReportedEvidenceDto(
        code = it.code,
        identifier = it.identifier,
        details = it.details,
        reporter = it.reporter
      )
    }.toList()

  private fun toReportedWitnesses(witnesses: MutableList<ReportedWitness>): List<ReportedWitnessDto> =
    witnesses.map {
      ReportedWitnessDto(
        code = it.code,
        firstName = it.firstName,
        lastName = it.lastName,
        reporter = it.reporter
      )
    }.toList()

  private fun toHearings(hearings: MutableList<Hearing>): List<HearingDto> =
    hearings.map {
      HearingDto(
        id = it.id,
        locationId = it.locationId,
        dateTimeOfHearing = it.dateTimeOfHearing
      )
    }.toList()
}

open class ReportedAdjudicationBaseService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  internal val authenticationFacade: AuthenticationFacade,
) : ReportedDtoService(offenceCodeLookupService) {

  fun findByAdjudicationNumber(adjudicationNumber: Long) =
    reportedAdjudicationRepository.findByReportNumber(adjudicationNumber) ?: throwEntityNotFoundException(
      adjudicationNumber
    )
  fun save(reportedAdjudication: ReportedAdjudication): ReportedAdjudication =
    reportedAdjudicationRepository.save(reportedAdjudication)
  fun saveToDto(reportedAdjudication: ReportedAdjudication): ReportedAdjudicationDto =
    reportedAdjudicationRepository.save(reportedAdjudication).toDto()

  fun findByReportNumberIn(adjudicationNumbers: List<Long>) = reportedAdjudicationRepository.findByReportNumberIn(adjudicationNumbers)

  companion object {
    fun throwEntityNotFoundException(id: Long): Nothing =
      throw EntityNotFoundException("ReportedAdjudication not found for $id")
  }
}
