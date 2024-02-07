package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase

class PrisonerMergeServiceTest : ReportedAdjudicationTestBase() {

  private val prisonerMergeService = PrisonerMergeService(reportedAdjudicationRepository)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }

  @Test
  fun `does not call repo if prisoner from is empty`() {
    prisonerMergeService.merge(
      prisonerFrom = null,
      prisonerTo = "TEST",
    )

    verify(reportedAdjudicationRepository, never()).findByPrisonerNumber(any())
  }

  @Test
  fun `does not call repo if prisoner to is empty`() {
    prisonerMergeService.merge(
      prisonerFrom = "TEST",
      prisonerTo = null,
    )

    verify(reportedAdjudicationRepository, never()).findByPrisonerNumber(any())
  }

  @Test
  fun `updates prisoner adjudications to prisoner to, from prisoner from`() {
    val fromReport = entityBuilder.reportedAdjudication(prisonerNumber = "FROM")
    whenever(reportedAdjudicationRepository.findByPrisonerNumber(any())).thenReturn(listOf(fromReport))

    prisonerMergeService.merge(
      prisonerFrom = "FROM",
      prisonerTo = "TO",
    )

    verify(reportedAdjudicationRepository, atLeastOnce()).findByPrisonerNumber("FROM")
    assertThat(fromReport.prisonerNumber).isEqualTo("TO")
  }
}
