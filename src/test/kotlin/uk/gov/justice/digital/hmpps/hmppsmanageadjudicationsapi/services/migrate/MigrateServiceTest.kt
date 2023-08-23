package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase

class MigrateServiceTest : ReportedAdjudicationTestBase() {

  private val migrateExistingRecordService: MigrateExistingRecordService = mock()
  private val migrateNewRecordService: MigrateNewRecordService = mock()
  private val featureFlagsConfig: FeatureFlagsConfig = mock()
  private val resetRecordService: ResetRecordService = mock()

  private val migrateService = MigrateService(
    reportedAdjudicationRepository,
    migrateNewRecordService,
    migrateExistingRecordService,
    featureFlagsConfig,
    resetRecordService,
  )

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }

  @CsvSource("true", "false")
  @ParameterizedTest
  fun `reset migration calls database reset`(skip: Boolean) {
    whenever(featureFlagsConfig.skipExistingRecords).thenReturn(skip)
    whenever(reportedAdjudicationRepository.findByMigratedIsFalse()).thenReturn(listOf("1"))
    migrateService.reset()
    verify(resetRecordService, atLeastOnce()).remove()
    verify(resetRecordService, if (skip) never() else atLeastOnce()).reset(any())
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

  @Test
  fun `accept redirects existing report to new record migration service ir the offence sequence is greater than 1`() {
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(entityBuilder.reportedAdjudication())
    migrateService.accept(migrationFixtures.ADULT_MULITPLE_OFFENCES.last())

    verify(migrateNewRecordService, atLeastOnce()).accept(any())
  }

  @Test
  fun `throws exception when skip is true`() {
    whenever(featureFlagsConfig.skipExistingRecords).thenReturn(true)
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(entityBuilder.reportedAdjudication())

    Assertions.assertThatThrownBy {
      migrateService.accept(migrationFixtures.ADULT_SINGLE_OFFENCE)
    }.isInstanceOf(SkipExistingRecordException::class.java)
      .hasMessageContaining("Skip existing record flag is true")
  }
}
