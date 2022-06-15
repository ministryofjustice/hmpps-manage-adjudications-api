package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.IncidentRoleRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.OffenceDetailsRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.NomisAdjudicationCreationRequest
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
  private val offenceCodeLookupService: OffenceCodeLookupService = mock()
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
        offenceCodeLookupService,
        dateCalculationService,
        authenticationFacade
      )
    whenever(offenceCodeLookupService.getParagraphCode(2)).thenReturn(OFFENCE_CODE_2_PARAGRAPH_CODE)
    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(2)).thenReturn(
      listOf(OFFENCE_CODE_2_NOMIS_CODE_ON_OWN)
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(2)).thenReturn(
      OFFENCE_CODE_2_NOMIS_CODE_ASSISTED
    )
    whenever(offenceCodeLookupService.getParagraphNumber(2)).thenReturn(OFFENCE_CODE_2_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(2)).thenReturn(OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION)
    whenever(offenceCodeLookupService.getParagraphCode(3)).thenReturn(OFFENCE_CODE_3_PARAGRAPH_CODE)
    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(3)).thenReturn(
      listOf(
        OFFENCE_CODE_3_NOMIS_CODE_ON_OWN
      )
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(3)).thenReturn(
      OFFENCE_CODE_3_NOMIS_CODE_ASSISTED
    )
    whenever(offenceCodeLookupService.getParagraphNumber(3)).thenReturn(OFFENCE_CODE_3_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(3)).thenReturn(OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION)
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
      whenever(dateCalculationService.calculate48WorkingHoursFrom(any())).thenReturn(
        DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
      )
    }

    @Test
    fun `makes a call to the repository to save the draft adjudication`() {
      val draftAdjudication =
        draftAdjudicationService.startNewAdjudication(
          "A12345",
          "MDI",
          2L,
          DATE_TIME_OF_INCIDENT,
          incidentRoleRequestWithAllValuesSet()
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
        .extracting("roleCode", "offenceRule", "associatedPrisonersNumber")
        .contains(
          incidentRoleDtoWithAllValuesSet().roleCode,
          IncidentRoleRuleLookup.getOffenceRuleDetails(incidentRoleDtoWithAllValuesSet().roleCode),
          incidentRoleDtoWithAllValuesSet().associatedPrisonersNumber
        )
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
          offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
          incidentStatement = IncidentStatement(
            statement = "Example statement",
            completed = false
          ),
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

      assertThat(draftAdjudicationDto.incidentRole)
        .extracting("roleCode", "offenceRule", "associatedPrisonersNumber")
        .contains(
          incidentRoleDtoWithAllValuesSet().roleCode,
          IncidentRoleRuleLookup.getOffenceRuleDetails(incidentRoleDtoWithAllValuesSet().roleCode),
          incidentRoleDtoWithAllValuesSet().associatedPrisonersNumber
        )

      assertThat(draftAdjudicationDto.offenceDetails).hasSize(2)
        .extracting(
          "offenceCode",
          "offenceRule.paragraphNumber",
          "offenceRule.paragraphDescription",
          "victimPrisonersNumber",
          "victimStaffUsername",
          "victimOtherPersonsName"
        )
        .contains(
          Tuple(
            BASIC_OFFENCE_DETAILS_RESPONSE_DTO.offenceCode,
            BASIC_OFFENCE_DETAILS_RESPONSE_DTO.offenceRule.paragraphNumber,
            BASIC_OFFENCE_DETAILS_RESPONSE_DTO.offenceRule.paragraphDescription,
            BASIC_OFFENCE_DETAILS_RESPONSE_DTO.victimPrisonersNumber,
            BASIC_OFFENCE_DETAILS_RESPONSE_DTO.victimStaffUsername,
            BASIC_OFFENCE_DETAILS_RESPONSE_DTO.victimOtherPersonsName
          ),
          Tuple(
            FULL_OFFENCE_DETAILS_RESPONSE_DTO.offenceCode,
            FULL_OFFENCE_DETAILS_RESPONSE_DTO.offenceRule.paragraphNumber,
            FULL_OFFENCE_DETAILS_RESPONSE_DTO.offenceRule.paragraphDescription,
            FULL_OFFENCE_DETAILS_RESPONSE_DTO.victimPrisonersNumber,
            FULL_OFFENCE_DETAILS_RESPONSE_DTO.victimStaffUsername,
            FULL_OFFENCE_DETAILS_RESPONSE_DTO.victimOtherPersonsName
          ),
        )

      assertThat(draftAdjudicationDto.incidentStatement)
        .extracting("statement", "completed")
        .contains("Example statement", false)
    }
  }

  @Nested
  inner class EditIncidentRole {
    private val draftAdjudication =
      DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        agencyId = "MDI",
        incidentDetails = IncidentDetails(
          locationId = 2,
          dateTimeOfIncident = LocalDateTime.now(),
          handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
        ),
        incidentRole = incidentRoleWithAllValuesSet(),
        offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
        incidentStatement = IncidentStatement(
          statement = "Example statement",
          completed = false
        ),
      )

    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

      assertThatThrownBy {
        draftAdjudicationService.editIncidentRole(1, IncidentRoleRequest("1", "1"), false)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `saves incident role`(deleteOffences: Boolean) {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudication))
      whenever(draftAdjudicationRepository.save(any())).thenReturn(draftAdjudication)

      draftAdjudicationService.editIncidentRole(1, IncidentRoleRequest("1", "2"), deleteOffences)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.offenceDetails!!.isEmpty()).isEqualTo(deleteOffences)
      assertThat(argumentCaptor.value.incidentRole.roleCode).isEqualTo("1")
      assertThat(argumentCaptor.value.incidentRole.associatedPrisonersNumber).isEqualTo("2")
    }
  }

  @Nested
  inner class SetOffenceDetails {
    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

      assertThatThrownBy {
        draftAdjudicationService.setOffenceDetails(1, listOf(BASIC_OFFENCE_DETAILS_REQUEST))
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

    @Test
    fun `adds the offence details to a draft adjudication`() {
      val offenceDetailsToAdd = listOf(BASIC_OFFENCE_DETAILS_REQUEST, FULL_OFFENCE_DETAILS_REQUEST)
      val offenceDetailsToSave = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY)
      val expectedOffenceDetailsResponse = listOf(BASIC_OFFENCE_DETAILS_RESPONSE_DTO, FULL_OFFENCE_DETAILS_RESPONSE_DTO)
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
          offenceDetails = offenceDetailsToSave
        )
      )

      val draftAdjudication = draftAdjudicationService.setOffenceDetails(1, offenceDetailsToAdd)

      assertThat(draftAdjudication)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")

      assertThat(draftAdjudication.offenceDetails).isEqualTo(expectedOffenceDetailsResponse)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.offenceDetails).isEqualTo(offenceDetailsToSave)
    }

    @Test
    fun `edits the offence details of an existing draft adjudication`() {
      val existingOffenceDetails = mutableListOf(Offence(offenceCode = 1, paragraphCode = "1"))
      val offenceDetailsToUse = listOf(BASIC_OFFENCE_DETAILS_REQUEST, FULL_OFFENCE_DETAILS_REQUEST)
      val offenceDetailsToSave = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY)
      val expectedOffenceDetailsResponse = listOf(BASIC_OFFENCE_DETAILS_RESPONSE_DTO, FULL_OFFENCE_DETAILS_RESPONSE_DTO)
      val existingDraftAdjudicationEntity = DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        agencyId = "MDI",
        incidentDetails = IncidentDetails(
          locationId = 1,
          dateTimeOfIncident = LocalDateTime.now(clock),
          handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
        ),
        incidentRole = incidentRoleWithNoValuesSet(),
        offenceDetails = existingOffenceDetails,
      )

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(existingDraftAdjudicationEntity))

      whenever(draftAdjudicationRepository.save(any())).thenReturn(
        existingDraftAdjudicationEntity.copy(
          offenceDetails = offenceDetailsToSave
        )
      )

      val draftAdjudication = draftAdjudicationService.setOffenceDetails(1, offenceDetailsToUse)

      assertThat(draftAdjudication)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")

      assertThat(draftAdjudication.offenceDetails).isEqualTo(expectedOffenceDetailsResponse)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.offenceDetails).isEqualTo(offenceDetailsToSave)
    }

    @Test
    fun `treats empty strings as null values`() {
      val offenceDetailsToAdd = listOf(
        OffenceDetailsRequestItem(
          offenceCode = 2,
          victimPrisonersNumber = "",
          victimStaffUsername = "",
          victimOtherPersonsName = "",
        )
      )
      val offenceDetailsToSave = mutableListOf(
        Offence(
          offenceCode = 2,
          paragraphCode = OFFENCE_CODE_2_PARAGRAPH_CODE,
        )
      )
      val expectedOffenceDetailsResponse = listOf(
        OffenceDetailsDto(
          offenceCode = 2,
          offenceRule = OffenceRuleDetailsDto(
            paragraphNumber = OFFENCE_CODE_2_PARAGRAPH_NUMBER,
            paragraphDescription = OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION,
          ),
        )
      )

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
          offenceDetails = offenceDetailsToSave
        )
      )

      val draftAdjudication = draftAdjudicationService.setOffenceDetails(1, offenceDetailsToAdd)

      assertThat(draftAdjudication)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")

      assertThat(draftAdjudication.offenceDetails).isEqualTo(expectedOffenceDetailsResponse)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.offenceDetails).isEqualTo(offenceDetailsToSave)
    }

    @Test
    fun `throws an IllegalArgumentException when no offence details are provided`() {
      assertThatThrownBy {
        draftAdjudicationService.setOffenceDetails(1, listOf())
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("Please supply at least one set of offence details")
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
          incidentRoleRequestWithNoValuesSet(),
          false,
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

    @Test
    fun `makes changes to the incident details`() {
      whenever(dateCalculationService.calculate48WorkingHoursFrom(any())).thenReturn(
        DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
      )

      val editedDateTimeOfIncident = DATE_TIME_OF_INCIDENT.plusMonths(1)
      val editedIncidentRole = incidentRoleWithNoValuesSet()
      val editedIncidentRoleDtoRequest = incidentRoleRequestWithNoValuesSet()
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
        editedIncidentRoleDtoRequest,
        false,
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

    @Test
    fun `throws an IllegalStateException when the draft adjudication is missing offence details`() {
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
            incidentStatement = IncidentStatement(
              statement = "A statement",
            )
          )
        )
      )

      assertThatThrownBy {
        draftAdjudicationService.completeDraftAdjudication(1)
      }.isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("Please supply at least one set of offence details")
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
              offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
              incidentStatement = IncidentStatement(statement = "test")
            )
          )
        )
        whenever(prisonApiGateway.requestAdjudicationCreationData(any())).thenReturn(
          NomisAdjudicationCreationRequest(
            adjudicationNumber = 123456L,
            bookingId = 1L,
          )
        )
        whenever(dateCalculationService.calculate48WorkingHoursFrom(any())).thenReturn(
          DATE_TIME_REPORTED_ADJUDICATION_EXPIRES
        )
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
          .extracting("prisonerNumber", "reportNumber", "bookingId", "agencyId")
          .contains("A12345", 123456L, 1L, "MDI")

        assertThat(reportedAdjudicationArgumentCaptor.value)
          .extracting(
            "locationId",
            "dateTimeOfIncident",
            "handoverDeadline",
            "incidentRoleCode",
            "incidentRoleAssociatedPrisonersNumber",
            "statement"
          )
          .contains(
            1L,
            INCIDENT_TIME,
            DATE_TIME_REPORTED_ADJUDICATION_EXPIRES,
            INCIDENT_ROLE_CODE,
            INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER,
            "test"
          )

        assertThat(reportedAdjudicationArgumentCaptor.value.offenceDetails)
          .extracting(
            "offenceCode",
            "paragraphCode",
            "victimPrisonersNumber",
            "victimStaffUsername",
            "victimOtherPersonsName"
          )
          .contains(
            Tuple(
              BASIC_OFFENCE_DETAILS_DB_ENTITY.offenceCode,
              BASIC_OFFENCE_DETAILS_DB_ENTITY.paragraphCode,
              BASIC_OFFENCE_DETAILS_DB_ENTITY.victimPrisonersNumber,
              BASIC_OFFENCE_DETAILS_DB_ENTITY.victimStaffUsername,
              BASIC_OFFENCE_DETAILS_DB_ENTITY.victimOtherPersonsName
            ),
            Tuple(
              FULL_OFFENCE_DETAILS_DB_ENTITY.offenceCode, FULL_OFFENCE_DETAILS_DB_ENTITY.paragraphCode,
              FULL_OFFENCE_DETAILS_DB_ENTITY.victimPrisonersNumber,
              FULL_OFFENCE_DETAILS_DB_ENTITY.victimStaffUsername, FULL_OFFENCE_DETAILS_DB_ENTITY.victimOtherPersonsName
            ),
          )
      }

      @Test
      fun `makes a call to prison api to get creation data`() {
        draftAdjudicationService.completeDraftAdjudication(1)

        verify(prisonApiGateway).requestAdjudicationCreationData("A12345")
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
    inner class CompleteAPreviouslyCompletedAdjudicationCheckStateChange {
      private val INCIDENT_TIME = LocalDateTime.now(clock)

      private fun reportedAdjudication(): ReportedAdjudication = ReportedAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        bookingId = 1,
        reportNumber = 123,
        agencyId = "MDI",
        locationId = 2,
        offenceDetails = mutableListOf(),
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
        dateTimeOfIncident = LocalDateTime.now(clock),
        handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
        incidentRoleAssociatedPrisonersNumber = null,
        incidentRoleCode = null,
        statement = "test"
      )

      @BeforeEach
      fun beforeEach() {
        whenever(draftAdjudicationRepository.findById(any())).thenReturn(
          Optional.of(
            DraftAdjudication(
              id = 1,
              prisonerNumber = "A12345",
              reportNumber = 123,
              agencyId = "MDI",
              incidentDetails = IncidentDetails(
                locationId = 2,
                dateTimeOfIncident = INCIDENT_TIME,
                handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
              ),
              incidentRole = incidentRoleWithNoValuesSet(),
              offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
              incidentStatement = IncidentStatement(statement = "test")
            )
          )
        )
        whenever(prisonApiGateway.requestAdjudicationCreationData(any())).thenReturn(
          NomisAdjudicationCreationRequest(
            adjudicationNumber = 123,
            bookingId = 33,
          )
        )
        whenever(reportedAdjudicationRepository.save(any())).thenAnswer {
          val passedInAdjudication = it.arguments[0] as ReportedAdjudication
          passedInAdjudication.createdByUserId = "A_SMITH"
          passedInAdjudication.createDateTime = REPORTED_DATE_TIME
          passedInAdjudication
        }
      }

      @ParameterizedTest
      @CsvSource(
        "ACCEPTED",
        "REJECTED"
      )
      fun `cannot complete when the reported adjudication is in the wrong state`(from: ReportedAdjudicationStatus) {
        whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
          reportedAdjudication().also {
            it.status = from
          }
        )
        Assertions.assertThrows(IllegalStateException::class.java) {
          draftAdjudicationService.completeDraftAdjudication(1)
        }
      }

      @ParameterizedTest
      @CsvSource(
        "AWAITING_REVIEW",
        "RETURNED"
      )
      fun `completes when the reported adjudication is in a correct state`(from: ReportedAdjudicationStatus) {
        whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
          reportedAdjudication().also {
            it.status = from
          }
        )
        draftAdjudicationService.completeDraftAdjudication(1)
        val reportedAdjudicationArgumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
        verify(reportedAdjudicationRepository).save(reportedAdjudicationArgumentCaptor.capture())
        assertThat(reportedAdjudicationArgumentCaptor.value).extracting("status")
          .isEqualTo(ReportedAdjudicationStatus.AWAITING_REVIEW)
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
              offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
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
            offenceDetails = mutableListOf(ReportedOffence(offenceCode = 3, paragraphCode = "4")),
            statement = "olddata",
            status = ReportedAdjudicationStatus.AWAITING_REVIEW,
            statusReason = null,
            statusDetails = null,
          )
        )
        whenever(prisonApiGateway.requestAdjudicationCreationData(any())).thenReturn(
          NomisAdjudicationCreationRequest(
            adjudicationNumber = 123,
            bookingId = 33,
          )
        )
        whenever(dateCalculationService.calculate48WorkingHoursFrom(any())).thenReturn(
          DATE_TIME_REPORTED_ADJUDICATION_EXPIRES
        )
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

        verify(reportedAdjudicationRepository, times(2)).findByReportNumber(123L)

        val reportedAdjudicationArgumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
        verify(reportedAdjudicationRepository).save(reportedAdjudicationArgumentCaptor.capture())

        assertThat(reportedAdjudicationArgumentCaptor.value)
          .extracting("prisonerNumber", "reportNumber", "bookingId", "agencyId")
          .contains("A12345", 123L, 33L, "MDI")

        assertThat(reportedAdjudicationArgumentCaptor.value)
          .extracting(
            "locationId",
            "dateTimeOfIncident",
            "handoverDeadline",
            "incidentRoleCode",
            "incidentRoleAssociatedPrisonersNumber",
            "statement"
          )
          .contains(
            1L,
            LocalDateTime.now(clock),
            DATE_TIME_REPORTED_ADJUDICATION_EXPIRES,
            INCIDENT_ROLE_CODE,
            INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER,
            "test"
          )

        assertThat(reportedAdjudicationArgumentCaptor.value.offenceDetails)
          .extracting(
            "offenceCode",
            "paragraphCode",
            "victimPrisonersNumber",
            "victimStaffUsername",
            "victimOtherPersonsName"
          )
          .contains(
            Tuple(
              BASIC_OFFENCE_DETAILS_DB_ENTITY.offenceCode,
              BASIC_OFFENCE_DETAILS_DB_ENTITY.paragraphCode,
              BASIC_OFFENCE_DETAILS_DB_ENTITY.victimPrisonersNumber,
              BASIC_OFFENCE_DETAILS_DB_ENTITY.victimStaffUsername,
              BASIC_OFFENCE_DETAILS_DB_ENTITY.victimOtherPersonsName
            ),
            Tuple(
              FULL_OFFENCE_DETAILS_DB_ENTITY.offenceCode, FULL_OFFENCE_DETAILS_DB_ENTITY.paragraphCode,
              FULL_OFFENCE_DETAILS_DB_ENTITY.victimPrisonersNumber,
              FULL_OFFENCE_DETAILS_DB_ENTITY.victimStaffUsername, FULL_OFFENCE_DETAILS_DB_ENTITY.victimOtherPersonsName
            ),
          )
      }

      @Test
      fun `does not call prison api to get creation data`() {
        draftAdjudicationService.completeDraftAdjudication(1)

        verify(prisonApiGateway, never()).requestAdjudicationCreationData(any())
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
      whenever(
        draftAdjudicationRepository.findDraftAdjudicationByAgencyIdAndCreatedByUserIdAndReportNumberIsNull(
          any(),
          any()
        )
      ).thenReturn(
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
            offenceDetails = mutableListOf(FULL_OFFENCE_DETAILS_DB_ENTITY),
            incidentStatement = IncidentStatement(
              statement = "Example statement",
              completed = false
            ),
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

      verify(draftAdjudicationRepository).findDraftAdjudicationByAgencyIdAndCreatedByUserIdAndReportNumberIsNull(
        "MDI",
        "ITAG_USER"
      )
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

      assertThat(adjudications)
        .extracting("offenceDetails")
        .contains(
          listOf(FULL_OFFENCE_DETAILS_RESPONSE_DTO),
          null as List<OffenceDetailsDto>?
        )

      assertThat(adjudications)
        .extracting("incidentStatement")
        .contains(
          IncidentStatementDto("Example statement", false),
          null as IncidentStatementDto?
        )
    }
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
    private val DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE = LocalDateTime.of(2010, 10, 14, 10, 0)
    private val DATE_TIME_REPORTED_ADJUDICATION_EXPIRES = LocalDateTime.of(2010, 10, 14, 10, 0)
    private val REPORTED_DATE_TIME = DATE_TIME_OF_INCIDENT.plusDays(1)

    private val INCIDENT_ROLE_CODE = "25a"
    private val INCIDENT_ROLE_PARAGRAPH_NUMBER = "25(a)"
    private val INCIDENT_ROLE_PARAGRAPH_DESCRIPTION = "Attempts to commit any of the foregoing offences:"
    private val INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER = "B23456"

    private val OFFENCE_CODE_2_PARAGRAPH_CODE = "5b"
    private val OFFENCE_CODE_2_NOMIS_CODE_ON_OWN = "5b"
    private val OFFENCE_CODE_2_NOMIS_CODE_ASSISTED = "25z"
    private val OFFENCE_CODE_2_PARAGRAPH_NUMBER = "5(b)"
    private val OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION = "A paragraph description"
    private val OFFENCE_CODE_3_PARAGRAPH_CODE = "6a"
    private val OFFENCE_CODE_3_NOMIS_CODE_ON_OWN = "5f"
    private val OFFENCE_CODE_3_NOMIS_CODE_ASSISTED = "25f"
    private val OFFENCE_CODE_3_PARAGRAPH_NUMBER = "6(a)"
    private val OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION = "Another paragraph description"

    private val BASIC_OFFENCE_DETAILS_REQUEST = OffenceDetailsRequestItem(offenceCode = 2)
    private val BASIC_OFFENCE_DETAILS_RESPONSE_DTO = OffenceDetailsDto(
      offenceCode = BASIC_OFFENCE_DETAILS_REQUEST.offenceCode,
      offenceRule = OffenceRuleDetailsDto(
        paragraphNumber = OFFENCE_CODE_2_PARAGRAPH_NUMBER,
        paragraphDescription = OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION,
      )
    )
    private val BASIC_OFFENCE_DETAILS_DB_ENTITY = Offence(
      offenceCode = BASIC_OFFENCE_DETAILS_RESPONSE_DTO.offenceCode,
      paragraphCode = OFFENCE_CODE_2_PARAGRAPH_CODE
    )

    private val FULL_OFFENCE_DETAILS_REQUEST = OffenceDetailsRequestItem(
      offenceCode = 3,
      victimPrisonersNumber = "A1234AA",
      victimStaffUsername = "ABC12D",
      victimOtherPersonsName = "A name",
    )
    private val FULL_OFFENCE_DETAILS_RESPONSE_DTO = OffenceDetailsDto(
      offenceCode = FULL_OFFENCE_DETAILS_REQUEST.offenceCode,
      offenceRule = OffenceRuleDetailsDto(
        paragraphNumber = OFFENCE_CODE_3_PARAGRAPH_NUMBER,
        paragraphDescription = OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION,
      ),
      victimPrisonersNumber = FULL_OFFENCE_DETAILS_REQUEST.victimPrisonersNumber,
      victimStaffUsername = FULL_OFFENCE_DETAILS_REQUEST.victimStaffUsername,
      victimOtherPersonsName = FULL_OFFENCE_DETAILS_REQUEST.victimOtherPersonsName,
    )
    private val FULL_OFFENCE_DETAILS_DB_ENTITY = Offence(
      offenceCode = FULL_OFFENCE_DETAILS_RESPONSE_DTO.offenceCode,
      paragraphCode = OFFENCE_CODE_3_PARAGRAPH_CODE,
      victimPrisonersNumber = FULL_OFFENCE_DETAILS_RESPONSE_DTO.victimPrisonersNumber,
      victimStaffUsername = FULL_OFFENCE_DETAILS_RESPONSE_DTO.victimStaffUsername,
      victimOtherPersonsName = FULL_OFFENCE_DETAILS_RESPONSE_DTO.victimOtherPersonsName,
    )

    fun incidentRoleRequestWithAllValuesSet(): IncidentRoleRequest =
      IncidentRoleRequest(
        INCIDENT_ROLE_CODE,
        INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER
      )

    fun incidentRoleDtoWithAllValuesSet(): IncidentRoleDto =
      IncidentRoleDto(
        INCIDENT_ROLE_CODE,
        OffenceRuleDetailsDto(
          INCIDENT_ROLE_PARAGRAPH_NUMBER,
          INCIDENT_ROLE_PARAGRAPH_DESCRIPTION,
        ),
        INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER
      )

    fun incidentRoleRequestWithNoValuesSet(): IncidentRoleRequest =
      IncidentRoleRequest(null, null)

    fun incidentRoleDtoWithNoValuesSet(): IncidentRoleDto =
      IncidentRoleDto(null, null, null)

    fun incidentRoleWithAllValuesSet(): IncidentRole =
      IncidentRole(null, INCIDENT_ROLE_CODE, INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER)

    fun incidentRoleWithNoValuesSet(): IncidentRole =
      IncidentRole(null, null, null)
  }
}
