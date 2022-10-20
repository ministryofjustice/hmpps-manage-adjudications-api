package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.DamageRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.EvidenceRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.IncidentRoleAssociatedPrisonerRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.IncidentRoleRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.OffenceDetailsRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.WitnessRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Damage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Evidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Witness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import java.time.Clock
import java.time.Instant.ofEpochMilli
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional
import java.util.function.Supplier
import javax.persistence.EntityNotFoundException

class DraftAdjudicationServiceTest {
  private val draftAdjudicationRepository: DraftAdjudicationRepository = mock()
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository = mock()
  private val prisonApiGateway: PrisonApiGateway = mock()
  private val offenceCodeLookupService: OffenceCodeLookupService = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val clock: Clock = Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault())

  private lateinit var draftAdjudicationService: DraftAdjudicationService

  @BeforeEach
  fun beforeEach() {
    draftAdjudicationService =
      DraftAdjudicationService(
        draftAdjudicationRepository,
        offenceCodeLookupService,
        authenticationFacade,
      )

    // Set up offence code mocks
    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(2, false)).thenReturn(
      OFFENCE_CODE_2_NOMIS_CODE_ON_OWN
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(2, false)).thenReturn(
      OFFENCE_CODE_2_NOMIS_CODE_ASSISTED
    )
    whenever(offenceCodeLookupService.getParagraphNumber(2, false)).thenReturn(OFFENCE_CODE_2_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(2, false)).thenReturn(OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION)

    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(3, false)).thenReturn(
      OFFENCE_CODE_3_NOMIS_CODE_ON_OWN
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(3, false)).thenReturn(
      OFFENCE_CODE_3_NOMIS_CODE_ASSISTED
    )
    whenever(offenceCodeLookupService.getParagraphNumber(3, false)).thenReturn(OFFENCE_CODE_3_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(3, false)).thenReturn(OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION)

    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(2, true)).thenReturn(
      YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ON_OWN
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(2, true)).thenReturn(
      YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ASSISTED
    )
    whenever(offenceCodeLookupService.getParagraphNumber(2, true)).thenReturn(YOUTH_OFFENCE_CODE_2_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(2, true)).thenReturn(YOUTH_OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION)

    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(3, true)).thenReturn(
      YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ON_OWN
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(3, true)).thenReturn(
      YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ASSISTED
    )
    whenever(offenceCodeLookupService.getParagraphNumber(3, true)).thenReturn(YOUTH_OFFENCE_CODE_3_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(3, true)).thenReturn(YOUTH_OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION)
  }

  @Nested
  inner class StartDraftAdjudications {
    val draftAdjudication = DraftAdjudication(
      id = 1,
      prisonerNumber = "A12345",
      agencyId = "MDI",
      incidentDetails = incidentDetails(2L, DATE_TIME_OF_INCIDENT),
    )

    @Test
    fun `throws exception if date of discovery before incident date`() {
      assertThatThrownBy {
        draftAdjudicationService.startNewAdjudication(
          "A12345",
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
          "MDI",
          2L,
          DATE_TIME_OF_INCIDENT,
          DATE_TIME_OF_INCIDENT.plusDays(1),
        )

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)

      verify(draftAdjudicationRepository).save(argumentCaptor.capture())
      verify(telemetryClient).trackEvent(
        DraftAdjudicationService.TELEMETRY_EVENT,
        mapOf(
          "adjudicationNumber" to draftAdjudication.id.toString(),
          "agencyId" to "MDI",
          "reportNumber" to "null"
        ),
        null
      )

      assertThat(draftAdjudication)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")

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
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

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
          agencyId = "MDI",
          incidentDetails = incidentDetails(2L, now),
          incidentRole = incidentRoleWithAllValuesSet(),
          offenceDetails = offenceDetails,
          incidentStatement = IncidentStatement(
            statement = "Example statement",
            completed = false
          ),
          isYouthOffender = isYouthOffender
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
        .extracting("roleCode", "offenceRule", "associatedPrisonersNumber", "associatedPrisonersName")
        .contains(
          incidentRoleDtoWithAllValuesSet().roleCode,
          IncidentRoleRuleLookup.getOffenceRuleDetails(
            incidentRoleDtoWithAllValuesSet().roleCode,
            draftAdjudication.isYouthOffender!!
          ),
          incidentRoleDtoWithAllValuesSet().associatedPrisonersNumber,
          incidentRoleDtoWithAllValuesSet().associatedPrisonersName,
        )

      if (isYouthOffender) {
        assertThat(draftAdjudicationDto.offenceDetails).hasSize(1)
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
              YOUTH_OFFENCE_DETAILS_RESPONSE_DTO.offenceCode,
              YOUTH_OFFENCE_DETAILS_RESPONSE_DTO.offenceRule.paragraphNumber,
              YOUTH_OFFENCE_DETAILS_RESPONSE_DTO.offenceRule.paragraphDescription,
              YOUTH_OFFENCE_DETAILS_RESPONSE_DTO.victimPrisonersNumber,
              YOUTH_OFFENCE_DETAILS_RESPONSE_DTO.victimStaffUsername,
              YOUTH_OFFENCE_DETAILS_RESPONSE_DTO.victimOtherPersonsName
            ),
          )
      } else {
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
        agencyId = "MDI",
        incidentDetails = incidentDetails(2L, now),
        offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
        incidentStatement = IncidentStatement(
          statement = "Example statement",
          completed = false,
        ),
        isYouthOffender = true
      )

    private fun draftAdjudicationWithRole(roleCode: String?) =
      DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        agencyId = "MDI",
        incidentDetails = incidentDetails(2L, now),
        offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
        incidentStatement = IncidentStatement(
          statement = "Example statement",
          completed = false,
        ),
        incidentRole = incidentRoleWithValuesSetForRoleCode(roleCode),
        isYouthOffender = true
      )

    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

      assertThatThrownBy {
        draftAdjudicationService.editIncidentRole(1, IncidentRoleRequest("1"), false)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

    @Test
    fun `throws state exception if isYouthOffender is not set`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          draftAdjudication.also { it.isYouthOffender = null }
        )
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
        Optional.of(draftAdjudicationWithRole(roleCode))
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
        Optional.of(draftAdjudicationWithRole(null))
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
        Optional.of(draftAdjudication)
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
            agencyId = "MDI",
            incidentDetails = incidentDetails(2L, now),
            incidentRole = IncidentRole(null, "25a", null, null),
            offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
            incidentStatement = IncidentStatement(
              statement = "Example statement",
              completed = false
            ),
            isYouthOffender = false
          )

        )
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
        agencyId = "MDI",
        incidentDetails = incidentDetails(2L, now),
        incidentRole = incidentRoleWithNoValuesSet(),
        offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
        incidentStatement = IncidentStatement(
          statement = "Example statement",
          completed = false,
        ),
        isYouthOffender = true
      )

    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

      assertThatThrownBy {
        draftAdjudicationService.setIncidentRoleAssociatedPrisoner(
          1,
          IncidentRoleAssociatedPrisonerRequest("A1234AA", "A name")
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

    @Test
    fun `throws state exception if incident role is not set`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          draftAdjudication.also { it.incidentRole = null }
        )
      )

      assertThatThrownBy {
        draftAdjudicationService.setIncidentRoleAssociatedPrisoner(
          1,
          IncidentRoleAssociatedPrisonerRequest("A1234AA", "A name")
        )
      }.isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining(ValidationChecks.INCIDENT_ROLE.errorMessage)
    }

    @Test
    fun `saves associated prisoner`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          draftAdjudication
        )
      )
      whenever(draftAdjudicationRepository.save(any())).thenReturn(
        draftAdjudication.also {
          it.incidentRole = incidentRoleWithAllValuesSet()
        }
      )

      val response = draftAdjudicationService.setIncidentRoleAssociatedPrisoner(
        1,
        IncidentRoleAssociatedPrisonerRequest("A1234AA", "A prisoner")
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
            INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME
          )
        )
      }
    }
  }

  @Nested
  inner class SetOffenceDetails {
    private val draftAdjudicationEntity = DraftAdjudication(
      id = 1,
      prisonerNumber = "A12345",
      agencyId = "MDI",
      incidentDetails = incidentDetails(2L, clock),
      incidentRole = incidentRoleWithNoValuesSet(),
    )

    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

      assertThatThrownBy {
        draftAdjudicationService.setOffenceDetails(1, listOf(BASIC_OFFENCE_DETAILS_REQUEST))
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

    @Test
    fun `throws state exception if isYouthOffender is not set`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudicationEntity))

      assertThatThrownBy {
        draftAdjudicationService.setOffenceDetails(1, listOf(BASIC_OFFENCE_DETAILS_REQUEST))
      }.isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining(ValidationChecks.APPLICABLE_RULES.errorMessage)
    }

    @ParameterizedTest
    @CsvSource(
      "true",
      "false",
    )
    fun `adds the offence details to a draft adjudication`(
      isYouthOffender: Boolean,
    ) {
      var offenceDetailsToAdd = listOf(BASIC_OFFENCE_DETAILS_REQUEST, FULL_OFFENCE_DETAILS_REQUEST)
      var offenceDetailsToSave = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY)
      var expectedOffenceDetailsResponse = listOf(BASIC_OFFENCE_DETAILS_RESPONSE_DTO, FULL_OFFENCE_DETAILS_RESPONSE_DTO)
      if (isYouthOffender) {
        offenceDetailsToAdd = listOf(YOUTH_OFFENCE_DETAILS_REQUEST)
        offenceDetailsToSave = mutableListOf(YOUTH_OFFENCE_DETAILS_DB_ENTITY)
        expectedOffenceDetailsResponse = listOf(YOUTH_OFFENCE_DETAILS_RESPONSE_DTO)
      }

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          draftAdjudicationEntity.also {
            it.isYouthOffender = isYouthOffender
          }
        )
      )

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
      val existingOffenceDetails = mutableListOf(Offence(offenceCode = 1))
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
          dateTimeOfDiscovery = LocalDateTime.now(clock).plusDays(1),
          handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
        ),
        incidentRole = incidentRoleWithNoValuesSet(),
        offenceDetails = existingOffenceDetails,
        isYouthOffender = false
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
        incidentDetails = incidentDetails(2L, clock),
        incidentRole = incidentRoleWithNoValuesSet(),
        isYouthOffender = false
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
        .hasMessageContaining("Please supply at least one set of items")
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
        incidentDetails = incidentDetails(2L, clock),
        incidentRole = incidentRoleWithNoValuesSet(),
        isYouthOffender = true
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
              incidentDetails = incidentDetails(2L, clock),
              incidentRole = incidentRoleWithNoValuesSet(),
              incidentStatement = IncidentStatement(id = 1, statement = "test"),
              isYouthOffender = true
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
          DATE_TIME_OF_INCIDENT.plusDays(1),
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

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
    fun `makes changes to the incident details`() {
      val editedDateTimeOfIncident = DATE_TIME_OF_INCIDENT.plusMonths(1)
      val editedIncidentRole = incidentRoleWithNoValuesSet()
      val draftAdjudicationEntity = DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        agencyId = "MDI",
        incidentDetails = IncidentDetails(
          id = 1,
          locationId = 2,
          dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
          dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT.plusDays(1),
          handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
        ),
        isYouthOffender = true
      )

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(draftAdjudicationEntity)
      )
      whenever(draftAdjudicationRepository.save(any())).thenReturn(
        draftAdjudicationEntity.copy(
          incidentDetails = IncidentDetails(
            id = 1,
            locationId = 3L,
            dateTimeOfIncident = editedDateTimeOfIncident,
            dateTimeOfDiscovery = editedDateTimeOfIncident.plusDays(1),
            handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
          ),
          incidentRole = editedIncidentRole
        )
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
        .extracting("locationId", "dateTimeOfIncident")
        .contains(3L, editedDateTimeOfIncident)
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
            incidentDetails = incidentDetails(2L, clock),
            incidentRole = incidentRoleWithNoValuesSet(),
            isYouthOffender = true
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
          incidentDetails = incidentDetails(2L, clock),
          incidentRole = incidentRoleWithNoValuesSet(),
          incidentStatement = IncidentStatement(statement = "old statement"),
          isYouthOffender = true
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
              dateTimeOfDiscovery = LocalDateTime.now(clock).plusMonths(2).plusDays(1),
              handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
            ),
            incidentRole = incidentRoleWithAllValuesSet(),
            offenceDetails = mutableListOf(FULL_OFFENCE_DETAILS_DB_ENTITY),
            incidentStatement = IncidentStatement(
              statement = "Example statement",
              completed = false
            ),
            isYouthOffender = false
          ),
          DraftAdjudication(
            id = 2,
            prisonerNumber = "A12346",
            agencyId = "MDI",
            incidentDetails = incidentDetails(3L, clock),
            incidentRole = incidentRoleWithNoValuesSet(),
            isYouthOffender = false
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
          IncidentDetailsDto(3, LocalDateTime.now(clock), LocalDateTime.now(clock).plusDays(1,), DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE),
          IncidentDetailsDto(2, LocalDateTime.now(clock).plusMonths(2), LocalDateTime.now(clock).plusMonths(2).plusDays(1), DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE)
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
          emptyList<OffenceDetailsDto>()
        )

      assertThat(adjudications)
        .extracting("incidentStatement")
        .contains(
          IncidentStatementDto("Example statement", false),
          null as IncidentStatementDto?
        )
    }
  }

  @Nested
  inner class SetApplicableRules {
    private val draftAdjudication =
      DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        agencyId = "MDI",
        incidentDetails = incidentDetails(2L, now),
        incidentRole = incidentRoleWithAllValuesSet(),
        incidentStatement = IncidentStatement(
          statement = "Example statement",
          completed = false
        ),
        offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
        isYouthOffender = true
      )

    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

      assertThatThrownBy {
        draftAdjudicationService.setIncidentApplicableRule(1, false, true)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

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
  inner class AddDamages {
    private val draftAdjudication =
      DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        agencyId = "MDI",
        incidentDetails = incidentDetails(2L, now),
        incidentRole = incidentRoleWithAllValuesSet(),
        incidentStatement = IncidentStatement(
          statement = "Example statement",
          completed = false
        ),
        offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
        isYouthOffender = true,
        damages = mutableListOf(
          Damage(code = DamageCode.CLEANING, details = "details", reporter = "Fred")
        )
      )

    @BeforeEach
    fun init() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudication))
      whenever(draftAdjudicationRepository.save(any())).thenReturn(draftAdjudication)
      whenever(authenticationFacade.currentUsername).thenReturn("Fred")
    }

    @Test
    fun `add damages to adjudication`() {
      val response = draftAdjudicationService.setDamages(
        1,
        listOf(
          DamageRequestItem(DamageCode.CLEANING, "details")
        )
      )

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.damages.size).isEqualTo(1)
      assertThat(argumentCaptor.value.damages.first().code).isEqualTo(DamageCode.CLEANING)
      assertThat(argumentCaptor.value.damages.first().details).isEqualTo("details")
      assertThat(argumentCaptor.value.damages.first().reporter).isEqualTo("Fred")
      assertThat(argumentCaptor.value.damagesSaved).isEqualTo(true)

      assertThat(response).isNotNull
    }

    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

      assertThatThrownBy {
        draftAdjudicationService.setDamages(1, listOf(DamageRequestItem(DamageCode.CLEANING, "details")))
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }
  }

  @Nested
  inner class AddEvidence {
    private val draftAdjudication =
      DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        agencyId = "MDI",
        incidentDetails = incidentDetails(2L, now),
        incidentRole = incidentRoleWithAllValuesSet(),
        incidentStatement = IncidentStatement(
          statement = "Example statement",
          completed = false
        ),
        offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
        isYouthOffender = true,
        evidence = mutableListOf(
          Evidence(code = EvidenceCode.PHOTO, details = "details", reporter = "Fred")
        )
      )

    @BeforeEach
    fun init() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudication))
      whenever(draftAdjudicationRepository.save(any())).thenReturn(draftAdjudication)
      whenever(authenticationFacade.currentUsername).thenReturn("Fred")
    }

    @Test
    fun `add evidence to adjudication`() {
      val response = draftAdjudicationService.setEvidence(
        1,
        listOf(
          EvidenceRequestItem(code = EvidenceCode.PHOTO, details = "details")
        )
      )

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.evidence.size).isEqualTo(1)
      assertThat(argumentCaptor.value.evidence.first().code).isEqualTo(EvidenceCode.PHOTO)
      assertThat(argumentCaptor.value.evidence.first().details).isEqualTo("details")
      assertThat(argumentCaptor.value.evidence.first().reporter).isEqualTo("Fred")
      assertThat(argumentCaptor.value.evidenceSaved).isEqualTo(true)

      assertThat(response).isNotNull
    }

    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

      assertThatThrownBy {
        draftAdjudicationService.setEvidence(1, listOf(EvidenceRequestItem(code = EvidenceCode.PHOTO, details = "details")))
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }
  }

  @Nested
  inner class AddWitnesses {
    private val draftAdjudication =
      DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        agencyId = "MDI",
        incidentDetails = incidentDetails(2L, now),
        incidentRole = incidentRoleWithAllValuesSet(),
        incidentStatement = IncidentStatement(
          statement = "Example statement",
          completed = false
        ),
        offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
        isYouthOffender = true,
        witnesses = mutableListOf(
          Witness(code = WitnessCode.OFFICER, firstName = "prison", lastName = "officer", reporter = "Fred")
        )
      )

    @BeforeEach
    fun init() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudication))
      whenever(draftAdjudicationRepository.save(any())).thenReturn(draftAdjudication)
      whenever(authenticationFacade.currentUsername).thenReturn("Fred")
    }

    @Test
    fun `add witnesses to adjudication`() {
      val response = draftAdjudicationService.setWitnesses(
        1,
        listOf(
          WitnessRequestItem(code = WitnessCode.OFFICER, firstName = "prison", lastName = "officer")
        )
      )

      val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.witnesses.size).isEqualTo(1)
      assertThat(argumentCaptor.value.witnesses.first().code).isEqualTo(WitnessCode.OFFICER)
      assertThat(argumentCaptor.value.witnesses.first().firstName).isEqualTo("prison")
      assertThat(argumentCaptor.value.witnesses.first().lastName).isEqualTo("officer")
      assertThat(argumentCaptor.value.witnesses.first().reporter).isEqualTo("Fred")
      assertThat(argumentCaptor.value.witnessesSaved).isEqualTo(true)

      assertThat(response).isNotNull
    }

    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

      assertThatThrownBy {
        draftAdjudicationService.setWitnesses(1, listOf(WitnessRequestItem(code = WitnessCode.OFFICER, firstName = "prison", lastName = "officer")))
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }
  }

  companion object {
    val now = LocalDateTime.now()

    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
    private val DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE = LocalDateTime.of(2010, 10, 14, 10, 0)
    private val REPORTED_DATE_TIME = DATE_TIME_OF_INCIDENT.plusDays(1)

    private val INCIDENT_ROLE_CODE = "25a"
    private val INCIDENT_ROLE_PARAGRAPH_NUMBER = "25(a)"
    private val INCIDENT_ROLE_PARAGRAPH_DESCRIPTION = "Attempts to commit any of the foregoing offences:"
    private val INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER = "B23456"
    private val INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME = "Associated Prisoner"

    private const val OFFENCE_CODE_2_NOMIS_CODE_ON_OWN = "5b"
    private const val OFFENCE_CODE_2_NOMIS_CODE_ASSISTED = "25z"
    private const val OFFENCE_CODE_2_PARAGRAPH_NUMBER = "5(b)"
    private const val OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION = "A paragraph description"
    private const val OFFENCE_CODE_3_NOMIS_CODE_ON_OWN = "5f"
    private const val OFFENCE_CODE_3_NOMIS_CODE_ASSISTED = "25f"
    private const val OFFENCE_CODE_3_PARAGRAPH_NUMBER = "6(a)"
    private const val OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION = "Another paragraph description"

    private const val YOUTH_OFFENCE_CODE_2_PARAGRAPH_NUMBER = "7(b)"
    private const val YOUTH_OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION = "A youth paragraph description"
    private const val YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ON_OWN = "7b"
    private const val YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ASSISTED = "29z"
    private const val YOUTH_OFFENCE_CODE_3_PARAGRAPH_NUMBER = "17(b)"
    private const val YOUTH_OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION = "Another youth paragraph description"
    private const val YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ON_OWN = "17b"
    private const val YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ASSISTED = "29f"

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
    )

    fun incidentDetails(locationId: Long, clock: Clock) = IncidentDetails(
      locationId = locationId,
      dateTimeOfIncident = LocalDateTime.now(clock),
      dateTimeOfDiscovery = LocalDateTime.now(clock).plusDays(1),
      handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
    )

    fun incidentDetails(locationId: Long, now: LocalDateTime) = IncidentDetails(
      locationId = locationId,
      dateTimeOfIncident = now,
      dateTimeOfDiscovery = now.plusDays(1),
      handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
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
      victimPrisonersNumber = FULL_OFFENCE_DETAILS_RESPONSE_DTO.victimPrisonersNumber,
      victimStaffUsername = FULL_OFFENCE_DETAILS_RESPONSE_DTO.victimStaffUsername,
      victimOtherPersonsName = FULL_OFFENCE_DETAILS_RESPONSE_DTO.victimOtherPersonsName,
    )

    private val YOUTH_OFFENCE_DETAILS_REQUEST = OffenceDetailsRequestItem(offenceCode = 2)
    private val YOUTH_OFFENCE_DETAILS_RESPONSE_DTO = OffenceDetailsDto(
      offenceCode = YOUTH_OFFENCE_DETAILS_REQUEST.offenceCode,
      offenceRule = OffenceRuleDetailsDto(
        paragraphNumber = YOUTH_OFFENCE_CODE_2_PARAGRAPH_NUMBER,
        paragraphDescription = YOUTH_OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION,
      )
    )
    private val YOUTH_OFFENCE_DETAILS_DB_ENTITY = Offence(
      offenceCode = YOUTH_OFFENCE_DETAILS_RESPONSE_DTO.offenceCode,
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
        agencyId = "MDI",
        incidentDetails = incidentDetails(2L, now),
        incidentRole = incidentRoleWithAllValuesSet(),
        offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
        incidentStatement = IncidentStatement(
          statement = "Example statement",
          completed = false
        ),
        isYouthOffender = false
      )
    draftAdjudication.createdByUserId = "A_USER" // Add audit information

    whenever(draftAdjudicationRepository.findById(any())).thenReturn(
      Optional.of(toFind.orElse(draftAdjudication))
    )

    whenever(draftAdjudicationRepository.save(any())).thenReturn(
      draftAdjudication
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
