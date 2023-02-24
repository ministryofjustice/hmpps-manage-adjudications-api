package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingService.Companion.getHearing
import javax.persistence.EntityNotFoundException

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
  ): ReportedAdjudicationDto {
    return createHearingOutcome(
      adjudicationNumber = adjudicationNumber, code = code.validateReferral(), adjudicator = adjudicator, details = details
    )
  }

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

  private fun createHearingOutcome(
    adjudicationNumber: Long,
    code: HearingOutcomeCode,
    adjudicator: String,
    reason: HearingOutcomeAdjournReason? = null,
    details: String? = null,
    plea: HearingOutcomePlea? = null
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    val hearingToAddOutcomeTo = reportedAdjudication.getHearing()
    val hearingOutcome = HearingOutcome(
      code = code,
      reason = reason,
      details = details,
      adjudicator = adjudicator,
      plea = plea,
    )

    hearingToAddOutcomeTo.hearingOutcome = hearingOutcome

    return saveToDto(reportedAdjudication)
  }

  fun deleteHearingOutcome(adjudicationNumber: Long, hearingId: Long): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    val outcomeToRemove = reportedAdjudication.getHearing()

    outcomeToRemove.hearingOutcome.hearingOutcomeExists()
    outcomeToRemove.hearingOutcome = null

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
  }
}
