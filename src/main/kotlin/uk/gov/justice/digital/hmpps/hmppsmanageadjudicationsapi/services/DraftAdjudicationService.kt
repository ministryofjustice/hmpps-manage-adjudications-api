package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.DamageRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.EvidenceRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.IncidentRoleAssociatedPrisonerRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.IncidentRoleRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.OffenceDetailsRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.WitnessRequestItem
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.IncidentRoleRuleLookup.Companion.associatedPrisonerInformationRequired
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

enum class ValidationChecks(val errorMessage: String) {
  APPLICABLE_RULES("No applicable rules set") {
    override fun validate(draftAdjudication: DraftAdjudication) {
      if (draftAdjudication.isYouthOffender == null)
        throw IllegalStateException(errorMessage)
    }
  },
  INCIDENT_ROLE("Please supply an incident role") {
    override fun validate(draftAdjudication: DraftAdjudication) {
      if (draftAdjudication.incidentRole == null)
        throw IllegalStateException(errorMessage)
    }
  },
  INCIDENT_ROLE_ASSOCIATED_PRISONER("Please supply the prisoner associated with the incident") {
    override fun validate(draftAdjudication: DraftAdjudication) {
      if (
        IncidentRoleRuleLookup.associatedPrisonerInformationRequired(draftAdjudication.incidentRole?.roleCode) &&
        draftAdjudication.incidentRole?.associatedPrisonersNumber == null
      )
        throw IllegalStateException(errorMessage)
    }
  },
  OFFENCE_DETAILS("Please supply at least one set of offence details") {
    override fun validate(draftAdjudication: DraftAdjudication) {
      if (draftAdjudication.offenceDetails.isEmpty())
        throw IllegalStateException(errorMessage)
    }
  },
  INCIDENT_STATEMENT("Please include an incident statement before completing this draft adjudication") {
    override fun validate(draftAdjudication: DraftAdjudication) {
      if (draftAdjudication.incidentStatement == null || draftAdjudication.incidentStatement!!.statement == null)
        throw IllegalStateException(errorMessage)
    }
  };

  abstract fun validate(draftAdjudication: DraftAdjudication)
}

