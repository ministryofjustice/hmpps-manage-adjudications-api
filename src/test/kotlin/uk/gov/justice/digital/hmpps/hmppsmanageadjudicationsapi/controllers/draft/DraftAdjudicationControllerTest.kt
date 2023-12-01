package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.ForbiddenException
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftAdjudicationService
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(
  DraftAdjudicationController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
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
          anyOrNull(),
          any(),
          any(),
          anyOrNull(),
          anyOrNull(),
        ),
      ).thenReturn(
        draftAdjudicationDto(),
      )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      startANewAdjudication().andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `calls the service to start a new adjudication for a prisoner`() {
      startANewAdjudication("A12345", "MDI", 1, DATE_TIME_OF_INCIDENT, null, INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST)
        .andExpect(status().isCreated)

      verify(draftAdjudicationService).startNewAdjudication(
        "A12345",
        Gender.MALE,
        "MDI",
        null,
        1,
        DATE_TIME_OF_INCIDENT,
        null,
      )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
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
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `returns a bad request when sent an empty body`() {
      startANewAdjudication().andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
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
        if (locationId == null && dateTimeOfIncident == null && prisonerNumber == null) {
          ""
        } else {
          objectMapper.writeValueAsString(
            mapOf(
              "prisonerNumber" to prisonerNumber,
              "gender" to Gender.MALE.name,
              "agencyId" to agencyId,
              "locationId" to locationId,
              "dateTimeOfIncident" to dateTimeOfIncident,
              "incidentRole" to incidentRole,
              "dateTimeOfDiscovery" to dateTimeOfDiscovery,
            ),
          )
        }

      return mockMvc
        .perform(
          post("/draft-adjudications")
            .header("Content-Type", "application/json")
            .content(jsonBody),
        )
    }
  }

  @Nested
  inner class DeleteDraftAdjudication {

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `responds with a unauthorised status code if authorities missing`() {
      deleteDraftAdjudication().andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "not_owner", authorities = ["SCOPE_write"])
    fun `returns status Forbidden if ForbiddenException is thrown`() {
      doThrow(ForbiddenException("Only owner can delete draft adjudication.")).`when`(draftAdjudicationService)
        .deleteDraftAdjudications(any())

      deleteDraftAdjudication()
        .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `calls the service to delete draft adjudication`() {
      val argumentCaptor = ArgumentCaptor.forClass(Long::class.java)
      doNothing().`when`(draftAdjudicationService).deleteDraftAdjudications(argumentCaptor.capture())

      deleteDraftAdjudication()
        .andExpect(status().isOk)

      assertThat(argumentCaptor.value).isEqualTo(1)
    }

    private fun deleteDraftAdjudication(
      id: Long = 1,
    ): ResultActions {
      val jsonBody = objectMapper.writeValueAsString(
        mapOf(
          "id" to id,
        ),
      )

      return mockMvc
        .perform(
          delete("/draft-adjudications/1")
            .header("Content-Type", "application/json")
            .content(jsonBody),
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
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `returns the draft adjudication for a given id`() {
      whenever(draftAdjudicationService.getDraftAdjudicationDetails(any())).thenReturn(
        DraftAdjudicationDto(
          id = 1,
          chargeNumber = null,
          prisonerNumber = "A12345",
          gender = Gender.MALE,
          incidentDetails = IncidentDetailsDto(
            locationId = 1L,
            dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
            dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT,
            handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
          ),
          incidentRole = INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO,
          isYouthOffender = true,
          originatingAgencyId = "MDI",
        ),
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
            INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO.roleCode,
          ),
        )
        .andExpect(
          jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").value(
            INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO.associatedPrisonersNumber,
          ),
        )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `returns the draft adjudication for a given id and without optional information`() {
      whenever(draftAdjudicationService.getDraftAdjudicationDetails(any())).thenReturn(
        DraftAdjudicationDto(
          id = 1,
          chargeNumber = null,
          prisonerNumber = "A12345",
          gender = Gender.MALE,
          incidentDetails = IncidentDetailsDto(
            locationId = 1L,
            dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
            dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT,
            handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
          ),
          incidentRole = INCIDENT_ROLE_WITH_NO_VALUES_RESPONSE_DTO,
          isYouthOffender = true,
          originatingAgencyId = "MDI",
        ),
      )
      makeGetDraftAdjudicationRequest(1)
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.draftAdjudication.id").isNumber)
        .andExpect(jsonPath("$.draftAdjudication.incidentRole.roleCode").doesNotExist())
        .andExpect(jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").doesNotExist())
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `responds with an not found status code`() {
      whenever(draftAdjudicationService.getDraftAdjudicationDetails(anyLong())).thenThrow(EntityNotFoundException::class.java)

      makeGetDraftAdjudicationRequest(1).andExpect(status().isNotFound)
    }

    private fun makeGetDraftAdjudicationRequest(id: Long): ResultActions {
      return mockMvc
        .perform(
          get("/draft-adjudications/$id")
            .header("Content-Type", "application/json"),
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
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `makes a call to add the incident statement to the draft adjudication`() {
      makeAddIncidentStatementRequest(1, "test")
        .andExpect(status().isCreated)

      verify(draftAdjudicationService).addIncidentStatement(1, "test", false)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `returns the draft adjudication including the new statement`() {
      makeAddIncidentStatementRequest(1, "test")
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.draftAdjudication.id").isNumber)
        .andExpect(jsonPath("$.draftAdjudication.prisonerNumber").value("A12345"))
        .andExpect(jsonPath("$.draftAdjudication.incidentStatement.statement").value("test"))
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
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
            .content(body),
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
        ),
      ).thenReturn(draftAdjudicationDto())
    }

    @Test
    fun `responds with a unauthorised status code`() {
      editIncidentDetailsRequest(
        1,
        1,
        DATE_TIME_OF_INCIDENT,
        DATE_TIME_OF_INCIDENT.plusDays(1),
        INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST,
      )
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `makes a call to edit the incident details`() {
      editIncidentDetailsRequest(
        1,
        2,
        DATE_TIME_OF_INCIDENT,
        DATE_TIME_OF_INCIDENT.plusDays(1),
        INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST,
      )
        .andExpect(status().isOk)

      verify(draftAdjudicationService).editIncidentDetails(
        1,
        2,
        DATE_TIME_OF_INCIDENT,
        DATE_TIME_OF_INCIDENT.plusDays(1),
      )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `returns the incident details`() {
      editIncidentDetailsRequest(
        1,
        2,
        DATE_TIME_OF_INCIDENT,
        DATE_TIME_OF_INCIDENT.plusDays(1),
        INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST,
      )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.draftAdjudication.id").isNumber)
        .andExpect(jsonPath("$.draftAdjudication.prisonerNumber").value("A12345"))
        .andExpect(jsonPath("$.draftAdjudication.incidentDetails.locationId").value(3))
        .andExpect(jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").value("2010-10-12T10:00:00"))
        .andExpect(jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfDiscovery").value("2010-10-13T10:00:00"))
        .andExpect(jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").value("2010-10-14T10:00:00"))
        .andExpect(
          jsonPath("$.draftAdjudication.incidentRole.roleCode").value(
            INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO.roleCode,
          ),
        )
        .andExpect(
          jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").value(
            INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO.associatedPrisonersNumber,
          ),
        )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `handles a bad request when an IllegalSateException is thrown`() {
      whenever(
        draftAdjudicationService.editIncidentDetails(
          anyLong(),
          anyLong(),
          any(),
          anyOrNull(),
        ),
      ).thenThrow(
        IllegalStateException::class.java,
      )
      editIncidentDetailsRequest(1, 2, DATE_TIME_OF_INCIDENT, null, INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST)
        .andExpect(status().isBadRequest)
    }

    private fun editIncidentDetailsRequest(
      id: Long,
      locationId: Long,
      dateTimeOfIncident: LocalDateTime?,
      dateTimeOfDiscovery: LocalDateTime?,
      incidentRole: IncidentRoleRequest?,
    ): ResultActions {
      val body =
        objectMapper.writeValueAsString(
          mapOf(
            "locationId" to locationId,
            "dateTimeOfIncident" to dateTimeOfIncident,
            "incidentRole" to incidentRole,
            "dateTimeOfDiscovery" to dateTimeOfDiscovery,
          ),
        )
      return mockMvc
        .perform(
          put("/draft-adjudications/$id/incident-details")
            .header("Content-Type", "application/json")
            .content(body),
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
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `makes a call to update the incident statement`() {
      editIncidentStatement(1, "test")
        .andExpect(status().isOk)

      verify(draftAdjudicationService).editIncidentStatement(1, "test", false)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
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
            .content(body),
        )
    }
  }

  @Nested
  inner class InProgressDraftAdjudications {
    @BeforeEach
    fun beforeEach() {
      whenever(draftAdjudicationService.getCurrentUsersInProgressDraftAdjudications(anyOrNull(), anyOrNull(), any())).thenReturn(
        PageImpl(
          listOf(
            DraftAdjudicationDto(
              id = 1,
              chargeNumber = null,
              prisonerNumber = "A12345",
              gender = Gender.MALE,
              incidentDetails = IncidentDetailsDto(
                locationId = 1,
                dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
                dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT,
                handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
              ),
              incidentRole = INCIDENT_ROLE_WITH_ALL_VALUES_RESPONSE_DTO,
              isYouthOffender = true,
              originatingAgencyId = "MDI",
            ),
            DraftAdjudicationDto(
              id = 2,
              chargeNumber = null,
              prisonerNumber = "A12346",
              gender = Gender.MALE,
              incidentDetails = IncidentDetailsDto(
                locationId = 2,
                dateTimeOfIncident = DATE_TIME_OF_INCIDENT.plusMonths(1),
                dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT.plusMonths(1),
                handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE.plusMonths(1),
              ),
              incidentRole = INCIDENT_ROLE_WITH_NO_VALUES_RESPONSE_DTO,
              isYouthOffender = true,
              originatingAgencyId = "MDI",
            ),
          ),
          PageRequest.of(0, 20, Sort.by("IncidentDetailsDateTimeOfDiscovery").descending()),
          2,
        ),
      )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      getInProgressDraftAdjudications()
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `makes a call to return all my in progress draft adjudications`() {
      getInProgressDraftAdjudications()
        .andExpect(status().isOk)

      verify(draftAdjudicationService).getCurrentUsersInProgressDraftAdjudications(
        LocalDate.now().minusWeeks(1),
        LocalDate.now(),
        PageRequest.of(0, 20, Sort.by("IncidentDetailsDateTimeOfDiscovery").descending()),
      )
    }

    private fun getInProgressDraftAdjudications(): ResultActions = mockMvc
      .perform(
        get("/draft-adjudications/my-reports")
          .header("Content-Type", "application/json"),
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
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
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
            .content(body),
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
        ),
      ).thenReturn(draftAdjudicationDto())
    }

    @Test
    fun `responds with a unauthorised status code`() {
      editIncidentRoleRequest(1, INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST)
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
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
      incidentRole: IncidentRoleRequest?,
    ): ResultActions {
      val body =
        objectMapper.writeValueAsString(
          mapOf(
            "incidentRole" to incidentRole,
            "removeExistingOffences" to false,
          ),
        )
      return mockMvc
        .perform(
          put("/draft-adjudications/$id/incident-role")
            .header("Content-Type", "application/json")
            .content(body),
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
        ),
      ).thenReturn(draftAdjudicationDto())
    }

    @Test
    fun `responds with a unauthorised status code`() {
      setAssociatedPrisonerRequest(1, ASSOCIATED_PRISONER_WITH_ALL_VALUES_REQUEST)
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
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
      associatedPrisoner: IncidentRoleAssociatedPrisonerRequest?,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(associatedPrisoner)
      return mockMvc
        .perform(
          put("/draft-adjudications/$id/associated-prisoner")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class AmendGender {

    @BeforeEach
    fun beforeEach() {
      whenever(
        draftAdjudicationService.setGender(
          anyLong(),
          any(),
        ),
      ).thenReturn(draftAdjudicationDto())
    }

    @Test
    fun `responds with a unauthorised status code`() {
      setGenderRequest(1, GENDER_REQUEST)
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `makes a call to set the gender`() {
      setGenderRequest(1, GENDER_REQUEST)
        .andExpect(status().isOk)

      verify(draftAdjudicationService).setGender(
        1,
        GENDER_REQUEST.gender,
      )
    }

    private fun setGenderRequest(
      id: Long,
      genderRequest: GenderRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(genderRequest)
      return mockMvc
        .perform(
          put("/draft-adjudications/$id/gender")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class SetCreatedOnBehalfOf {

    @BeforeEach
    fun beforeEach() {
      whenever(
        draftAdjudicationService.setCreatedOnBehalfOf(
          anyLong(),
          anyString(),
          anyString(),
        ),
      ).thenReturn(draftAdjudicationDto())
    }

    @Test
    fun `responds with a unauthorised status code`() {
      setCreatedOnBehalfOfRequest(1, "offender", "some reason")
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `makes a call to set the created on behalf of`() {
      setCreatedOnBehalfOfRequest(1, "offender", "some reason")
        .andExpect(status().isOk)

      verify(draftAdjudicationService).setCreatedOnBehalfOf(
        1,
        "offender",
        "some reason",
      )
    }

    private fun setCreatedOnBehalfOfRequest(
      id: Long,
      createdOnBehalfOfOfficer: String,
      createdOnBehalfOfReason: String,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(
        mapOf(
          "createdOnBehalfOfOfficer" to createdOnBehalfOfOfficer,
          "createdOnBehalfOfReason" to createdOnBehalfOfReason,
        ),
      )
      return mockMvc
        .perform(
          put("/draft-adjudications/$id/created-on-behalf-of")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0, 0)
    private val DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE = LocalDateTime.of(2010, 10, 14, 10, 0)

    private val INCIDENT_ROLE_WITH_ALL_VALUES_REQUEST = IncidentRoleRequest("25a")

    private val INCIDENT_ROLE_WITH_NO_VALUES_RESPONSE_DTO = IncidentRoleDto(null, null, null, null)

    private val ASSOCIATED_PRISONER_WITH_ALL_VALUES_REQUEST =
      IncidentRoleAssociatedPrisonerRequest("B23456", "Associated Prisoner")
    private val GENDER_REQUEST = GenderRequest(Gender.MALE)
  }
}
