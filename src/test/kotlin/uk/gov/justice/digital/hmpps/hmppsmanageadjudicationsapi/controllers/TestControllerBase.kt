package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.JwtAuthHelper
import java.time.LocalDateTime

@ActiveProfiles("test")
@Import(JwtAuthHelper::class)
open class TestControllerBase {
  @Autowired
  internal lateinit var mockMvc: MockMvc

  @Autowired
  internal lateinit var objectMapper: ObjectMapper

  companion object {

    val DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE = LocalDateTime.of(2010, 10, 14, 10, 0)
    const val INCIDENT_STATEMENT = "A statement"
    val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0, 0)
    val REPORTED_DATE_TIME = DATE_TIME_OF_INCIDENT.plusDays(1)
    val INCIDENT_ROLE_WITH_ALL_VALUES = IncidentRoleDto(
      "25a",
      OffenceRuleDetailsDto(
        "25(a)",
        "Commits an assault"
      ),
      "B23456",
      "Associated Prisoner",
    )

    val REPORTED_ADJUDICATION_DTO =
      ReportedAdjudicationDto(
        adjudicationNumber = 1,
        prisonerNumber = "A12345",
        bookingId = 123,
        incidentDetails = IncidentDetailsDto(
          locationId = 2,
          dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
          dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT,
          handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
        ),
        isYouthOffender = false,
        incidentRole = INCIDENT_ROLE_WITH_ALL_VALUES,
        offenceDetails =
        OffenceDto(
          offenceCode = 2,
          OffenceRuleDto(
            paragraphNumber = "3",
            paragraphDescription = "A paragraph description",
          )
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
        gender = Gender.MALE,
        outcomes = listOf(),
      )

    val INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO =
      IncidentRoleDto("25a", OffenceRuleDetailsDto("", ""), "B23456", "Associated Prisoner")

    val BASIC_OFFENCE_RESPONSE_DTO = OffenceDetailsDto(
      offenceCode = 3,
      offenceRule = OffenceRuleDetailsDto(
        paragraphNumber = "3",
        paragraphDescription = "A description"
      )
    )
    fun draftAdjudicationDto(statement: String = "test") = DraftAdjudicationDto(
      id = 1L,
      adjudicationNumber = null,
      prisonerNumber = "A12345",
      gender = Gender.MALE,
      incidentDetails = IncidentDetailsDto(
        locationId = 3,
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT.plusDays(1),
        handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
      ),
      incidentRole = INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO,
      isYouthOffender = true,
      incidentStatement = IncidentStatementDto(statement = statement),
      offenceDetails = BASIC_OFFENCE_RESPONSE_DTO,
    )
  }
}
