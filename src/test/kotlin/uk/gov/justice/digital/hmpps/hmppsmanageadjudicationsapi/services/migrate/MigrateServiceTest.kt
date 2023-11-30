package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase

class MigrateServiceTest : ReportedAdjudicationTestBase() {

  private val migrateExistingRecordService: MigrateExistingRecordService = mock()
  private val migrateNewRecordService: MigrateNewRecordService = mock()
  private val featureFlagsConfig: FeatureFlagsConfig = mock()

  private val migrateService = MigrateService(
    reportedAdjudicationRepository,
    migrateNewRecordService,
    migrateExistingRecordService,
    featureFlagsConfig,
  )

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }

  @Test
  fun `accept a new record calls new record migration service`() {
    whenever(reportedAdjudicationRepository.findByChargeNumberIn(any())).thenReturn(emptyList())
    migrateService.accept(migrationFixtures.ADULT_SINGLE_OFFENCE)

    verify(migrateNewRecordService, atLeastOnce()).accept(any())
  }

  @Test
  fun `accept an existing record calls existing record migration service`() {
    whenever(reportedAdjudicationRepository.findByChargeNumberIn(any())).thenReturn(listOf(entityBuilder.reportedAdjudication()))
    migrateService.accept(migrationFixtures.ADULT_SINGLE_OFFENCE)

    verify(migrateExistingRecordService, atLeastOnce()).accept(any(), any())
  }

  @Test
  fun `accept redirects existing report to new record migration service ir the offence sequence is greater than 1`() {
    whenever(reportedAdjudicationRepository.findByChargeNumberIn(any())).thenReturn(listOf(entityBuilder.reportedAdjudication()))
    migrateService.accept(migrationFixtures.ADULT_MULITPLE_OFFENCES.last())

    verify(migrateNewRecordService, atLeastOnce()).accept(any())
  }

  @Test
  fun `throws exception when skip is true`() {
    whenever(featureFlagsConfig.skipExistingRecords).thenReturn(true)
    whenever(reportedAdjudicationRepository.findByChargeNumberIn(any())).thenReturn(listOf(entityBuilder.reportedAdjudication()))

    Assertions.assertThatThrownBy {
      migrateService.accept(migrationFixtures.ADULT_SINGLE_OFFENCE)
    }.isInstanceOf(SkipExistingRecordException::class.java)
      .hasMessageContaining("Skip existing record flag is true")
  }

  @Test
  fun `throws exception if we have already processed record - avoids duplicate key error`() {
    whenever(reportedAdjudicationRepository.findByChargeNumberIn(any())).thenReturn(
      listOf(
        entityBuilder.reportedAdjudication().also {
          it.migrated = true
        },
      ),
    )

    Assertions.assertThatThrownBy {
      migrateService.accept(migrationFixtures.ADULT_SINGLE_OFFENCE)
    }.isInstanceOf(DuplicateCreationException::class.java)
      .hasMessageContaining("already processed this record")
  }

  @Test
  fun `ignore rejected records in DPS, if present in nomis `() {
    whenever(reportedAdjudicationRepository.findByChargeNumberIn(any())).thenReturn(
      listOf(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.REJECTED
        },
      ),
    )

    migrateService.accept(migrationFixtures.ADULT_SINGLE_OFFENCE)

    verify(migrateNewRecordService, never()).accept(any())
    verify(migrateExistingRecordService, never()).accept(any(), any())
  }
}
