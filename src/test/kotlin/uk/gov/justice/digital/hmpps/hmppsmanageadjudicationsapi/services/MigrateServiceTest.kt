package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.junit.jupiter.api.Test
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase

class MigrateServiceTest : ReportedAdjudicationTestBase() {

  private val migrateService = MigrateService(reportedAdjudicationRepository)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }

  @Test
  fun `reset migration calls database reset`() {
    migrateService.reset()
    verify(reportedAdjudicationRepository, atLeastOnce()).deleteByMigratedIsTrue()
  }
}
