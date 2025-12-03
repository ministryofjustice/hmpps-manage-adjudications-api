package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.CombinedOutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OutcomeHistoryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.QuashedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReferGovReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationDomainEventType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService

@WebMvcTest(
  OutcomeController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class OutcomeControllerTest : TestControllerBase() {

  @MockitoBean
  lateinit var outcomeService: OutcomeService

  @Nested
  inner class CreateProsecution {
    @BeforeEach
    fun beforeEach() {
      whenever(
        outcomeService.createProsecution(
          anyString(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createProsecutionRequest(
        1,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createProsecutionRequest(
        1,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createProsecutionRequest(
        1,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create an prosecution`() {
      val response = REPORTED_ADJUDICATION_DTO

      whenever(outcomeService.createProsecution("1")).thenReturn(response)

      createProsecutionRequest(1)
        .andExpect(MockMvcResultMatchers.status().isCreated)

      verify(eventPublishService, atLeastOnce()).publishEvent(
        AdjudicationDomainEventType.PROSECUTION_REFERRAL_OUTCOME,
        response,
      )
    }

    private fun createProsecutionRequest(
      id: Long,
    ): ResultActions = mockMvc
      .perform(
        MockMvcRequestBuilders.post("/reported-adjudications/$id/outcome/prosecution")
          .header("Content-Type", "application/json"),
      )
  }

  @Nested
  inner class CreateNotProceed {

    @Test
    fun `responds with a unauthorised status code`() {
      createNotProceedRequest(
        1,
        NOT_PROCEED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createNotProceedRequest(
        1,
        NOT_PROCEED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createNotProceedRequest(
        1,
        NOT_PROCEED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @CsvSource("true", "false")
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create a not proceed referral outcome`(fromHearing: Boolean) {
      val response = reportedAdjudicationDto(
        status = ReportedAdjudicationStatus.NOT_PROCEED,
        hearingIdActioned = if (fromHearing) 1 else null,
        outcomes =
        listOf(
          OutcomeHistoryDto(
            outcome = CombinedOutcomeDto(
              outcome = OutcomeDto(code = OutcomeCode.REFER_POLICE),
              referralOutcome = OutcomeDto(code = OutcomeCode.NOT_PROCEED),
            ),
          ),
        ),
      )
      whenever(
        outcomeService.createNotProceed(
          anyString(),
          any(),
          any(),
          any(),
        ),
      ).thenReturn(response)

      createNotProceedRequest(
        1,
        NOT_PROCEED_REQUEST,
      )
        .andExpect(MockMvcResultMatchers.status().isCreated)

      verify(outcomeService).createNotProceed("1", NotProceedReason.NOT_FAIR, "details")
      verify(eventPublishService, atLeastOnce()).publishEvent(
        AdjudicationDomainEventType.NOT_PROCEED_REFERRAL_OUTCOME,
        response,
      )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create a not proceed outcome`() {
      val response = reportedAdjudicationDto(status = ReportedAdjudicationStatus.NOT_PROCEED)
      whenever(
        outcomeService.createNotProceed(
          anyString(),
          any(),
          any(),
          any(),
        ),
      ).thenReturn(response)

      createNotProceedRequest(
        1,
        NOT_PROCEED_REQUEST,
      )
        .andExpect(MockMvcResultMatchers.status().isCreated)

      verify(outcomeService).createNotProceed("1", NotProceedReason.NOT_FAIR, "details")
      verify(eventPublishService, atLeastOnce()).publishEvent(AdjudicationDomainEventType.NOT_PROCEED_OUTCOME, response)
    }

    private fun createNotProceedRequest(
      id: Long,
      notProceedRequest: NotProceedRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(notProceedRequest)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/outcome/not-proceed")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class CreateReferGov {
    @BeforeEach
    fun beforeEach() {
      whenever(
        outcomeService.createReferGov(
          anyString(),
          anyOrNull(),
          anyString(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createOutcomeRequest(
        1,
        REFER_GOV_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createOutcomeRequest(
        1,
        REFER_GOV_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createOutcomeRequest(
        1,
        REFER_GOV_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create a refer gov outcome`() {
      createOutcomeRequest(
        1,
        REFER_GOV_REQUEST,
      )
        .andExpect(MockMvcResultMatchers.status().isCreated)

      verify(outcomeService).createReferGov(
        chargeNumber = "1",
        referGovReason = ReferGovReason.GOV_INQUIRY,
        details = "details",
      )
      verify(eventPublishService, atLeastOnce()).publishEvent(
        AdjudicationDomainEventType.REFERRAL_OUTCOME_REFER_GOV,
        REPORTED_ADJUDICATION_DTO,
      )
    }

    private fun createOutcomeRequest(
      id: Long,
      referGovRequest: ReferralGovRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(referGovRequest)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/outcome/refer-gov")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class CreateReferPolice {
    @BeforeEach
    fun beforeEach() {
      whenever(
        outcomeService.createReferral(
          anyString(),
          any(),
          anyOrNull(),
          any(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createOutcomeRequest(
        1,
        POLICE_REFER_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createOutcomeRequest(
        1,
        POLICE_REFER_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createOutcomeRequest(
        1,
        POLICE_REFER_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create ref police outcome`() {
      createOutcomeRequest(
        1,
        POLICE_REFER_REQUEST,
      )
        .andExpect(MockMvcResultMatchers.status().isCreated)
      verify(outcomeService).createReferral(
        chargeNumber = "1",
        code = OutcomeCode.REFER_POLICE,
        details = "details",
      )
      verify(eventPublishService, atLeastOnce()).publishEvent(
        AdjudicationDomainEventType.REF_POLICE_OUTCOME,
        REPORTED_ADJUDICATION_DTO,
      )
    }

    private fun createOutcomeRequest(
      id: Long,
      policeReferralRequest: ReferralDetailsRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(policeReferralRequest)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/outcome/refer-police")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class DeleteOutcome {

    @Test
    fun `responds with a unauthorised status code`() {
      deleteOutcomeRequest(
        1,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      deleteOutcomeRequest(
        1,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      deleteOutcomeRequest(
        1,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @CsvSource("QUASHED", "NOT_PROCEED")
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to delete an outcome`(outcomeCode: OutcomeCode) {
      val response =
        reportedAdjudicationDto(status = if (outcomeCode == OutcomeCode.QUASHED) ReportedAdjudicationStatus.CHARGE_PROVED else ReportedAdjudicationStatus.UNSCHEDULED)

      whenever(
        outcomeService.deleteOutcome(
          anyString(),
          anyOrNull(),
        ),
      ).thenReturn(response)

      deleteOutcomeRequest(1)
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(outcomeService).deleteOutcome(
        "1",
      )

      verify(
        eventPublishService,
        atLeastOnce(),
      ).publishEvent(
        if (outcomeCode == OutcomeCode.QUASHED) AdjudicationDomainEventType.UNQUASHED else AdjudicationDomainEventType.NOT_PROCEED_OUTCOME_DELETED,
        response,
      )
    }

    private fun deleteOutcomeRequest(
      id: Long,
    ): ResultActions = mockMvc
      .perform(
        MockMvcRequestBuilders.delete("/reported-adjudications/$id/outcome")
          .header("Content-Type", "application/json"),
      )
  }

  @Nested
  inner class CreateQuashed {
    @BeforeEach
    fun beforeEach() {
      whenever(
        outcomeService.createQuashed(
          anyString(),
          any(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createQuashedRequest(
        1,
        QUASHED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createQuashedRequest(
        1,
        QUASHED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createQuashedRequest(
        1,
        QUASHED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create a quashed outcome`() {
      createQuashedRequest(1, QUASHED_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isCreated)

      verify(outcomeService).createQuashed("1", QUASHED_REQUEST.reason, QUASHED_REQUEST.details)
      verify(eventPublishService, atLeastOnce()).publishEvent(
        AdjudicationDomainEventType.QUASHED,
        REPORTED_ADJUDICATION_DTO,
      )
    }

    private fun createQuashedRequest(
      id: Long,
      request: QuashedRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(request)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/outcome/quashed")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class AmendOutcome {
    @BeforeEach
    fun beforeEach() {
      whenever(
        outcomeService.amendOutcomeViaApi(
          anyString(),
          any(),
          anyOrNull(),
          anyOrNull(),
          anyOrNull(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      amendOutcomeRequest(
        1,
        AMEND_REFER_POLICE_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      amendOutcomeRequest(
        1,
        AMEND_REFER_POLICE_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      amendOutcomeRequest(
        1,
        AMEND_REFER_POLICE_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to amend a not proceed outcome`() {
      amendOutcomeRequest(1, AMEND_NOT_PROCEED_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(outcomeService).amendOutcomeViaApi(
        chargeNumber = "1",
        details = "details",
        reason = AMEND_NOT_PROCEED_REQUEST.reason,
      )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to amend a refer police outcome`() {
      amendOutcomeRequest(1, AMEND_REFER_POLICE_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(outcomeService).amendOutcomeViaApi(
        chargeNumber = "1",
        details = "details",
      )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to amend a quashed outcome`() {
      amendOutcomeRequest(1, AMEND_QUASHED_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(outcomeService).amendOutcomeViaApi(
        chargeNumber = "1",
        details = "details",
        quashedReason = AMEND_QUASHED_REQUEST.quashedReason,
      )

      verify(eventPublishService, atLeastOnce()).publishEvent(
        AdjudicationDomainEventType.OUTCOME_UPDATED,
        REPORTED_ADJUDICATION_DTO,
      )
    }

    private fun amendOutcomeRequest(
      id: Long,
      request: AmendOutcomeRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(request)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$id/outcome")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  companion object {
    private val POLICE_REFER_REQUEST = ReferralDetailsRequest(details = "details")
    private val REFER_GOV_REQUEST = ReferralGovRequest(referGovReason = ReferGovReason.GOV_INQUIRY, details = "details")
    private val NOT_PROCEED_REQUEST = NotProceedRequest(reason = NotProceedReason.NOT_FAIR, details = "details")
    private val QUASHED_REQUEST = QuashedRequest(reason = QuashedReason.APPEAL_UPHELD, details = "details")
    private val AMEND_REFER_POLICE_REQUEST = AmendOutcomeRequest(details = "details")
    private val AMEND_NOT_PROCEED_REQUEST = AmendOutcomeRequest(details = "details", reason = NotProceedReason.NOT_FAIR)
    private val AMEND_QUASHED_REQUEST =
      AmendOutcomeRequest(details = "details", quashedReason = QuashedReason.JUDICIAL_REVIEW)
  }
}
