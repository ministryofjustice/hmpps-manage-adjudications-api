package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.IncidentRoleRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.OffenceDetailsRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
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
  OFFENCE_DETAILS("Please supply at least one set of offence details") {
    override fun validate(draftAdjudication: DraftAdjudication) {
      if (draftAdjudication.offenceDetails == null || draftAdjudication.offenceDetails!!.isEmpty())
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

@Service
class DraftAdjudicationService(
  val draftAdjudicationRepository: DraftAdjudicationRepository,
  val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  val prisonApiGateway: PrisonApiGateway,
  val offenceCodeLookupService: OffenceCodeLookupService,
  val authenticationFacade: AuthenticationFacade,
) {

  companion object {
    private const val DAYS_TO_ACTION = 2L
    const val DAYS_TO_DELETE = 1L

    fun daysToActionFromIncident(incidentDate: LocalDateTime): LocalDateTime = incidentDate.plusDays(DAYS_TO_ACTION)
  }

  @Transactional
  fun deleteOrphanedDraftAdjudications() {
    draftAdjudicationRepository.deleteDraftAdjudicationByCreateDateTimeBeforeAndReportNumberIsNotNull(
      LocalDateTime.now().minusDays(DAYS_TO_DELETE)
    )
  }

  @Transactional
  fun startNewAdjudication(
    prisonerNumber: String,
    agencyId: String,
    locationId: Long,
    dateTimeOfIncident: LocalDateTime
  ): DraftAdjudicationDto {
    val draftAdjudication = DraftAdjudication(
      prisonerNumber = prisonerNumber,
      agencyId = agencyId,
      incidentDetails = IncidentDetails(
        locationId = locationId,
        dateTimeOfIncident = dateTimeOfIncident,
        handoverDeadline = daysToActionFromIncident(dateTimeOfIncident)
      ),
      reportNumber = null,
      reportByUserId = null,
    )
    return draftAdjudicationRepository
      .save(draftAdjudication)
      .toDto(offenceCodeLookupService)
  }

  fun getDraftAdjudicationDetails(id: Long): DraftAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    return draftAdjudication.toDto(offenceCodeLookupService)
  }

  @Transactional
  fun setOffenceDetails(id: Long, offenceDetails: List<OffenceDetailsRequestItem>): DraftAdjudicationDto {
    throwIfNoOffenceDetails(offenceDetails)

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
    if (draftAdjudication.offenceDetails != null) {
      draftAdjudication.offenceDetails!!.clear()
      draftAdjudication.offenceDetails!!.addAll(newValuesToStore)
    } else {
      draftAdjudication.offenceDetails = newValuesToStore
    }

    return draftAdjudicationRepository.save(draftAdjudication).toDto(offenceCodeLookupService)
  }

  @Transactional
  fun addIncidentStatement(id: Long, statement: String?, completed: Boolean?): DraftAdjudicationDto {
    throwIfStatementAndCompletedIsNull(statement, completed)

    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    if (draftAdjudication.incidentStatement != null)
      throw IllegalStateException("DraftAdjudication already includes the incident statement")

    draftAdjudication.incidentStatement = IncidentStatement(statement = statement, completed = completed)

    return draftAdjudicationRepository.save(draftAdjudication).toDto(offenceCodeLookupService)
  }

  @Transactional
  fun editIncidentDetails(
    id: Long,
    locationId: Long?,
    dateTimeOfIncident: LocalDateTime?
  ): DraftAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    locationId?.let { draftAdjudication.incidentDetails.locationId = it }
    dateTimeOfIncident?.let {
      draftAdjudication.incidentDetails.dateTimeOfIncident = it
      draftAdjudication.incidentDetails.handoverDeadline = daysToActionFromIncident(it)
    }

    return draftAdjudicationRepository
      .save(draftAdjudication)
      .toDto(offenceCodeLookupService)
  }

  @Transactional
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
      draftAdjudication.incidentRole = draftAdjudication.incidentRole ?: IncidentRole(roleCode = null, associatedPrisonersNumber = null)

      draftAdjudication.incidentRole!!.roleCode = it.roleCode
      draftAdjudication.incidentRole!!.associatedPrisonersNumber = it.associatedPrisonersNumber
    }

    return draftAdjudicationRepository
      .save(draftAdjudication)
      .toDto(offenceCodeLookupService)
  }

  @Transactional
  fun editIncidentStatement(id: Long, statement: String?, completed: Boolean?): DraftAdjudicationDto {
    throwIfStatementAndCompletedIsNull(statement, completed)

    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    if (draftAdjudication.incidentStatement == null)
      throw EntityNotFoundException("DraftAdjudication does not have any incident statement to update")

    statement?.let { draftAdjudication.incidentStatement?.statement = statement }
    completed?.let { draftAdjudication.incidentStatement?.completed = completed }

    return draftAdjudicationRepository.save(draftAdjudication).toDto(offenceCodeLookupService)
  }

  @Transactional
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

    return draftAdjudicationRepository
      .save(draftAdjudication)
      .toDto(offenceCodeLookupService)
  }

  @Transactional
  fun completeDraftAdjudication(id: Long): ReportedAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    ValidationChecks.values().toList().stream()
      .forEach { it.validate(draftAdjudication) }

    val isNew = draftAdjudication.reportNumber == null
    val generatedReportedAdjudication = saveAdjudication(draftAdjudication, isNew)

    draftAdjudicationRepository.delete(draftAdjudication)

    return generatedReportedAdjudication
      .toDto(offenceCodeLookupService)
  }

  fun getCurrentUsersInProgressDraftAdjudications(agencyId: String): List<DraftAdjudicationDto> {
    val username = authenticationFacade.currentUsername ?: return emptyList()

    return draftAdjudicationRepository.findDraftAdjudicationByAgencyIdAndCreatedByUserIdAndReportNumberIsNull(
      agencyId,
      username
    )
      .sortedBy { it.incidentDetails.dateTimeOfIncident }
      .map { it.toDto(offenceCodeLookupService) }
  }

  fun lookupRuleDetails(offenceCode: Int, isYouthOffender: Boolean): OffenceRuleDetailsDto {
    return OffenceRuleDetailsDto(
      paragraphNumber = offenceCodeLookupService.getParagraphNumber(offenceCode, isYouthOffender),
      paragraphDescription = offenceCodeLookupService.getParagraphDescription(offenceCode, isYouthOffender),
    )
  }

  private fun saveAdjudication(draftAdjudication: DraftAdjudication, isNew: Boolean): ReportedAdjudication {
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
    val reportedAdjudication = reportedAdjudicationRepository.findByReportNumber(reportNumber)
      ?: ReportedAdjudicationService.throwEntityNotFoundException(reportNumber)
    val fromStatus = reportedAdjudication.status
    if (!ReportedAdjudicationStatus.AWAITING_REVIEW.canTransitionFrom(fromStatus)) {
      throw IllegalStateException("Unable to complete draft adjudication ${draftAdjudication.reportNumber} as it is in the state $fromStatus")
    }
  }

  private fun createReportedAdjudication(draftAdjudication: DraftAdjudication): ReportedAdjudication {
    val nomisAdjudicationCreationRequestData = prisonApiGateway.requestAdjudicationCreationData(draftAdjudication.prisonerNumber)
    return reportedAdjudicationRepository.save(
      ReportedAdjudication(
        bookingId = nomisAdjudicationCreationRequestData.bookingId,
        reportNumber = nomisAdjudicationCreationRequestData.adjudicationNumber,
        prisonerNumber = draftAdjudication.prisonerNumber,
        agencyId = draftAdjudication.agencyId,
        locationId = draftAdjudication.incidentDetails.locationId,
        dateTimeOfIncident = draftAdjudication.incidentDetails.dateTimeOfIncident,
        handoverDeadline = draftAdjudication.incidentDetails.handoverDeadline,
        isYouthOffender = draftAdjudication.isYouthOffender!!,
        incidentRoleCode = draftAdjudication.incidentRole!!.roleCode,
        incidentRoleAssociatedPrisonersNumber = draftAdjudication.incidentRole!!.associatedPrisonersNumber,
        offenceDetails = toReportedOffence(draftAdjudication.offenceDetails, draftAdjudication),
        statement = draftAdjudication.incidentStatement!!.statement!!,
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
      )
    )
  }

  private fun updateReportedAdjudication(
    draftAdjudication: DraftAdjudication,
  ): ReportedAdjudication {
    val reportedAdjudicationNumber = draftAdjudication.reportNumber
      ?: throw EntityNotFoundException("No reported adjudication number set on the draft adjudication")
    val previousReportedAdjudication =
      reportedAdjudicationRepository.findByReportNumber(reportedAdjudicationNumber)
    previousReportedAdjudication?.let {
      it.bookingId = previousReportedAdjudication.bookingId
      it.reportNumber = previousReportedAdjudication.reportNumber
      it.prisonerNumber = draftAdjudication.prisonerNumber
      it.agencyId = draftAdjudication.agencyId
      it.locationId = draftAdjudication.incidentDetails.locationId
      it.dateTimeOfIncident = draftAdjudication.incidentDetails.dateTimeOfIncident
      it.handoverDeadline = draftAdjudication.incidentDetails.handoverDeadline
      it.isYouthOffender = draftAdjudication.isYouthOffender!!
      it.incidentRoleCode = draftAdjudication.incidentRole!!.roleCode
      it.incidentRoleAssociatedPrisonersNumber = draftAdjudication.incidentRole!!.associatedPrisonersNumber
      it.offenceDetails!!.clear()
      it.offenceDetails!!.addAll(toReportedOffence(draftAdjudication.offenceDetails, draftAdjudication))
      it.statement = draftAdjudication.incidentStatement!!.statement!!
      it.transition(ReportedAdjudicationStatus.AWAITING_REVIEW)

      return reportedAdjudicationRepository.save(it)
    } ?: ReportedAdjudicationService.throwEntityNotFoundException(reportedAdjudicationNumber)
  }

  private fun toReportedOffence(draftOffences: MutableList<Offence>?, draftAdjudication: DraftAdjudication): MutableList<ReportedOffence> {
    return (draftOffences ?: listOf()).map {
      ReportedOffence(
        offenceCode = it.offenceCode,
        paragraphCode = offenceCodeLookupService.getParagraphCode(it.offenceCode, draftAdjudication.isYouthOffender!!),
        victimPrisonersNumber = it.victimPrisonersNumber,
        victimStaffUsername = it.victimStaffUsername,
        victimOtherPersonsName = it.victimOtherPersonsName,
      )
    }.toMutableList()
  }

  private fun throwIfStatementAndCompletedIsNull(statement: String?, completed: Boolean?) {
    if (statement == null && completed == null)
      throw IllegalArgumentException("Please supply either a statement or the completed value")
  }

  private fun throwIfNoOffenceDetails(offenceDetails: List<OffenceDetailsRequestItem>) {
    if (offenceDetails.isEmpty())
      throw IllegalArgumentException("Please supply at least one set of offence details")
  }

  fun throwEntityNotFoundException(id: Long): Nothing =
    throw EntityNotFoundException("DraftAdjudication not found for $id")
}

