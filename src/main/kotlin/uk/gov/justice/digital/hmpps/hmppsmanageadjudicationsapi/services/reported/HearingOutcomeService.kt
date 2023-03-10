package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingOutcomeService.Companion.latestOutcomeIsAdjourn
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingService.Companion.getHearing
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException

@Service
@Transactional
class HearingOutcomeService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {
  fun createReferral(
    adjudicationNumber: Long,
    code: HearingOutcomeCode,
    adjudicator: String,
    details: String,
  ): ReportedAdjudicationDto =
    createHearingOutcome(
      adjudicationNumber = adjudicationNumber,
      code = code.validateReferral(),
      adjudicator = adjudicator,
      details = details
    )

  fun createAdjourn(
    adjudicationNumber: Long,
    adjudicator: String,
    reason: HearingOutcomeAdjournReason,
    details: String,
    plea: HearingOutcomePlea,
  ): ReportedAdjudicationDto =
    createHearingOutcome(
      adjudicationNumber = adjudicationNumber,
      code = HearingOutcomeCode.ADJOURN,
      adjudicator = adjudicator,
      reason = reason,
      plea = plea,
      details = details
    )

  fun createCompletedHearing(
    adjudicationNumber: Long,
    adjudicator: String,
    plea: HearingOutcomePlea,
  ): ReportedAdjudicationDto = createHearingOutcome(
    adjudicationNumber = adjudicationNumber,
    code = HearingOutcomeCode.COMPLETE,
    adjudicator = adjudicator,
    plea = plea
  )

  fun removeAdjourn(
    adjudicationNumber: Long
  ): ReportedAdjudicationDto {
    findByAdjudicationNumber(adjudicationNumber).latestOutcomeIsAdjourn()

    return deleteHearingOutcome(adjudicationNumber = adjudicationNumber)
  }

  private fun createHearingOutcome(
    adjudicationNumber: Long,
    code: HearingOutcomeCode,
    adjudicator: String,
    reason: HearingOutcomeAdjournReason? = null,
    details: String? = null,
    plea: HearingOutcomePlea? = null
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      if (code == HearingOutcomeCode.ADJOURN) it.status = ReportedAdjudicationStatus.ADJOURNED
    }

    reportedAdjudication.getHearing().hearingOutcome = HearingOutcome(
      code = code,
      reason = reason,
      details = details,
      adjudicator = adjudicator,
      plea = plea,
    )

    return saveToDto(reportedAdjudication)
  }

  fun deleteHearingOutcome(adjudicationNumber: Long): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      if (it.status == ReportedAdjudicationStatus.ADJOURNED) it.calculateStatus()
    }
    val outcomeToRemove = reportedAdjudication.getHearing()

    outcomeToRemove.hearingOutcome.hearingOutcomeExists()
    outcomeToRemove.hearingOutcome = null

    return saveToDto(reportedAdjudication)
  }

  fun getCurrentStatusAndLatestOutcome(
    adjudicationNumber: Long
  ): Pair<ReportedAdjudicationStatus, HearingOutcome> {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)

    return Pair(reportedAdjudication.status, reportedAdjudication.latestHearingOutcome())
  }

  fun amendHearingOutcome(
    adjudicationNumber: Long,
    outcomeCodeToAmend: HearingOutcomeCode,
    adjudicator: String? = null,
    details: String? = null,
    plea: HearingOutcomePlea? = null,
    adjournedReason: HearingOutcomeAdjournReason? = null,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    val hearingOutcomeToAmend = reportedAdjudication.latestHearingOutcome().isLatestSameAsAmendRequest(outcomeCodeToAmend)

    adjudicator?.let { hearingOutcomeToAmend.adjudicator = it }

    when (outcomeCodeToAmend) {
      HearingOutcomeCode.COMPLETE -> plea?.let { hearingOutcomeToAmend.plea = it }
      HearingOutcomeCode.REFER_POLICE, HearingOutcomeCode.REFER_INAD  -> details?.let { hearingOutcomeToAmend.details = it }
      HearingOutcomeCode.ADJOURN -> {
        details?.let { hearingOutcomeToAmend.details = it }
        plea?.let { hearingOutcomeToAmend.plea = it }
        adjournedReason?.let { hearingOutcomeToAmend.reason = it }
      }
    }

    return saveToDto(reportedAdjudication)
  }

  fun getHearingOutcomeForReferral(adjudicationNumber: Long, code: OutcomeCode, outcomeIndex: Int): HearingOutcome? {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    if (reportedAdjudication.hearings.none { it.hearingOutcome?.code?.outcomeCode == code }) return null
    val matched = reportedAdjudication.hearings.filter { it.hearingOutcome?.code?.outcomeCode == code }.sortedBy { it.dateTimeOfHearing }
    // note: you can only REFER_POLICE once without a hearing
    val actualIndex = if (matched.size > outcomeIndex) outcomeIndex else outcomeIndex - 1

    return matched[actualIndex].hearingOutcome
  }

  companion object {
    fun HearingOutcome?.hearingOutcomeExists() = this ?: throw EntityNotFoundException("outcome not found for hearing")

    fun ReportedAdjudication.latestOutcomeIsAdjourn() {
      val latest = this.latestHearingOutcome()
      if (latest.code != HearingOutcomeCode.ADJOURN)
        throw ValidationException("latest outcome is not an adjourn")
    }

    fun ReportedAdjudication.latestHearingOutcome(): HearingOutcome =
      this.getHearing().hearingOutcome ?: throw EntityNotFoundException("outcome not found for hearing")
  }

  fun HearingOutcome.isLatestSameAsAmendRequest(outcomeCodeToAmend: HearingOutcomeCode): HearingOutcome {
    if (this.code != outcomeCodeToAmend) throw ValidationException("latest outcome is not of same type")
    return this
  }
}
