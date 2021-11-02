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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.BaseEntity
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import java.time.LocalDateTime
import java.util.Optional
import javax.persistence.EntityNotFoundException

class DraftAdjudicationServiceTest {
  private val draftAdjudicationRepository: DraftAdjudicationRepository = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private lateinit var draftAdjudicationService: DraftAdjudicationService

  @BeforeEach
  fun beforeEach() {
    whenever(authenticationFacade.currentUsername).thenReturn("ITAG_USER")
    draftAdjudicationService =
      DraftAdjudicationService(draftAdjudicationRepository, authenticationFacade)
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

    @Test
    fun `throws an NotAuthorisedToUpdateStatementException`() {
      val incidentStatement = IncidentStatement(statement = "old statement") as BaseEntity
      incidentStatement.createdByUserId = "ANOTHER_USER"

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          DraftAdjudication(
            id = 1, prisonerNumber = "A12345", incidentStatement = incidentStatement as IncidentStatement
          )
        )
      )

      assertThatThrownBy {
        draftAdjudicationService.editIncidentStatement(1, "new statement")
      }.isInstanceOf(UnAuthorisedToEditIncidentStatementException::class.java)
        .hasMessageContaining("Only the original author can make changes to this incident statement")
    }
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
  }
}
