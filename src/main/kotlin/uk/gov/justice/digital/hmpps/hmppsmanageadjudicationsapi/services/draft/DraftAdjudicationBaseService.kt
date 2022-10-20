package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DamageDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.EvidenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.WitnessDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Damage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Evidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Witness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.IncidentRoleRuleLookup
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import javax.persistence.EntityNotFoundException

open class DraftAdjudicationBaseService(
  val draftAdjudicationRepository: DraftAdjudicationRepository,
  val offenceCodeLookupService: OffenceCodeLookupService,
) {

  fun find(id: Long): DraftAdjudication =
    draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

  fun findToDto(id: Long): DraftAdjudicationDto =
    draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }
      .toDto()

  fun saveToDto(draftAdjudication: DraftAdjudication): DraftAdjudicationDto =
    draftAdjudicationRepository.save(draftAdjudication).toDto()

  fun delete(draftAdjudication: DraftAdjudication) = draftAdjudicationRepository.delete(draftAdjudication)

  fun DraftAdjudication.toDto(): DraftAdjudicationDto =
    DraftAdjudicationDto(
      id = this.id!!,
      prisonerNumber = this.prisonerNumber,
      incidentStatement = this.incidentStatement?.toDto(),
      incidentDetails = this.incidentDetails.toDto(),
      incidentRole = this.incidentRole?.toDto(this.isYouthOffender!!),
      offenceDetails = this.offenceDetails?.map { it.toDto(offenceCodeLookupService, this.isYouthOffender!!) },
      adjudicationNumber = this.reportNumber,
      startedByUserId = this.reportNumber?.let { this.reportByUserId } ?: this.createdByUserId,
      isYouthOffender = this.isYouthOffender,
      damages = this.damages.map { it.toDto() },
      evidence = this.evidence.map { it.toDto() },
      witnesses = this.witnesses.map { it.toDto() },
      damagesSaved = this.damagesSaved,
      evidenceSaved = this.evidenceSaved,
      witnessesSaved = this.witnessesSaved
    )

  private fun IncidentDetails.toDto(): IncidentDetailsDto = IncidentDetailsDto(
    locationId = this.locationId,
    dateTimeOfIncident = this.dateTimeOfIncident,
    dateTimeOfDiscovery = this.dateTimeOfDiscovery,
    handoverDeadline = this.handoverDeadline,
  )

  private fun IncidentRole.toDto(isYouthOffender: Boolean): IncidentRoleDto = IncidentRoleDto(
    roleCode = this.roleCode,
    offenceRule = IncidentRoleRuleLookup.getOffenceRuleDetails(this.roleCode, isYouthOffender),
    associatedPrisonersNumber = this.associatedPrisonersNumber,
    associatedPrisonersName = this.associatedPrisonersName,
  )

  private fun Offence.toDto(offenceCodeLookupService: OffenceCodeLookupService, isYouthOffender: Boolean): OffenceDetailsDto = OffenceDetailsDto(
    offenceCode = this.offenceCode,
    offenceRule = OffenceRuleDetailsDto(
      paragraphNumber = offenceCodeLookupService.getParagraphNumber(offenceCode, isYouthOffender),
      paragraphDescription = offenceCodeLookupService.getParagraphDescription(offenceCode, isYouthOffender),
    ),
    victimPrisonersNumber = this.victimPrisonersNumber,
    victimStaffUsername = this.victimStaffUsername,
    victimOtherPersonsName = this.victimOtherPersonsName,
  )

  private fun IncidentStatement.toDto(): IncidentStatementDto = IncidentStatementDto(
    statement = this.statement!!,
    completed = this.completed,
  )

  private fun Damage.toDto(): DamageDto = DamageDto(
    code = this.code,
    details = this.details,
    reporter = this.reporter
  )

  private fun Evidence.toDto(): EvidenceDto = EvidenceDto(
    code = this.code,
    identifier = this.identifier,
    details = this.details,
    reporter = this.reporter
  )

  fun Witness.toDto(): WitnessDto = WitnessDto(
    code = this.code,
    firstName = this.firstName,
    lastName = this.lastName,
    reporter = this.reporter
  )

  companion object {
    fun throwEntityNotFoundException(id: Long): Nothing =
      throw EntityNotFoundException("DraftAdjudication not found for $id")
  }
}
