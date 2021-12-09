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
import org.mockito.ArgumentCaptor
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.SubmittedAdjudicationHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToPublish
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToUpdate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.SubmittedAdjudicationHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import java.time.Clock
import java.time.Instant.ofEpochMilli
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional
import javax.persistence.EntityNotFoundException

class DraftAdjudicationServiceTest {
  private val draftAdjudicationRepository: DraftAdjudicationRepository = mock()
  private val submittedAdjudicationHistoryRepository: SubmittedAdjudicationHistoryRepository = mock()
  private val prisonApiGateway: PrisonApiGateway = mock()
  private val dateCalculationService: DateCalculationService = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val clock: Clock = Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault())

  private lateinit var draftAdjudicationService: DraftAdjudicationService

  @BeforeEach
  fun beforeEach() {
    draftAdjudicationService =
      DraftAdjudicationService(
        draftAdjudicationRepository,
        submittedAdjudicationHistoryRepository,
        prisonApiGateway,
        dateCalculationService,
        authenticationFacade,
        clock
      )
  }

  @Nested
  inner class StartDraftAdjudications {
    @BeforeEach
    fun beforeEach() {
      whenever(draftAdjudicationRepository.save(any())).thenReturn(
        DraftAdjudication(
          id = 1,
          prisonerNumber = "A12345",
          agencyId = "MDI",
          incidentDetails = IncidentDetails(
            locationId = 2,
            dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
            handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
          )
        )
      )
      whenever(dateCalculationService.calculate48WorkingHoursFrom(any())).thenReturn(DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE)
    }

    @Test
    fun `makes a call to the repository to save the draft adjudication`() {
      val draftAdjudication =
        draftAdjudicationService.startNewAdjudication("A12345", "MDI", 2L, DATE_TIME_OF_INCIDENT)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)

      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(draftAdjudication)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")

      assertThat(draftAdjudication.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident", "handoverDeadline")
        .contains(2L, DATE_TIME_OF_INCIDENT, DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE)
    }
  }

  @Nested
  inner class DraftAdjudicationDetails {
    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

      assertThatThrownBy {
        draftAdjudicationService.getDraftAdjudicationDetails(1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

    @Test
    fun `returns the draft adjudication`() {
      val now = LocalDateTime.now()
      val draftAdjudication =
        DraftAdjudication(
          id = 1,
          prisonerNumber = "A12345",
          agencyId = "MDI",
          incidentDetails = IncidentDetails(
            locationId = 2,
            dateTimeOfIncident = now,
            handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
          )
        )
      draftAdjudication.createdByUserId = "A_USER" // Add audit information

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(draftAdjudication)
      )

      val draftAdjudicationDto = draftAdjudicationService.getDraftAdjudicationDetails(1)

      assertThat(draftAdjudicationDto)
        .extracting("id", "prisonerNumber", "startedByUserId")
        .contains(1L, "A12345", "A_USER")

      assertThat(draftAdjudicationDto.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident")
        .contains(2L, now)
    }
  }

  @Nested
  inner class AddIncidentStatement {
    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

      assertThatThrownBy {
        draftAdjudicationService.addIncidentStatement(1, "test", false)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

    @Test
    fun `adds an incident statement to a draft adjudication`() {
      val draftAdjudicationEntity = DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        agencyId = "MDI",
        incidentDetails = IncidentDetails(
          locationId = 1,
          dateTimeOfIncident = LocalDateTime.now(clock),
          handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
        ),
      )

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudicationEntity))

      whenever(draftAdjudicationRepository.save(any())).thenReturn(
        draftAdjudicationEntity.copy(
          incidentStatement = IncidentStatement(id = 1, statement = "test")
        )
      )

      val draftAdjudication = draftAdjudicationService.addIncidentStatement(1, "test", false)

      assertThat(draftAdjudication)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")

      assertThat(draftAdjudication.incidentStatement?.statement).isEqualTo("test")

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.incidentStatement?.statement).isEqualTo("test")
    }

    @Test
    fun `throws an IllegalStateException if a statement already exists`() {
      whenever(draftAdjudicationRepository.findById(any()))
        .thenReturn(
          Optional.of(
            DraftAdjudication(
              id = 1,
              prisonerNumber = "A12345",
              agencyId = "MDI",
              incidentDetails = IncidentDetails(
                locationId = 1,
                dateTimeOfIncident = LocalDateTime.now(clock),
                handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
              ),
              incidentStatement = IncidentStatement(id = 1, statement = "test")
            )
          )
        )

      assertThatThrownBy {
        draftAdjudicationService.addIncidentStatement(1, "test", false)
      }.isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("DraftAdjudication already includes the incident statement")
    }

    @Test
    fun `throws an IllegalArgumentException when statement and complete is null when adding an incident statement`() {
      assertThatThrownBy {
        draftAdjudicationService.addIncidentStatement(1, null, null)
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("Please supply either a statement or the completed value")
    }
  }

  @Nested
  inner class EditIncidentDetails {
    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

      assertThatThrownBy {
        draftAdjudicationService.editIncidentDetails(1, 2, DATE_TIME_OF_INCIDENT)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

    @Test
    fun `makes changes to the incident details`() {
      whenever(dateCalculationService.calculate48WorkingHoursFrom(any())).thenReturn(DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE)

      val dateTimeOfIncident = DATE_TIME_OF_INCIDENT.plusMonths(1)
      val draftAdjudicationEntity = DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        agencyId = "MDI",
        incidentDetails = IncidentDetails(
          id = 1,
          locationId = 2,
          dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
          handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
        )
      )

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudicationEntity))
      whenever(draftAdjudicationRepository.save(any())).thenReturn(
        draftAdjudicationEntity.copy(
          incidentDetails = IncidentDetails(
            id = 1,
            locationId = 3L,
            dateTimeOfIncident = dateTimeOfIncident,
            handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
          )
        )
      )

      val draftAdjudication = draftAdjudicationService.editIncidentDetails(1, 3, dateTimeOfIncident)

      assertThat(draftAdjudication)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")

      assertThat(draftAdjudication.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident", "handoverDeadline")
        .contains(3L, dateTimeOfIncident, DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident")
        .contains(3L, dateTimeOfIncident)
    }

    @Test
    fun `throws an IllegalArgumentException when statement and complete is null when editing an incident statement`() {
      assertThatThrownBy {
        draftAdjudicationService.editIncidentStatement(1, null, null)
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("Please supply either a statement or the completed value")
    }
  }

  @Nested
  inner class EditIncidentStatement {
    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

      assertThatThrownBy {
        draftAdjudicationService.editIncidentStatement(1, "new statement", false)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

    @Test
    fun `throws an entity not found if the incident statement does not exist`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          DraftAdjudication(
            id = 1,
            prisonerNumber = "A12345",
            agencyId = "MDI",
            incidentDetails = IncidentDetails(
              locationId = 1,
              dateTimeOfIncident = LocalDateTime.now(clock),
              handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
            ),
          )
        )
      )

      assertThatThrownBy {
        draftAdjudicationService.editIncidentStatement(1, "new statement", false)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication does not have any incident statement to update")
    }

    @Nested
    inner class WithValidExistingIncidentStatement {
      @BeforeEach
      fun beforeEach() {
        val draftAdjudicationEntity = DraftAdjudication(
          id = 1,
          prisonerNumber = "A12345",
          agencyId = "MDI",
          incidentDetails = IncidentDetails(
            locationId = 1,
            dateTimeOfIncident = LocalDateTime.now(clock),
            handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
          ),
          incidentStatement = IncidentStatement(statement = "old statement")
        )

        whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudicationEntity))
        whenever(draftAdjudicationRepository.save(any())).thenReturn(
          draftAdjudicationEntity.copy(incidentStatement = IncidentStatement(id = 1, statement = "new statement"))
        )
      }

      @Test
      fun `makes changes to the statement`() {
        val statementChanges = "new statement"
        val draftAdjudication = draftAdjudicationService.editIncidentStatement(1, statementChanges, false)

        assertThat(draftAdjudication)
          .extracting("id", "prisonerNumber")
          .contains(1L, "A12345")

        assertThat(draftAdjudication.incidentStatement?.statement).isEqualTo(statementChanges)

        val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
        verify(draftAdjudicationRepository).save(argumentCaptor.capture())

        assertThat(argumentCaptor.value.incidentStatement?.statement).isEqualTo(statementChanges)
      }
    }
  }

  @Nested
  inner class CompleteDraftAdjudication {
    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

      assertThatThrownBy {
        draftAdjudicationService.completeDraftAdjudication(1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

    @Test
    fun `throws an IllegalStateException when the draft adjudication is missing the incident statement`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          DraftAdjudication(
            prisonerNumber = "A12345",
            agencyId = "MDI",
            incidentDetails = IncidentDetails(
              locationId = 1,
              dateTimeOfIncident = LocalDateTime.now(),
              handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
            )
          )
        )
      )

      assertThatThrownBy {
        draftAdjudicationService.completeDraftAdjudication(1)
      }.isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("Please include an incident statement before completing this draft adjudication")
    }

    @Nested
    inner class WithAValidDraftAdjudication {
      private val INCIDENT_TIME = LocalDateTime.now(clock)

      @BeforeEach
      fun beforeEach() {
        whenever(draftAdjudicationRepository.findById(any())).thenReturn(
          Optional.of(
            DraftAdjudication(
              id = 1,
              prisonerNumber = "A12345",
              agencyId = "MDI",
              incidentDetails = IncidentDetails(
                locationId = 1,
                dateTimeOfIncident = INCIDENT_TIME,
                handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
              ),
              incidentStatement = IncidentStatement(statement = "test")
            )
          )
        )
        whenever(prisonApiGateway.publishAdjudication(any())).thenReturn(
          ReportedAdjudication(
            adjudicationNumber = 123456L,
            offenderNo = "A12345",
            bookingId = 1L,
            agencyId = "MDI",
            statement = "test",
            incidentLocationId = 2L,
            incidentTime = LocalDateTime.now(clock),
            reporterStaffId = 2,
            createdByUserId = "A_SMITH"
          )
        )
        whenever(dateCalculationService.calculate48WorkingHoursFrom(any())).thenReturn(DATE_TIME_REPORTED_ADJUDICATION_EXPIRES)
      }

      @Test
      fun `stores a new completed adjudication record`() {
        draftAdjudicationService.completeDraftAdjudication(1)

        val argumentCaptor = ArgumentCaptor.forClass(SubmittedAdjudicationHistory::class.java)
        verify(submittedAdjudicationHistoryRepository).save(argumentCaptor.capture())

        assertThat(argumentCaptor.value)
          .extracting("adjudicationNumber", "agencyId", "dateTimeOfIncident", "dateTimeSent")
          .contains(123456L, "MDI", INCIDENT_TIME, LocalDateTime.now(clock))
      }

      @Test
      fun `makes a call to prison api to publish the draft adjudication`() {
        draftAdjudicationService.completeDraftAdjudication(1)

        val expectedAdjudicationToPublish = AdjudicationDetailsToPublish(
          offenderNo = "A12345",
          agencyId = "MDI",
          incidentLocationId = 1L,
          incidentTime = LocalDateTime.now(clock),
          statement = "test"
        )

        verify(prisonApiGateway).publishAdjudication(expectedAdjudicationToPublish)
      }

      @Test
      fun `deletes the draft adjudication once complete`() {
        draftAdjudicationService.completeDraftAdjudication(1)

        val argumentCaptor: ArgumentCaptor<DraftAdjudication> = ArgumentCaptor.forClass(DraftAdjudication::class.java)
        verify(draftAdjudicationRepository).delete(argumentCaptor.capture())

        assertThat(argumentCaptor.value)
          .extracting("id", "prisonerNumber")
          .contains(1L, "A12345")
      }
    }

    @Nested
    inner class CompleteAPreviouslyCompletedAdjudication {
      @BeforeEach
      fun beforeEach() {
        whenever(draftAdjudicationRepository.findById(any())).thenReturn(
          Optional.of(
            DraftAdjudication(
              id = 1,
              prisonerNumber = "A12345",
              reportNumber = 123L,
              reportByUserId = "A_SMITH",
              agencyId = "MDI",
              incidentDetails = IncidentDetails(
                locationId = 1,
                dateTimeOfIncident = LocalDateTime.now(clock),
                handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
              ),
              incidentStatement = IncidentStatement(statement = "test")
            )
          )
        )
        whenever(submittedAdjudicationHistoryRepository.findByAdjudicationNumber(any())).thenReturn(
          SubmittedAdjudicationHistory(
            adjudicationNumber = 123L,
            agencyId = "MDI",
            dateTimeOfIncident = LocalDateTime.now(clock),
            dateTimeSent = LocalDateTime.now(clock)
          )
        )
        whenever(prisonApiGateway.updateAdjudication(any(), any())).thenReturn(
          ReportedAdjudication(
            adjudicationNumber = 123L,
            offenderNo = "A12345",
            bookingId = 1L,
            agencyId = "MDI",
            statement = "test",
            incidentLocationId = 2L,
            incidentTime = LocalDateTime.now(clock),
            reporterStaffId = 2,
            createdByUserId = "A_SMITH",
          )
        )
        whenever(dateCalculationService.calculate48WorkingHoursFrom(any())).thenReturn(DATE_TIME_REPORTED_ADJUDICATION_EXPIRES)
      }

      @Test
      fun `updates the completed adjudication record`() {
        draftAdjudicationService.completeDraftAdjudication(1)

        verify(submittedAdjudicationHistoryRepository).findByAdjudicationNumber(123L)

        val argumentCaptor = ArgumentCaptor.forClass(SubmittedAdjudicationHistory::class.java)
        verify(submittedAdjudicationHistoryRepository).save(argumentCaptor.capture())

        assertThat(argumentCaptor.value)
          .extracting("adjudicationNumber", "agencyId", "dateTimeSent")
          .contains(123L, "MDI", LocalDateTime.now(clock))
      }

      @Test
      fun `makes a call to prison api to update the draft adjudication`() {
        draftAdjudicationService.completeDraftAdjudication(1)

        val expectedAdjudicationToUpdate = AdjudicationDetailsToUpdate(
          incidentLocationId = 1L,
          incidentTime = LocalDateTime.now(clock),
          statement = "test"
        )

        verify(prisonApiGateway).updateAdjudication(123L, expectedAdjudicationToUpdate)
      }

      @Test
      fun `deletes the draft adjudication once complete`() {
        draftAdjudicationService.completeDraftAdjudication(1)

        val argumentCaptor: ArgumentCaptor<DraftAdjudication> = ArgumentCaptor.forClass(DraftAdjudication::class.java)
        verify(draftAdjudicationRepository).delete(argumentCaptor.capture())

        assertThat(argumentCaptor.value)
          .extracting("id", "prisonerNumber")
          .contains(1L, "A12345")
      }
    }
  }

  @Nested
  inner class InProgressDraftAdjudications {
    @BeforeEach
    fun beforeEach() {
      whenever(draftAdjudicationRepository.findUnsubmittedByAgencyIdAndCreatedByUserId(any(), any())).thenReturn(
        listOf(
          DraftAdjudication(
            id = 1,
            prisonerNumber = "A12345",
            agencyId = "MDI",
            incidentDetails = IncidentDetails(
              id = 2,
              locationId = 2,
              dateTimeOfIncident = LocalDateTime.now(clock).plusMonths(2),
              handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
            )
          ),
          DraftAdjudication(
            id = 2,
            prisonerNumber = "A12346",
            agencyId = "MDI",
            incidentDetails = IncidentDetails(
              id = 3,
              locationId = 3,
              dateTimeOfIncident = LocalDateTime.now(clock),
              handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
            )
          )
        )
      )
    }

    @Test
    fun `calls the repository method for all draft adjudications created by ITAG_USER`() {
      whenever(authenticationFacade.currentUsername).thenReturn("ITAG_USER")

      draftAdjudicationService.getCurrentUsersInProgressDraftAdjudications("MDI")

      verify(draftAdjudicationRepository).findUnsubmittedByAgencyIdAndCreatedByUserId("MDI", "ITAG_USER")
    }

    @Test
    fun `given no user return an empty set`() {
      whenever(authenticationFacade.currentUsername).thenReturn(null)
      val draftAdjudications = draftAdjudicationService.getCurrentUsersInProgressDraftAdjudications("MDI")

      assertThat(draftAdjudications).isEmpty()
    }

    @Test
    fun `sorts draft adjudications by incident date time`() {
      whenever(authenticationFacade.currentUsername).thenReturn("ITAG_USER")

      val adjudications = draftAdjudicationService.getCurrentUsersInProgressDraftAdjudications("MDI")

      assertThat(adjudications)
        .extracting("id", "prisonerNumber", "incidentDetails")
        .contains(
          Tuple(2L, "A12346", IncidentDetailsDto(3, LocalDateTime.now(clock), DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE)),
          Tuple(1L, "A12345", IncidentDetailsDto(2, LocalDateTime.now(clock).plusMonths(2), DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE))
        )
    }
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
    private val DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE = LocalDateTime.of(2010, 10, 14, 10, 0)
    private val DATE_TIME_REPORTED_ADJUDICATION_EXPIRES = LocalDateTime.of(2010, 10, 14, 10, 0)
  }
}
