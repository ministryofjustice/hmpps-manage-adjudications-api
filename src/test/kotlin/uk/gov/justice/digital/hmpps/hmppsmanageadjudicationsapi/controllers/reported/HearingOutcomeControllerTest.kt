package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationDomainEventType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.AmendHearingOutcomeService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.CompletedHearingService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingOutcomeService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReferralService

@WebMvcTest(
  HearingOutcomeController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class HearingOutcomeControllerTest : TestControllerBase() {

  @MockitoBean
  lateinit var hearingOutcomeService: HearingOutcomeService

  @MockitoBean
  lateinit var referralService: ReferralService

  @MockitoBean
  lateinit var amendHearingOutcomeService: AmendHearingOutcomeService

  @MockitoBean
  lateinit var completedHearingService: CompletedHearingService

  @Nested
  inner class CreateReferral {

    @Test
    fun `responds with a unauthorised status code`() {
      createReferralRequest(
        1,
        referralRequest(),
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createReferralRequest(
        1,
        referralRequest(),
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @CsvSource("ADJOURN", "COMPLETE")
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `returns bad request if not a referral `(code: HearingOutcomeCode) {
      createReferralRequest(
        1,
        referralRequest(code),
      ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @ParameterizedTest
    @CsvSource("REFER_POLICE", "REFER_INAD", "REFER_GOV")
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create a hearing outcome`(code: HearingOutcomeCode) {
      val response = reportedAdjudicationDto(status = code.outcomeCode?.status!!)
      whenever(
        referralService.createReferral(
          ArgumentMatchers.anyString(),
          any(),
          any(),
          anyOrNull(),
          any(),
          any(),
        ),
      ).thenReturn(response)

      createReferralRequest(1, referralRequest(code))
        .andExpect(MockMvcResultMatchers.status().isCreated)
      verify(referralService).createReferral(
        chargeNumber = "1",
        code = code,
        adjudicator = "test",
        details = "details",
      )
      verify(eventPublishService, atLeastOnce()).publishEvent(
        AdjudicationDomainEventType.HEARING_REFERRAL_CREATED,
        response,
      )
    }

    private fun createReferralRequest(
      id: Long,
      referralRequest: ReferralRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(referralRequest)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/hearing/outcome/referral")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class RemoveReferral {

    @Test
    fun `responds with a unauthorised status code`() {
      removeReferralRequest(1).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      removeReferralRequest(1).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      removeReferralRequest(1).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @CsvSource(
      ",false,REFERRAL_DELETED",
      "SCHEDULE_HEARING,true,HEARING_REFERRAL_DELETED",
      "REFER_POLICE,true,REFERRAL_OUTCOME_DELETED",
      "REFER_POLICE,false,REFERRAL_OUTCOME_DELETED",
    )
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to remove a referral`(
      nextState: OutcomeCode? = null,
      hasHearings: Boolean,
      eventToSend: AdjudicationDomainEventType,
    ) {
      val response = reportedAdjudicationDto(
        status = nextState?.status ?: ReportedAdjudicationStatus.UNSCHEDULED,
        hearingIdActioned = if (hasHearings) 1 else null,
      )

      whenever(
        referralService.removeReferral(
          ArgumentMatchers.anyString(),
        ),
      ).thenReturn(response)

      removeReferralRequest(1)
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(referralService).removeReferral("1")
      verify(eventPublishService, atLeastOnce()).publishEvent(eventToSend, response)
    }

    private fun removeReferralRequest(
      id: Long,
    ): ResultActions = mockMvc
      .perform(
        MockMvcRequestBuilders.delete("/reported-adjudications/$id/remove-referral")
          .header("Content-Type", "application/json"),
      )
  }

  @Nested
  inner class CreateAdjourn {
    @BeforeEach
    fun beforeEach() {
      whenever(
        hearingOutcomeService.createAdjourn(
          ArgumentMatchers.anyString(),
          any(),
          anyOrNull(),
          anyOrNull(),
          anyOrNull(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createAdjournRequest(
        1,
        adjournRequest,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createAdjournRequest(
        1,
        adjournRequest,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create an adjourn`() {
      createAdjournRequest(1, adjournRequest)
        .andExpect(MockMvcResultMatchers.status().isCreated)
      verify(hearingOutcomeService).createAdjourn(
        chargeNumber = "1",
        adjudicator = "test",
        adjournReason = HearingOutcomeAdjournReason.HELP,
        plea = HearingOutcomePlea.ABSTAIN,
        details = "details",
      )
      verify(eventPublishService, atLeastOnce()).publishEvent(
        AdjudicationDomainEventType.HEARING_ADJOURN_CREATED,
        REPORTED_ADJUDICATION_DTO,
      )
    }

    private fun createAdjournRequest(
      id: Long,
      adjournRequest: AdjournRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(adjournRequest)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/hearing/outcome/adjourn")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class DeleteAdjourn {
    @BeforeEach
    fun beforeEach() {
      whenever(
        hearingOutcomeService.removeAdjourn(
          chargeNumber = ArgumentMatchers.anyString(),
          recalculateStatus = any(),

        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      deleteAdjournRequest(
        1,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      deleteAdjournRequest(
        1,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to delete an adjourn`() {
      deleteAdjournRequest(1)
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(hearingOutcomeService).removeAdjourn(
        chargeNumber = "1",
      )
      verify(eventPublishService, atLeastOnce()).publishEvent(
        AdjudicationDomainEventType.HEARING_ADJOURN_DELETED,
        REPORTED_ADJUDICATION_DTO,
      )
    }

    private fun deleteAdjournRequest(
      id: Long,
    ): ResultActions = mockMvc
      .perform(
        MockMvcRequestBuilders.delete("/reported-adjudications/$id/hearing/outcome/adjourn")
          .header("Content-Type", "application/json"),
      )
  }

  @Nested
  inner class AmendHearingOutcome {

    @Test
    fun `responds with a unauthorised status code`() {
      amendHearingOutcomeRequest(
        1,
        AMEND_OUTCOME_REQUEST,
        ReportedAdjudicationStatus.REFER_POLICE,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      amendHearingOutcomeRequest(
        1,
        AMEND_OUTCOME_REQUEST,
        ReportedAdjudicationStatus.REFER_POLICE,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @CsvSource("CHARGE_PROVED", "DISMISSED", "NOT_PROCEED")
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to amend a hearing outcome`(status: ReportedAdjudicationStatus) {
      whenever(
        amendHearingOutcomeService.amendHearingOutcome(
          ArgumentMatchers.anyString(),
          any(),
          any(),
        ),
      ).thenReturn(
        REPORTED_ADJUDICATION_DTO.also {
          it.punishmentsRemoved = status == ReportedAdjudicationStatus.CHARGE_PROVED
        },
      )

      amendHearingOutcomeRequest(1, AMEND_OUTCOME_REQUEST, status)
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(amendHearingOutcomeService).amendHearingOutcome(
        chargeNumber = "1",
        status = status,
        AMEND_OUTCOME_REQUEST,
      )
      verify(eventPublishService, atLeastOnce()).publishEvent(
        AdjudicationDomainEventType.HEARING_OUTCOME_UPDATED,
        REPORTED_ADJUDICATION_DTO,
      )
      verify(
        eventPublishService,
        if (status == ReportedAdjudicationStatus.CHARGE_PROVED) atLeastOnce() else never(),
      ).publishEvent(AdjudicationDomainEventType.PUNISHMENTS_DELETED, REPORTED_ADJUDICATION_DTO)
    }

    private fun amendHearingOutcomeRequest(
      id: Long,
      amendRequest: AmendHearingOutcomeRequest,
      status: ReportedAdjudicationStatus,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(amendRequest)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$id/hearing/outcome/$status/v2")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class CreateHearingCompletedOutcome {
    @BeforeEach
    fun beforeEach() {
      whenever(
        completedHearingService.createNotProceed(
          ArgumentMatchers.anyString(),
          any(),
          any(),
          any(),
          any(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)

      whenever(
        completedHearingService.createDismissed(
          ArgumentMatchers.anyString(),
          any(),
          any(),
          any(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @CsvSource("NOT_PROCEED", "DISMISSED")
    @ParameterizedTest
    fun `responds with a unauthorised status code`(code: OutcomeCode) {
      createOutcomeRequest(
        1,
        code,
        if (code != OutcomeCode.NOT_PROCEED) COMPLETED_DISMISSED_REQUEST else null,
        if (code == OutcomeCode.NOT_PROCEED) COMPLETED_NOT_PROCEED_REQUEST else null,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @CsvSource("NOT_PROCEED", "DISMISSED")
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`(code: OutcomeCode) {
      createOutcomeRequest(
        1,
        code,
        if (code != OutcomeCode.NOT_PROCEED) COMPLETED_DISMISSED_REQUEST else null,
        if (code == OutcomeCode.NOT_PROCEED) COMPLETED_NOT_PROCEED_REQUEST else null,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @CsvSource("NOT_PROCEED", "DISMISSED")
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`(code: OutcomeCode) {
      createOutcomeRequest(
        1,
        code,
        if (code != OutcomeCode.NOT_PROCEED) COMPLETED_DISMISSED_REQUEST else null,
        if (code == OutcomeCode.NOT_PROCEED) COMPLETED_NOT_PROCEED_REQUEST else null,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @CsvSource("NOT_PROCEED", "DISMISSED")
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create an outcome`(code: OutcomeCode) {
      createOutcomeRequest(
        1,
        code,
        if (code != OutcomeCode.NOT_PROCEED) COMPLETED_DISMISSED_REQUEST else null,
        if (code == OutcomeCode.NOT_PROCEED) COMPLETED_NOT_PROCEED_REQUEST else null,
      )
        .andExpect(MockMvcResultMatchers.status().isCreated)

      when (code) {
        OutcomeCode.DISMISSED -> verify(completedHearingService).createDismissed(
          "1",
          "test",
          HearingOutcomePlea.UNFIT,
          "details",
        )

        OutcomeCode.NOT_PROCEED -> verify(completedHearingService).createNotProceed(
          "1",
          "test",
          HearingOutcomePlea.UNFIT,
          NotProceedReason.NOT_FAIR,
          "details",
        )

        else -> {}
      }
      verify(eventPublishService, atLeastOnce()).publishEvent(
        AdjudicationDomainEventType.HEARING_COMPLETED_CREATED,
        REPORTED_ADJUDICATION_DTO,
      )
    }

    private fun createOutcomeRequest(
      id: Long,
      code: OutcomeCode,
      dismissed: HearingCompletedDismissedRequest? = null,
      notProceed: HearingCompletedNotProceedRequest? = null,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(notProceed ?: dismissed)
      val path = when (code) {
        OutcomeCode.NOT_PROCEED -> "not-proceed"
        OutcomeCode.DISMISSED -> "dismissed"
        else -> ""
      }
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/complete-hearing/$path")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class RemoveCompletedHearingOutcome {

    @Test
    fun `responds with a unauthorised status code`() {
      removeCompletedHearingOutcomeRequest(1).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      removeCompletedHearingOutcomeRequest(1).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      removeCompletedHearingOutcomeRequest(1).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @CsvSource("CHARGE_PROVED", "DISMISSED", "NOT_PROCEED")
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to remove a completed hearing`(outcomeCode: OutcomeCode) {
      whenever(
        completedHearingService.removeOutcome(
          ArgumentMatchers.anyString(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO.also { it.punishmentsRemoved = outcomeCode == OutcomeCode.CHARGE_PROVED })

      removeCompletedHearingOutcomeRequest(1)
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(completedHearingService).removeOutcome("1")
      verify(eventPublishService, atLeastOnce()).publishEvent(
        AdjudicationDomainEventType.HEARING_COMPLETED_DELETED,
        REPORTED_ADJUDICATION_DTO,
      )
      verify(
        eventPublishService,
        if (outcomeCode == OutcomeCode.CHARGE_PROVED) atLeastOnce() else never(),
      ).publishEvent(AdjudicationDomainEventType.PUNISHMENTS_DELETED, REPORTED_ADJUDICATION_DTO)
    }

    private fun removeCompletedHearingOutcomeRequest(
      id: Long,
    ): ResultActions = mockMvc
      .perform(
        MockMvcRequestBuilders.delete("/reported-adjudications/$id/remove-completed-hearing")
          .header("Content-Type", "application/json"),
      )
  }

  @Nested
  inner class CreateChargeProved {
    @BeforeEach
    fun beforeEach() {
      whenever(
        completedHearingService.createChargeProved(
          ArgumentMatchers.anyString(),
          any(),
          any(),
          anyOrNull(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createChargeProvedRequest(
        1,
        CHARGE_PROVED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createChargeProvedRequest(
        1,
        CHARGE_PROVED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createChargeProvedRequest(
        1,
        CHARGE_PROVED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create a proven outcome`() {
      createChargeProvedRequest(1, CHARGE_PROVED_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isCreated)
      verify(completedHearingService).createChargeProved(
        "1",
        CHARGE_PROVED_REQUEST.adjudicator,
        CHARGE_PROVED_REQUEST.plea,
      )
      verify(eventPublishService, atLeastOnce()).publishEvent(
        AdjudicationDomainEventType.HEARING_COMPLETED_CREATED,
        REPORTED_ADJUDICATION_DTO,
      )
    }

    private fun createChargeProvedRequest(
      id: Long,
      proven: HearingCompletedChargeProvedRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(proven)

      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/complete-hearing/charge-proved/v2")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  companion object {
    private fun referralRequest(code: HearingOutcomeCode? = HearingOutcomeCode.REFER_POLICE) = ReferralRequest(adjudicator = "test", code = code!!, details = "details")

    private val adjournRequest = AdjournRequest(
      adjudicator = "test",
      details = "details",
      reason = HearingOutcomeAdjournReason.HELP,
      plea = HearingOutcomePlea.ABSTAIN,
    )
    private val AMEND_OUTCOME_REQUEST = AmendHearingOutcomeRequest()
    private val COMPLETED_NOT_PROCEED_REQUEST = HearingCompletedNotProceedRequest(
      adjudicator = "test",
      plea = HearingOutcomePlea.UNFIT,
      reason = NotProceedReason.NOT_FAIR,
      details = "details",
    )
    private val COMPLETED_DISMISSED_REQUEST =
      HearingCompletedDismissedRequest(adjudicator = "test", plea = HearingOutcomePlea.UNFIT, details = "details")
    private val CHARGE_PROVED_REQUEST =
      HearingCompletedChargeProvedRequest(adjudicator = "test", plea = HearingOutcomePlea.GUILTY)
  }
}
