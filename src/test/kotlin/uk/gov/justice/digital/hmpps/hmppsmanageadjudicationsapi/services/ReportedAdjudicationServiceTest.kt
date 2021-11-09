package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.ReportedAdjudication
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException

class ReportedAdjudicationServiceTest {
  private val prisonApiGateway: PrisonApiGateway = mock()
  private lateinit var reportedAdjudicationService: ReportedAdjudicationService

  @BeforeEach
  fun beforeEach() {
    reportedAdjudicationService = ReportedAdjudicationService(prisonApiGateway)
  }

  @Nested
  inner class ReportedAdjudicationDetails {
    @Test
    fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
      whenever(prisonApiGateway.getReportedAdjudication(any())).thenThrow(EntityNotFoundException("ReportedAdjudication not found for 1"))

      assertThatThrownBy {
        reportedAdjudicationService.getReportedAdjudicationDetails(1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }

    @Test
    fun `returns the reported adjudication`() {
      val reportedAdjudication =
        ReportedAdjudication(
          adjudicationNumber = 1, bookingId = 123, reporterStaffId = 234,
          incidentTime = DATE_TIME_OF_INCIDENT, incidentLocationId = 345, statement = INCIDENT_STATEMENT,
          offenderNo = "A12345"
        )

      whenever(prisonApiGateway.getReportedAdjudication(any())).thenReturn(
        reportedAdjudication
      )

      val reportedAdjudicationDto = reportedAdjudicationService.getReportedAdjudicationDetails(1)

      assertThat(reportedAdjudicationDto)
        .extracting("adjudicationNumber", "prisonerNumber")
        .contains(1L, "A12345")

      assertThat(reportedAdjudicationDto.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident")
        .contains(345L, DATE_TIME_OF_INCIDENT)
    }
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
    private const val INCIDENT_STATEMENT = "A statement"
  }
}
