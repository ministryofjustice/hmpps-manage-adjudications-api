package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.CombinedOutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import javax.validation.ValidationException

class ReferralServiceTest : ReportedAdjudicationTestBase() {

  private val outcomeService: OutcomeService = mock()
  private val hearingOutcomeService: HearingOutcomeService = mock()

  private var referralService = ReferralService(hearingOutcomeService, outcomeService)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // not applicable
  }

  @Test
  fun `create outcome and hearing outcome for referral`() {

    referralService.createReferral(
      1, HearingOutcomeCode.REFER_POLICE, "test", "details",
    )

    verify(hearingOutcomeService, atLeastOnce()).createReferral(
      adjudicationNumber = 1, code = HearingOutcomeCode.REFER_POLICE, adjudicator = "test", details = "details",
    )

    verify(outcomeService, atLeastOnce()).createReferral(adjudicationNumber = 1, code = OutcomeCode.REFER_POLICE, details = "details")
  }

  @Test
  fun `remove referral should only remove outcome`() {
    whenever(outcomeService.getOutcomes(1)).thenReturn(
      listOf(
        CombinedOutcomeDto(
          outcome = OutcomeDto(
            id = 1,
            code = OutcomeCode.REFER_POLICE
          )
        )
      )
    )

    referralService.removeReferral(1,)

    verify(outcomeService, atLeastOnce()).deleteOutcome(1, 1)
    verify(hearingOutcomeService, never()).deleteHearingOutcome(1,)
  }

  @Test
  fun `remove referral should remove outcome and hearing outcome`() {
    whenever(outcomeService.getOutcomes(1)).thenReturn(
      listOf(
        CombinedOutcomeDto(
          outcome = OutcomeDto(
            id = 1,
            code = OutcomeCode.REFER_POLICE
          ),
        )
      )
    )

    whenever(hearingOutcomeService.getHearingOutcomeForReferral(1, OutcomeCode.REFER_POLICE, 0)).thenReturn(
      HearingOutcome(id = 1, code = HearingOutcomeCode.REFER_POLICE, adjudicator = "")
    )

    referralService.removeReferral(1,)

    verify(outcomeService, atLeastOnce()).deleteOutcome(1, 1)
    verify(hearingOutcomeService, atLeastOnce()).deleteHearingOutcome(1,)
  }

  @Test
  fun `remove referral should remove outcome and hearing outcome and referral outcome`() {
    whenever(outcomeService.getOutcomes(1)).thenReturn(
      listOf(
        CombinedOutcomeDto(
          outcome = OutcomeDto(
            id = 3,
            code = OutcomeCode.REFER_INAD
          ),
        ),
        CombinedOutcomeDto(
          outcome = OutcomeDto(
            id = 1,
            code = OutcomeCode.REFER_POLICE
          ),
          referralOutcome = OutcomeDto(
            id = 2,
            code = OutcomeCode.NOT_PROCEED
          )
        )
      )
    )

    whenever(hearingOutcomeService.getHearingOutcomeForReferral(1, OutcomeCode.REFER_POLICE, 0)).thenReturn(
      HearingOutcome(id = 1, code = HearingOutcomeCode.REFER_POLICE, adjudicator = "")
    )

    referralService.removeReferral(1,)

    verify(outcomeService, atLeast(1)).deleteOutcome(1, 1)
    verify(outcomeService, atLeast(1)).deleteOutcome(1, 2)
    verify(hearingOutcomeService, atLeastOnce()).deleteHearingOutcome(1,)
  }

  @Test
  fun `responds with bad request if no referral on adjudication`() {
    whenever(outcomeService.getOutcomes(1)).thenReturn(
      listOf(
        CombinedOutcomeDto(
          outcome = OutcomeDto(
            code = OutcomeCode.NOT_PROCEED
          )
        )
      )
    )

    Assertions.assertThatThrownBy {
      referralService.removeReferral(1,)
    }.isInstanceOf(ValidationException::class.java)
      .hasMessageContaining("No referral for adjudication")
  }
}
