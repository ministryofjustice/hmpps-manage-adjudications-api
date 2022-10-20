package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.DraftAdjudicationService
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException

@WebMvcTest(value = [DraftAdjudicationController::class])
class DraftAdjudicationControllerTest : TestControllerBase() {

  @MockBean
  lateinit var draftAdjudicationService: DraftAdjudicationService

  @Nested
  inner class StartDraftAdjudications {
    @BeforeEach
    fun beforeEach() {
      whenever(
        draftAdjudicationService.startNewAdjudication(
          any(),
          any(),
          any(),
          any(),
          anyOrNull(),
        )
      ).thenReturn(
        draftAdjudicationDto()
      )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      startANewAdjudication().andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `calls the service to start a new adjudication for a prisoner`() {
      startANewAdjudication("A12345", "MDI", 1, DATE_TIME_OF_INCIDENT, null, INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST)
        .andExpect(status().isCreated)

      verify(draftAdjudicationService).startNewAdjudication(
        "A12345",
        "MDI",
        1,
        DATE_TIME_OF_INCIDENT,
        null
      )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `returns the newly created draft adjudication`() {
      startANewAdjudication("A12345", "MDI", 1, DATE_TIME_OF_INCIDENT, null, INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST)
        .andExpect(status().isCreated)
        .andExpect(jsonPath("draftAdjudication.id").isNumber)
        .andExpect(jsonPath("draftAdjudication.adjudicationNumber").doesNotExist())
        .andExpect(jsonPath("draftAdjudication.prisonerNumber").value("A12345"))
        .andExpect(jsonPath("draftAdjudication.incidentDetails.locationId").value(3))
        .andExpect(jsonPath("draftAdjudication.incidentDetails.dateTimeOfIncident").value("2010-10-12T10:00:00"))
        .andExpect(jsonPath("draftAdjudication.incidentDetails.handoverDeadline").value("2010-10-14T10:00:00"))
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `returns a bad request when sent an empty body`() {
      startANewAdjudication().andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `returns a bad request when required fields are missing`() {
      startANewAdjudication(prisonerNumber = "A12345").andExpect(status().isBadRequest)
    }

    private fun startANewAdjudication(
      prisonerNumber: String? = null,
      agencyId: String = "MDI",
      locationId: Long? = null,
      dateTimeOfIncident: LocalDateTime? = null,
      dateTimeOfDiscovery: LocalDateTime? = null,
      incidentRole: IncidentRoleRequest? = null,
    ): ResultActions {
      val jsonBody =
        if (locationId == null && dateTimeOfIncident == null && prisonerNumber == null) "" else objectMapper.writeValueAsString(
          mapOf(
            "prisonerNumber" to prisonerNumber,
            "agencyId" to agencyId,
            "locationId" to locationId,
            "dateTimeOfIncident" to dateTimeOfIncident,
            "incidentRole" to incidentRole,
            "dateTimeOfDiscovery" to dateTimeOfDiscovery,
          )
        )

      return mockMvc
        .perform(
          post("/draft-adjudications")
            .header("Content-Type", "application/json")
            .content(jsonBody)
        )
    }
  }

  @Nested
  inner class DraftAdjudicationDetails {
    @Test
    fun `responds with a unauthorised status code`() {
      makeGetDraftAdjudicationRequest(1)
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `returns the draft adjudication for a given id`() {
      whenever(draftAdjudicationService.getDraftAdjudicationDetails(any())).thenReturn(
        DraftAdjudicationDto(
          id = 1,
          adjudicationNumber = null,
          prisonerNumber = "A12345",
          incidentDetails = IncidentDetailsDto(
            locationId = 1L,
            dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
            dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT,
            handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
          ),
          incidentRole = INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO,
          isYouthOffender = true
        )
      )
      makeGetDraftAdjudicationRequest(1)
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.draftAdjudication.id").isNumber)
        .andExpect(jsonPath("$.draftAdjudication.prisonerNumber").value("A12345"))
        .andExpect(jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").value("2010-10-12T10:00:00"))
        .andExpect(jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").value("2010-10-14T10:00:00"))
        .andExpect(jsonPath("$.draftAdjudication.incidentDetails.locationId").value(1))
        .andExpect(
          jsonPath("$.draftAdjudication.incidentRole.roleCode").value(
            INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO.roleCode
          )
        )
        .andExpect(
          jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").value(
            INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO.associatedPrisonersNumber
          )
        )
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `returns the draft adjudication for a given id and without optional information`() {
      whenever(draftAdjudicationService.getDraftAdjudicationDetails(any())).thenReturn(
        DraftAdjudicationDto(
          id = 1,
          adjudicationNumber = null,
          prisonerNumber = "A12345",
          incidentDetails = IncidentDetailsDto(
            locationId = 1L,
            dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
            dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT,
            handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
          ),
          incidentRole = INCIDENT_ROLE_WITH_NO_VALUES_RESPONSE_DTO,
          isYouthOffender = true
        )
      )
      makeGetDraftAdjudicationRequest(1)
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.draftAdjudication.id").isNumber)
        .andExpect(jsonPath("$.draftAdjudication.incidentRole.roleCode").doesNotExist())
        .andExpect(jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").doesNotExist())
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `responds with an not found status code`() {
      whenever(draftAdjudicationService.getDraftAdjudicationDetails(anyLong())).thenThrow(EntityNotFoundException::class.java)

      makeGetDraftAdjudicationRequest(1).andExpect(status().isNotFound)
    }

    private fun makeGetDraftAdjudicationRequest(id: Long): ResultActions {
      return mockMvc
        .perform(
          get("/draft-adjudications/$id")
            .header("Content-Type", "application/json")
        )
    }
  }

  @Nested
  inner class SetOffenceDetails {
    @BeforeEach
    fun beforeEach() {
      whenever(draftAdjudicationService.setOffenceDetails(anyLong(), any())).thenReturn(
        draftAdjudicationDto()
      )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      makeSetOffenceDetailsRequest(1, BASIC_OFFENCE_REQUEST)
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `makes a call to set the offence details to the draft adjudication`() {
      makeSetOffenceDetailsRequest(1, BASIC_OFFENCE_REQUEST)
        .andExpect(status().isCreated)

      verify(draftAdjudicationService).setOffenceDetails(
        1,
        listOf(
          OffenceDetailsRequestItem(
            offenceCode = BASIC_OFFENCE_REQUEST.offenceCode,
          )
        )
      )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `returns the draft adjudication including the new offence details`() {
      makeSetOffenceDetailsRequest(1, BASIC_OFFENCE_REQUEST)
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.draftAdjudication.id").isNumber)
        .andExpect(jsonPath("$.draftAdjudication.prisonerNumber").value("A12345"))
        .andExpect(jsonPath("$.draftAdjudication.offenceDetails[0].offenceCode").value(BASIC_OFFENCE_RESPONSE_DTO.offenceCode))
    }

    private fun makeSetOffenceDetailsRequest(id: Long, offenceDetails: OffenceDetailsRequestItem): ResultActions {
      val body = objectMapper.writeValueAsString(mapOf("offenceDetails" to listOf(offenceDetails)))

      return mockMvc
        .perform(
          put("/draft-adjudications/$id/offence-details")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class AddIncidentStatement {
    @BeforeEach
    fun beforeEach() {
      whenever(draftAdjudicationService.addIncidentStatement(anyLong(), any(), any()))
        .thenReturn(draftAdjudicationDto())
    }

    @Test
    fun `responds with a unauthorised status code`() {
      makeAddIncidentStatementRequest(1, "test")
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `makes a call to add the incident statement to the draft adjudication`() {
      makeAddIncidentStatementRequest(1, "test")
        .andExpect(status().isCreated)

      verify(draftAdjudicationService).addIncidentStatement(1, "test", false)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `returns the draft adjudication including the new statement`() {
      makeAddIncidentStatementRequest(1, "test")
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.draftAdjudication.id").isNumber)
        .andExpect(jsonPath("$.draftAdjudication.prisonerNumber").value("A12345"))
        .andExpect(jsonPath("$.draftAdjudication.incidentStatement.statement").value("test"))
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `returns a bad request when the maximum statement length has been exceeded`() {
      val largeStatement = IntRange(0, 40000).joinToString("") { "A" }

      makeAddIncidentStatementRequest(1, largeStatement)
        .andExpect(status().isBadRequest)
        .andExpect(jsonPath("$.userMessage").value("The incident statement exceeds the maximum character limit of 4000"))
    }

    private fun makeAddIncidentStatementRequest(id: Long, statement: String? = null): ResultActions {
      val body = if (statement == null) "" else objectMapper.writeValueAsString(mapOf("statement" to statement))

      return mockMvc
        .perform(
          post("/draft-adjudications/$id/incident-statement")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class EditIncidentDetails {
    @BeforeEach
    fun beforeEach() {
      whenever(
        draftAdjudicationService.editIncidentDetails(
          anyLong(),
          anyLong(),
          any(),
          anyOrNull(),
        )
      ).thenReturn(draftAdjudicationDto())
    }

    @Test
    fun `responds with a unauthorised status code`() {
      editIncidentDetailsRequest(1, 1, DATE_TIME_OF_INCIDENT, DATE_TIME_OF_INCIDENT.plusDays(1), INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST)
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `makes a call to edit the incident details`() {
      editIncidentDetailsRequest(1, 2, DATE_TIME_OF_INCIDENT, DATE_TIME_OF_INCIDENT.plusDays(1), INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST)
        .andExpect(status().isOk)

      verify(draftAdjudicationService).editIncidentDetails(
        1,
        2,
        DATE_TIME_OF_INCIDENT,
        DATE_TIME_OF_INCIDENT.plusDays(1)
      )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `returns the incident details`() {
      editIncidentDetailsRequest(1, 2, DATE_TIME_OF_INCIDENT, DATE_TIME_OF_INCIDENT.plusDays(1), INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST)
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.draftAdjudication.id").isNumber)
        .andExpect(jsonPath("$.draftAdjudication.prisonerNumber").value("A12345"))
        .andExpect(jsonPath("$.draftAdjudication.incidentDetails.locationId").value(3))
        .andExpect(jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").value("2010-10-12T10:00:00"))
        .andExpect(jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfDiscovery").value("2010-10-13T10:00:00"))
        .andExpect(jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").value("2010-10-14T10:00:00"))
        .andExpect(
          jsonPath("$.draftAdjudication.incidentRole.roleCode").value(
            INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO.roleCode
          )
        )
        .andExpect(
          jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").value(
            INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO.associatedPrisonersNumber
          )
        )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `handles a bad request when an IllegalSateException is thrown`() {
      whenever(
        draftAdjudicationService.editIncidentDetails(
          anyLong(),
          anyLong(),
          any(),
          anyOrNull(),
        )
      ).thenThrow(
        IllegalStateException::class.java
      )
      editIncidentDetailsRequest(1, 2, DATE_TIME_OF_INCIDENT, null, INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST)
        .andExpect(status().isBadRequest)
    }

    private fun editIncidentDetailsRequest(
      id: Long,
      locationId: Long,
      dateTimeOfIncident: LocalDateTime?,
      dateTimeOfDiscovery: LocalDateTime?,
      incidentRole: IncidentRoleRequest?
    ): ResultActions {
      val body =
        objectMapper.writeValueAsString(
          mapOf(
            "locationId" to locationId,
            "dateTimeOfIncident" to dateTimeOfIncident,
            "incidentRole" to incidentRole,
            "dateTimeOfDiscovery" to dateTimeOfDiscovery,
          )
        )
      return mockMvc
        .perform(
          put("/draft-adjudications/$id/incident-details")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class EditIncidentStatement {

    @BeforeEach
    fun beforeEach() {
      whenever(draftAdjudicationService.editIncidentStatement(anyLong(), any(), any()))
        .thenReturn(draftAdjudicationDto("new statement"))
    }

    @Test
    fun `responds with a unauthorised status code`() {
      editIncidentStatement(1, "test")
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `makes a call to update the incident statement`() {
      editIncidentStatement(1, "test")
        .andExpect(status().isOk)

      verify(draftAdjudicationService).editIncidentStatement(1, "test", false)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `returns the incident statement`() {
      editIncidentStatement(1, "new statement")
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.draftAdjudication.id").isNumber)
        .andExpect(jsonPath("$.draftAdjudication.prisonerNumber").value("A12345"))
        .andExpect(jsonPath("$.draftAdjudication.incidentStatement.statement").value("new statement"))
    }

    private fun editIncidentStatement(id: Long, statement: String): ResultActions {
      val body = objectMapper.writeValueAsString(mapOf("statement" to statement))
      return mockMvc
        .perform(
          put("/draft-adjudications/$id/incident-statement")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class CompleteDraftAdjudication {
    @Test
    fun `responds with a unauthorised status code`() {
      completeDraftAdjudication(1)
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `make a call to complete a draft adjudication`() {
      completeDraftAdjudication(1)
        .andExpect(status().isCreated)

      verify(adjudicationWorkflowService).completeDraftAdjudication(1)
    }

    fun completeDraftAdjudication(id: Long): ResultActions = mockMvc
      .perform(
        post("/draft-adjudications/$id/complete-draft-adjudication")
          .header("Content-Type", "application/json")
      )
  }

  @Nested
  inner class InProgressDraftAdjudications {
    @BeforeEach
    fun beforeEach() {
      whenever(draftAdjudicationService.getCurrentUsersInProgressDraftAdjudications(any())).thenReturn(
        listOf(
          DraftAdjudicationDto(
            id = 1,
            adjudicationNumber = null,
            prisonerNumber = "A12345",
            incidentDetails = IncidentDetailsDto(
              locationId = 1,
              dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
              dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT,
              handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
            ),
            incidentRole = INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO,
            isYouthOffender = true
          ),
          DraftAdjudicationDto(
            id = 2,
            adjudicationNumber = null,
            prisonerNumber = "A12346",
            incidentDetails = IncidentDetailsDto(
              locationId = 2,
              dateTimeOfIncident = DATE_TIME_OF_INCIDENT.plusMonths(1),
              dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT.plusMonths(1),
              handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE.plusMonths(1)
            ),
            incidentRole = INCIDENT_ROLE_WITH_NO_VALUES_RESPONSE_DTO,
            isYouthOffender = true
          )
        )
      )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      getInProgressDraftAdjudications()
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `makes a call to return all mny in progress draft adjudications`() {
      getInProgressDraftAdjudications()
        .andExpect(status().isOk)

      verify(draftAdjudicationService).getCurrentUsersInProgressDraftAdjudications("MDI")
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `returns all in progress draft adjudications`() {
      getInProgressDraftAdjudications()
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.draftAdjudications[0].id").value(1))
        .andExpect(jsonPath("$.draftAdjudications[0].prisonerNumber").value("A12345"))
        .andExpect(jsonPath("$.draftAdjudications[0].incidentDetails.locationId").value(1))
        .andExpect(jsonPath("$.draftAdjudications[0].incidentDetails.dateTimeOfIncident").value("2010-10-12T10:00:00"))
        .andExpect(jsonPath("$.draftAdjudications[0].incidentDetails.handoverDeadline").value("2010-10-14T10:00:00"))
        .andExpect(
          jsonPath("$.draftAdjudications[0].incidentRole.roleCode").value(
            INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO.roleCode
          )
        )
        .andExpect(
          jsonPath("$.draftAdjudications[0].incidentRole.associatedPrisonersNumber").value(
            INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO.associatedPrisonersNumber
          )
        )
        .andExpect(jsonPath("$.draftAdjudications[1].id").value(2))
        .andExpect(jsonPath("$.draftAdjudications[1].prisonerNumber").value("A12346"))
    }

    fun getInProgressDraftAdjudications(): ResultActions = mockMvc
      .perform(
        get("/draft-adjudications/my/agency/MDI")
          .header("Content-Type", "application/json")
      )
  }

  @Nested
  inner class SetApplicableRules {

    @BeforeEach
    fun beforeEach() {
      whenever(draftAdjudicationService.setIncidentApplicableRule(anyLong(), anyBoolean(), anyBoolean()))
        .thenReturn(draftAdjudicationDto())
    }

    @Test
    fun `responds with a unauthorised status code`() {
      setApplicableRules(1, false)
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `makes a call to set the applicable rule`() {
      setApplicableRules(1, true)
        .andExpect(status().isOk)

      verify(draftAdjudicationService).setIncidentApplicableRule(1, true, false)
    }

    private fun setApplicableRules(id: Long, isYoungOffender: Boolean): ResultActions {
      val body = objectMapper.writeValueAsString(mapOf("isYouthOffenderRule" to isYoungOffender))
      return mockMvc
        .perform(
          put("/draft-adjudications/$id/applicable-rules")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class EditIncidentRole {
    @BeforeEach
    fun beforeEach() {
      whenever(
        draftAdjudicationService.editIncidentRole(
          anyLong(),
          any(),
          anyBoolean(),
        )
      ).thenReturn(draftAdjudicationDto())
    }

    @Test
    fun `responds with a unauthorised status code`() {
      editIncidentRoleRequest(1, INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST)
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `makes a call to edit the incident role`() {
      editIncidentRoleRequest(1, INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST)
        .andExpect(status().isOk)

      verify(draftAdjudicationService).editIncidentRole(
        1,
        INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST,
        false,
      )
    }

    private fun editIncidentRoleRequest(
      id: Long,
      incidentRole: IncidentRoleRequest?
    ): ResultActions {
      val body =
        objectMapper.writeValueAsString(
          mapOf(
            "incidentRole" to incidentRole,
            "removeExistingOffences" to false
          )
        )
      return mockMvc
        .perform(
          put("/draft-adjudications/$id/incident-role")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class SetAssociatedPrisoner {
    @BeforeEach
    fun beforeEach() {
      whenever(
        draftAdjudicationService.setIncidentRoleAssociatedPrisoner(
          anyLong(),
          any(),
        )
      ).thenReturn(draftAdjudicationDto())
    }

    @Test
    fun `responds with a unauthorised status code`() {
      setAssociatedPrisonerRequest(1, ASSOCIATED_PRISONER_WITH_ALL_VALUES_REQUEST)
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `makes a call to set the associated prisoner information`() {
      setAssociatedPrisonerRequest(1, ASSOCIATED_PRISONER_WITH_ALL_VALUES_REQUEST)
        .andExpect(status().isOk)

      verify(draftAdjudicationService).setIncidentRoleAssociatedPrisoner(
        1,
        ASSOCIATED_PRISONER_WITH_ALL_VALUES_REQUEST,
      )
    }

    private fun setAssociatedPrisonerRequest(
      id: Long,
      associatedPrisoner: IncidentRoleAssociatedPrisonerRequest?
    ): ResultActions {
      val body = objectMapper.writeValueAsString(associatedPrisoner)
      return mockMvc
        .perform(
          put("/draft-adjudications/$id/associated-prisoner")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class Damages {
    @BeforeEach
    fun beforeEach() {
      whenever(
        draftAdjudicationService.setDamages(
          anyLong(),
          any(),
        )
      ).thenReturn(draftAdjudicationDto())
    }

    @Test
    fun `responds with a unauthorised status code`() {
      setDamagesRequest(1, DAMAGES_REQUEST).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `makes a call to set the damages`() {
      setDamagesRequest(1, DAMAGES_REQUEST)
        .andExpect(status().isCreated)

      verify(draftAdjudicationService).setDamages(1, DAMAGES_REQUEST.damages)
    }

    private fun setDamagesRequest(
      id: Long,
      damages: DamagesRequest?
    ): ResultActions {
      val body = objectMapper.writeValueAsString(damages)
      return mockMvc
        .perform(
          put("/draft-adjudications/$id/damages")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class Evidence {
    @BeforeEach
    fun beforeEach() {
      whenever(
        draftAdjudicationService.setEvidence(
          anyLong(),
          any(),
        )
      ).thenReturn(draftAdjudicationDto())
    }

    @Test
    fun `responds with a unauthorised status code`() {
      setEvidenceRequest(1, EVIDENCE_REQUEST).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `makes a call to update the evidence`() {
      setEvidenceRequest(1, EVIDENCE_REQUEST)
        .andExpect(status().isCreated)

      verify(draftAdjudicationService).setEvidence(1, EVIDENCE_REQUEST.evidence)
    }

    private fun setEvidenceRequest(
      id: Long,
      evidence: EvidenceRequest?
    ): ResultActions {
      val body = objectMapper.writeValueAsString(evidence)
      return mockMvc
        .perform(
          put("/draft-adjudications/$id/evidence")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class Witnesses {
    @BeforeEach
    fun beforeEach() {
      whenever(
        draftAdjudicationService.setWitnesses(
          anyLong(),
          any(),
        )
      ).thenReturn(draftAdjudicationDto())
    }

    @Test
    fun `responds with a unauthorised status code`() {
      setWitnessesRequest(1, WITNESSES_REQUEST).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `makes a call to update the witnesses`() {
      setWitnessesRequest(1, WITNESSES_REQUEST)
        .andExpect(status().isCreated)

      verify(draftAdjudicationService).setWitnesses(1, WITNESSES_REQUEST.witnesses)
    }

    private fun setWitnessesRequest(
      id: Long,
      witnesses: WitnessesRequest?
    ): ResultActions {
      val body = objectMapper.writeValueAsString(witnesses)
      return mockMvc
        .perform(
          put("/draft-adjudications/$id/witnesses")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0, 0)
    private val DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE = LocalDateTime.of(2010, 10, 14, 10, 0)

    private val INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST = IncidentRoleRequest("25a")
    private val INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO =
      IncidentRoleDto("25a", OffenceRuleDetailsDto("", ""), "B23456", "Associated Prisoner")

    private val INCIDENT_ROLE_WITH_NO_VALUES_RESPONSE_DTO = IncidentRoleDto(null, null, null, null)

    private val ASSOCIATED_PRISONER_WITH_ALL_VALUES_REQUEST = IncidentRoleAssociatedPrisonerRequest("B23456", "Associated Prisoner")
    private val DAMAGES_REQUEST = DamagesRequest(listOf(DamageRequestItem(DamageCode.CLEANING, "details")))
    private val EVIDENCE_REQUEST = EvidenceRequest(listOf(EvidenceRequestItem(code = EvidenceCode.PHOTO, details = "details")))
    private val WITNESSES_REQUEST = WitnessesRequest(listOf(WitnessRequestItem(code = WitnessCode.OFFICER, firstName = "prison", lastName = "officer")))
    private val BASIC_OFFENCE_REQUEST = OffenceDetailsRequestItem(offenceCode = 3)
    private val BASIC_OFFENCE_RESPONSE_DTO = OffenceDetailsDto(
      offenceCode = 3,
      offenceRule = OffenceRuleDetailsDto(
        paragraphNumber = "3",
        paragraphDescription = "A description"
      )
    )
    private fun draftAdjudicationDto(statement: String = "test") = DraftAdjudicationDto(
      id = 1L,
      adjudicationNumber = null,
      prisonerNumber = "A12345",
      incidentDetails = IncidentDetailsDto(
        locationId = 3,
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT.plusDays(1),
        handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
      ),
      incidentRole = INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO,
      isYouthOffender = true,
      incidentStatement = IncidentStatementDto(statement = statement),
      offenceDetails = listOf(BASIC_OFFENCE_RESPONSE_DTO),
    )
  }
}
