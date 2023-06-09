package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.TransferService.Companion.TRANSFERABLE_STATUSES
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase

class TransferServiceTest : ReportedAdjudicationTestBase() {

  private val transferService = TransferService(reportedAdjudicationRepository)

  @Test
  fun `prisoner has override value on records updated `() {
    val reportedAdjudication = entityBuilder.reportedAdjudication()
    val reportedAdjudication2 = entityBuilder.reportedAdjudication().also { it.agencyId = "TJW" }

    whenever(reportedAdjudicationRepository.findByPrisonerNumberAndStatusIn("AA1234A", TRANSFERABLE_STATUSES)).thenReturn(
      listOf(reportedAdjudication, reportedAdjudication2),
    )

    transferService.processTransferEvent(prisonerNumber = "AA1234A", agencyId = "TJW")

    assertThat(reportedAdjudication.overrideAgencyId).isEqualTo("TJW")
    assertThat(reportedAdjudication2.overrideAgencyId).isNull()
  }

  @CsvSource(",", "XYZ,", ",XYZ")
  @ParameterizedTest
  fun `does not call repo if agency or prisoner id null `(prisonerNumber: String?, agencyId: String?) {
    transferService.processTransferEvent(prisonerNumber = prisonerNumber, agencyId = agencyId)

    verify(reportedAdjudicationRepository, never()).findByPrisonerNumberAndStatusIn(any(), any())
  }

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // not applicable
  }
}
