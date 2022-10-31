package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.HearingRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

@Transactional
@Service
class HearingService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
  private val hearingRepository: HearingRepository,
  private val prisonApiGateway: PrisonApiGateway,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {

  fun createHearing(adjudicationNumber: Long, locationId: Long, dateTimeOfHearing: LocalDateTime): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    val oicHearingId = prisonApiGateway.createHearing(
      adjudicationNumber = adjudicationNumber,
      oicHearingRequest = OicHearingRequest(
        dateTimeOfHearing = dateTimeOfHearing,
        hearingLocationId = locationId,
        oicHearingType = OicHearingType.getOicHearingType(reportedAdjudication),
      )
    )

    reportedAdjudication.hearings.add(
      Hearing(
        agencyId = reportedAdjudication.agencyId,
        reportNumber = reportedAdjudication.reportNumber,
        locationId = locationId,
        dateTimeOfHearing = dateTimeOfHearing,
        oicHearingId = oicHearingId,
      )
    )

    return saveToDto(reportedAdjudication)
  }

  fun amendHearing(adjudicationNumber: Long, hearingId: Long, locationId: Long, dateTimeOfHearing: LocalDateTime): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)

    val hearingToEdit = reportedAdjudication.hearings.find { it.id!! == hearingId } ?: throwHearingNotFoundException(
      hearingId
    )

    prisonApiGateway.amendHearing(
      adjudicationNumber = adjudicationNumber,
      oicHearingId = hearingToEdit.oicHearingId,
      oicHearingRequest = OicHearingRequest(
        dateTimeOfHearing = dateTimeOfHearing,
        hearingLocationId = locationId,
        oicHearingType = OicHearingType.getOicHearingType(reportedAdjudication)
      )
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

    prisonApiGateway.deleteHearing(
      adjudicationNumber = adjudicationNumber,
      oicHearingId = hearingToRemove.oicHearingId
    )

    reportedAdjudication.hearings.remove(hearingToRemove)

    return saveToDto(reportedAdjudication)
  }

  fun getAllHearingsByAgencyIdAndDate(agencyId: String, dateOfHearing: LocalDate): List<HearingSummaryDto> {
    val hearings = hearingRepository.findByAgencyIdAndDateTimeOfHearingBetween(
      agencyId, dateOfHearing.atStartOfDay(), dateOfHearing.plusDays(1).atStartOfDay()
    )

    val adjudicationsMap = findByReportNumberIn(
      hearings.map { it.reportNumber }
    ).associateBy { it.reportNumber }

    return toHearingSummaries(hearings, adjudicationsMap)
  }

  private fun toHearingSummaries(hearings: List<Hearing>, adjudications: Map<Long, ReportedAdjudication>): List<HearingSummaryDto> =
    hearings.map {
      val adjudication = adjudications[it.reportNumber]!!
      HearingSummaryDto(
        id = it.id!!,
        dateTimeOfHearing = it.dateTimeOfHearing,
        dateTimeOfDiscovery = adjudication.dateTimeOfDiscovery,
        prisonerNumber = adjudication.prisonerNumber,
        adjudicationNumber = it.reportNumber,
      )
    }

  companion object {
    fun throwHearingNotFoundException(id: Long): Nothing =
      throw EntityNotFoundException("Hearing not found for $id")
  }
}
