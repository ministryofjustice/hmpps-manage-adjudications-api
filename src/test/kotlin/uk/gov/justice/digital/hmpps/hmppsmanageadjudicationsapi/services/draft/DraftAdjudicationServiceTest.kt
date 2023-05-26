package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.IncidentRoleAssociatedPrisonerRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.IncidentRoleRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.ForbiddenException
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.IncidentRoleRuleLookup
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import java.util.function.Supplier

class DraftAdjudicationServiceTest : DraftAdjudicationTestBase() {

  private val draftAdjudicationService =
    DraftAdjudicationService(
      draftAdjudicationRepository,
      offenceCodeLookupService,
      authenticationFacade,
    )

  @Nested
  inner class StartDraftAdjudications {
    val draftAdjudication = DraftAdjudication(
      id = 1,
      prisonerNumber = "A12345",
      gender = Gender.MALE,
      agencyId = "MDI",
      incidentDetails = incidentDetails(2L, DATE_TIME_OF_INCIDENT),
    )

    @Test
    fun `throws exception if date of discovery before incident date`() {
      assertThatThrownBy {
        draftAdjudicationService.startNewAdjudication(
          "A12345",
          Gender.MALE,
          "MDI",
          2L,
          DATE_TIME_OF_INCIDENT,
          DATE_TIME_OF_INCIDENT.minusDays(1),
        )
      }.isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("Date of discovery is before incident date")
    }

    @Test
    fun `makes a call to the repository to save the draft adjudication`() {
      whenever(draftAdjudicationRepository.save(any())).thenReturn(draftAdjudication)

      val draftAdjudication =
        draftAdjudicationService.startNewAdjudication(
          "A12345",
          Gender.MALE,
          "MDI",
          2L,
          DATE_TIME_OF_INCIDENT,
          DATE_TIME_OF_INCIDENT.plusDays(1),
        )

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)

      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(draftAdjudication)
        .extracting("id", "prisonerNumber", "gender")
        .contains(1L, "A12345", Gender.MALE)

      assertThat(draftAdjudication.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident", "dateTimeOfDiscovery", "handoverDeadline")
        .contains(2L, DATE_TIME_OF_INCIDENT, DATE_TIME_OF_INCIDENT.plusDays(1), DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE)

      assertThat(draftAdjudication.incidentRole).isNull()
      assertThat(draftAdjudication.isYouthOffender).isNull()
    }
  }

  @Nested
  inner class DraftAdjudicationDetails {

    @Test
    fun `adjudication is not part of active case load throws exception `() {
      val draftAdjudication =
        DraftAdjudication(
          id = 1,
          prisonerNumber = "A12345",
          gender = Gender.MALE,
          agencyId = "MDI",
          incidentDetails = incidentDetails(2L, now),
          incidentRole = incidentRoleWithAllValuesSet(),
          incidentStatement = IncidentStatement(
            statement = "Example statement",
            completed = false,
          ),
          isYouthOffender = true,
        )

      whenever(authenticationFacade.activeCaseload).thenReturn("OTHER")
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudication))

