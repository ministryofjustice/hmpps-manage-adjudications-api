package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationWorkflowService

@WebMvcTest(
  ReportedAdjudicationWorkflowController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class ReportedAdjudicationWorkflowControllerTest : TestControllerBase() {
  @MockBean
  lateinit var adjudicationWorkflowService: AdjudicationWorkflowService

  @BeforeEach
  fun beforeEach() {
    whenever(adjudicationWorkflowService.createDraftFromReportedAdjudication(any())).thenReturn(
      DraftAdjudicationDto(
        id = 1,
        adjudicationNumber = 123L,
        prisonerNumber = "A12345",
        gender = Gender.MALE,
        incidentDetails = IncidentDetailsDto(
          locationId = 2,
          dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
          dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT.plusDays(1),
          handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
        ),
        incidentRole = IncidentRoleDto(
          roleCode = "25a",
          offenceRule = OffenceRuleDetailsDto(
            paragraphNumber = "25(a)",
            paragraphDescription = "Commits an assault",
          ),
          associatedPrisonersNumber = "B2345BB",
          associatedPrisonersName = "Associated Prisoner",
        ),
        incidentStatement = IncidentStatementDto(statement = INCIDENT_STATEMENT),
        isYouthOffender = true,
        originatingAgencyId = "MDI",
      ),
    )
  }

  @Test
  fun `responds with a unauthorised status code`() {
    makeCreateDraftFromReportedAdjudicationRequest(123).andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }

  private fun makeCreateDraftFromReportedAdjudicationRequest(
    adjudicationNumber: Long,
  ): ResultActions {
    return mockMvc
      .perform(
        MockMvcRequestBuilders.get("/reported-adjudications/$adjudicationNumber/create-draft-adjudication")
          .header("Content-Type", "application/json"),
      )
  }
}
