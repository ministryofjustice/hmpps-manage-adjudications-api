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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException

class ReportedAdjudicationServiceTest {
  private val draftAdjudicationRepository: DraftAdjudicationRepository = mock()
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private lateinit var reportedAdjudicationService: ReportedAdjudicationService

  @BeforeEach
  fun beforeEach() {
    whenever(authenticationFacade.currentUsername).thenReturn("ITAG_USER")

    reportedAdjudicationService =
      ReportedAdjudicationService(draftAdjudicationRepository, reportedAdjudicationRepository, authenticationFacade)
  }

  @Nested
  inner class ReportedAdjudicationDetails {
    @Test
    fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(null)

      assertThatThrownBy {
        reportedAdjudicationService.getReportedAdjudicationDetails(1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }

    @Test
    fun `returns the reported adjudication`() {
      val reportedAdjudication =
        ReportedAdjudication(
          reportNumber = 1, prisonerNumber = "AA1234A", bookingId = 123, agencyId = "MDI",
          dateTimeOfIncident = DATE_TIME_OF_INCIDENT, locationId = 345, statement = INCIDENT_STATEMENT,
          incidentRoleCode = "25b", incidentRoleAssociatedPrisonersNumber = "BB2345B",
          handoverDeadline = DATE_TIME_REPORTED_ADJUDICATION_EXPIRES,
        )
      reportedAdjudication.createdByUserId = "A_SMITH" // Add audit information

      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication
      )

      val reportedAdjudicationDto = reportedAdjudicationService.getReportedAdjudicationDetails(1)

      assertThat(reportedAdjudicationDto)
        .extracting("adjudicationNumber", "prisonerNumber", "bookingId", "dateTimeReportExpires", "createdByUserId")
        .contains(1L, "AA1234A", 123L, DATE_TIME_REPORTED_ADJUDICATION_EXPIRES, "A_SMITH")

      assertThat(reportedAdjudicationDto.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident")
        .contains(345L, DATE_TIME_OF_INCIDENT)

      assertThat(reportedAdjudicationDto.incidentRole)
        .extracting("roleCode", "associatedPrisonersNumber")
        .contains("25b", "BB2345B")

      assertThat(reportedAdjudicationDto.incidentStatement)
        .extracting("statement", "completed")
        .contains(INCIDENT_STATEMENT, true)
    }
  }

  @Nested
  inner class AllReportedAdjudications {
    @BeforeEach
    fun beforeEach() {
      val reportedAdjudication1 = ReportedAdjudication(
        id = 1,
        prisonerNumber = "AA1234A",
        bookingId = 123,
        reportNumber = 1,
        agencyId = "MDI",
        locationId = 345,
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        handoverDeadline = DATE_TIME_OF_INCIDENT.plusDays(2),
        incidentRoleCode = "25b",
        incidentRoleAssociatedPrisonersNumber = "BB2345B",
        statement = INCIDENT_STATEMENT,
      )
      reportedAdjudication1.createdByUserId = "A_SMITH"
      val reportedAdjudication2 = ReportedAdjudication(
        id = 2,
        prisonerNumber = "AA1234B",
        bookingId = 456,
        reportNumber = 2,
        agencyId = "MDI",
        locationId = 345,
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        handoverDeadline = DATE_TIME_OF_INCIDENT.plusDays(2),
        incidentRoleCode = null,
        incidentRoleAssociatedPrisonersNumber = null,
        statement = INCIDENT_STATEMENT,
      )
      reportedAdjudication2.createdByUserId = "P_SMITH"
      whenever(reportedAdjudicationRepository.findByAgencyId(any(), any())).thenReturn(
        PageImpl(
          listOf(reportedAdjudication1, reportedAdjudication2)
        )
      )
    }

    @Test
    fun `makes a call to the reported adjudication repository to get the page of adjudications`() {
      reportedAdjudicationService.getAllReportedAdjudications("MDI", Pageable.ofSize(20).withPage(0))

      verify(reportedAdjudicationRepository).findByAgencyId("MDI", Pageable.ofSize(20).withPage(0))
    }

    @Test
    fun `returns all reported adjudications`() {
      val myReportedAdjudications = reportedAdjudicationService.getAllReportedAdjudications("MDI", Pageable.ofSize(20).withPage(0))

      assertThat(myReportedAdjudications.content)
        .extracting("adjudicationNumber", "prisonerNumber", "bookingId", "createdByUserId")
        .contains(
          Tuple.tuple(1L, "AA1234A", 123L, "A_SMITH"),
          Tuple.tuple(2L, "AA1234B", 456L, "P_SMITH")
        )
    }
  }

  @Nested
  inner class MyReportedAdjudications {
    @BeforeEach
    fun beforeEach() {
      val reportedAdjudication1 = ReportedAdjudication(
        id = 1,
        prisonerNumber = "AA1234A",
        bookingId = 123,
        reportNumber = 1,
        agencyId = "MDI",
        locationId = 345,
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        handoverDeadline = DATE_TIME_OF_INCIDENT.plusDays(2),
        incidentRoleCode = "25b",
        incidentRoleAssociatedPrisonersNumber = "BB2345B",
        statement = INCIDENT_STATEMENT,
      )
      reportedAdjudication1.createdByUserId = "A_SMITH"
      val reportedAdjudication2 = ReportedAdjudication(
        id = 2,
        prisonerNumber = "AA1234B",
        bookingId = 456,
        reportNumber = 2,
        agencyId = "MDI",
        locationId = 345,
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        handoverDeadline = DATE_TIME_OF_INCIDENT.plusDays(2),
        incidentRoleCode = null,
        incidentRoleAssociatedPrisonersNumber = null,
        statement = INCIDENT_STATEMENT,
      )
      reportedAdjudication2.createdByUserId = "A_SMITH"
      whenever(reportedAdjudicationRepository.findByCreatedByUserIdAndAgencyId(any(), any(), any())).thenReturn(
        PageImpl(
          listOf(reportedAdjudication1, reportedAdjudication2)
        )
      )
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
    private val reportedAdjudication = ReportedAdjudication(
      reportNumber = 123, prisonerNumber = "AA1234A", bookingId = 123, agencyId = "MDI",
      dateTimeOfIncident = DATE_TIME_OF_INCIDENT, locationId = 345, statement = INCIDENT_STATEMENT,
      incidentRoleCode = "25b", incidentRoleAssociatedPrisonersNumber = "BB2345B",
      handoverDeadline = DATE_TIME_REPORTED_ADJUDICATION_EXPIRES
    )

    private val expectedSavedDraftAdjudication = DraftAdjudication(
      prisonerNumber = "AA1234A",
      reportNumber = 123L,
      reportByUserId = "A_SMITH",
      agencyId = "MDI",
      incidentDetails = IncidentDetails(
        locationId = 345L,
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        handoverDeadline = DATE_TIME_REPORTED_ADJUDICATION_EXPIRES
      ),
      incidentRole = IncidentRole(
        roleCode = "25b",
        associatedPrisonersNumber = "BB2345B",
      ),
      incidentStatement = IncidentStatement(
        completed = true,
        statement = INCIDENT_STATEMENT
      )
    )

    private val savedDraftAdjudication = expectedSavedDraftAdjudication.copy(
      id = 1,
    )

    @BeforeEach
    fun beforeEach() {
      reportedAdjudication.createdByUserId = "A_SMITH"
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
      whenever(draftAdjudicationRepository.save(any())).thenReturn(savedDraftAdjudication)
    }

    @Test
    fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(null)

      assertThatThrownBy {
        reportedAdjudicationService.createDraftFromReportedAdjudication(1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
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
      assertThat(createdDraft.incidentRole)
        .extracting("roleCode", "associatedPrisonersNumber")
        .contains("25b", "BB2345B")
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
