package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationDomainEventType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingService
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(
  HearingController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class HearingControllerTest : TestControllerBase() {

  @MockBean
  lateinit var hearingService: HearingService

  @Nested
  inner class CreateHearing {
    @BeforeEach
    fun beforeEach() {
      whenever(
        hearingService.createHearing(
          ArgumentMatchers.anyString(),
          ArgumentMatchers.anyLong(),
          any(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createHearingRequest(
        1,
        HEARING_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createHearingRequest(
        1,
        HEARING_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createHearingRequest(
        1,
        HEARING_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create a hearing`() {
      createHearingRequest(1, HEARING_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isCreated)
      verify(hearingService).createHearing("1", HEARING_REQUEST.locationId, HEARING_REQUEST.dateTimeOfHearing, HEARING_REQUEST.oicHearingType)
      verify(eventPublishService, atLeastOnce()).publishEvent(AdjudicationDomainEventType.HEARING_CREATED, REPORTED_ADJUDICATION_DTO)
    }

    private fun createHearingRequest(
      id: Long,
      hearing: HearingRequest?,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(hearing)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/hearing/v2")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class DeleteHearing {

    @BeforeEach
    fun beforeEach() {
      whenever(
        hearingService.deleteHearing(
          ArgumentMatchers.anyString(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      deleteHearingRequest(1).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      deleteHearingRequest(1).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      deleteHearingRequest(1).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to delete a hearing`() {
      deleteHearingRequest(1)
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(hearingService).deleteHearing("1")
      verify(eventPublishService, atLeastOnce()).publishEvent(AdjudicationDomainEventType.HEARING_DELETED, REPORTED_ADJUDICATION_DTO)
    }

    private fun deleteHearingRequest(
      id: Long,
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.delete("/reported-adjudications/$id/hearing/v2")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class AmendHearing {
    @BeforeEach
    fun beforeEach() {
      whenever(
        hearingService.amendHearing(
          ArgumentMatchers.anyString(),
          ArgumentMatchers.anyLong(),
          any(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      amendHearingRequest(
        1,
        HEARING_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      amendHearingRequest(
        1,
        HEARING_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      amendHearingRequest(
        1,
        HEARING_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to amend a hearing`() {
      amendHearingRequest(1, HEARING_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(hearingService).amendHearing("1", HEARING_REQUEST.locationId, HEARING_REQUEST.dateTimeOfHearing, HEARING_REQUEST.oicHearingType)
      verify(eventPublishService, atLeastOnce()).publishEvent(AdjudicationDomainEventType.HEARING_UPDATED, REPORTED_ADJUDICATION_DTO)
    }

    private fun amendHearingRequest(
      id: Long,
      hearing: HearingRequest?,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(hearing)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$id/hearing/v2")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class AllHearings {

    @BeforeEach
    fun beforeEach() {
      whenever(
        hearingService.getAllHearingsByAgencyIdAndDate(
          any(),
        ),
      ).thenReturn(ALL_HEARINGS_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      allHearingsRequest(LocalDate.now()).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_MIGRATE_ADJUDICATIONS"])
    fun `responds with a forbidden status code for wrong role`() {
      allHearingsRequest(LocalDate.now()).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `get all hearings for agency `() {
      val now = LocalDate.now()
      allHearingsRequest(now)
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(hearingService).getAllHearingsByAgencyIdAndDate(now)
    }

    private fun allHearingsRequest(
      date: LocalDate,
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/hearings?hearingDate=$date")
            .header("Content-Type", "application/json"),
        )
    }
  }

  companion object {
    private val HEARING_REQUEST = HearingRequest(locationId = 1L, dateTimeOfHearing = LocalDateTime.now(), oicHearingType = OicHearingType.GOV)

    private val ALL_HEARINGS_DTO =
      listOf(
        HearingSummaryDto(
          id = 1,
          dateTimeOfHearing = LocalDateTime.now(),
          dateTimeOfDiscovery = LocalDateTime.now(),
          chargeNumber = "123",
          prisonerNumber = "123",
          oicHearingType = OicHearingType.GOV,
          status = ReportedAdjudicationStatus.SCHEDULED,
        ),
      )
  }
}
