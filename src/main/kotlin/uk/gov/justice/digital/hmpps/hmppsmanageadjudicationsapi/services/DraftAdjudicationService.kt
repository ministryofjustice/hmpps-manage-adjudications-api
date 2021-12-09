package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.SubmittedAdjudicationHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToPublish
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToUpdate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.SubmittedAdjudicationHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import java.time.Clock
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

@Service
class DraftAdjudicationService(
  val draftAdjudicationRepository: DraftAdjudicationRepository,
  val submittedAdjudicationHistoryRepository: SubmittedAdjudicationHistoryRepository,
  val prisonApiGateway: PrisonApiGateway,
  val dateCalculationService: DateCalculationService,
  val authenticationFacade: AuthenticationFacade,
  val clock: Clock,
) {

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
        handoverDeadline = dateCalculationService.calculate48WorkingHoursFrom(dateTimeOfIncident)
      ),
      reportNumber = null,
      reportByUserId = null,
    )
    return draftAdjudicationRepository
      .save(draftAdjudication)
      .toDto()
  }

  fun getDraftAdjudicationDetails(id: Long): DraftAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    return draftAdjudication.toDto()
  }

  @Transactional
  fun addIncidentStatement(id: Long, statement: String?, completed: Boolean?): DraftAdjudicationDto {
    throwIfStatementAndCompletedIsNull(statement, completed)

    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    if (draftAdjudication.incidentStatement != null)
      throw IllegalStateException("DraftAdjudication already includes the incident statement")

    draftAdjudication.incidentStatement = IncidentStatement(statement = statement, completed = completed)

    return draftAdjudicationRepository.save(draftAdjudication).toDto()
  }

  @Transactional
  fun editIncidentDetails(id: Long, locationId: Long?, dateTimeOfIncident: LocalDateTime?): DraftAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    locationId?.let { draftAdjudication.incidentDetails.locationId = it }
    dateTimeOfIncident?.let {
      draftAdjudication.incidentDetails.dateTimeOfIncident = it
      draftAdjudication.incidentDetails.handoverDeadline = dateCalculationService.calculate48WorkingHoursFrom(it)
    }

    return draftAdjudicationRepository
      .save(draftAdjudication)
      .toDto()
  }

  @Transactional
  fun editIncidentStatement(id: Long, statement: String?, completed: Boolean?): DraftAdjudicationDto {
    throwIfStatementAndCompletedIsNull(statement, completed)

    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    if (draftAdjudication.incidentStatement == null)
      throw EntityNotFoundException("DraftAdjudication does not have any incident statement to update")

    statement?.let { draftAdjudication.incidentStatement?.statement = statement }
    completed?.let { draftAdjudication.incidentStatement?.completed = completed }

    return draftAdjudicationRepository.save(draftAdjudication).toDto()
  }

  @Transactional
  fun completeDraftAdjudication(id: Long): ReportedAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    if (draftAdjudication.incidentStatement == null)
      throw IllegalStateException("Please include an incident statement before completing this draft adjudication")

    val isNew = draftAdjudication.reportNumber == null
    val reportedAdjudication = saveToPrisonApi(draftAdjudication, isNew)
    saveToSubmittedReports(reportedAdjudication, isNew)

    draftAdjudicationRepository.delete(draftAdjudication)

    return reportedAdjudication
      .toDto(dateCalculationService.calculate48WorkingHoursFrom(reportedAdjudication.incidentTime))
  }

  fun getCurrentUsersInProgressDraftAdjudications(agencyId: String): List<DraftAdjudicationDto> {
    val username = authenticationFacade.currentUsername ?: return emptyList()

    return draftAdjudicationRepository.findUnsubmittedByAgencyIdAndCreatedByUserId(agencyId, username)
      .sortedBy { it.incidentDetails.dateTimeOfIncident }
      .map { it.toDto() }
  }

  private fun saveToPrisonApi(draftAdjudication: DraftAdjudication, isNew: Boolean): ReportedAdjudication {
    if (isNew) {
      return prisonApiGateway.publishAdjudication(
        AdjudicationDetailsToPublish(
          offenderNo = draftAdjudication.prisonerNumber,
          agencyId = draftAdjudication.agencyId,
          incidentTime = draftAdjudication.incidentDetails.dateTimeOfIncident,
          incidentLocationId = draftAdjudication.incidentDetails.locationId,
          statement = draftAdjudication.incidentStatement?.statement!!
        )
      )
    } else {
      return prisonApiGateway.updateAdjudication(
        draftAdjudication.reportNumber!!,
        AdjudicationDetailsToUpdate(
          incidentTime = draftAdjudication.incidentDetails.dateTimeOfIncident,
          incidentLocationId = draftAdjudication.incidentDetails.locationId,
          statement = draftAdjudication.incidentStatement?.statement!!
        )
      )
    }
  }

  private fun saveToSubmittedReports(reportedAdjudication: ReportedAdjudication, isNew: Boolean) {
    if (isNew) {
      submittedAdjudicationHistoryRepository.save(
        SubmittedAdjudicationHistory(
          adjudicationNumber = reportedAdjudication.adjudicationNumber,
          agencyId = reportedAdjudication.agencyId,
          dateTimeOfIncident = reportedAdjudication.incidentTime,
          LocalDateTime.now(clock)
        )
      )
    } else {
      val previousSubmittedAdjudicationHistory = submittedAdjudicationHistoryRepository.findByAdjudicationNumber(reportedAdjudication.adjudicationNumber)
      previousSubmittedAdjudicationHistory?.let {
        it.dateTimeOfIncident = reportedAdjudication.incidentTime
        it.dateTimeSent = LocalDateTime.now(clock)
        submittedAdjudicationHistoryRepository.save(previousSubmittedAdjudicationHistory)
      }
    }
  }

  private fun throwIfStatementAndCompletedIsNull(statement: String?, completed: Boolean?) {
    if (statement == null && completed == null)
      throw IllegalArgumentException("Please supply either a statement or the completed value")
  }

  fun throwEntityNotFoundException(id: Long): Nothing =
    throw EntityNotFoundException("DraftAdjudication not found for $id")
}

fun DraftAdjudication.toDto(): DraftAdjudicationDto = DraftAdjudicationDto(
  id = this.id!!,
  prisonerNumber = this.prisonerNumber,
  createdByUserId = this.createdByUserId,
  createdDateTime = this.createDateTime,
  incidentStatement = this.incidentStatement?.toDo(),
  incidentDetails = this.incidentDetails.toDto(),
  adjudicationNumber = this.reportNumber,
  startedByUserId = this.reportNumber?.let { this.reportByUserId } ?: this.createdByUserId,
)

fun IncidentDetails.toDto(): IncidentDetailsDto = IncidentDetailsDto(
  locationId = this.locationId,
  dateTimeOfIncident = this.dateTimeOfIncident,
  handoverDeadline = this.handoverDeadline,
)

fun IncidentStatement.toDo(): IncidentStatementDto = IncidentStatementDto(
  statement = this.statement!!,
  completed = this.completed,
)
