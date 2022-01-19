package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToPublish
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToUpdate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.NomisAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import java.time.Clock
import java.time.Instant.ofEpochMilli
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional
import javax.persistence.EntityNotFoundException

class DraftAdjudicationServiceTest {
  private val draftAdjudicationRepository: DraftAdjudicationRepository = mock()
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository = mock()
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
        reportedAdjudicationRepository,
        prisonApiGateway,
        dateCalculationService,
        authenticationFacade
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
          ),
          incidentRole = incidentRoleWithAllValuesSet()
        )
      )
      whenever(dateCalculationService.calculate48WorkingHoursFrom(any())).thenReturn(DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE)
    }

    @Test
    fun `makes a call to the repository to save the draft adjudication`() {
      val draftAdjudication =
        draftAdjudicationService.startNewAdjudication(
          "A12345",
          "MDI",
          2L,
          DATE_TIME_OF_INCIDENT,
          incidentRoleDtoWithAllValuesSet()
        )

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)

      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(draftAdjudication)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")

      assertThat(draftAdjudication.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident", "handoverDeadline")
        .contains(2L, DATE_TIME_OF_INCIDENT, DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE)

      assertThat(draftAdjudication.incidentRole)
        .extracting("roleCode", "associatedPrisonersNumber")
        .contains(incidentRoleDtoWithAllValuesSet().roleCode, incidentRoleDtoWithAllValuesSet().associatedPrisonersNumber)
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
          ),
          incidentRole = incidentRoleWithAllValuesSet(),
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

      assertThat(draftAdjudication.incidentRole)
        .extracting("roleCode", "associatedPrisonersNumber")
        .contains(incidentRoleDtoWithAllValuesSet().roleCode, incidentRoleDtoWithAllValuesSet().associatedPrisonersNumber)
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
        incidentRole = incidentRoleWithNoValuesSet(),
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
              incidentRole = incidentRoleWithNoValuesSet(),
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
        draftAdjudicationService.editIncidentDetails(
          1,
          2,
          DATE_TIME_OF_INCIDENT,
          incidentRoleDtoWithNoValuesSet(),
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

    @Test
    fun `makes changes to the incident details`() {
      whenever(dateCalculationService.calculate48WorkingHoursFrom(any())).thenReturn(DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE)

      val editedDateTimeOfIncident = DATE_TIME_OF_INCIDENT.plusMonths(1)
      val editedIncidentRole = incidentRoleWithNoValuesSet()
      val editedIncidentRoleDtoRequest = incidentRoleDtoWithNoValuesSet()
      val draftAdjudicationEntity = DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        agencyId = "MDI",
        incidentDetails = IncidentDetails(
          id = 1,
          locationId = 2,
          dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
          handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
        ),
        incidentRole = incidentRoleWithAllValuesSet(),
      )

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudicationEntity))
      whenever(draftAdjudicationRepository.save(any())).thenReturn(
        draftAdjudicationEntity.copy(
          incidentDetails = IncidentDetails(
            id = 1,
            locationId = 3L,
            dateTimeOfIncident = editedDateTimeOfIncident,
            handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
          ),
          incidentRole = editedIncidentRole
        )
      )

      val draftAdjudication = draftAdjudicationService.editIncidentDetails(
        1,
        3,
        editedDateTimeOfIncident,
        editedIncidentRoleDtoRequest
      )

      assertThat(draftAdjudication)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")

      assertThat(draftAdjudication.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident", "handoverDeadline")
        .contains(3L, editedDateTimeOfIncident, DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident")
        .contains(3L, editedDateTimeOfIncident)

      assertThat(argumentCaptor.value.incidentRole)
        .extracting("roleCode", "associatedPrisonersNumber")
        .contains(editedIncidentRole.roleCode, editedIncidentRole.associatedPrisonersNumber)
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
            incidentRole = incidentRoleWithNoValuesSet(),
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
          incidentRole = incidentRoleWithNoValuesSet(),
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
            ),
            incidentRole = incidentRoleWithAllValuesSet(),
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
              incidentRole = incidentRoleWithAllValuesSet(),
              incidentStatement = IncidentStatement(statement = "test")
            )
          )
        )
        whenever(prisonApiGateway.publishAdjudication(any())).thenReturn(
          NomisAdjudication(
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
        whenever(reportedAdjudicationRepository.save(any())).thenAnswer {
          val passedInAdjudication = it.arguments[0] as ReportedAdjudication
          passedInAdjudication.createdByUserId = "A_SMITH"
          passedInAdjudication.createDateTime = REPORTED_DATE_TIME
          passedInAdjudication
        }
      }

      @Test
      fun `stores a new completed adjudication record`() {
        draftAdjudicationService.completeDraftAdjudication(1)

        val reportedAdjudicationArgumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
        verify(reportedAdjudicationRepository).save(reportedAdjudicationArgumentCaptor.capture())

        assertThat(reportedAdjudicationArgumentCaptor.value)
          .extracting("prisonerNumber", "reportNumber", "bookingId", "agencyId", "locationId", "dateTimeOfIncident", "handoverDeadline", "incidentRoleCode", "incidentRoleAssociatedPrisonersNumber", "statement")
          .contains("A12345", 123456L, 1L, "MDI", 1L, INCIDENT_TIME, DATE_TIME_REPORTED_ADJUDICATION_EXPIRES, INCIDENT_ROLE_CODE, INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER, "test")
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
              incidentRole = incidentRoleWithAllValuesSet(),
              incidentStatement = IncidentStatement(statement = "test")
            )
          )
        )
        whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
          ReportedAdjudication(
            id = 1,
            prisonerNumber = "A12345",
            bookingId = 33L,
            reportNumber = 123L,
            agencyId = "MDI",
            locationId = 2,
            dateTimeOfIncident = LocalDateTime.now(clock).minusDays(2),
            handoverDeadline = LocalDateTime.now(clock),
            incidentRoleCode = null,
            incidentRoleAssociatedPrisonersNumber = null,
            statement = "olddata",
          )
        )
        whenever(prisonApiGateway.updateAdjudication(any(), any())).thenReturn(
          NomisAdjudication(
            adjudicationNumber = 123L,
            offenderNo = "A12345",
            bookingId = 33L,
            agencyId = "MDI",
            statement = "olddata",
            incidentLocationId = 2L,
            incidentTime = LocalDateTime.now(clock).minusDays(2),
            reporterStaffId = 2,
            createdByUserId = "A_SMITH",
          )
        )
        whenever(dateCalculationService.calculate48WorkingHoursFrom(any())).thenReturn(DATE_TIME_REPORTED_ADJUDICATION_EXPIRES)
        whenever(reportedAdjudicationRepository.save(any())).thenAnswer {
          val passedInAdjudication = it.arguments[0] as ReportedAdjudication
          passedInAdjudication.createdByUserId = "A_SMITH"
          passedInAdjudication.createDateTime = REPORTED_DATE_TIME
          passedInAdjudication
        }
      }

      @Test
      fun `throws an entity not found exception if the reported adjudication for the supplied id does not exists`() {
        whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(null)

        assertThatThrownBy {
          draftAdjudicationService.completeDraftAdjudication(1)
        }.isInstanceOf(EntityNotFoundException::class.java)
          .hasMessageContaining("ReportedAdjudication not found for 1")
      }

      @Test
      fun `updates the completed adjudication record`() {
        draftAdjudicationService.completeDraftAdjudication(1)

        verify(reportedAdjudicationRepository).findByReportNumber(123L)

        val reportedAdjudicationArgumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
        verify(reportedAdjudicationRepository).save(reportedAdjudicationArgumentCaptor.capture())

        assertThat(reportedAdjudicationArgumentCaptor.value)
          .extracting("prisonerNumber", "reportNumber", "bookingId", "agencyId", "locationId", "dateTimeOfIncident", "handoverDeadline", "incidentRoleCode", "incidentRoleAssociatedPrisonersNumber", "statement")
          .contains("A12345", 123L, 1L, "MDI", 1L, LocalDateTime.now(clock), DATE_TIME_REPORTED_ADJUDICATION_EXPIRES, INCIDENT_ROLE_CODE, INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER, "test")
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
            ),
            incidentRole = incidentRoleWithAllValuesSet(),
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
            ),
            incidentRole = incidentRoleWithNoValuesSet(),
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
        .extracting("id", "prisonerNumber")
        .contains(
          Tuple(2L, "A12346"),
          Tuple(1L, "A12345")
        )

      assertThat(adjudications)
        .extracting("incidentDetails")
        .contains(
          IncidentDetailsDto(3, LocalDateTime.now(clock), DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE),
          IncidentDetailsDto(2, LocalDateTime.now(clock).plusMonths(2), DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE)
        )

      assertThat(adjudications)
        .extracting("incidentRole")
        .contains(
          incidentRoleDtoWithAllValuesSet(),
          incidentRoleDtoWithNoValuesSet(),
        )
    }
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
    private val DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE = LocalDateTime.of(2010, 10, 14, 10, 0)
    private val DATE_TIME_REPORTED_ADJUDICATION_EXPIRES = LocalDateTime.of(2010, 10, 14, 10, 0)
    private val REPORTED_DATE_TIME = DATE_TIME_OF_INCIDENT.plusDays(1)
    private val INCIDENT_ROLE_CODE = "25a"
    private val INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER = "B23456"

    fun incidentRoleDtoWithAllValuesSet(): IncidentRoleDto =
      IncidentRoleDto(INCIDENT_ROLE_CODE, INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER)

    fun incidentRoleDtoWithNoValuesSet(): IncidentRoleDto =
      IncidentRoleDto(null, null)

    fun incidentRoleWithAllValuesSet(): IncidentRole =
      IncidentRole(null, INCIDENT_ROLE_CODE, INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER)

    fun incidentRoleWithNoValuesSet(): IncidentRole =
      IncidentRole(null, null, null)
  }
}
