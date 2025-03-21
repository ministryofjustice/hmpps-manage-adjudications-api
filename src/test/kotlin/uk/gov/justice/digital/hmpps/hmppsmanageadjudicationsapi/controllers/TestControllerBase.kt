package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.ResourceServerConfiguration
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OutcomeHistoryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.EventPublishService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.JwtAuthHelper
import java.time.LocalDateTime
import java.util.*

@ActiveProfiles("test")
@Import(JwtAuthHelper::class, ResourceServerConfiguration::class)
open class TestControllerBase {
  @Autowired
  internal lateinit var mockMvc: MockMvc

  @Autowired
  internal lateinit var objectMapper: ObjectMapper

  @MockitoBean
  lateinit var eventPublishService: EventPublishService

  companion object {

    val DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE = LocalDateTime.of(2010, 10, 14, 10, 0)
    const val INCIDENT_STATEMENT = "A statement"
    val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0, 0)
    private val REPORTED_DATE_TIME = DATE_TIME_OF_INCIDENT.plusDays(1)
    private val INCIDENT_ROLE_WITH_ALL_VALUES = IncidentRoleDto(
      "25a",
      OffenceRuleDetailsDto(
        "25(a)",
        "Commits an assault",
      ),
      "B23456",
      "Associated Prisoner",
    )

    val REPORTED_ADJUDICATION_DTO =
      ReportedAdjudicationDto(
        chargeNumber = "1",
        prisonerNumber = "A12345",
        incidentDetails = IncidentDetailsDto(
          locationId = 2,
          locationUuid = UUID.fromString("0194ac90-2def-7c63-9f46-b3ccc911fdff"),
          dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
          dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT,
          handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
        ),
        isYouthOffender = false,
        incidentRole = INCIDENT_ROLE_WITH_ALL_VALUES,
        offenceDetails =
        OffenceDto(
          offenceCode = 2,
          protectedCharacteristics = emptyList(),
          offenceRule = OffenceRuleDto(
            paragraphNumber = "3",
            paragraphDescription = "A paragraph description",
            nomisCode = null,
            withOthersNomisCode = null,
          ),
        ),
        incidentStatement = IncidentStatementDto(statement = INCIDENT_STATEMENT),
        createdByUserId = "A_SMITH",
        createdDateTime = REPORTED_DATE_TIME,
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
        reviewedByUserId = null,
        statusReason = null,
        statusDetails = null,
        damages = listOf(),
        evidence = listOf(),
        witnesses = listOf(),
        hearings = listOf(),
        disIssueHistory = listOf(),
        gender = Gender.MALE,
        outcomes = listOf(),
        punishments = mutableListOf(),
        punishmentComments = listOf(),
        overrideAgencyId = null,
        originatingAgencyId = "MDI",
        linkedChargeNumbers = emptyList(),
      )

    fun reportedAdjudicationDto(
      status: ReportedAdjudicationStatus,
      hearingIdActioned: Long? = null,
      outcomes: List<OutcomeHistoryDto> = emptyList(),
    ) = ReportedAdjudicationDto(
      chargeNumber = "1",
      prisonerNumber = "A12345",
      incidentDetails = IncidentDetailsDto(
        locationId = 2,
        locationUuid = UUID.fromString("0194ac90-2def-7c63-9f46-b3ccc911fdff"),
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT,
        handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
      ),
      isYouthOffender = false,
      incidentRole = INCIDENT_ROLE_WITH_ALL_VALUES,
      offenceDetails =
      OffenceDto(
        offenceCode = 2,
        protectedCharacteristics = emptyList(),
        offenceRule = OffenceRuleDto(
          paragraphNumber = "3",
          paragraphDescription = "A paragraph description",
          nomisCode = null,
          withOthersNomisCode = null,
        ),
      ),
      incidentStatement = IncidentStatementDto(statement = INCIDENT_STATEMENT),
      createdByUserId = "A_SMITH",
      createdDateTime = REPORTED_DATE_TIME,
      status = status,
      reviewedByUserId = null,
      statusReason = null,
      statusDetails = null,
      damages = listOf(),
      evidence = listOf(),
      witnesses = listOf(),
      hearings = listOf(),
      disIssueHistory = listOf(),
      gender = Gender.MALE,
      outcomes = outcomes,
      punishments = mutableListOf(),
      punishmentComments = listOf(),
      overrideAgencyId = null,
      originatingAgencyId = "MDI",
      hearingIdActioned = hearingIdActioned,
      linkedChargeNumbers = emptyList(),
    )

    val INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO =
      IncidentRoleDto("25a", OffenceRuleDetailsDto("", ""), "B23456", "Associated Prisoner")

    val BASIC_OFFENCE_RESPONSE_DTO = OffenceDetailsDto(
      offenceCode = 3,
      protectedCharacteristics = emptyList(),
      offenceRule = OffenceRuleDetailsDto(
        paragraphNumber = "3",
        paragraphDescription = "A description",
      ),
    )

    fun draftAdjudicationDto(statement: String = "test") = DraftAdjudicationDto(
      id = 1L,
      chargeNumber = null,
      prisonerNumber = "A12345",
      gender = Gender.MALE,
      incidentDetails = IncidentDetailsDto(
        locationId = 3,
        locationUuid = UUID.fromString("0194ac91-0968-75b1-b304-73e905ab934d"),
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT.plusDays(1),
        handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
      ),
      incidentRole = INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO,
      isYouthOffender = true,
      incidentStatement = IncidentStatementDto(statement = statement),
      offenceDetails = BASIC_OFFENCE_RESPONSE_DTO,
      originatingAgencyId = "MDI",
    )
  }
}
