package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.SubmittedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToPublish
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.SubmittedAdjudicationRepository
import java.time.Clock
import java.time.Instant.ofEpochMilli
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional
import javax.persistence.EntityNotFoundException

class DraftAdjudicationServiceTest {
  private val draftAdjudicationRepository: DraftAdjudicationRepository = mock()
  private val submittedAdjudicationRepository: SubmittedAdjudicationRepository = mock()
  private val prisonApiGateway: PrisonApiGateway = mock()
  private val clock: Clock = Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault())

  private lateinit var draftAdjudicationService: DraftAdjudicationService

  @BeforeEach
  fun beforeEach() {
    draftAdjudicationService =
      DraftAdjudicationService(draftAdjudicationRepository, submittedAdjudicationRepository, prisonApiGateway, clock)
  }

  @Nested
  inner class StartDraftAdjudications {
    @BeforeEach
    fun beforeEach() {
      whenever(draftAdjudicationRepository.save(any())).thenReturn(
        DraftAdjudication(
          id = 1,
          prisonerNumber = "A12345",
          incidentDetails = IncidentDetails(locationId = 2, dateTimeOfIncident = DATE_TIME_OF_INCIDENT)
        )
      )
    }

    @Test
    fun `makes a call to the repository to save the draft adjudication`() {
      val draftAdjudication =
        draftAdjudicationService.startNewAdjudication("A12345", 2L, DATE_TIME_OF_INCIDENT)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)

      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(draftAdjudication)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")

      assertThat(draftAdjudication.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident")
        .contains(2L, DATE_TIME_OF_INCIDENT)
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
          incidentDetails = IncidentDetails(locationId = 2, dateTimeOfIncident = now)
        )

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(draftAdjudication)
      )

      val draftAdjudicationDto = draftAdjudicationService.getDraftAdjudicationDetails(1)

      assertThat(draftAdjudicationDto)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")

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
        draftAdjudicationService.addIncidentStatement(1, "test")
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

    @Test
    fun `adds an incident statement to a draft adjudication`() {
      val draftAdjudicationEntity = DraftAdjudication(id = 1, prisonerNumber = "A12345")

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudicationEntity))

      whenever(draftAdjudicationRepository.save(any())).thenReturn(
        draftAdjudicationEntity.copy(
          incidentStatement = IncidentStatement(id = 1, statement = "test")
        )
      )

      val draftAdjudication = draftAdjudicationService.addIncidentStatement(1, "test")

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
              incidentStatement = IncidentStatement(id = 1, statement = "test")
            )
          )
        )

      assertThatThrownBy {
        draftAdjudicationService.addIncidentStatement(1, "test")
      }.isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("DraftAdjudication already includes the incident statement")
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
    fun `throws an entity not found if the incident details does not exist`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          DraftAdjudication(
            id = 1,
            prisonerNumber = "A12345"
          )
        )
      )

      assertThatThrownBy {
        draftAdjudicationService.editIncidentDetails(1, 2, DATE_TIME_OF_INCIDENT)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication does not include an incident statement")
    }

    @Test
    fun `makes changes to the incident details`() {
      val dateTimeOfIncident = DATE_TIME_OF_INCIDENT.plusMonths(1)
      val draftAdjudicationEntity = DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        incidentDetails = IncidentDetails(id = 1, locationId = 2, dateTimeOfIncident = DATE_TIME_OF_INCIDENT)
      )

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudicationEntity))
      whenever(draftAdjudicationRepository.save(any())).thenReturn(
        draftAdjudicationEntity.copy(
          incidentDetails = IncidentDetails(
            id = 1,
            locationId = 3L,
            dateTimeOfIncident = dateTimeOfIncident
          )
        )
      )

      val draftAdjudication = draftAdjudicationService.editIncidentDetails(1, 3, dateTimeOfIncident)

      assertThat(draftAdjudication)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")

      assertThat(draftAdjudication.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident")
        .contains(3L, dateTimeOfIncident)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident")
        .contains(3L, dateTimeOfIncident)
    }
  }

  @Nested
  inner class EditIncidentStatement {
    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

      assertThatThrownBy {
        draftAdjudicationService.editIncidentStatement(1, "new statement")
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

    @Test
    fun `throws an entity not found if the incident statement does not exist`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          DraftAdjudication(
            id = 1,
            prisonerNumber = "A12345"
          )
        )
      )

      assertThatThrownBy {
        draftAdjudicationService.editIncidentStatement(1, "new statement")
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication does not have any incident statement to update")
    }

    @Test
    fun `makes changes to the statement`() {
      val statementChanges = "new statement"
      val draftAdjudicationEntity = DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        incidentStatement = IncidentStatement(statement = "old statement")
      )
      draftAdjudicationEntity.incidentStatement?.createdByUserId = "ITAG_USER"

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudicationEntity))
      whenever(draftAdjudicationRepository.save(any())).thenReturn(
        draftAdjudicationEntity.copy(incidentStatement = IncidentStatement(id = 1, statement = statementChanges))
      )

      val draftAdjudication = draftAdjudicationService.editIncidentStatement(1, statementChanges)

      assertThat(draftAdjudication)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")

      assertThat(draftAdjudication.incidentStatement?.statement).isEqualTo(statementChanges)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.incidentStatement?.statement).isEqualTo(statementChanges)
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
            incidentDetails = IncidentDetails(locationId = 1, dateTimeOfIncident = LocalDateTime.now())
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
      @BeforeEach
      fun beforeEach() {
        whenever(draftAdjudicationRepository.findById(any())).thenReturn(
          Optional.of(
            DraftAdjudication(
              id = 1,
              prisonerNumber = "A12345",
              incidentDetails = IncidentDetails(locationId = 1, dateTimeOfIncident = LocalDateTime.now(clock)),
              incidentStatement = IncidentStatement(statement = "test")
            )
          )
        )
        whenever(prisonApiGateway.publishAdjudication(any())).thenReturn(
          ReportedAdjudication(
            bookingId = 1L,
            adjudicationNumber = 123456L,
            statement = "test",
            incidentLocationId = 2L,
            incidentTime = LocalDateTime.now(clock),
            reporterStaffId = 2
          )
        )
      }

      @Test
      fun `store a new completed adjudication record`() {
        draftAdjudicationService.completeDraftAdjudication(1)

        val argumentCaptor = ArgumentCaptor.forClass(SubmittedAdjudication::class.java)
        verify(submittedAdjudicationRepository).save(argumentCaptor.capture())

        assertThat(argumentCaptor.value)
          .extracting("adjudicationNumber", "dateTimeSent")
          .contains(123456L, LocalDateTime.now(clock))
      }

      @Test
      fun `makes a call to prison api to publish the draft adjudication`() {
        draftAdjudicationService.completeDraftAdjudication(1)

        val expectedAdjudicationToPublish = AdjudicationDetailsToPublish(
          prisonerNumber = "A12345",
          incidentLocationId = 1L,
          incidentTime = LocalDateTime.now(clock),
          statement = "test"
        )

        verify(prisonApiGateway).publishAdjudication(expectedAdjudicationToPublish)
      }

      @Test
      fun `delete the draft adjudication once complete`() {
        draftAdjudicationService.completeDraftAdjudication(1)

        val argumentCaptor: ArgumentCaptor<DraftAdjudication> = ArgumentCaptor.forClass(DraftAdjudication::class.java)
        verify(draftAdjudicationRepository).delete(argumentCaptor.capture())

        assertThat(argumentCaptor.value)
          .extracting("id", "prisonerNumber")
          .contains(1L, "A12345")
      }
    }
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
  }
}
