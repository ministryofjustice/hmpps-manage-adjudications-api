package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode

class ReferralServiceTest : ReportedAdjudicationTestBase() {

  private var referralService = ReferralService(
    reportedAdjudicationRepository, offenceCodeLookupService, authenticationFacade
  )

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    TODO("Not yet implemented")
  }

  @ParameterizedTest
  @CsvSource("REFER_POLICE", "REFER_INAD")
  fun `create outcome and hearing outcome for referral`(code: HearingOutcomeCode) {
  }
}
