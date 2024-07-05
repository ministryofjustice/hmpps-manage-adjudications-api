package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.HearingsByPrisoner
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OutcomeHistoryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication.Companion.getOutcomeHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus.Companion.validateTransition
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.HearingRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService.Companion.lastOutcomeIsRefer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Transactional
@Service
class HearingService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
  private val hearingRepository: HearingRepository,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {

  fun createHearing(chargeNumber: String, locationId: Long, dateTimeOfHearing: LocalDateTime, oicHearingType: OicHearingType): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber).also {
      oicHearingType.isValidState(it.isYouthOffender)
      it.status.validateTransition(ReportedAdjudicationStatus.SCHEDULED)
      it.hearings.validateHearingDate(dateTimeOfHearing)
    }.validateCanCreate()

    if (reportedAdjudication.lastOutcomeIsRefer()) {
      reportedAdjudication.addOutcome(Outcome(code = OutcomeCode.SCHEDULE_HEARING))
    }

    reportedAdjudication.let {
      it.hearings.add(
        Hearing(
          agencyId = authenticationFacade.activeCaseload,
          chargeNumber = reportedAdjudication.chargeNumber,
          locationId = locationId,
          dateTimeOfHearing = dateTimeOfHearing,
          oicHearingType = oicHearingType,
        ),
      )
      if (it.status != ReportedAdjudicationStatus.SCHEDULED) {
        it.status = ReportedAdjudicationStatus.SCHEDULED
      }

      it.dateTimeOfFirstHearing = it.calcFirstHearingDate()
    }

    return saveToDto(reportedAdjudication).also {
      it.hearingIdActioned = it.hearings.getLatestHearingId()
    }
  }

  fun amendHearing(chargeNumber: String, locationId: Long, dateTimeOfHearing: LocalDateTime, oicHearingType: OicHearingType): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber).also {
      oicHearingType.isValidState(it.isYouthOffender)
      it.status.validateTransition(ReportedAdjudicationStatus.SCHEDULED, ReportedAdjudicationStatus.UNSCHEDULED)
    }

    val hearingToEdit = reportedAdjudication.getHearing()
    reportedAdjudication.hearings.filter { it.id != hearingToEdit.id }.validateHearingDate(dateTimeOfHearing)

    hearingToEdit.let {
      it.dateTimeOfHearing = dateTimeOfHearing
      it.locationId = locationId
      it.oicHearingType = oicHearingType
    }

    reportedAdjudication.dateTimeOfFirstHearing = reportedAdjudication.calcFirstHearingDate()

    return saveToDto(reportedAdjudication).also {
      it.hearingIdActioned = hearingToEdit.id
    }
  }

  fun deleteHearing(chargeNumber: String): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    val hearingToRemove = reportedAdjudication.getHearing().canDelete()

    reportedAdjudication.also {
      it.hearings.remove(hearingToRemove)
      it.dateTimeOfFirstHearing = it.calcFirstHearingDate()

      if (reportedAdjudication.getOutcomeHistory().shouldRemoveLastScheduleHearing()) it.getOutcomeToRemove().deleted = true
    }.calculateStatus()

    return saveToDto(reportedAdjudication).also {
      it.hearingIdActioned = hearingToRemove.id
    }
  }

  fun getAllHearingsByAgencyIdAndDate(dateOfHearing: LocalDate): List<HearingSummaryDto> {
    val hearings = hearingRepository.findByAgencyIdAndDateTimeOfHearingBetween(
      agencyId = authenticationFacade.activeCaseload,
      start = dateOfHearing.atStartOfDay(),
      end = dateOfHearing.plusDays(1).atStartOfDay(),
    )

    val adjudicationsMap = findByChargeNumberIn(
      hearings.map { it.chargeNumber },
    ).associateBy { it.chargeNumber }

    return toHearingSummaries(hearings, adjudicationsMap)
  }

  fun getHearingsByPrisoner(
    agencyId: String,
    startDate: LocalDate,
    endDate: LocalDate,
    prisoners: List<String>,
  ): List<HearingsByPrisoner> = hearingRepository.getHearingsByPrisoner(
    agencyId = agencyId,
    startDate = startDate.atStartOfDay(),
    endDate = endDate.atTime(LocalTime.MAX),
    prisoners = prisoners,
  ).groupBy { it.getPrisonerNumber() }
    .map {
      HearingsByPrisoner(
        prisonerNumber = it.key,
        hearings = it.value.map { hearing ->
          HearingDto(
            id = hearing.getHearingId(),
            locationId = hearing.getLocationId(),
            dateTimeOfHearing = hearing.getDateTimeOfHearing(),
            oicHearingType = OicHearingType.valueOf(hearing.getOicHearingType()),
            agencyId = agencyId,
          )
        },
      )
    }

  private fun toHearingSummaries(hearings: List<Hearing>, adjudications: Map<String, ReportedAdjudication>): List<HearingSummaryDto> =
    hearings.map {
      val adjudication = adjudications[it.chargeNumber]!!
      HearingSummaryDto(
        id = it.id!!,
        dateTimeOfHearing = it.dateTimeOfHearing,
        dateTimeOfDiscovery = adjudication.dateTimeOfDiscovery,
        prisonerNumber = adjudication.prisonerNumber,
        chargeNumber = it.chargeNumber,
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
      if (this.any { it.dateTimeOfHearing.isAfter(date) || it.dateTimeOfHearing.isEqual(date) }) {
        throw ValidationException("A hearing can not be before or at the same time as the previous hearing")
      }
    }

    fun List<OutcomeHistoryDto>.shouldRemoveLastScheduleHearing(): Boolean {
      val lastItem = this.lastOrNull() ?: return false
      val outcome = lastItem.outcome?.referralOutcome ?: lastItem.outcome?.outcome ?: return false
      return outcome.code == OutcomeCode.SCHEDULE_HEARING
    }

    fun ReportedAdjudication.getHearing(): Hearing = this.getLatestHearing() ?: throwHearingNotFoundException()

    fun ReportedAdjudication.calcFirstHearingDate(): LocalDateTime? = this.hearings.minOfOrNull { it.dateTimeOfHearing }

    fun Hearing.canDelete(): Hearing {
      if (OutcomeCode.referrals().contains(this.hearingOutcome?.code?.outcomeCode) || this.hearingOutcome?.code == HearingOutcomeCode.COMPLETE) {
        throw ValidationException("Unable to delete hearing via api DEL/hearing - outcome associated to this hearing")
      }
      return this
    }

    fun List<HearingDto>.getLatestHearingId(): Long? =
      this.maxByOrNull { h -> h.dateTimeOfHearing }?.id

    private fun throwHearingNotFoundException(): Nothing = throw EntityNotFoundException("Hearing not found")
  }
}
