package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Witness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.IncidentRoleRuleLookup
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

open class DraftAdjudicationBaseService(
  private val draftAdjudicationRepository: DraftAdjudicationRepository,
  protected val offenceCodeLookupService: OffenceCodeLookupService,
  protected val authenticationFacade: AuthenticationFacade,
) {

  protected fun find(id: Long): DraftAdjudication {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }
    if (draftAdjudication.agencyId != authenticationFacade.activeCaseload) throwEntityNotFoundException(id)

    return draftAdjudication
  }

  protected fun findToDto(id: Long): DraftAdjudicationDto = find(id).toDto()

  protected fun saveToDto(draftAdjudication: DraftAdjudication): DraftAdjudicationDto =
    draftAdjudicationRepository.save(draftAdjudication).toDto()

  protected fun delete(draftAdjudication: DraftAdjudication) = draftAdjudicationRepository.delete(draftAdjudication)

  protected fun delete() {
    draftAdjudicationRepository.deleteDraftAdjudicationByCreateDateTimeBeforeAndChargeNumberIsNotNull(
      LocalDateTime.now().minusDays(DraftAdjudicationService.DAYS_TO_DELETE),
    )
  }

  protected fun getInProgress(
    agencyId: String,
    username: String,
    startDate: LocalDate,
    endDate: LocalDate,
    pageable: Pageable,
  ): Page<DraftAdjudicationDto> =
    draftAdjudicationRepository.findByAgencyIdAndCreatedByUserIdAndChargeNumberIsNullAndIncidentDetailsDateTimeOfDiscoveryBetween(
      agencyId,
      username,
      startDate.atStartOfDay(),
      endDate.atTime(LocalTime.MAX),
      pageable,
    ).map { it.toDto() }

  private fun DraftAdjudication.toDto(): DraftAdjudicationDto =
    DraftAdjudicationDto(
      id = this.id!!,
      prisonerNumber = this.prisonerNumber,
      gender = this.gender,
      incidentStatement = this.incidentStatement?.toDto(),
      incidentDetails = this.incidentDetails.toDto(),
      incidentRole = this.incidentRole?.toDto(this.isYouthOffender!!),
      offenceDetails = this.offenceDetails.firstOrNull()
        ?.toDto(offenceCodeLookupService, this.isYouthOffender!!, this.gender),
      chargeNumber = this.chargeNumber,
      startedByUserId = this.chargeNumber?.let { this.reportByUserId } ?: this.createdByUserId,
      isYouthOffender = this.isYouthOffender,
      damages = this.damages.map { it.toDto() },
      evidence = this.evidence.map { it.toDto() },
      witnesses = this.witnesses.map { it.toDto() },
      damagesSaved = this.damagesSaved,
      evidenceSaved = this.evidenceSaved,
      witnessesSaved = this.witnessesSaved,
      overrideAgencyId = this.overrideAgencyId,
      originatingAgencyId = this.agencyId,
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

  private fun Offence.toDto(
    offenceCodeLookupService: OffenceCodeLookupService,
    isYouthOffender: Boolean,
    gender: Gender,
  ): OffenceDetailsDto {
    val offenceDetails = offenceCodeLookupService.getOffenceCode(
      offenceCode = offenceCode,
      isYouthOffender = isYouthOffender,
    )
    return OffenceDetailsDto(
      offenceCode = this.offenceCode,
      offenceRule = OffenceRuleDetailsDto(
        paragraphNumber = offenceDetails.paragraph,
        paragraphDescription = offenceDetails.paragraphDescription.getParagraphDescription(gender),
      ),
      victimPrisonersNumber = this.victimPrisonersNumber,
      victimStaffUsername = this.victimStaffUsername,
      victimOtherPersonsName = this.victimOtherPersonsName,
    )
  }

  private fun IncidentStatement.toDto(): IncidentStatementDto = IncidentStatementDto(
    statement = this.statement!!,
    completed = this.completed,
  )

  private fun Damage.toDto(): DamageDto = DamageDto(
    code = this.code,
    details = this.details,
    reporter = this.reporter,
  )

  private fun Evidence.toDto(): EvidenceDto = EvidenceDto(
    code = this.code,
    identifier = this.identifier,
    details = this.details,
    reporter = this.reporter,
  )

  fun Witness.toDto(): WitnessDto = WitnessDto(
    code = this.code,
    firstName = this.firstName,
    lastName = this.lastName,
    reporter = this.reporter,
  )

  companion object {
    fun throwEntityNotFoundException(id: Long): Nothing =
      throw EntityNotFoundException("DraftAdjudication not found for $id")
  }
}
