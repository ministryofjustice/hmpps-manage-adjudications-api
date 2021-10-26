package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
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
      whenever(draftAdjudicationService.startNewAdjudication(any())).thenReturn(
        DraftAdjudicationDto(id = 1, prisonerNumber = "A123456")
      )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      startANewAdjudication().andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser("ITAG_USER")
    fun `calls the service to start a new adjudication for a prisoner`() {
      startANewAdjudication().andExpect(status().isCreated)

      verify(draftAdjudicationService).startNewAdjudication("A123456")
    }

    @Test
    @WithMockUser("ITAG_USER")
    fun `returns the newly created draft adjudication`() {
      startANewAdjudication()
        .andExpect(status().isCreated)
        .andExpect(jsonPath("draftAdjudication.id").isNumber)
        .andExpect(jsonPath("draftAdjudication.prisonerNumber").value("A123456"))
    }

    @Test
    @WithMockUser("ITAG_USER")
    fun `returns a bad request when sent an empty body`() {
      startANewAdjudication(emptyBody = true).andExpect(status().isBadRequest)
    }

    private fun startANewAdjudication(emptyBody: Boolean = false): ResultActions {
      val jsonBody = if (emptyBody) "" else objectMapper.writeValueAsString(mapOf("prisonerNumber" to "A123456"))

      return mockMvc
        .perform(
          post("/draft-adjudications")
            .header("Content-Type", "application/json")
            .content(jsonBody)
        )
    }
  }

  @Nested
  inner class AddIncidentDetails {
    @BeforeEach
    fun beforeEach() {
      whenever(draftAdjudicationService.addIncidentDetails(anyLong(), anyLong(), any())).thenReturn(
        DraftAdjudicationDto(
          id = 1,
          prisonerNumber = "A123456",
          incidentDetails = IncidentDetailsDto(
            locationId = 1,
            dateTimeOfIncident = DATE_TIME_OF_INCIDENT
          )
        )
      )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      makeAddIncidentDetailsRequest(makeIncidentDetailsBody(1, DATE_TIME_OF_INCIDENT))
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `makes a call to add the incident details to the draft adjudication`() {
      makeAddIncidentDetailsRequest(makeIncidentDetailsBody(1, DATE_TIME_OF_INCIDENT))
        .andExpect(status().isOk)

      verify(draftAdjudicationService).addIncidentDetails(1, 1, DATE_TIME_OF_INCIDENT)
    }

    @Test
    @WithMockUser(username = "TIAG_USER")
    fun `returns a bad request when sent an empty body`() {
      makeAddIncidentDetailsRequest(null).andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `returns a bad request when required fields are missing`() {
      makeAddIncidentDetailsRequest(makeIncidentDetailsBody(1))
        .andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `responds with an not found status code`() {
      whenever(draftAdjudicationService.addIncidentDetails(anyLong(), anyLong(), any())).thenThrow(
        EntityNotFoundException::class.java
      )

      makeAddIncidentDetailsRequest(makeIncidentDetailsBody(1, DATE_TIME_OF_INCIDENT)).andExpect(status().isNotFound)
    }

    private fun makeAddIncidentDetailsRequest(jsonBody: Any?): ResultActions {
      val body = if (jsonBody == null) "" else objectMapper.writeValueAsString(jsonBody)

      return mockMvc
        .perform(
          put("/draft-adjudications/1")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }

    private fun makeIncidentDetailsBody(
      locationId: Int? = null,
      dateTimeOfIncident: LocalDateTime? = null
    ): Map<String, Any?> = mapOf(
      "locationId" to locationId,
      "dateTimeOfIncident" to dateTimeOfIncident
    )
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
          prisonerNumber = "A12345",
          incidentDetails = IncidentDetailsDto(locationId = 1L, dateTimeOfIncident = DATE_TIME_OF_INCIDENT)
        )
      )
      makeGetDraftAdjudicationRequest(1)
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.draftAdjudication.id").isNumber)
        .andExpect(jsonPath("$.draftAdjudication.prisonerNumber").value("A12345"))
        .andExpect(jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").value("2010-10-12T10:00:00"))
        .andExpect(jsonPath("$.draftAdjudication.incidentDetails.locationId").value(1))
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

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0, 0)
  }
}
