package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class PrintSupportServiceTest : ReportedAdjudicationTestBase() {

  private val printSupportService = PrintSupportService(reportedAdjudicationRepository, offenceCodeLookupService, authenticationFacade)

  @Test
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    Assertions.assertThatThrownBy {
      printSupportService.getDis5Data(
        chargeNumber = "1",
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }

  @Nested
  inner class Dis5 {
    @Test
    fun `get dis5 data for current establishment equal to originating agency`() {
      val report = entityBuilder.reportedAdjudication()
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(report)
      whenever(reportedAdjudicationRepository.findByOffenderBookingIdAndStatus(any(), any())).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication(chargeNumber = "99999"),
          entityBuilder.reportedAdjudication(chargeNumber = "88888").also {
            it.overrideAgencyId = report.originatingAgencyId
          },
        ),
      )

      val data = printSupportService.getDis5Data(chargeNumber = "12345")
      assertThat(data.chargeNumber).isEqualTo(report.chargeNumber)
      assertThat(data.dateOfDiscovery).isEqualTo(report.dateTimeOfDiscovery.toLocalDate())
      assertThat(data.dateOfIncident).isEqualTo(report.dateTimeOfIncident.toLocalDate())
    }

    @Test
    fun `get dis5 data for current establishment equal to override agency`() {
      val report = entityBuilder.reportedAdjudication().also {
        it.overrideAgencyId = "LEI"
      }
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(report)
      whenever(reportedAdjudicationRepository.findByOffenderBookingIdAndStatus(any(), any())).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication(chargeNumber = "99999").also {
            it.originatingAgencyId = "LEI"
          },
          entityBuilder.reportedAdjudication(chargeNumber = "88888").also {
            it.originatingAgencyId = "LEI"
          },
        ),
      )

      val data = printSupportService.getDis5Data(chargeNumber = "12345")
    }
  }
}
