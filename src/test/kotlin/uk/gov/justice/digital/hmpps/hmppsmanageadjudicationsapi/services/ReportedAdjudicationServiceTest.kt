package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.SubmittedAdjudicationHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.SubmittedAdjudicationHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException

class ReportedAdjudicationServiceTest {
  private val prisonApiGateway: PrisonApiGateway = mock()
  private val dateCalculationService: DateCalculationService = mock()
  private val draftAdjudicationRepository: DraftAdjudicationRepository = mock()
  private val submittedAdjudicationHistoryRepository: SubmittedAdjudicationHistoryRepository = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private lateinit var reportedAdjudicationService: ReportedAdjudicationService

  @BeforeEach
  fun beforeEach() {
    whenever(authenticationFacade.currentUsername).thenReturn("ITAG_USER")

    reportedAdjudicationService =
      ReportedAdjudicationService(draftAdjudicationRepository, submittedAdjudicationHistoryRepository, authenticationFacade, prisonApiGateway, dateCalculationService)
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
          adjudicationNumber = 1, offenderNo = "AA1234A", bookingId = 123, reporterStaffId = 234, agencyId = "MDI",
          incidentTime = DATE_TIME_OF_INCIDENT, incidentLocationId = 345, statement = INCIDENT_STATEMENT,
          createdByUserId = "A_SMITH",
        )

      whenever(prisonApiGateway.getReportedAdjudication(any())).thenReturn(
        reportedAdjudication
      )
      whenever(dateCalculationService.calculate48WorkingHoursFrom(any())).thenReturn(DATE_TIME_REPORTED_ADJUDICATION_EXPIRES)

      val reportedAdjudicationDto = reportedAdjudicationService.getReportedAdjudicationDetails(1)

      assertThat(reportedAdjudicationDto)
        .extracting("adjudicationNumber", "prisonerNumber", "bookingId", "dateTimeReportExpires", "createdByUserId")
        .contains(1L, "AA1234A", 123L, DATE_TIME_REPORTED_ADJUDICATION_EXPIRES, "A_SMITH")

