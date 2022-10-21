package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.IncidentRoleAssociatedPrisonerRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.IncidentRoleRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.IncidentRoleRuleLookup.Companion.associatedPrisonerInformationRequired
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
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
        associatedPrisonerInformationRequired(draftAdjudication.incidentRole?.roleCode) &&
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
  private val authenticationFacade: AuthenticationFacade,
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
    delete()
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

  fun addIncidentStatement(id: Long, statement: String?, completed: Boolean?): DraftAdjudicationDto {
    throwIfStatementAndCompletedIsNull(statement, completed)

    val draftAdjudication = find(id)

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

    val draftAdjudication = find(id)

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
    val draftAdjudication = find(id)

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
    val draftAdjudication = find(id)

    ValidationChecks.INCIDENT_ROLE.validate(draftAdjudication)

    incidentRoleAssociatedPrisoner.let {
      draftAdjudication.incidentRole!!.associatedPrisonersNumber = it.associatedPrisonersNumber
      draftAdjudication.incidentRole!!.associatedPrisonersName = it.associatedPrisonersName
    }

    return saveToDto(draftAdjudication)
  }

  fun editIncidentStatement(id: Long, statement: String?, completed: Boolean?): DraftAdjudicationDto {
    throwIfStatementAndCompletedIsNull(statement, completed)

    val draftAdjudication = find(id)

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
    val draftAdjudication = find(id)

    if (removeExistingOffences) {
      draftAdjudication.offenceDetails?.clear()
    }

    draftAdjudication.isYouthOffender = isYouthOffender

    return saveToDto(draftAdjudication)
  }

  fun getCurrentUsersInProgressDraftAdjudications(agencyId: String): List<DraftAdjudicationDto> {
    val username = authenticationFacade.currentUsername ?: return emptyList()
    return getInProgress(agencyId, username)
  }

  private fun throwIfStatementAndCompletedIsNull(statement: String?, completed: Boolean?) {
    if (statement == null && completed == null)
      throw IllegalArgumentException("Please supply either a statement or the completed value")
  }
}
