package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeFinding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
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
import javax.validation.ValidationException

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

  fun createHearing(adjudicationNumber: Long, locationId: Long, dateTimeOfHearing: LocalDateTime, oicHearingType: OicHearingType): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      oicHearingType.isValidState(it.isYouthOffender)
      it.status.validateTransition(ReportedAdjudicationStatus.SCHEDULED)
    }

    val oicHearingId = prisonApiGateway.createHearing(
      adjudicationNumber = adjudicationNumber,
      oicHearingRequest = OicHearingRequest(
        dateTimeOfHearing = dateTimeOfHearing,
        hearingLocationId = locationId,
        oicHearingType = oicHearingType,
      )
    )

    reportedAdjudication.let {
      it.hearings.add(
        Hearing(
          agencyId = reportedAdjudication.agencyId,
          reportNumber = reportedAdjudication.reportNumber,
          locationId = locationId,
          dateTimeOfHearing = dateTimeOfHearing,
          oicHearingId = oicHearingId,
          oicHearingType = oicHearingType,
        )
      )
      if (it.status != ReportedAdjudicationStatus.SCHEDULED)
        it.status = ReportedAdjudicationStatus.SCHEDULED

      it.dateTimeOfFirstHearing = it.calcFirstHearingDate()
    }

    return saveToDto(reportedAdjudication)
  }

  fun amendHearing(adjudicationNumber: Long, hearingId: Long, locationId: Long, dateTimeOfHearing: LocalDateTime, oicHearingType: OicHearingType): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      oicHearingType.isValidState(it.isYouthOffender)
      it.status.validateTransition(ReportedAdjudicationStatus.SCHEDULED, ReportedAdjudicationStatus.UNSCHEDULED)
    }

    val hearingToEdit = reportedAdjudication.getHearing(hearingId)

    prisonApiGateway.amendHearing(
      adjudicationNumber = adjudicationNumber,
      oicHearingId = hearingToEdit.oicHearingId,
      oicHearingRequest = OicHearingRequest(
        dateTimeOfHearing = dateTimeOfHearing,
        hearingLocationId = locationId,
        oicHearingType = oicHearingType
      )
    )

    hearingToEdit.let {
      it.dateTimeOfHearing = dateTimeOfHearing
      it.locationId = locationId
      it.oicHearingType = oicHearingType
    }

    reportedAdjudication.dateTimeOfFirstHearing = reportedAdjudication.calcFirstHearingDate()

    return saveToDto(reportedAdjudication)
  }

  fun deleteHearing(adjudicationNumber: Long, hearingId: Long): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    val hearingToRemove = reportedAdjudication.getHearing(hearingId)

    prisonApiGateway.deleteHearing(
      adjudicationNumber = adjudicationNumber,
      oicHearingId = hearingToRemove.oicHearingId
    )

    reportedAdjudication.let {
      it.hearings.remove(hearingToRemove)
      if (it.hearings.isEmpty())
        it.status = ReportedAdjudicationStatus.UNSCHEDULED

      it.dateTimeOfFirstHearing = it.calcFirstHearingDate()
    }

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

  fun createHearingOutcome(
    adjudicationNumber: Long,
    hearingId: Long,
    adjudicator: String,
    code: HearingOutcomeCode,
    reason: HearingOutcomeReason? = null,
    details: String? = null,
    finding: HearingOutcomeFinding? = null,
    plea: HearingOutcomePlea? = null
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    val hearingToAddOutcomeTo = reportedAdjudication.getHearing(hearingId)
    val hearingOutcome = HearingOutcome(
      code = code,
      reason = reason,
      details = details,
      adjudicator = adjudicator,
      finding = finding,
      plea = plea,
    ).validate()

    hearingToAddOutcomeTo.hearingOutcome = hearingOutcome

    return saveToDto(reportedAdjudication)
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
        oicHearingType = it.oicHearingType,
      )
    }.sortedBy { it.dateTimeOfHearing }

  companion object {

    fun ReportedAdjudication.getHearing(hearingId: Long) =
      this.hearings.find { it.id!! == hearingId } ?: throwHearingNotFoundException(hearingId)
    private fun throwHearingNotFoundException(id: Long): Nothing =
      throw EntityNotFoundException("Hearing not found for $id")

    fun ReportedAdjudicationStatus.validateTransition(vararg next: ReportedAdjudicationStatus) {
      next.toList().forEach {
        if (this != it && !this.canTransitionTo(it)) throw ValidationException("Invalid status transition ${this.name} - ${it.name}")
      }
    }

    fun ReportedAdjudication.calcFirstHearingDate(): LocalDateTime? =
      this.hearings.minOfOrNull { it.dateTimeOfHearing }

    fun HearingOutcome.validate(): HearingOutcome {
      when (this.code) {
        HearingOutcomeCode.COMPLETE -> {
          validateField(this.plea)
          validateField(this.finding)
        }
        HearingOutcomeCode.ADJOURN -> validateField(this.plea)
        else -> {}
      }
      return this
    }

    private fun validateField(field: Any?) = field ?: throw ValidationException("missing mandatory field")
  }
}
