package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.IncidentRoleAssociatedPrisonerRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.IncidentRoleRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.ForbiddenException
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.IncidentRoleRuleLookup.Companion.associatedPrisonerInformationRequired
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

enum class ValidationChecks(val errorMessage: String) {
  APPLICABLE_RULES("No applicable rules set") {
    override fun validate(draftAdjudication: DraftAdjudication) {
      if (draftAdjudication.isYouthOffender == null) {
        throw IllegalStateException(errorMessage)
      }
    }
  },
  INCIDENT_ROLE("Please supply an incident role") {
    override fun validate(draftAdjudication: DraftAdjudication) {
      if (draftAdjudication.incidentRole == null) {
        throw IllegalStateException(errorMessage)
      }
    }
  },
  INCIDENT_ROLE_ASSOCIATED_PRISONER("Please supply the prisoner associated with the incident") {
    override fun validate(draftAdjudication: DraftAdjudication) {
      if (
        associatedPrisonerInformationRequired(draftAdjudication.incidentRole?.roleCode) &&
        draftAdjudication.incidentRole?.associatedPrisonersNumber == null
      ) {
        throw IllegalStateException(errorMessage)
      }
    }
  },
  OFFENCE_DETAILS("Please supply at least one set of offence details") {
    override fun validate(draftAdjudication: DraftAdjudication) {
      if (draftAdjudication.offenceDetails.isEmpty()) {
        throw IllegalStateException(errorMessage)
      }
    }
  },
  INCIDENT_STATEMENT("Please include an incident statement before completing this draft adjudication") {
    override fun validate(draftAdjudication: DraftAdjudication) {
      if (draftAdjudication.incidentStatement == null || draftAdjudication.incidentStatement!!.statement == null) {
        throw IllegalStateException(errorMessage)
      }
    }
  },
  ;

  abstract fun validate(draftAdjudication: DraftAdjudication)
}