fun DraftAdjudication.toDto(offenceCodeLookupService: OffenceCodeLookupService): DraftAdjudicationDto =
  DraftAdjudicationDto(
    id = this.id!!,
    prisonerNumber = this.prisonerNumber,
    incidentStatement = this.incidentStatement?.toDo(),
    incidentDetails = this.incidentDetails.toDto(),
    incidentRole = this.incidentRole?.toDto(this.isYouthOffender!!),
    offenceDetails = this.offenceDetails?.map { it.toDto(offenceCodeLookupService, this.isYouthOffender!!) },
    adjudicationNumber = this.reportNumber,
    startedByUserId = this.reportNumber?.let { this.reportByUserId } ?: this.createdByUserId,
    isYouthOffender = this.isYouthOffender
  )

fun IncidentDetails.toDto(): IncidentDetailsDto = IncidentDetailsDto(
  locationId = this.locationId,
  dateTimeOfIncident = this.dateTimeOfIncident,
  handoverDeadline = this.handoverDeadline,
)

fun IncidentRole.toDto(isYouthOffender: Boolean): IncidentRoleDto = IncidentRoleDto(
  roleCode = this.roleCode,
  offenceRule = IncidentRoleRuleLookup.getOffenceRuleDetails(this.roleCode, isYouthOffender),
  associatedPrisonersNumber = this.associatedPrisonersNumber,
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

fun IncidentStatement.toDo(): IncidentStatementDto = IncidentStatementDto(
  statement = this.statement!!,
  completed = this.completed,
)