      assertThatThrownBy {
        draftAdjudicationService.getDraftAdjudicationDetails(1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

    @Test
    fun `returns the draft adjudication`() {
      testDto { draftAdjudicationService.getDraftAdjudicationDetails(1) }
    }

    @ParameterizedTest
    @CsvSource(
      "true",
      "false",
    )
    fun `returns the draft adjudication`(
      isYouthOffender: Boolean,
    ) {
      var offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY)
      if (isYouthOffender) {
        offenceDetails = mutableListOf(YOUTH_OFFENCE_DETAILS_DB_ENTITY)
      }

      val draftAdjudication =
        DraftAdjudication(
          id = 1,
          prisonerNumber = "A12345",
          gender = Gender.MALE,
          agencyId = "MDI",
          incidentDetails = incidentDetails(2L, now),
          incidentRole = incidentRoleWithAllValuesSet(),
          offenceDetails = offenceDetails,
          incidentStatement = IncidentStatement(
            statement = "Example statement",
            completed = false,
          ),
          isYouthOffender = isYouthOffender,
        )
      draftAdjudication.createdByUserId = "A_USER" // Add audit information

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(draftAdjudication),
      )

      val draftAdjudicationDto = draftAdjudicationService.getDraftAdjudicationDetails(1)

      assertThat(draftAdjudicationDto)
        .extracting("id", "prisonerNumber", "startedByUserId")
        .contains(1L, "A12345", "A_USER")

      assertThat(draftAdjudicationDto.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident")
        .contains(2L, now)

      assertThat(draftAdjudicationDto.incidentRole)
        .extracting("roleCode", "offenceRule", "associatedPrisonersNumber", "associatedPrisonersName")
        .contains(
          incidentRoleDtoWithAllValuesSet().roleCode,
          IncidentRoleRuleLookup.getOffenceRuleDetails(
            incidentRoleDtoWithAllValuesSet().roleCode,
            draftAdjudication.isYouthOffender!!,
          ),
          incidentRoleDtoWithAllValuesSet().associatedPrisonersNumber,
          incidentRoleDtoWithAllValuesSet().associatedPrisonersName,
        )

      if (isYouthOffender) {
        assertThat(draftAdjudicationDto.offenceDetails)
          .extracting(
            "offenceCode",
            "offenceRule.paragraphNumber",
            "offenceRule.paragraphDescription",
            "victimPrisonersNumber",
            "victimStaffUsername",
            "victimOtherPersonsName",
          )
          .contains(
            YOUTH_OFFENCE_DETAILS_RESPONSE_DTO.offenceCode,
            YOUTH_OFFENCE_DETAILS_RESPONSE_DTO.offenceRule.paragraphNumber,
            YOUTH_OFFENCE_DETAILS_RESPONSE_DTO.offenceRule.paragraphDescription,
            YOUTH_OFFENCE_DETAILS_RESPONSE_DTO.victimPrisonersNumber,
            YOUTH_OFFENCE_DETAILS_RESPONSE_DTO.victimStaffUsername,
            YOUTH_OFFENCE_DETAILS_RESPONSE_DTO.victimOtherPersonsName,
          )
      } else {
        assertThat(draftAdjudicationDto.offenceDetails)
          .extracting(
            "offenceCode",
            "offenceRule.paragraphNumber",
            "offenceRule.paragraphDescription",
            "victimPrisonersNumber",
            "victimStaffUsername",
            "victimOtherPersonsName",
          )
          .contains(
            BASIC_OFFENCE_DETAILS_RESPONSE_DTO.offenceCode,
            BASIC_OFFENCE_DETAILS_RESPONSE_DTO.offenceRule.paragraphNumber,
            BASIC_OFFENCE_DETAILS_RESPONSE_DTO.offenceRule.paragraphDescription,
            BASIC_OFFENCE_DETAILS_RESPONSE_DTO.victimPrisonersNumber,
            BASIC_OFFENCE_DETAILS_RESPONSE_DTO.victimStaffUsername,
            BASIC_OFFENCE_DETAILS_RESPONSE_DTO.victimOtherPersonsName,
          )
      }

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
        gender = Gender.MALE,
        agencyId = "MDI",
        incidentDetails = incidentDetails(2L, now),
        offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
        incidentStatement = IncidentStatement(
          statement = "Example statement",
          completed = false,
        ),
        isYouthOffender = true,
      )

    private fun draftAdjudicationWithRole(roleCode: String?) =
      DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        gender = Gender.MALE,
        agencyId = "MDI",
        incidentDetails = incidentDetails(2L, now),
        offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
        incidentStatement = IncidentStatement(
          statement = "Example statement",
          completed = false,
        ),
        incidentRole = incidentRoleWithValuesSetForRoleCode(roleCode),
        isYouthOffender = true,
      )