@Transactional
@Service
class DraftAdjudicationService(
  draftAdjudicationRepository: DraftAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  val authenticationFacade: AuthenticationFacade,
) : DraftAdjudicationBaseService(
  draftAdjudicationRepository, offenceCodeLookupService
) {

  companion object {
    private const val DAYS_TO_ACTION = 2L
    const val DAYS_TO_DELETE = 1L
    const val TELEMETRY_EVENT = "DraftAdjudicationEvent"

    fun daysToActionFromIncident(incidentDate: LocalDateTime): LocalDateTime = incidentDate.plusDays(DAYS_TO_ACTION)
    fun dateOfDiscoveryValidation(dateTimeOfDiscovery: LocalDateTime?, dateTimeOfIncident: LocalDateTime) {
      dateTimeOfDiscovery?.let {
        if (it.isBefore(dateTimeOfIncident)) throw IllegalStateException("Date of discovery is before incident date")
      }
    }
  }

  fun deleteOrphanedDraftAdjudications() {
    draftAdjudicationRepository.deleteDraftAdjudicationByCreateDateTimeBeforeAndReportNumberIsNotNull(
      LocalDateTime.now().minusDays(DAYS_TO_DELETE)
    )
  }

  fun startNewAdjudication(
    prisonerNumber: String,
    agencyId: String,
    locationId: Long,
    dateTimeOfIncident: LocalDateTime,
    dateTimeOfDiscovery: LocalDateTime? = null,
  ): DraftAdjudicationDto {
    dateOfDiscoveryValidation(dateTimeOfDiscovery, dateTimeOfIncident)

    val actualDateTimeOfDiscovery = dateTimeOfDiscovery ?: dateTimeOfIncident

    val draftAdjudication = DraftAdjudication(
      prisonerNumber = prisonerNumber,
      agencyId = agencyId,
      incidentDetails = IncidentDetails(
        locationId = locationId,
        dateTimeOfIncident = dateTimeOfIncident,
        dateTimeOfDiscovery = actualDateTimeOfDiscovery,
        handoverDeadline = daysToActionFromIncident(actualDateTimeOfDiscovery)
      ),
      reportNumber = null,
      reportByUserId = null,
    )

    return saveToDto(draftAdjudication)
  }

  fun getDraftAdjudicationDetails(id: Long): DraftAdjudicationDto = findToDto(id)

  fun setOffenceDetails(id: Long, offenceDetails: List<OffenceDetailsRequestItem>): DraftAdjudicationDto {
    throwIfEmpty(offenceDetails)

    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }
    // NOTE: new flow sets isYouthOffender first, therefore if we do not have this set we must throw as .Dto requires it
    ValidationChecks.APPLICABLE_RULES.validate(draftAdjudication)

    val newValuesToStore = offenceDetails.map {
      Offence(
        offenceCode = it.offenceCode,
        victimPrisonersNumber = it.victimPrisonersNumber?.ifBlank { null },
        victimStaffUsername = it.victimStaffUsername?.ifBlank { null },
        victimOtherPersonsName = it.victimOtherPersonsName?.ifBlank { null },
      )
    }.toMutableList()

    draftAdjudication.offenceDetails.clear()
    draftAdjudication.offenceDetails.addAll(newValuesToStore)

    return saveToDto(draftAdjudication)
  }

  fun addIncidentStatement(id: Long, statement: String?, completed: Boolean?): DraftAdjudicationDto {
    throwIfStatementAndCompletedIsNull(statement, completed)

    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    if (draftAdjudication.incidentStatement != null)
      throw IllegalStateException("DraftAdjudication already includes the incident statement")

    draftAdjudication.incidentStatement = IncidentStatement(statement = statement, completed = completed)

    return saveToDto(draftAdjudication)
  }

  fun editIncidentDetails(
    id: Long,
    locationId: Long?,
    dateTimeOfIncident: LocalDateTime?,
    dateTimeOfDiscovery: LocalDateTime?
  ): DraftAdjudicationDto {
    dateTimeOfIncident?.let {
      dateOfDiscoveryValidation(dateTimeOfDiscovery, it)
    }

    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    locationId?.let { draftAdjudication.incidentDetails.locationId = it }
    dateTimeOfIncident?.let {
      draftAdjudication.incidentDetails.dateTimeOfIncident = it
      draftAdjudication.incidentDetails.handoverDeadline = daysToActionFromIncident(dateTimeOfDiscovery ?: it)
    }
    dateTimeOfDiscovery?.let {
      draftAdjudication.incidentDetails.dateTimeOfDiscovery = it
      draftAdjudication.incidentDetails.handoverDeadline = daysToActionFromIncident(dateTimeOfDiscovery)
    }

    return saveToDto(draftAdjudication)
  }

  fun editIncidentRole(
    id: Long,
    incidentRole: IncidentRoleRequest,
    removeExistingOffences: Boolean,
  ): DraftAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    // NOTE: new flow sets isYouthOffender first, therefore if we do not have this set we must throw as .Dto requires it
    ValidationChecks.APPLICABLE_RULES.validate(draftAdjudication)

    if (removeExistingOffences) {
      draftAdjudication.offenceDetails?.clear()
    }

    incidentRole.let {
      draftAdjudication.incidentRole = draftAdjudication.incidentRole ?: IncidentRole(
        roleCode = null,
        associatedPrisonersNumber = null,
        associatedPrisonersName = null,
      )

      draftAdjudication.incidentRole!!.roleCode = it.roleCode
      if (!associatedPrisonerInformationRequired(it.roleCode)) {
        draftAdjudication.incidentRole!!.associatedPrisonersNumber = null
        draftAdjudication.incidentRole!!.associatedPrisonersName = null
      }
    }

    return saveToDto(draftAdjudication)
  }

  fun setIncidentRoleAssociatedPrisoner(
    id: Long,
    incidentRoleAssociatedPrisoner: IncidentRoleAssociatedPrisonerRequest,
  ): DraftAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    ValidationChecks.INCIDENT_ROLE.validate(draftAdjudication)

    incidentRoleAssociatedPrisoner.let {
      draftAdjudication.incidentRole!!.associatedPrisonersNumber = it.associatedPrisonersNumber
      draftAdjudication.incidentRole!!.associatedPrisonersName = it.associatedPrisonersName
    }

    return saveToDto(draftAdjudication)
  }

  fun editIncidentStatement(id: Long, statement: String?, completed: Boolean?): DraftAdjudicationDto {
    throwIfStatementAndCompletedIsNull(statement, completed)

    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    if (draftAdjudication.incidentStatement == null)
      throw EntityNotFoundException("DraftAdjudication does not have any incident statement to update")

    statement?.let { draftAdjudication.incidentStatement?.statement = statement }
    completed?.let { draftAdjudication.incidentStatement?.completed = completed }

    return saveToDto(draftAdjudication)
  }

  fun setIncidentApplicableRule(
    id: Long,
    isYouthOffender: Boolean,
    removeExistingOffences: Boolean,
  ): DraftAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    if (removeExistingOffences) {
      draftAdjudication.offenceDetails?.clear()
    }

    draftAdjudication.isYouthOffender = isYouthOffender

    return saveToDto(draftAdjudication)
  }

  fun setDamages(id: Long, damages: List<DamageRequestItem>): DraftAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }
    val reporter = authenticationFacade.currentUsername!!

    draftAdjudication.damages.clear()
    draftAdjudication.damages.addAll(
      damages.map {
        Damage(
          code = it.code,
          details = it.details,
          reporter = reporter
        )
      }
    )
    draftAdjudication.damagesSaved = true

    return saveToDto(draftAdjudication)
  }

  fun setEvidence(id: Long, evidence: List<EvidenceRequestItem>): DraftAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }
    val reporter = authenticationFacade.currentUsername!!

    draftAdjudication.evidence.clear()
    draftAdjudication.evidence.addAll(
      evidence.map {
        Evidence(
          code = it.code,
          identifier = it.identifier,
          details = it.details,
          reporter = reporter
        )
      }
    )
    draftAdjudication.evidenceSaved = true

    return saveToDto(draftAdjudication)
  }

  fun setWitnesses(id: Long, witnesses: List<WitnessRequestItem>): DraftAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }
    val reporter = authenticationFacade.currentUsername!!

    draftAdjudication.witnesses.clear()
    draftAdjudication.witnesses.addAll(
      witnesses.map {
        Witness(
          code = it.code,
          firstName = it.firstName,
          lastName = it.lastName,
          reporter = reporter
        )
      }
    )
    draftAdjudication.witnessesSaved = true

    return saveToDto(draftAdjudication)
  }

  fun getCurrentUsersInProgressDraftAdjudications(agencyId: String): List<DraftAdjudicationDto> {
    val username = authenticationFacade.currentUsername ?: return emptyList()

    return draftAdjudicationRepository.findDraftAdjudicationByAgencyIdAndCreatedByUserIdAndReportNumberIsNull(
      agencyId,
      username
    )
      .sortedBy { it.incidentDetails.dateTimeOfIncident }
      .map { it.toDto() }
  }

  fun lookupRuleDetails(offenceCode: Int, isYouthOffender: Boolean): OffenceRuleDetailsDto {
    return OffenceRuleDetailsDto(
      paragraphNumber = offenceCodeLookupService.getParagraphNumber(offenceCode, isYouthOffender),
      paragraphDescription = offenceCodeLookupService.getParagraphDescription(offenceCode, isYouthOffender),
    )
  }

  private fun throwIfStatementAndCompletedIsNull(statement: String?, completed: Boolean?) {
    if (statement == null && completed == null)
      throw IllegalArgumentException("Please supply either a statement or the completed value")
  }

  private fun throwIfEmpty(toTest: List<Any>) {
    if (toTest.isEmpty())
      throw IllegalArgumentException("Please supply at least one set of items")
  }
}

