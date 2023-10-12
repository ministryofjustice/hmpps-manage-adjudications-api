package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingDto
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.LegacySyncService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
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
  private val legacySyncService: LegacySyncService,
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

    val oicHearingId = legacySyncService.createHearing(
      adjudicationNumber = chargeNumber,
      oicHearingRequest = OicHearingRequest(
        dateTimeOfHearing = dateTimeOfHearing,
        hearingLocationId = locationId,
        oicHearingType = oicHearingType,
      ),
    )

    reportedAdjudication.let {
      it.hearings.add(
        Hearing(
          agencyId = authenticationFacade.activeCaseload,
          chargeNumber = reportedAdjudication.chargeNumber,
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

    legacySyncService.amendHearing(
      adjudicationNumber = chargeNumber.toLong(),
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

    return saveToDto(reportedAdjudication).also {
      it.hearingIdActioned = hearingToEdit.id
    }
  }

  fun deleteHearing(chargeNumber: String): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    val hearingToRemove = reportedAdjudication.getHearing().canDelete()

    legacySyncService.deleteHearing(
      adjudicationNumber = chargeNumber.toLong(),
      oicHearingId = hearingToRemove.oicHearingId,
    )

    reportedAdjudication.also {
      it.hearings.remove(hearingToRemove)
      it.dateTimeOfFirstHearing = it.calcFirstHearingDate()

      if (it.getOutcomeHistory().shouldRemoveLastScheduleHearing()) it.getOutcomeToRemove().deleted = true
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
      if (this.any { it.dateTimeOfHearing.isAfter(date) }) {
        throw ValidationException("A hearing can not be before the previous hearing")
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
