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
  fun startNewAdjudication(prisonerNumber: String): DraftAdjudicationDto {
    return draftAdjudicationRepository
      .save(DraftAdjudication(prisonerNumber = prisonerNumber))
      .toDto()
  }

  @Transactional
  fun addIncidentDetails(id: Long, locationId: Long, dateTimeOfIncident: LocalDateTime): DraftAdjudicationDto {
    val draftAdjudication =
      draftAdjudicationRepository.findById(id)
        .orElseThrow { throw EntityNotFoundException("DraftAdjudication not found for $id") }

    draftAdjudication.addIncidentDetails(IncidentDetails(locationId, dateTimeOfIncident))

    return draftAdjudication.toDto()
  }

  fun getDraftAdjudicationDetails(id: Long): DraftAdjudicationDto {
    val draftAdjudication =
      draftAdjudicationRepository.findById(id)
        .orElseThrow { throw EntityNotFoundException("DraftAdjudication not found for $id") }

    return draftAdjudication.toDto()
  }
}

fun DraftAdjudication.toDto(): DraftAdjudicationDto = DraftAdjudicationDto(
  id = this.id!!,
  prisonerNumber = this.prisonerNumber,
  incidentDetails = this.getIncidentDetails()?.toDto(),
  incidentStatement = this.incidentStatement?.toDo(),
  adjudicationSent = this.adjudicationSent,
)

fun IncidentDetails.toDto(): IncidentDetailsDto = IncidentDetailsDto(
  locationId = this.locationId,
  dateTimeOfIncident = this.dateTimeOfIncident
)

fun IncidentStatement.toDo(): IncidentStatementDto = IncidentStatementDto(
  statement = this.statement
)