    @Test
    fun `throws state exception if isYouthOffender is not set`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          draftAdjudication.also { it.isYouthOffender = null },
        ),
      )

      assertThatThrownBy {
        draftAdjudicationService.editIncidentRole(1, IncidentRoleRequest("1"), false)
      }.isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining(ValidationChecks.APPLICABLE_RULES.errorMessage)
    }

    @ParameterizedTest
    @CsvSource("25b", "25c")
    fun `saves incident role with assisted or incited and retains previous values`(roleCode: String) {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(draftAdjudicationWithRole(roleCode)),
      )

      whenever(draftAdjudicationRepository.save(any())).thenReturn(draftAdjudicationWithRole(roleCode))

      val response = draftAdjudicationService.editIncidentRole(1, IncidentRoleRequest(roleCode), false)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.incidentRole!!.roleCode).isEqualTo(roleCode)
      assertThat(argumentCaptor.value.incidentRole!!.associatedPrisonersNumber).isEqualTo("2")
      assertThat(argumentCaptor.value.incidentRole!!.associatedPrisonersName).isEqualTo("3")

      assertThat(response).isNotNull
    }

    @Test
    fun `saves existing incident role and removes associated name and number`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(draftAdjudicationWithRole(null)),
      )

      whenever(draftAdjudicationRepository.save(any())).thenReturn(draftAdjudicationWithRole(null))

      val response = draftAdjudicationService.editIncidentRole(1, IncidentRoleRequest(null), false)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.incidentRole!!.roleCode).isEqualTo(null)
      assertThat(argumentCaptor.value.incidentRole!!.associatedPrisonersNumber).isEqualTo(null)
      assertThat(argumentCaptor.value.incidentRole!!.associatedPrisonersName).isEqualTo(null)

      assertThat(response).isNotNull
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `saves new incident role`(deleteOffences: Boolean) {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(draftAdjudication),
      )

      whenever(draftAdjudicationRepository.save(any())).thenReturn(draftAdjudication)

      val response = draftAdjudicationService.editIncidentRole(1, IncidentRoleRequest(null), deleteOffences)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.offenceDetails!!.isEmpty()).isEqualTo(deleteOffences)
      assertThat(argumentCaptor.value.incidentRole!!.roleCode).isEqualTo(null)
      assertThat(argumentCaptor.value.incidentRole!!.associatedPrisonersNumber).isEqualTo(null)

      assertThat(response).isNotNull
    }

    @Test
    fun `returns the draft adjudication`() {
      testDto(
        Optional.of(
          DraftAdjudication(
            id = 1,
            prisonerNumber = "A12345",
            gender = Gender.MALE,
            agencyId = "MDI",
            incidentDetails = incidentDetails(2L, now),
            incidentRole = IncidentRole(null, "25a", null, null),
            offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
            incidentStatement = IncidentStatement(
              statement = "Example statement",
              completed = false,
            ),
            isYouthOffender = false,
          ),

        ),
      ) {
        draftAdjudicationService.editIncidentRole(
          1,
          IncidentRoleRequest("25a"),
          false,
        )
      }
    }
  }

  @Nested
  inner class SetIncidentRoleAssociatedPrisoner {
    private val draftAdjudication =
      DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        gender = Gender.MALE,
        agencyId = "MDI",
        incidentDetails = incidentDetails(2L, now),
        incidentRole = incidentRoleWithNoValuesSet(),
        offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
        incidentStatement = IncidentStatement(
          statement = "Example statement",
          completed = false,
        ),
        isYouthOffender = true,
      )

    @Test
    fun `throws exception if offender is also associate `() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(draftAdjudication),
      )

      assertThatThrownBy {
        draftAdjudicationService.setIncidentRoleAssociatedPrisoner(
          1,
          IncidentRoleAssociatedPrisonerRequest(draftAdjudication.prisonerNumber, "A name"),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("offender can not be an associate")
    }

    @Test
    fun `throws state exception if incident role is not set`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          draftAdjudication.also { it.incidentRole = null },
        ),
      )

      assertThatThrownBy {
        draftAdjudicationService.setIncidentRoleAssociatedPrisoner(
          1,
          IncidentRoleAssociatedPrisonerRequest("A1234AA", "A name"),
        )
      }.isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining(ValidationChecks.INCIDENT_ROLE.errorMessage)
    }

    @Test
    fun `saves associated prisoner`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          draftAdjudication,
        ),
      )
      whenever(draftAdjudicationRepository.save(any())).thenReturn(
        draftAdjudication.also {
          it.incidentRole = incidentRoleWithAllValuesSet()
        },
      )

      val response = draftAdjudicationService.setIncidentRoleAssociatedPrisoner(
        1,
        IncidentRoleAssociatedPrisonerRequest("A1234AA", "A prisoner"),
      )

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.incidentRole!!.associatedPrisonersNumber).isEqualTo("A1234AA")
      assertThat(argumentCaptor.value.incidentRole!!.associatedPrisonersName).isEqualTo("A prisoner")

      assertThat(response).isNotNull
    }

    @Test
    fun `returns the draft adjudication`() {
      testDto {
        draftAdjudicationService.setIncidentRoleAssociatedPrisoner(
          1,
          IncidentRoleAssociatedPrisonerRequest(
            INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER,
            INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME,
          ),
        )
      }
    }
  }

  @Nested
  inner class AddIncidentStatement {

    @Test
    fun `adds an incident statement to a draft adjudication`() {
      val draftAdjudicationEntity = DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        gender = Gender.MALE,
        agencyId = "MDI",
        incidentDetails = incidentDetails(2L, clock),
        incidentRole = incidentRoleWithNoValuesSet(),
        isYouthOffender = true,
      )

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudicationEntity))

      whenever(draftAdjudicationRepository.save(any())).thenReturn(
        draftAdjudicationEntity.copy(
          incidentStatement = IncidentStatement(id = 1, statement = "test"),
        ),
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
              gender = Gender.MALE,
              agencyId = "MDI",
              incidentDetails = incidentDetails(2L, clock),
              incidentRole = incidentRoleWithNoValuesSet(),
              incidentStatement = IncidentStatement(id = 1, statement = "test"),
              isYouthOffender = true,
            ),
          ),
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
    fun `throws exception if date of discovery before incident date`() {
      assertThatThrownBy {
        draftAdjudicationService.editIncidentDetails(
          1,
          2,
          DATE_TIME_OF_INCIDENT,
          DATE_TIME_OF_INCIDENT.minusDays(1),
        )
      }.isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("Date of discovery is before incident date")
    }

    @Test
    fun `ensure discovery date is updated when only incident date is provided `() {
      val editedDateTimeOfIncident = DATE_TIME_OF_INCIDENT.plusMonths(1)
      val editedIncidentRole = incidentRoleWithNoValuesSet()
      val draftAdjudicationEntity = DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        gender = Gender.MALE,
        agencyId = "MDI",
        incidentDetails = IncidentDetails(
          id = 1,
          locationId = 2,
          dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
          dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT.plusDays(1),
          handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
        ),
        isYouthOffender = true,
      )

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(draftAdjudicationEntity),
      )
      whenever(draftAdjudicationRepository.save(any())).thenReturn(
        draftAdjudicationEntity.copy(
          incidentDetails = IncidentDetails(
            id = 1,
            locationId = 3L,
            dateTimeOfIncident = editedDateTimeOfIncident,
            dateTimeOfDiscovery = editedDateTimeOfIncident,
            handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
          ),
          incidentRole = editedIncidentRole,
        ),
      )

      val draftAdjudication = draftAdjudicationService.editIncidentDetails(
        1,
        3,
        editedDateTimeOfIncident,
        null,
      )

      assertThat(draftAdjudication)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")

      assertThat(draftAdjudication.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident", "dateTimeOfDiscovery", "handoverDeadline")
        .contains(3L, editedDateTimeOfIncident, editedDateTimeOfIncident, DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident", "dateTimeOfDiscovery")
        .contains(3L, editedDateTimeOfIncident, editedDateTimeOfIncident)
    }

    @Test
    fun `makes changes to the incident details`() {
      val editedDateTimeOfIncident = DATE_TIME_OF_INCIDENT.plusMonths(1)
      val editedIncidentRole = incidentRoleWithNoValuesSet()
      val draftAdjudicationEntity = DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        gender = Gender.MALE,
        agencyId = "MDI",
        incidentDetails = IncidentDetails(
          id = 1,
          locationId = 2,
          dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
          dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT.plusDays(1),
          handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
        ),
        isYouthOffender = true,
      )

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(draftAdjudicationEntity),
      )
      whenever(draftAdjudicationRepository.save(any())).thenReturn(
        draftAdjudicationEntity.copy(
          incidentDetails = IncidentDetails(
            id = 1,
            locationId = 3L,
            dateTimeOfIncident = editedDateTimeOfIncident,
            dateTimeOfDiscovery = editedDateTimeOfIncident.plusDays(1),
            handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
          ),
          incidentRole = editedIncidentRole,
        ),
      )

      val draftAdjudication = draftAdjudicationService.editIncidentDetails(
        1,
        3,
        editedDateTimeOfIncident,
        editedDateTimeOfIncident.plusDays(1),
      )

      assertThat(draftAdjudication)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")

      assertThat(draftAdjudication.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident", "dateTimeOfDiscovery", "handoverDeadline")
        .contains(3L, editedDateTimeOfIncident, editedDateTimeOfIncident.plusDays(1), DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident", "dateTimeOfDiscovery")
        .contains(3L, editedDateTimeOfIncident, editedDateTimeOfIncident.plusDays(1))
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
    fun `throws an entity not found if the incident statement does not exist`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          DraftAdjudication(
            id = 1,
            prisonerNumber = "A12345",
            gender = Gender.MALE,
            agencyId = "MDI",
            incidentDetails = incidentDetails(2L, clock),
            incidentRole = incidentRoleWithNoValuesSet(),
            isYouthOffender = true,
          ),
        ),
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
          gender = Gender.MALE,
          agencyId = "MDI",
          incidentDetails = incidentDetails(2L, clock),
          incidentRole = incidentRoleWithNoValuesSet(),
          incidentStatement = IncidentStatement(statement = "old statement"),
          isYouthOffender = true,
        )

        whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudicationEntity))
        whenever(draftAdjudicationRepository.save(any())).thenReturn(
          draftAdjudicationEntity.copy(incidentStatement = IncidentStatement(id = 1, statement = "new statement")),
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
  inner class InProgressDraftAdjudications {
    @BeforeEach
    fun beforeEach() {
      whenever(
        draftAdjudicationRepository.findByAgencyIdAndCreatedByUserIdAndReportNumberIsNullAndIncidentDetailsDateTimeOfDiscoveryBetween(
          any(),
          any(),
          any(),
          any(),
          any(),
        ),
      ).thenReturn(
        PageImpl(
          listOf(
            DraftAdjudication(
              id = 1,
              prisonerNumber = "A12345",
              gender = Gender.MALE,
              agencyId = "MDI",
              incidentDetails = IncidentDetails(
                id = 2,
                locationId = 2,
                dateTimeOfIncident = LocalDateTime.now(clock).plusMonths(2),
                dateTimeOfDiscovery = LocalDateTime.now(clock).plusMonths(2).plusDays(1),
                handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
              ),
              incidentRole = incidentRoleWithAllValuesSet(),
              offenceDetails = mutableListOf(FULL_OFFENCE_DETAILS_DB_ENTITY),
              incidentStatement = IncidentStatement(
                statement = "Example statement",
                completed = false,
              ),
              isYouthOffender = false,
            ),
            DraftAdjudication(
              id = 2,
              prisonerNumber = "A12346",
              gender = Gender.MALE,
              agencyId = "MDI",
              incidentDetails = incidentDetails(3L, clock),
              incidentRole = incidentRoleWithNoValuesSet(),
              isYouthOffender = false,
            ),
          ),
        ),
      )
    }

    @Test
    fun `calls the repository method for all draft adjudications created by ITAG_USER`() {
      whenever(authenticationFacade.currentUsername).thenReturn("ITAG_USER")

      draftAdjudicationService.getCurrentUsersInProgressDraftAdjudications(
        "MDI",
        LocalDate.now().minusWeeks(1),
        LocalDate.now(),
        pageable,
      )

      verify(draftAdjudicationRepository).findByAgencyIdAndCreatedByUserIdAndReportNumberIsNullAndIncidentDetailsDateTimeOfDiscoveryBetween(
        "MDI",
        "ITAG_USER",
        LocalDate.now().minusWeeks(1).atStartOfDay(),
        LocalDate.now().atTime(LocalTime.MAX),
        pageable,
      )
    }

    @Test
    fun `given no user return an empty set`() {
      whenever(authenticationFacade.currentUsername).thenReturn(null)
      val draftAdjudications = draftAdjudicationService.getCurrentUsersInProgressDraftAdjudications(
        "MDI",
        LocalDate.now(),
        LocalDate.now(),
        pageable,
      )

      assertThat(draftAdjudications).isEmpty()
    }
  }

  @Nested
  inner class SetApplicableRules {
    private val draftAdjudication =
      DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        gender = Gender.MALE,
        agencyId = "MDI",
        incidentDetails = incidentDetails(2L, now),
        incidentRole = incidentRoleWithAllValuesSet(),
        incidentStatement = IncidentStatement(
          statement = "Example statement",
          completed = false,
        ),
        offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
        isYouthOffender = true,
      )

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `saves incident applicable rule`(deleteOffences: Boolean) {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudication))
      whenever(draftAdjudicationRepository.save(any())).thenReturn(draftAdjudication)

      val response = draftAdjudicationService.setIncidentApplicableRule(1, true, deleteOffences)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.offenceDetails.isEmpty()).isEqualTo(deleteOffences)
      assertThat(argumentCaptor.value.isYouthOffender).isEqualTo(true)
      assertThat(response).isNotNull
    }

    @Test
    fun `returns the draft adjudication`() {
      testDto(Optional.of(draftAdjudication)) { draftAdjudicationService.setIncidentApplicableRule(1, false, true) }
    }
  }

  @Nested
  inner class SetGender {

    private val draftAdjudication =
      DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        gender = Gender.MALE,
        agencyId = "MDI",
        incidentDetails = incidentDetails(2L, now),
        incidentRole = incidentRoleWithAllValuesSet(),
        incidentStatement = IncidentStatement(
          statement = "Example statement",
          completed = false,
        ),
        offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY),
        isYouthOffender = true,
      )

    @Test
    fun `sets gender to female`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudication))
      whenever(draftAdjudicationRepository.save(any())).thenReturn(draftAdjudication)
      whenever(offenceCodeLookupService.getParagraphDescription(1001, true, Gender.FEMALE)).thenReturn(OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION)

      val response = draftAdjudicationService.setGender(1, Gender.FEMALE)

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.gender).isEqualTo(Gender.FEMALE)
      assertThat(response).isNotNull
    }

    @Nested
    inner class DeleteDraftAdjudication {

      private val draftAdjudication =
        DraftAdjudication(
          id = 1,
          prisonerNumber = "A12345",
          gender = Gender.MALE,
          agencyId = "MDI",
          incidentDetails = incidentDetails(2L, now),
          incidentRole = incidentRoleWithAllValuesSet(),
          incidentStatement = IncidentStatement(
            statement = "Example statement",
            completed = false,
          ),
          offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY),
          isYouthOffender = true,
        ).apply { createdByUserId = "ITAG_USER" }

      @Test
      fun `throw exception if not owner trying to delete draft adjudication`() {
        whenever(authenticationFacade.currentUsername).thenReturn("not_owner")
        whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudication))

        assertThatThrownBy {
          draftAdjudicationService.deleteDraftAdjudications(1)
        }.isInstanceOf(ForbiddenException::class.java)
          .hasMessageContaining("Only creator(owner) of draft adjudication can delete draft adjudication report. Owner username: ITAG_USER, deletion attempt by username: not_owner.")
      }

      @Test
      fun `delete draft adjudication by owner`() {
        whenever(authenticationFacade.currentUsername).thenReturn("ITAG_USER")
        whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudication))

        draftAdjudicationService.deleteDraftAdjudications(1)

        val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)

        verify(draftAdjudicationRepository, times(1)).delete(draftAdjudication)
        verify(draftAdjudicationRepository).delete(argumentCaptor.capture())
        assertThat(argumentCaptor.value.id).isEqualTo(1)
      }
    }
  }

  companion object {

    val pageable = Pageable.ofSize(20).withPage(0)

    fun incidentDetails(locationId: Long, clock: Clock) = IncidentDetails(
      locationId = locationId,
      dateTimeOfIncident = LocalDateTime.now(clock),
      dateTimeOfDiscovery = LocalDateTime.now(clock).plusDays(1),
      handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
    )

    fun incidentDetails(locationId: Long, now: LocalDateTime) = IncidentDetails(
      locationId = locationId,
      dateTimeOfIncident = now,
      dateTimeOfDiscovery = now.plusDays(1),
      handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
    )

    fun incidentRoleDtoWithAllValuesSet(): IncidentRoleDto =
      IncidentRoleDto(
        INCIDENT_ROLE_CODE,
        OffenceRuleDetailsDto(
          INCIDENT_ROLE_PARAGRAPH_NUMBER,
          INCIDENT_ROLE_PARAGRAPH_DESCRIPTION,
        ),
        INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER,
        INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME,
      )

    fun incidentRoleDtoWithNoValuesSet(): IncidentRoleDto =
      IncidentRoleDto(null, null, null, null)

    fun incidentRoleWithAllValuesSet(): IncidentRole =
      IncidentRole(null, INCIDENT_ROLE_CODE, INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER, INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME)

    fun incidentRoleWithNoValuesSet(): IncidentRole =
      IncidentRole(null, null, null, null)

    fun incidentRoleWithValuesSetForRoleCode(roleCode: String?): IncidentRole =
      IncidentRole(null, roleCode, "2", "3")
  }

  fun testDto(toFind: Optional<DraftAdjudication> = Optional.empty(), toTest: Supplier<DraftAdjudicationDto>) {
    val draftAdjudication =
      DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        gender = Gender.MALE,
        agencyId = "MDI",
        incidentDetails = incidentDetails(2L, now),
        incidentRole = incidentRoleWithAllValuesSet(),
        offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
        incidentStatement = IncidentStatement(
          statement = "Example statement",
          completed = false,
        ),
        isYouthOffender = false,
      )
    draftAdjudication.createdByUserId = "A_USER" // Add audit information

    whenever(draftAdjudicationRepository.findById(any())).thenReturn(
      Optional.of(toFind.orElse(draftAdjudication)),
    )

    whenever(draftAdjudicationRepository.save(any())).thenReturn(
      draftAdjudication,
    )

    val draftAdjudicationDto = toTest.get()

    assertThat(draftAdjudicationDto)
      .extracting("id", "prisonerNumber", "startedByUserId")
      .contains(1L, "A12345", "A_USER")

    assertThat(draftAdjudicationDto.incidentDetails)
      .extracting("locationId", "dateTimeOfIncident")
      .contains(2L, now)

    assertThat(draftAdjudicationDto.incidentRole)
      .extracting("roleCode", "offenceRule", "associatedPrisonersNumber", "associatedPrisonersName")
      .contains(
        incidentRoleDtoWithAllValuesSet().roleCode,
        IncidentRoleRuleLookup.getOffenceRuleDetails(incidentRoleDtoWithAllValuesSet().roleCode, false),
        incidentRoleDtoWithAllValuesSet().associatedPrisonersNumber,
        incidentRoleDtoWithAllValuesSet().associatedPrisonersName,
      )

    assertThat(draftAdjudicationDto.offenceDetails)
      .extracting(
        "offenceCode",
        "offenceRule.paragraphNumber",
        "offenceRule.paragraphDescription",
        "victimPrisonersNumber",
        "victimStaffUsername",
        "victimOtherPersonsName",
      )
      .contains(
        BASIC_OFFENCE_DETAILS_RESPONSE_DTO.offenceCode,
        BASIC_OFFENCE_DETAILS_RESPONSE_DTO.offenceRule.paragraphNumber,
        BASIC_OFFENCE_DETAILS_RESPONSE_DTO.offenceRule.paragraphDescription,
        BASIC_OFFENCE_DETAILS_RESPONSE_DTO.victimPrisonersNumber,
        BASIC_OFFENCE_DETAILS_RESPONSE_DTO.victimStaffUsername,
        BASIC_OFFENCE_DETAILS_RESPONSE_DTO.victimOtherPersonsName,
      )

    assertThat(draftAdjudicationDto.incidentStatement)
      .extracting("statement", "completed")
      .contains("Example statement", false)
  }

  @Test
  override fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
    whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

    assertThatThrownBy {
      draftAdjudicationService.getDraftAdjudicationDetails(1)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("DraftAdjudication not found for 1")
    assertThatThrownBy {
      draftAdjudicationService.editIncidentRole(1, IncidentRoleRequest("1"), false)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("DraftAdjudication not found for 1")
    assertThatThrownBy {
      draftAdjudicationService.setIncidentRoleAssociatedPrisoner(
        1,
        IncidentRoleAssociatedPrisonerRequest("A1234AA", "A name"),
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("DraftAdjudication not found for 1")
    assertThatThrownBy {
      draftAdjudicationService.addIncidentStatement(1, "test", false)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("DraftAdjudication not found for 1")

    assertThatThrownBy {
      draftAdjudicationService.editIncidentDetails(
        1,
        2,
        DATE_TIME_OF_INCIDENT,
        DATE_TIME_OF_INCIDENT.plusDays(1),
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("DraftAdjudication not found for 1")

    assertThatThrownBy {
      draftAdjudicationService.editIncidentStatement(1, "new statement", false)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("DraftAdjudication not found for 1")

    assertThatThrownBy {
      draftAdjudicationService.setIncidentApplicableRule(1, false, true)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("DraftAdjudication not found for 1")

    assertThatThrownBy {
      draftAdjudicationService.setGender(1, Gender.MALE)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("DraftAdjudication not found for 1")

    assertThatThrownBy {
      draftAdjudicationService.deleteDraftAdjudications(1)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("DraftAdjudication not found for 1")
  }
}
