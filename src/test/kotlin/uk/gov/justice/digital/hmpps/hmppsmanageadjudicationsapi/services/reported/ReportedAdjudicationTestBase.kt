package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.EntityBuilder
import java.time.LocalDateTime

interface TestsToImplement {
  @Test
  fun `throws an entity not found if the reported adjudication for the supplied id does not exists`()
}
abstract class ReportedAdjudicationTestBase : TestsToImplement {
  internal val offenceCodeLookupService: OffenceCodeLookupService = mock()
  internal val authenticationFacade: AuthenticationFacade = mock()
  internal val reportedAdjudicationRepository: ReportedAdjudicationRepository = mock()

  @BeforeEach
  fun beforeEach() {
    whenever(authenticationFacade.currentUsername).thenReturn("ITAG_USER")

    whenever(offenceCodeLookupService.getParagraphNumber(2, false)).thenReturn(OFFENCE_CODE_2_PARAGRAPH_NUMBER)
    whenever(
      offenceCodeLookupService.getParagraphDescription(
        2,
        false
      )
    ).thenReturn(OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION)
    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(2, false)).thenReturn(
      OFFENCE_CODE_2_NOMIS_CODE_ON_OWN
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(2, false)).thenReturn(
      OFFENCE_CODE_2_NOMIS_CODE_ASSISTED
    )

    whenever(offenceCodeLookupService.getParagraphNumber(3, false)).thenReturn(OFFENCE_CODE_3_PARAGRAPH_NUMBER)
    whenever(
      offenceCodeLookupService.getParagraphDescription(
        3,
        false
      )
    ).thenReturn(OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION)
    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(3, false)).thenReturn(
      OFFENCE_CODE_3_NOMIS_CODE_ON_OWN
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(3, false)).thenReturn(
      OFFENCE_CODE_3_NOMIS_CODE_ASSISTED
    )

    whenever(offenceCodeLookupService.getParagraphNumber(2, true)).thenReturn(YOUTH_OFFENCE_CODE_2_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(2, true)).thenReturn(
      YOUTH_OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION
    )
    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(2, true)).thenReturn(
      YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ON_OWN
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(2, true)).thenReturn(
      YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ASSISTED
    )

    // TODO - Review whether this is required
    whenever(offenceCodeLookupService.getParagraphNumber(3, true)).thenReturn(YOUTH_OFFENCE_CODE_3_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(3, true)).thenReturn(
      YOUTH_OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION
    )
    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(3, true)).thenReturn(
      YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ON_OWN
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(3, true)).thenReturn(
      YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ASSISTED
    )
  }

  companion object {
    val entityBuilder: EntityBuilder = EntityBuilder()
    val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)

    const val OFFENCE_CODE_2_PARAGRAPH_NUMBER = "5(b)"
    const val OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION = "A paragraph description"
    const val OFFENCE_CODE_2_NOMIS_CODE_ON_OWN = "5b"
    const val OFFENCE_CODE_2_NOMIS_CODE_ASSISTED = "25z"

    const val OFFENCE_CODE_3_PARAGRAPH_NUMBER = "6(a)"
    const val OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION = "Another paragraph description"
    const val OFFENCE_CODE_3_NOMIS_CODE_ON_OWN = "5f"
    const val OFFENCE_CODE_3_NOMIS_CODE_ASSISTED = "25f"

    const val YOUTH_OFFENCE_CODE_2_PARAGRAPH_NUMBER = "7(b)"
    const val YOUTH_OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION = "A youth paragraph description"
    const val YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ON_OWN = "7b"
    const val YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ASSISTED = "29z"

    const val YOUTH_OFFENCE_CODE_3_PARAGRAPH_NUMBER = "17(b)"
    const val YOUTH_OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION = "Another youth paragraph description"
    const val YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ON_OWN = "17b"
    const val YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ASSISTED = "29f"
  }
}
