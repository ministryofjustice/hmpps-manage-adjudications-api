package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

@Transactional
@Service
class HearingService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {

  fun createHearing(adjudicationNumber: Long, locationId: Long, dateTimeOfHearing: LocalDateTime): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)

    reportedAdjudication.hearings.add(
      Hearing(
        agencyId = reportedAdjudication.agencyId,
        reportNumber = reportedAdjudication.reportNumber,
        locationId = locationId,
        dateTimeOfHearing = dateTimeOfHearing
      )
    )

    return saveToDto(reportedAdjudication)
  }

  fun amendHearing(adjudicationNumber: Long, hearingId: Long, locationId: Long, dateTimeOfHearing: LocalDateTime): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)

    val hearingToEdit = reportedAdjudication.hearings.find { it.id!! == hearingId } ?: throwHearingNotFoundException(
      hearingId
    )

    hearingToEdit.let {
      it.dateTimeOfHearing = dateTimeOfHearing
      it.locationId = locationId
    }

    return saveToDto(reportedAdjudication)
  }

  fun deleteHearing(adjudicationNumber: Long, hearingId: Long): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)

    val hearingToRemove = reportedAdjudication.hearings.find { it.id!! == hearingId } ?: throwHearingNotFoundException(
      hearingId
    )
    reportedAdjudication.hearings.remove(hearingToRemove)

    return saveToDto(reportedAdjudication)
  }

  companion object {
    fun throwHearingNotFoundException(id: Long): Nothing =
      throw EntityNotFoundException("Hearing not found for $id")
  }
}
