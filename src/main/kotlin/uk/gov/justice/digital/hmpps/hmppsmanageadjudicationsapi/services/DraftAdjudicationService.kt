package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

@Service
class DraftAdjudicationService(val draftAdjudicationRepository: DraftAdjudicationRepository) {

  @Transactional
  fun startNewAdjudication(
    prisonerNumber: String,
    locationId: Long,
    dateTimeOfIncident: LocalDateTime
  ): DraftAdjudicationDto {
    val draftAdjudication = DraftAdjudication(
      prisonerNumber = prisonerNumber,
      incidentDetails = IncidentDetails(locationId, dateTimeOfIncident)
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

    draftAdjudication.addIncidentStatement(statement)

    return draftAdjudication.toDto()
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

    return draftAdjudication.toDto()
  }
}

fun DraftAdjudication.toDto(): DraftAdjudicationDto = DraftAdjudicationDto(
  id = this.id!!,
  prisonerNumber = this.prisonerNumber,
  incidentDetails = this.incidentDetails?.toDto(),
  incidentStatement = this.getIncidentStatement()?.toDo()
)

fun IncidentDetails.toDto(): IncidentDetailsDto = IncidentDetailsDto(
  locationId = this.locationId,
  dateTimeOfIncident = this.dateTimeOfIncident
)

fun IncidentStatement.toDo(): IncidentStatementDto = IncidentStatementDto(
  statement = this.statement
)