@Transactional
@Service
class DraftAdjudicationService(
  draftAdjudicationRepository: DraftAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : DraftAdjudicationBaseService(
  draftAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {

  companion object {
    private const val DAYS_TO_ACTION = 2L
    const val DAYS_TO_DELETE = 1L

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

  fun deleteDraftAdjudications(id: Long) {
    val draftAdjudication = find(id)
    val username = authenticationFacade.currentUsername
    if (draftAdjudication.createdByUserId == username) {
      delete(draftAdjudication)
    } else {
      throw ForbiddenException("Only creator(owner) of draft adjudication can delete draft adjudication report. Owner username: ${draftAdjudication.createdByUserId}, deletion attempt by username: $username.")
    }
  }

  fun startNewAdjudication(
    prisonerNumber: String,
    gender: Gender,
    agencyId: String,
    overrideAgencyId: String?,
    locationId: Long,
    dateTimeOfIncident: LocalDateTime,
    dateTimeOfDiscovery: LocalDateTime? = null,
    offenderBookingId: Long? = null,
    locationUuid: UUID? = null,
  ): DraftAdjudicationDto {
    dateOfDiscoveryValidation(dateTimeOfDiscovery, dateTimeOfIncident)

    val actualDateTimeOfDiscovery = dateTimeOfDiscovery ?: dateTimeOfIncident

    val draftAdjudication = DraftAdjudication(
      prisonerNumber = prisonerNumber,
      offenderBookingId = offenderBookingId,
      gender = gender,
      agencyId = agencyId,
      overrideAgencyId = overrideAgencyId,
      incidentDetails = IncidentDetails(
        locationId = locationId,
        locationUuid = locationUuid,
        dateTimeOfIncident = dateTimeOfIncident,
        dateTimeOfDiscovery = actualDateTimeOfDiscovery,
        handoverDeadline = daysToActionFromIncident(actualDateTimeOfDiscovery),
      ),
      chargeNumber = null,
      reportByUserId = null,
    )

    return saveToDto(draftAdjudication)
  }

  fun getDraftAdjudicationDetails(id: Long): DraftAdjudicationDto = findToDto(id)

  fun addIncidentStatement(id: Long, statement: String?, completed: Boolean?): DraftAdjudicationDto {
    throwIfStatementAndCompletedIsNull(statement, completed)

    val draftAdjudication = find(id)

    if (draftAdjudication.incidentStatement != null) {
      throw IllegalStateException("DraftAdjudication already includes the incident statement")
    }

    draftAdjudication.incidentStatement = IncidentStatement(statement = statement, completed = completed)

    return saveToDto(draftAdjudication)
  }

  fun editIncidentDetails(
    id: Long,
    locationId: Long,
    locationUuid: UUID? = null,
    dateTimeOfIncident: LocalDateTime,
    dateTimeOfDiscovery: LocalDateTime?,
  ): DraftAdjudicationDto {
    dateOfDiscoveryValidation(dateTimeOfDiscovery, dateTimeOfIncident)

    val draftAdjudication = find(id)
    val actualDateTimeOfDiscovery = dateTimeOfDiscovery ?: dateTimeOfIncident

    draftAdjudication.incidentDetails.locationId = locationId
    draftAdjudication.incidentDetails.locationUuid = locationUuid
    draftAdjudication.incidentDetails.dateTimeOfIncident = dateTimeOfIncident
    draftAdjudication.incidentDetails.dateTimeOfDiscovery = actualDateTimeOfDiscovery
    draftAdjudication.incidentDetails.handoverDeadline = daysToActionFromIncident(actualDateTimeOfDiscovery)

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
      draftAdjudication.offenceDetails.clear()
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

    if (draftAdjudication.prisonerNumber == incidentRoleAssociatedPrisoner.associatedPrisonersNumber) {
      throw ValidationException(
        "offender can not be an associate",
      )
    }

    incidentRoleAssociatedPrisoner.let {
      draftAdjudication.incidentRole!!.associatedPrisonersNumber = it.associatedPrisonersNumber
      draftAdjudication.incidentRole!!.associatedPrisonersName = it.associatedPrisonersName
    }

    return saveToDto(draftAdjudication)
  }

  fun editIncidentStatement(id: Long, statement: String?, completed: Boolean?): DraftAdjudicationDto {
    throwIfStatementAndCompletedIsNull(statement, completed)

    val draftAdjudication = find(id)

    if (draftAdjudication.incidentStatement == null) {
      throw EntityNotFoundException("DraftAdjudication does not have any incident statement to update")
    }

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
      draftAdjudication.offenceDetails.clear()
    }

    draftAdjudication.isYouthOffender = isYouthOffender

    return saveToDto(draftAdjudication)
  }

  fun setGender(id: Long, gender: Gender): DraftAdjudicationDto {
    val draftAdjudication = find(id).also {
      it.gender = gender
    }

    return saveToDto(draftAdjudication)
  }

  fun getCurrentUsersInProgressDraftAdjudications(
    startDate: LocalDate,
    endDate: LocalDate,
    pageable: Pageable,
  ): Page<DraftAdjudicationDto> {
    val username = authenticationFacade.currentUsername ?: return Page.empty()
    return getInProgress(authenticationFacade.activeCaseload, username, startDate, endDate, pageable)
  }

  fun setCreatedOnBehalfOf(
    id: Long,
    createdOnBehalfOfOfficer: String,
    createdOnBehalfOfReason: String,
  ): DraftAdjudicationDto {
    val draftAdjudication = find(id).also {
      it.createdOnBehalfOfOfficer = createdOnBehalfOfOfficer
      it.createdOnBehalfOfReason = createdOnBehalfOfReason
    }

    return saveToDto(draftAdjudication)
  }

  private fun throwIfStatementAndCompletedIsNull(statement: String?, completed: Boolean?) {
    if (statement == null && completed == null) {
      throw IllegalArgumentException("Please supply either a statement or the completed value")
    }
  }
}
