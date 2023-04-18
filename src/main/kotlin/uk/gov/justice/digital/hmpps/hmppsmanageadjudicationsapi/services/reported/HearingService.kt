package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OutcomeHistoryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus.Companion.validateTransition
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.HearingRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService.Companion.lastOutcomeIsRefer
import java.time.LocalDate
import java.time.LocalDateTime

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

  @Deprecated(message = "to be removed when outcomes goes live")
  fun createHearingV1(adjudicationNumber: Long, locationId: Long, dateTimeOfHearing: LocalDateTime, oicHearingType: OicHearingType): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      oicHearingType.isValidState(it.isYouthOffender)
    }

    val oicHearingId = prisonApiGateway.createHearing(
      adjudicationNumber = adjudicationNumber,
      oicHearingRequest = OicHearingRequest(
        dateTimeOfHearing = dateTimeOfHearing,
        hearingLocationId = locationId,
        oicHearingType = oicHearingType,
      ),
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
        ),
      )
      if (it.status != ReportedAdjudicationStatus.SCHEDULED) {
        it.status = ReportedAdjudicationStatus.SCHEDULED
      }

      it.dateTimeOfFirstHearing = it.calcFirstHearingDate()
    }

    return saveToDto(reportedAdjudication)
  }

  @Deprecated(message = "to be removed when outcomes goes live")
  fun amendHearingV1(adjudicationNumber: Long, hearingId: Long, locationId: Long, dateTimeOfHearing: LocalDateTime, oicHearingType: OicHearingType): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      oicHearingType.isValidState(it.isYouthOffender)
    }

    val hearingToEdit = reportedAdjudication.hearings.find { it.id!! == hearingId } ?: throwHearingNotFoundException()

    prisonApiGateway.amendHearing(
      adjudicationNumber = adjudicationNumber,
      oicHearingId = hearingToEdit.oicHearingId,
      oicHearingRequest = OicHearingRequest(
        dateTimeOfHearing = dateTimeOfHearing,
        hearingLocationId = locationId,
        oicHearingType = oicHearingType,
      ),
    )

    hearingToEdit.let {
      it.dateTimeOfHearing = dateTimeOfHearing
      it.locationId = locationId
      it.oicHearingType = oicHearingType
    }

    reportedAdjudication.dateTimeOfFirstHearing = reportedAdjudication.calcFirstHearingDate()

    return saveToDto(reportedAdjudication)
  }

  @Deprecated(message = "to be removed when outcomes goes live")
  fun deleteHearingV1(adjudicationNumber: Long, hearingId: Long): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    val hearingToRemove = reportedAdjudication.hearings.find { it.id!! == hearingId } ?: throwHearingNotFoundException()

    prisonApiGateway.deleteHearing(
      adjudicationNumber = adjudicationNumber,
      oicHearingId = hearingToRemove.oicHearingId,
    )

    reportedAdjudication.let {
      it.hearings.remove(hearingToRemove)
      if (it.hearings.isEmpty()) {
        it.status = ReportedAdjudicationStatus.UNSCHEDULED
      }

      it.dateTimeOfFirstHearing = it.calcFirstHearingDate()
    }

    return saveToDto(reportedAdjudication)
  }

  fun createHearing(adjudicationNumber: Long, locationId: Long, dateTimeOfHearing: LocalDateTime, oicHearingType: OicHearingType): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      oicHearingType.isValidState(it.isYouthOffender)
      it.status.validateTransition(ReportedAdjudicationStatus.SCHEDULED)
      it.hearings.validateHearingDate(dateTimeOfHearing)
    }.validateCanCreate()

    if (reportedAdjudication.lastOutcomeIsRefer()) {
      reportedAdjudication.outcomes.add(Outcome(code = OutcomeCode.SCHEDULE_HEARING))
    }

    val oicHearingId = prisonApiGateway.createHearing(
      adjudicationNumber = adjudicationNumber,
      oicHearingRequest = OicHearingRequest(
        dateTimeOfHearing = dateTimeOfHearing,
        hearingLocationId = locationId,
        oicHearingType = oicHearingType,
      ),
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
        ),
      )
      if (it.status != ReportedAdjudicationStatus.SCHEDULED) {
        it.status = ReportedAdjudicationStatus.SCHEDULED
      }

      it.dateTimeOfFirstHearing = it.calcFirstHearingDate()
    }

    return saveToDto(reportedAdjudication)
  }

  fun amendHearing(adjudicationNumber: Long, locationId: Long, dateTimeOfHearing: LocalDateTime, oicHearingType: OicHearingType): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      oicHearingType.isValidState(it.isYouthOffender)
      it.status.validateTransition(ReportedAdjudicationStatus.SCHEDULED, ReportedAdjudicationStatus.UNSCHEDULED)
    }

    val hearingToEdit = reportedAdjudication.getHearing()
    reportedAdjudication.hearings.filter { it.id != hearingToEdit.id }.validateHearingDate(dateTimeOfHearing)

    prisonApiGateway.amendHearing(
      adjudicationNumber = adjudicationNumber,
      oicHearingId = hearingToEdit.oicHearingId,
      oicHearingRequest = OicHearingRequest(
        dateTimeOfHearing = dateTimeOfHearing,
        hearingLocationId = locationId,
        oicHearingType = oicHearingType,
      ),
    )

    hearingToEdit.let {
      it.dateTimeOfHearing = dateTimeOfHearing
      it.locationId = locationId
      it.oicHearingType = oicHearingType
    }

    reportedAdjudication.dateTimeOfFirstHearing = reportedAdjudication.calcFirstHearingDate()

    return saveToDto(reportedAdjudication)
  }

  fun deleteHearing(adjudicationNumber: Long): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    val hearingToRemove = reportedAdjudication.getHearing().canDelete()

    prisonApiGateway.deleteHearing(
      adjudicationNumber = adjudicationNumber,
      oicHearingId = hearingToRemove.oicHearingId,
    )

    reportedAdjudication.also {
      it.hearings.remove(hearingToRemove)
      it.dateTimeOfFirstHearing = it.calcFirstHearingDate()

      if (it.getOutcomeHistory().shouldRemoveLastScheduleHearing()) it.outcomes.remove(it.outcomes.getOutcomeToRemove())
    }.calculateStatus()

    return saveToDto(reportedAdjudication)
  }

  fun getAllHearingsByAgencyIdAndDate(agencyId: String, dateOfHearing: LocalDate): List<HearingSummaryDto> {
    val hearings = hearingRepository.findByAgencyIdAndDateTimeOfHearingBetween(
      agencyId,
      dateOfHearing.atStartOfDay(),
      dateOfHearing.plusDays(1).atStartOfDay(),
    )

    val adjudicationsMap = findByReportNumberIn(
      hearings.map { it.reportNumber },
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
        oicHearingType = it.oicHearingType,
        status = adjudication.status,
      )
    }.sortedBy { it.dateTimeOfHearing }

  companion object {

    fun ReportedAdjudication.validateCanCreate(): ReportedAdjudication {
      val latestHearing = this.getLatestHearing() ?: return this
      latestHearing.hearingOutcome ?: throw ValidationException("Adjudication already has a hearing without outcome")
      return this
    }

    fun List<Hearing>.validateHearingDate(date: LocalDateTime) {
      if (this.any { it.dateTimeOfHearing.isAfter(date) }) {
        throw ValidationException("A hearing can not be before the previous hearing")
      }
    }

    fun List<OutcomeHistoryDto>.shouldRemoveLastScheduleHearing(): Boolean {
      val lastItem = this.lastOrNull() ?: return false
      val referralOutcome = lastItem.outcome?.referralOutcome ?: return false
      return referralOutcome.code == OutcomeCode.SCHEDULE_HEARING
    }

    fun List<Outcome>.getOutcomeToRemove() = this.maxBy { it.createDateTime!! }

    fun ReportedAdjudication.getHearing(): Hearing = this.getLatestHearing() ?: throwHearingNotFoundException()

    fun ReportedAdjudication.calcFirstHearingDate(): LocalDateTime? = this.hearings.minOfOrNull { it.dateTimeOfHearing }

    fun Hearing.canDelete(): Hearing {
      if (OutcomeCode.referrals().contains(this.hearingOutcome?.code?.outcomeCode) || this.hearingOutcome?.code == HearingOutcomeCode.COMPLETE) {
        throw ValidationException("Unable to delete hearing via api DEL/hearing - outcome associated to this hearing")
      }
      return this
    }

    private fun throwHearingNotFoundException(): Nothing = throw EntityNotFoundException("Hearing not found")
  }
}