fun IncidentDetails.toDto(): IncidentDetailsDto = IncidentDetailsDto(
  locationId = this.locationId,
  dateTimeOfIncident = this.dateTimeOfIncident,
  dateTimeOfDiscovery = this.dateTimeOfDiscovery,
  handoverDeadline = this.handoverDeadline,
)

fun IncidentRole.toDto(isYouthOffender: Boolean): IncidentRoleDto = IncidentRoleDto(
  roleCode = this.roleCode,
  offenceRule = IncidentRoleRuleLookup.getOffenceRuleDetails(this.roleCode, isYouthOffender),
  associatedPrisonersNumber = this.associatedPrisonersNumber,
  associatedPrisonersName = this.associatedPrisonersName,
)

fun Offence.toDto(offenceCodeLookupService: OffenceCodeLookupService, isYouthOffender: Boolean): OffenceDetailsDto = OffenceDetailsDto(
  offenceCode = this.offenceCode,
  offenceRule = OffenceRuleDetailsDto(
    paragraphNumber = offenceCodeLookupService.getParagraphNumber(offenceCode, isYouthOffender),
    paragraphDescription = offenceCodeLookupService.getParagraphDescription(offenceCode, isYouthOffender),
  ),
  victimPrisonersNumber = this.victimPrisonersNumber,
  victimStaffUsername = this.victimStaffUsername,
  victimOtherPersonsName = this.victimOtherPersonsName,
)

fun IncidentStatement.toDto(): IncidentStatementDto = IncidentStatementDto(
  statement = this.statement!!,
  completed = this.completed,
)

fun Damage.toDto(): DamageDto = DamageDto(
  code = this.code,
  details = this.details,
  reporter = this.reporter
)

fun Evidence.toDto(): EvidenceDto = EvidenceDto(
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