      assertThat(reportedAdjudicationDto.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident")
        .contains(345L, DATE_TIME_OF_INCIDENT)
    }
  }

  @Nested
  inner class AllReportedAdjudications {
    @BeforeEach
    fun beforeEach() {
      whenever(submittedAdjudicationHistoryRepository.findByAgencyId(any(), any())).thenReturn(
        PageImpl(
          listOf(
            SubmittedAdjudicationHistory(adjudicationNumber = 1, agencyId = "MDI", dateTimeOfIncident = DATE_TIME_OF_INCIDENT, dateTimeSent = DATE_TIME_OF_INCIDENT),
            SubmittedAdjudicationHistory(adjudicationNumber = 2, agencyId = "MDI", dateTimeOfIncident = DATE_TIME_OF_INCIDENT, dateTimeSent = DATE_TIME_OF_INCIDENT),
          )
        )
      )
      whenever(prisonApiGateway.getReportedAdjudications(any())).thenReturn(
        listOf(
          ReportedAdjudication(
            adjudicationNumber = 1, offenderNo = "AA1234A", bookingId = 123, reporterStaffId = 234, agencyId = "MDI",
            incidentTime = DATE_TIME_OF_INCIDENT, incidentLocationId = 345, statement = INCIDENT_STATEMENT,
            createdByUserId = "A_SMITH",
          ),
          ReportedAdjudication(
            adjudicationNumber = 2, offenderNo = "AA1234B", bookingId = 456, reporterStaffId = 234, agencyId = "MDI",
            incidentTime = DATE_TIME_OF_INCIDENT, incidentLocationId = 345, statement = INCIDENT_STATEMENT,
            createdByUserId = "A_SMITH",
          )
        )
      )
      whenever(dateCalculationService.calculate48WorkingHoursFrom(any())).thenReturn(DATE_TIME_REPORTED_ADJUDICATION_EXPIRES)
    }

    @Test
    fun `makes a call to the submitted adjudication repository to get the page of adjudications`() {
      reportedAdjudicationService.getAllReportedAdjudications("MDI", Pageable.ofSize(20).withPage(0))

      verify(submittedAdjudicationHistoryRepository).findByAgencyId("MDI", Pageable.ofSize(20).withPage(0))
    }

    @Test
    fun `makes a call to prison api to retrieve the found adjudication details`() {
      reportedAdjudicationService.getAllReportedAdjudications("MDI", Pageable.ofSize(20).withPage(0))

      verify(prisonApiGateway).getReportedAdjudications(listOf(1, 2))
    }

    @Test
    fun `returns all reported adjudications`() {
      val myReportedAdjudications = reportedAdjudicationService.getAllReportedAdjudications("MDI", Pageable.ofSize(20).withPage(0))

      assertThat(myReportedAdjudications.content)
        .extracting("adjudicationNumber", "prisonerNumber", "bookingId")
        .contains(
          Tuple.tuple(1L, "AA1234A", 123L),
          Tuple.tuple(2L, "AA1234B", 456L)
        )
    }
  }

  @Nested
  inner class MyReportedAdjudications {
    @BeforeEach
    fun beforeEach() {
      whenever(submittedAdjudicationHistoryRepository.findByCreatedByUserIdAndAgencyId(any(), any(), any())).thenReturn(
        PageImpl(
          listOf(
            SubmittedAdjudicationHistory(adjudicationNumber = 1, agencyId = "MDI", dateTimeOfIncident = DATE_TIME_OF_INCIDENT, dateTimeSent = DATE_TIME_OF_INCIDENT),
            SubmittedAdjudicationHistory(adjudicationNumber = 2, agencyId = "MDI", dateTimeOfIncident = DATE_TIME_OF_INCIDENT, dateTimeSent = DATE_TIME_OF_INCIDENT),
          )
        )
      )
      whenever(prisonApiGateway.getReportedAdjudications(any())).thenReturn(
        listOf(
          ReportedAdjudication(
            adjudicationNumber = 1, offenderNo = "AA1234A", bookingId = 123, reporterStaffId = 234, agencyId = "MDI",
            incidentTime = DATE_TIME_OF_INCIDENT, incidentLocationId = 345, statement = INCIDENT_STATEMENT,
            createdByUserId = "A_SMITH",
          ),
          ReportedAdjudication(
            adjudicationNumber = 2, offenderNo = "AA1234B", bookingId = 456, reporterStaffId = 234, agencyId = "MDI",
            incidentTime = DATE_TIME_OF_INCIDENT, incidentLocationId = 345, statement = INCIDENT_STATEMENT,
            createdByUserId = "A_SMITH",
          )
        )
      )
      whenever(dateCalculationService.calculate48WorkingHoursFrom(any())).thenReturn(DATE_TIME_REPORTED_ADJUDICATION_EXPIRES)
    }

    @Test
    fun `makes a call to prison api to retrieve reported adjudications`() {
      reportedAdjudicationService.getMyReportedAdjudications("MDI", Pageable.ofSize(20).withPage(0))

      verify(prisonApiGateway).getReportedAdjudications(listOf(1, 2))
    }

    @Test
    fun `returns my reported adjudications`() {
      val myReportedAdjudications = reportedAdjudicationService.getMyReportedAdjudications("MDI", Pageable.ofSize(20).withPage(0))

      assertThat(myReportedAdjudications.content)
        .extracting("adjudicationNumber", "prisonerNumber", "bookingId")
        .contains(
          Tuple.tuple(1L, "AA1234A", 123L),
          Tuple.tuple(2L, "AA1234B", 456L)
        )
    }
  }

  @Nested
  inner class CreateDraftFromReported {
    val reportedAdjudication = ReportedAdjudication(
      adjudicationNumber = 123, offenderNo = "AA1234A", bookingId = 123, reporterStaffId = 234, agencyId = "MDI",
      incidentTime = DATE_TIME_OF_INCIDENT, incidentLocationId = 345, statement = INCIDENT_STATEMENT,
      createdByUserId = "A_SMITH",
    )

    val expectedSavedDraftAdjudication = DraftAdjudication(
      prisonerNumber = "AA1234A",
      reportNumber = 123L,
      reportByUserId = "A_SMITH",
      agencyId = "MDI",
      incidentDetails = IncidentDetails(
        locationId = 345L,
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        handoverDeadline = DATE_TIME_REPORTED_ADJUDICATION_EXPIRES
      ),
      incidentStatement = IncidentStatement(
        completed = true,
        statement = INCIDENT_STATEMENT
      )
    )

    val savedDraftAdjudication = expectedSavedDraftAdjudication.copy(
      id = 1,
    )

    @BeforeEach
    fun beforeEach() {
      whenever(prisonApiGateway.getReportedAdjudication(any())).thenReturn(reportedAdjudication)
      whenever(draftAdjudicationRepository.save(any())).thenReturn(savedDraftAdjudication)
      whenever(dateCalculationService.calculate48WorkingHoursFrom(any())).thenReturn(DATE_TIME_REPORTED_ADJUDICATION_EXPIRES)
    }

    @Test
    fun `makes a call to prison api to retrieve the reported adjudication`() {
      reportedAdjudicationService.createDraftFromReportedAdjudication(123)

      verify(prisonApiGateway).getReportedAdjudication(123)
    }

    @Test
    fun `adds the relevant draft data to the repository`() {
      reportedAdjudicationService.createDraftFromReportedAdjudication(123)

      verify(draftAdjudicationRepository).save(expectedSavedDraftAdjudication)
    }

    @Test
    fun `returns the correct data`() {
      val createdDraft = reportedAdjudicationService.createDraftFromReportedAdjudication(123)

      assertThat(createdDraft)
        .extracting("prisonerNumber", "id", "adjudicationNumber", "startedByUserId")
        .contains("AA1234A", 1L, 123L, "A_SMITH")
      assertThat(createdDraft.incidentDetails)
        .extracting("dateTimeOfIncident", "handoverDeadline", "locationId")
        .contains(DATE_TIME_OF_INCIDENT, DATE_TIME_REPORTED_ADJUDICATION_EXPIRES, 345L)
      assertThat(createdDraft.incidentStatement)
        .extracting("completed", "statement")
        .contains(true, INCIDENT_STATEMENT)
    }
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
    private val DATE_TIME_REPORTED_ADJUDICATION_EXPIRES = LocalDateTime.of(2010, 10, 14, 10, 0)
    private const val INCIDENT_STATEMENT = "A statement"
  }
}
