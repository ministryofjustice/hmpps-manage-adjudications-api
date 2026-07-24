package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.LossOfVisitsChangeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.LossOfVisitsDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.LossOfVisitsEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.LossOfVisitsPunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.toLossOfVisitsEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Measurement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationDomainEventType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsService
import java.time.LocalDate

@WebMvcTest(
  PunishmentsController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class PunishmentsControllerTest : TestControllerBase() {

  @MockitoBean
  lateinit var punishmentsService: PunishmentsService

  @Nested
  inner class CreatePunishments {

    @BeforeEach
    fun beforeEach() {
      whenever(
        punishmentsService.create(
          ArgumentMatchers.anyString(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createPunishmentsRequest(
        1,
        PUNISHMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createPunishmentsRequest(
        1,
        PUNISHMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createPunishmentsRequest(
        1,
        PUNISHMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create a set of punishments`() {
      createPunishmentsRequest(1, PUNISHMENT_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isCreated)

      verify(punishmentsService).create(
        chargeNumber = "1",
        listOf(PunishmentRequest(type = PUNISHMENT_REQUEST.type, duration = PUNISHMENT_REQUEST.duration)),
      )
      verify(eventPublishService, atLeastOnce()).publishEvent(
        AdjudicationDomainEventType.PUNISHMENTS_CREATED,
        REPORTED_ADJUDICATION_DTO,
      )
      verify(eventPublishService, never()).publishLossOfVisitsEvent(any())
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `publishes loss of visits event when a visits punishment is awarded`() {
      val response = REPORTED_ADJUDICATION_DTO.copy(lossOfVisitsChangeType = LossOfVisitsChangeType.AWARDED)
      whenever(punishmentsService.create(ArgumentMatchers.anyString(), any())).thenReturn(response)

      createPunishmentsRequest(1, PUNISHMENT_REQUEST).andExpect(MockMvcResultMatchers.status().isCreated)

      verify(eventPublishService).publishLossOfVisitsEvent(response.toLossOfVisitsEvent(LossOfVisitsChangeType.AWARDED))
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `publishes the final visits snapshot for an original charge when its suspended punishment changes twice`() {
      val lossOfVisitsEvent = LossOfVisitsEventDto(
        chargeNumber = "original-charge",
        prisonerNumber = REPORTED_ADJUDICATION_DTO.prisonerNumber,
        prisonId = REPORTED_ADJUDICATION_DTO.originatingAgencyId,
        status = ReportedAdjudicationStatus.CHARGE_PROVED,
        details = LossOfVisitsDetailsDto(
          changeType = LossOfVisitsChangeType.UPDATED,
          visitsPunishments = listOf(
            LossOfVisitsPunishmentDto(
              punishmentId = 1,
              type = PunishmentType.RESTRICTION_OF_SOCIAL_VISITS,
              duration = 28,
              measurement = Measurement.DAYS,
              startDate = LocalDate.now(),
              endDate = LocalDate.now().plusDays(27),
              activatedByChargeNumber = REPORTED_ADJUDICATION_DTO.chargeNumber,
              hasChildUnder18 = true,
            ),
          ),
        ),
      )
      val response = REPORTED_ADJUDICATION_DTO.copy(
        supplementalLossOfVisitsEvents = listOf(
          lossOfVisitsEvent.copy(
            details = lossOfVisitsEvent.details.copy(
              visitsPunishments = lossOfVisitsEvent.details.visitsPunishments.map {
                it.copy(
                  startDate = null,
                  endDate = null,
                  suspendedUntil = LocalDate.now().plusDays(30),
                  activatedByChargeNumber = null,
                )
              },
            ),
          ),
          lossOfVisitsEvent,
        ),
        suspendedPunishmentEvents = setOf(
          SuspendedPunishmentEvent(
            agencyId = REPORTED_ADJUDICATION_DTO.originatingAgencyId,
            chargeNumber = "original-charge",
            status = ReportedAdjudicationStatus.CHARGE_PROVED,
          ),
        ),
      )
      whenever(punishmentsService.create(ArgumentMatchers.anyString(), any())).thenReturn(response)

      createPunishmentsRequest(1, PUNISHMENT_REQUEST).andExpect(MockMvcResultMatchers.status().isCreated)

      verify(eventPublishService).publishLossOfVisitsEvent(lossOfVisitsEvent)
      verify(eventPublishService, never()).publishLossOfVisitsEvent(response.supplementalLossOfVisitsEvents.first())
    }

    private fun createPunishmentsRequest(
      id: Long,
      punishmentRequest: PunishmentRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(
        PunishmentsRequest(
          punishments = listOf(punishmentRequest),
        ),
      )
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/punishments/v2")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class UpdatePunishments {
    @BeforeEach
    fun beforeEach() {
      whenever(
        punishmentsService.update(
          ArgumentMatchers.anyString(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      updatePunishmentsRequest(
        1,
        PUNISHMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      updatePunishmentsRequest(
        1,
        PUNISHMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      updatePunishmentsRequest(
        1,
        PUNISHMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to update a set of punishments`() {
      updatePunishmentsRequest(1, PUNISHMENT_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(punishmentsService).update(
        chargeNumber = "1",
        listOf(PunishmentRequest(type = PUNISHMENT_REQUEST.type, duration = PUNISHMENT_REQUEST.duration)),
      )
      verify(eventPublishService, atLeastOnce()).publishEvent(
        AdjudicationDomainEventType.PUNISHMENTS_UPDATED,
        REPORTED_ADJUDICATION_DTO,
      )
      verify(eventPublishService, never()).publishLossOfVisitsEvent(any())
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `publishes loss of visits event when a visits punishment is changed`() {
      val response = REPORTED_ADJUDICATION_DTO.copy(lossOfVisitsChangeType = LossOfVisitsChangeType.UPDATED)
      whenever(punishmentsService.update(ArgumentMatchers.anyString(), any())).thenReturn(response)

      updatePunishmentsRequest(1, PUNISHMENT_REQUEST).andExpect(MockMvcResultMatchers.status().isOk)

      verify(eventPublishService).publishLossOfVisitsEvent(response.toLossOfVisitsEvent(LossOfVisitsChangeType.UPDATED))
    }

    private fun updatePunishmentsRequest(
      id: Long,
      punishmentRequest: PunishmentRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(
        PunishmentsRequest(
          punishments = listOf(punishmentRequest),
        ),
      )
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$id/punishments/v2")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class CompleteRehabilitativeActivity {
    @BeforeEach
    fun beforeEach() {
      whenever(
        punishmentsService.completeRehabilitativeActivity(
          ArgumentMatchers.anyString(),
          any(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      completeRehabilitativeActivityRequest(1).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      completeRehabilitativeActivityRequest(1).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      completeRehabilitativeActivityRequest(1).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to complete a rehabilitative activity`() {
      completeRehabilitativeActivityRequest(1)
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(punishmentsService).completeRehabilitativeActivity(
        chargeNumber = "1",
        punishmentId = 1,
        completeRehabilitativeActivityRequest = CompleteRehabilitativeActivityRequest(completed = true),
      )

      verify(eventPublishService, atLeastOnce()).publishEvent(
        AdjudicationDomainEventType.PUNISHMENTS_UPDATED,
        REPORTED_ADJUDICATION_DTO,
      )
      verify(eventPublishService, never()).publishLossOfVisitsEvent(any())
    }

    private fun completeRehabilitativeActivityRequest(
      id: Long,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(CompleteRehabilitativeActivityRequest(completed = true))

      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/punishments/1/complete-rehabilitative-activity")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  companion object {
    val PUNISHMENT_REQUEST = PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, duration = 10)
  }
}
