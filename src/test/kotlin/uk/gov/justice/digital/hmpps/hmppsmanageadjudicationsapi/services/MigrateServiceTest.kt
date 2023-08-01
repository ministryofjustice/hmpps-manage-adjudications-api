package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateExistingRecordService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase

class MigrateServiceTest : ReportedAdjudicationTestBase() {

  private val migrateExistingRecordService: MigrateExistingRecordService = mock()
  private val migrateNewRecordService: MigrateNewRecordService = mock()

  private val migrateService = MigrateService(reportedAdjudicationRepository, migrateNewRecordService, migrateExistingRecordService)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }

  @Test
  fun `reset migration calls database reset`() {
    migrateService.reset()
    verify(reportedAdjudicationRepository, atLeastOnce()).deleteByMigratedIsTrue()
  }

  @Test
  fun `accept a new record calls new record migration service`() {
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(null)
    migrateService.accept(migrationFixtures.ADULT_SINGLE_OFFENCE)

    verify(migrateNewRecordService, atLeastOnce()).accept(any())
  }

  @Test
  fun `accept an existing record calls existing record migration service`() {
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(entityBuilder.reportedAdjudication())
    migrateService.accept(migrationFixtures.ADULT_SINGLE_OFFENCE)

    verify(migrateExistingRecordService, atLeastOnce()).accept(any(), any())
  }
}
