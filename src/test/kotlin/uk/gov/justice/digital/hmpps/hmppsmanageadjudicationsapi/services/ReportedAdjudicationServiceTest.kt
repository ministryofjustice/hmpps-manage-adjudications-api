package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.SubmittedAdjudicationHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.SubmittedAdjudicationHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException

class ReportedAdjudicationServiceTest {
  private val prisonApiGateway: PrisonApiGateway = mock()
  private val submittedAdjudicationHistoryRepository: SubmittedAdjudicationHistoryRepository = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private lateinit var reportedAdjudicationService: ReportedAdjudicationService

  @BeforeEach
  fun beforeEach() {
    whenever(authenticationFacade.currentUsername).thenReturn("ITAG_USER")

    reportedAdjudicationService =
      ReportedAdjudicationService(submittedAdjudicationHistoryRepository, authenticationFacade, prisonApiGateway)
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
          adjudicationNumber = 1, offenderNo = "AA1234A", bookingId = 123, reporterStaffId = 234,
          incidentTime = DATE_TIME_OF_INCIDENT, incidentLocationId = 345, statement = INCIDENT_STATEMENT
        )

      whenever(prisonApiGateway.getReportedAdjudication(any())).thenReturn(
        reportedAdjudication
      )

      val reportedAdjudicationDto = reportedAdjudicationService.getReportedAdjudicationDetails(1)

      assertThat(reportedAdjudicationDto)
        .extracting("adjudicationNumber", "prisonerNumber", "bookingId")
        .contains(1L, "AA1234A", 123L)

      assertThat(reportedAdjudicationDto.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident")
        .contains(345L, DATE_TIME_OF_INCIDENT)
    }
  }

  @Nested
  inner class MyReportedAdjudications {
    @BeforeEach
    fun beforeEach() {
      whenever(submittedAdjudicationHistoryRepository.findByCreatedByUserId(any())).thenReturn(
        listOf(
          SubmittedAdjudicationHistory(adjudicationNumber = 1, dateTimeSent = DATE_TIME_OF_INCIDENT),
          SubmittedAdjudicationHistory(adjudicationNumber = 2, dateTimeSent = DATE_TIME_OF_INCIDENT),
        )
      )
      whenever(prisonApiGateway.getReportedAdjudications(any<Set<Long>>())).thenReturn(
        listOf(
          ReportedAdjudication(
            adjudicationNumber = 1, offenderNo = "AA1234A", bookingId = 123, reporterStaffId = 234,
            incidentTime = DATE_TIME_OF_INCIDENT, incidentLocationId = 345, statement = INCIDENT_STATEMENT
          ),
          ReportedAdjudication(
            adjudicationNumber = 2, offenderNo = "AA1234B", bookingId = 456, reporterStaffId = 234,
            incidentTime = DATE_TIME_OF_INCIDENT, incidentLocationId = 345, statement = INCIDENT_STATEMENT
          )
        )
      )
    }

    @Test
    fun `makes a call to retrieve all my reported adjudication submissions history`() {
      reportedAdjudicationService.getMyReportedAdjudications()

      verify(submittedAdjudicationHistoryRepository).findByCreatedByUserId("ITAG_USER")
    }

    @Test
    fun `makes a call to prison api to retrieve all reported adjudications created my the current user`() {
      reportedAdjudicationService.getMyReportedAdjudications()

      verify(prisonApiGateway).getReportedAdjudications(setOf(1, 2))
    }

    @Test
    fun `returns my reported adjudications`() {
      val myReportedAdjudications = reportedAdjudicationService.getMyReportedAdjudications()

      assertThat(myReportedAdjudications)
        .extracting("adjudicationNumber", "prisonerNumber", "bookingId")
        .contains(
          Tuple.tuple(1L, "AA1234A", 123L),
          Tuple.tuple(2L, "AA1234B", 456L)
        )
    }
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
    private const val INCIDENT_STATEMENT = "A statement"
  }
}
