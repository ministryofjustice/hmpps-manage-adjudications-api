package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

class UnAuthorisedToEditIncidentStatementException() :
  RuntimeException("Only the original author can make changes to this incident statement")

@Service
class DraftAdjudicationService(
  val draftAdjudicationRepository: DraftAdjudicationRepository,
  val authenticationFacade: AuthenticationFacade
) {

  @Transactional
  fun startNewAdjudication(
    prisonerNumber: String,
    locationId: Long,
    dateTimeOfIncident: LocalDateTime
  ): DraftAdjudicationDto {
    val draftAdjudication = DraftAdjudication(
      prisonerNumber = prisonerNumber,
      incidentDetails = IncidentDetails(locationId = locationId, dateTimeOfIncident = dateTimeOfIncident)
    )
    return draftAdjudicationRepository
      .save(draftAdjudication)
      .toDto()
  }

  fun getDraftAdjudicationDetails(id: Long): DraftAdjudicationDto {
    val draftAdjudication =
      draftAdjudicationRepository.findById(id)
        .orElseThrow { throwEntityNotFoundException(id) }

    return draftAdjudication.toDto()
  }

  @Transactional
  fun addIncidentStatement(id: Long, statement: String): DraftAdjudicationDto {
    val draftAdjudication =
      draftAdjudicationRepository.findById(id)
        .orElseThrow { throwEntityNotFoundException(id) }

    if (draftAdjudication.incidentStatement != null)
      throw IllegalStateException("DraftAdjudication already includes the incident statement")

    draftAdjudication.incidentStatement = IncidentStatement(statement = statement)

    return draftAdjudicationRepository.save(draftAdjudication).toDto()
  }

  fun throwEntityNotFoundException(id: Long): Nothing =
    throw EntityNotFoundException("DraftAdjudication not found for $id")

  @Transactional
  fun editIncidentDetails(id: Long, locationId: Long?, dateTimeOfIncident: LocalDateTime?): DraftAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    if (draftAdjudication.incidentDetails == null)
      throw EntityNotFoundException("DraftAdjudication does not have any incident details to update")

    locationId?.let { draftAdjudication.incidentDetails?.locationId = it }
    dateTimeOfIncident?.let { draftAdjudication.incidentDetails?.dateTimeOfIncident = it }

    return draftAdjudicationRepository
      .save(draftAdjudication)
      .toDto()
  }

  @Transactional
  fun editIncidentStatement(id: Long, statement: String): DraftAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    if (draftAdjudication.incidentStatement == null)
      throw EntityNotFoundException("DraftAdjudication does not have any incident statement to update")

    if (authenticationFacade.currentUsername != draftAdjudication.incidentStatement?.createdByUserId)
      throw UnAuthorisedToEditIncidentStatementException()

    draftAdjudication.incidentStatement?.statement = statement

    return draftAdjudicationRepository.save(draftAdjudication).toDto()
  }
}

fun DraftAdjudication.toDto(): DraftAdjudicationDto = DraftAdjudicationDto(
  id = this.id!!,
  prisonerNumber = this.prisonerNumber,
  createdByUserId = this.createdByUserId,
  createdDateTime = this.createDateTime,
  incidentStatement = this.incidentStatement?.toDo(),
  incidentDetails = this.incidentDetails?.toDto(),
)

fun IncidentDetails.toDto(): IncidentDetailsDto = IncidentDetailsDto(
  locationId = this.locationId,
  dateTimeOfIncident = this.dateTimeOfIncident,
  createdByUserId = this.createdByUserId,
  createdDateTime = this.createDateTime,
  modifiedByUserId = this.modifiedByUserId,
  modifiedByDateTime = this.modifiedDateTime
)

fun IncidentStatement.toDo(): IncidentStatementDto = IncidentStatementDto(
  id = this.id!!,
  statement = this.statement,
  createdByUserId = this.createdByUserId,
  createdDateTime = this.createDateTime,
  modifiedByUserId = this.modifiedByUserId,
  modifiedByDateTime = this.modifiedDateTime
)
