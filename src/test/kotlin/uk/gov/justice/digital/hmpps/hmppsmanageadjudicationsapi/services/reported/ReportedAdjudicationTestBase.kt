package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.EntityBuilder
import java.time.LocalDateTime

interface TestsToImplement {
  fun `throws an entity not found if the reported adjudication for the supplied id does not exists`()
}
abstract class ReportedAdjudicationTestBase : TestsToImplement {
  internal val offenceCodeLookupService: OffenceCodeLookupService = OffenceCodeLookupService()
  internal val authenticationFacade: AuthenticationFacade = mock()
  internal val reportedAdjudicationRepository: ReportedAdjudicationRepository = mock()

  @BeforeEach
  fun beforeEach() {
    whenever(authenticationFacade.currentUsername).thenReturn("ITAG_USER")
    whenever(authenticationFacade.activeCaseload).thenReturn("MDI")
  }

  companion object {
    val entityBuilder: EntityBuilder = EntityBuilder()
    val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
  }
}
