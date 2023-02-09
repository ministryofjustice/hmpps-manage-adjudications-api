package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeFinding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
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

  fun createHearingOutcome(
    adjudicationNumber: Long,
    hearingId: Long,
    code: HearingOutcomeCode,
    adjudicator: String,
    reason: HearingOutcomeAdjournReason? = null,
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

  fun updateHearingOutcome(
    adjudicationNumber: Long,
    hearingId: Long,
    code: HearingOutcomeCode,
    adjudicator: String,
    reason: HearingOutcomeAdjournReason? = null,
    details: String? = null,
    finding: HearingOutcomeFinding? = null,
    plea: HearingOutcomePlea? = null
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    val outcomeToAmend = reportedAdjudication.getHearingOutcome(hearingId)

    outcomeToAmend.code = code
    outcomeToAmend.adjudicator = adjudicator

    when (outcomeToAmend.code) {
      HearingOutcomeCode.COMPLETE -> {
        outcomeToAmend.details = null
        outcomeToAmend.reason = null
        outcomeToAmend.plea = plea
        outcomeToAmend.finding = finding
      }
      HearingOutcomeCode.ADJOURN -> {
        outcomeToAmend.details = details
        outcomeToAmend.reason = reason
        outcomeToAmend.plea = plea
        outcomeToAmend.finding = null
      }
      else -> {
        outcomeToAmend.details = details
        outcomeToAmend.reason = null
        outcomeToAmend.plea = null
        outcomeToAmend.finding = null
      }
    }

    outcomeToAmend.validate()

    return saveToDto(reportedAdjudication)
  }

  fun deleteHearingOutcome(adjudicationNumber: Long, hearingId: Long): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    val outcomeToRemove = reportedAdjudication.getHearing(hearingId)

    outcomeToRemove.hearingOutcome.hearingOutcomeExists(hearingId)
    outcomeToRemove.hearingOutcome = null

    return saveToDto(reportedAdjudication)
  }

  fun getHearingOutcomeForReferral(adjudicationNumber: Long, code: OutcomeCode, outcomeIndex: Int): HearingOutcome? {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    if (reportedAdjudication.hearings.none { it.hearingOutcome?.code?.outcomeCode == code }) return null
    val matched = reportedAdjudication.hearings.filter { it.hearingOutcome?.code?.outcomeCode == code }
    val actualIndex = if (matched.size > outcomeIndex) outcomeIndex else outcomeIndex - 1

    return matched[actualIndex].hearingOutcome
  }

  companion object {
    fun HearingOutcome.validate(): HearingOutcome {
      when (this.code) {
        HearingOutcomeCode.COMPLETE -> {
          validateField(this.plea)
          validateField(this.finding)
        }
        HearingOutcomeCode.ADJOURN -> {
          validateField(this.details)
          validateField(this.reason)
          validateField(this.plea)
        }
        else -> {}
      }
      return this
    }

    private fun validateField(field: Any?) = field ?: throw ValidationException("missing mandatory field")

    fun ReportedAdjudication.getHearingOutcome(hearingId: Long) =
      this.getHearing(hearingId).hearingOutcome.hearingOutcomeExists(hearingId)

    fun HearingOutcome?.hearingOutcomeExists(hearingId: Long) =
      this ?: throw EntityNotFoundException("outcome not found for hearing $hearingId")
  }
}
