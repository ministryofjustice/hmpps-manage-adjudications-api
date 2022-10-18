package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.ReportedAdjudicationService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import javax.persistence.EntityNotFoundException

@WebMvcTest(value = [ReportedAdjudicationController::class])
class ReportedAdjudicationControllerTest : TestControllerBase() {

  @MockBean
  lateinit var reportedAdjudicationService: ReportedAdjudicationService

  @Nested
  inner class ReportedAdjudicationDetails {
    @Test
    fun `responds with a unauthorised status code`() {
      makeGetAdjudicationRequest(1)
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `returns the adjudication for a given id`() {
      whenever(reportedAdjudicationService.getReportedAdjudicationDetails(anyLong())).thenReturn(REPORTED_ADJUDICATION_DTO)
      makeGetAdjudicationRequest(1)
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.reportedAdjudication.adjudicationNumber").value(1))
      verify(reportedAdjudicationService).getReportedAdjudicationDetails(1)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `responds with an not found status code`() {
      whenever(reportedAdjudicationService.getReportedAdjudicationDetails(anyLong())).thenThrow(EntityNotFoundException::class.java)

      makeGetAdjudicationRequest(1).andExpect(status().isNotFound)
    }

    private fun makeGetAdjudicationRequest(
      adjudicationNumber: Long
    ): ResultActions {
      return mockMvc
        .perform(
          get("/reported-adjudications/$adjudicationNumber")
            .header("Content-Type", "application/json")
        )
    }
  }

  @Nested
  inner class MyReportedAdjudications {
    @BeforeEach
    fun beforeEach() {
      whenever(
        reportedAdjudicationService.getMyReportedAdjudications(
          any(), any(), any(), any(), any()
        )
      ).thenReturn(
        PageImpl(
          listOf(REPORTED_ADJUDICATION_DTO),
          Pageable.ofSize(20).withPage(0),
          1
        ),
      )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      getMyAdjudications().andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `makes a call to return my reported adjudications`() {
      getMyAdjudications().andExpect(status().isOk)
      verify(reportedAdjudicationService).getMyReportedAdjudications(
        "MDI",
        LocalDate.now().minusDays(3), LocalDate.now(), Optional.empty(),
        PageRequest.ofSize(20).withPage(0).withSort(
          Sort.by(
            Sort.Direction.DESC,
            "incidentDate"
          )
        )
      )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `makes a call to return all reported adjudications`() {
      getAllAdjudications().andExpect(status().isOk)
      verify(reportedAdjudicationService).getAllReportedAdjudications(
        "MDI",
        LocalDate.now().minusDays(3), LocalDate.now(), Optional.empty(),
        PageRequest.ofSize(20).withPage(0).withSort(
          Sort.by(
            Sort.Direction.DESC,
            "incidentDate"
          )
        )
      )
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `returns my reported adjudications`() {
      getMyAdjudications()
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.content[0].adjudicationNumber").value(1))
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `returns my reported adjudications with date and status filter`() {
      getMyAdjudicationsWithFilter(LocalDate.now().plusDays(5), ReportedAdjudicationStatus.AWAITING_REVIEW)
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.content[0].adjudicationNumber").value(1))

      verify(reportedAdjudicationService).getMyReportedAdjudications(
        "MDI",
        LocalDate.now().plusDays(5),
        LocalDate.now().plusDays(5),
        Optional.of(ReportedAdjudicationStatus.AWAITING_REVIEW),
        PageRequest.ofSize(20).withPage(0).withSort(
          Sort.by(
            Sort.Direction.DESC,
            "incidentDate"
          )
        )
      )
    }

    @Test
    fun `paged responds with a unauthorised status code`() {
      getMyAdjudications().andExpect(status().isUnauthorized)
    }

    private fun getMyAdjudications(): ResultActions {
      return mockMvc
        .perform(
          get("/reported-adjudications/my/agency/MDI?page=0&size=20&sort=incidentDate,DESC")
            .header("Content-Type", "application/json")
        )
    }

    private fun getAllAdjudications(): ResultActions {
      return mockMvc
        .perform(
          get("/reported-adjudications/agency/MDI?page=0&size=20&sort=incidentDate,DESC")
            .header("Content-Type", "application/json")
        )
    }

    private fun getMyAdjudicationsWithFilter(date: LocalDate, reportedAdjudicationStatus: ReportedAdjudicationStatus): ResultActions {
      return mockMvc
        .perform(
          get("/reported-adjudications/my/agency/MDI?startDate=$date&endDate=$date&status=$reportedAdjudicationStatus&page=0&size=20&sort=incidentDate,DESC")
            .header("Content-Type", "application/json")
        )
    }
  }

  @Nested
  inner class CreateDraftFromReportedAdjudication {
    @BeforeEach
    fun beforeEach() {
      whenever(reportedAdjudicationService.createDraftFromReportedAdjudication(any())).thenReturn(
        DraftAdjudicationDto(
          id = 1,
          adjudicationNumber = 123L,
          prisonerNumber = "A12345",
          incidentDetails = IncidentDetailsDto(
            locationId = 2,
            dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
            dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT.plusDays(1),
            handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
          ),
          incidentRole = IncidentRoleDto(
            roleCode = "25a",
            offenceRule = OffenceRuleDetailsDto(
              paragraphNumber = "25(a)",
              paragraphDescription = "Commits an assault"
            ),
            associatedPrisonersNumber = "B2345BB",
            associatedPrisonersName = "Associated Prisoner",
          ),
          incidentStatement = IncidentStatementDto(statement = INCIDENT_STATEMENT),
          isYouthOffender = true
        )
      )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      makeCreateDraftFromReportedAdjudicationRequest(123).andExpect(status().isUnauthorized)
    }

    private fun makeCreateDraftFromReportedAdjudicationRequest(
      adjudicationNumber: Long
    ): ResultActions {
      return mockMvc
        .perform(
          get("/reported-adjudications/$adjudicationNumber/create-draft-adjudication")
            .header("Content-Type", "application/json")
        )
    }
  }

  @Nested
  inner class ReportedAdjudicationSetReportedAdjudicationStatus {

    private fun makeReportedAdjudicationSetStatusRequest(
      adjudicationNumber: Long,
      body: Map<String, Any>
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$adjudicationNumber/status")
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(body))
        )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `returns a bad request when the maximum details length has been exceeded`() {
      val largeStatement = IntRange(0, 4001).joinToString("") { "A" }
      makeReportedAdjudicationSetStatusRequest(
        123,
        mapOf("status" to ReportedAdjudicationStatus.RETURNED, "statusDetails" to largeStatement)
      ).andExpect(status().isBadRequest)
        .andExpect(jsonPath("$.userMessage").value("The details of why the status has been set exceeds the maximum character limit of 4000"))
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `returns a bad request when the maximum reason length has been exceeded`() {
      val largeStatement = IntRange(0, 128).joinToString("") { "A" }
      makeReportedAdjudicationSetStatusRequest(
        123,
        mapOf(
          "status" to ReportedAdjudicationStatus.RETURNED,
          "statusReason" to largeStatement
        )
      ).andExpect(status().isBadRequest)
        .andExpect(jsonPath("$.userMessage").value("The reason the status has been set exceeds the maximum character limit of 128"))
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `makes a call to set the status of the reported adjudication`() {
      makeReportedAdjudicationSetStatusRequest(
        123,
        mapOf("status" to ReportedAdjudicationStatus.RETURNED, "statusReason" to "reason", "statusDetails" to "details")
      )
      verify(reportedAdjudicationService).setStatus(123, ReportedAdjudicationStatus.RETURNED, "reason", "details")
    }

    @Test
    fun `responds with a unauthorised status code`() {
      makeReportedAdjudicationSetStatusRequest(123, mapOf("status" to ReportedAdjudicationStatus.RETURNED)).andExpect(
        status().isUnauthorized
      )
    }
  }

  @Nested
  inner class UpdateDamages {
    @BeforeEach
    fun beforeEach() {
      whenever(
        reportedAdjudicationService.updateDamages(
          anyLong(),
          any(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      setDamagesRequest(1, DAMAGES_REQUEST).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `makes a call to set the damages`() {
      setDamagesRequest(1, DAMAGES_REQUEST)
        .andExpect(status().isOk)

      verify(reportedAdjudicationService).updateDamages(1, DAMAGES_REQUEST.damages)
    }

    private fun setDamagesRequest(
      id: Long,
      damages: DamagesRequest?
    ): ResultActions {
      val body = objectMapper.writeValueAsString(damages)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$id/damages/edit")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class UpdateEvidence {
    @BeforeEach
    fun beforeEach() {
      whenever(
        reportedAdjudicationService.updateEvidence(
          anyLong(),
          any(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      setEvidenceRequest(1, EVIDENCE_REQUEST).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `makes a call to set the evidence`() {
      setEvidenceRequest(1, EVIDENCE_REQUEST)
        .andExpect(status().isOk)

      verify(reportedAdjudicationService).updateEvidence(1, EVIDENCE_REQUEST.evidence)
    }

    private fun setEvidenceRequest(
      id: Long,
      evidence: EvidenceRequest?
    ): ResultActions {
      val body = objectMapper.writeValueAsString(evidence)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$id/evidence/edit")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class UpdateWitnesses {
    @BeforeEach
    fun beforeEach() {
      whenever(
        reportedAdjudicationService.updateWitnesses(
          anyLong(),
          any(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      setWitnessesRequest(1, WITNESSES_REQUEST).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `makes a call to set the witnesses`() {
      setWitnessesRequest(1, WITNESSES_REQUEST)
        .andExpect(status().isOk)

      verify(reportedAdjudicationService).updateWitnesses(1, WITNESSES_REQUEST.witnesses)
    }

    private fun setWitnessesRequest(
      id: Long,
      witnesses: WitnessesRequest?
    ): ResultActions {
      val body = objectMapper.writeValueAsString(witnesses)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$id/witnesses/edit")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class CreateHearing {
    @BeforeEach
    fun beforeEach() {
      whenever(
        reportedAdjudicationService.createHearing(
          anyLong(),
          anyLong(),
          any()
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createHearingRequest(1, HEARING_REQUEST).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createHearingRequest(1, HEARING_REQUEST).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createHearingRequest(1, HEARING_REQUEST).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create a hearing`() {
      createHearingRequest(1, HEARING_REQUEST)
        .andExpect(status().isCreated)
      verify(reportedAdjudicationService).createHearing(1, HEARING_REQUEST.locationId, HEARING_REQUEST.dateTimeOfHearing)
    }

    private fun createHearingRequest(
      id: Long,
      hearing: HearingRequest?
    ): ResultActions {
      val body = objectMapper.writeValueAsString(hearing)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/hearing")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class DeleteHearing {

    @BeforeEach
    fun beforeEach() {
      whenever(
        reportedAdjudicationService.deleteHearing(
          anyLong(),
          anyLong(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      deleteHearingRequest(1, 1).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      deleteHearingRequest(1, 1).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      deleteHearingRequest(1, 1).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to delete a hearing`() {
      deleteHearingRequest(1, 1)
        .andExpect(status().isOk)
      verify(reportedAdjudicationService).deleteHearing(1, 1)
    }

    private fun deleteHearingRequest(
      id: Long,
      hearingId: Long
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.delete("/reported-adjudications/$id/hearing/$hearingId")
            .header("Content-Type", "application/json")
        )
    }
  }

  @Nested
  inner class AllHearings {

    @BeforeEach
    fun beforeEach() {
      whenever(
        reportedAdjudicationService.getAllHearingsByAgencyIdAndDate(
          anyString(),
          any(),
        )
      ).thenReturn(ALL_HEARINGS_DTO)
    }
    @Test
    fun `responds with a unauthorised status code`() {
      allHearingsRequest("MDI", LocalDate.now()).andExpect(status().isUnauthorized)
    }
    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `get all hearings for agency `() {
      val now = LocalDate.now()
      allHearingsRequest("MDI", now)
        .andExpect(status().isOk)
      verify(reportedAdjudicationService).getAllHearingsByAgencyIdAndDate("MDI", now)
    }

    private fun allHearingsRequest(
      agency: String,
      date: LocalDate,
    ): ResultActions {
      return mockMvc
        .perform(
          get("/reported-adjudications/hearings/agency/$agency?hearingDate=$date")
            .header("Content-Type", "application/json")
        )
    }
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0, 0)
    private val DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE = LocalDateTime.of(2010, 10, 14, 10, 0)
    private val REPORTED_DATE_TIME = DATE_TIME_OF_INCIDENT.plusDays(1)
    private const val INCIDENT_STATEMENT = "A statement"
    private val DAMAGES_REQUEST = DamagesRequest(listOf(DamageRequestItem(DamageCode.CLEANING, "details")))
    private val WITNESSES_REQUEST = WitnessesRequest(listOf(WitnessRequestItem(WitnessCode.STAFF, "first", "last")))
    private val EVIDENCE_REQUEST = EvidenceRequest(listOf(EvidenceRequestItem(EvidenceCode.PHOTO, "identifier", "details")))
    private val HEARING_REQUEST = HearingRequest(locationId = 1L, dateTimeOfHearing = LocalDateTime.now())
    private val INCIDENT_ROLE_WITH_ALL_VALUES = IncidentRoleDto(
      "25a",
      OffenceRuleDetailsDto(
        "25(a)",
        "Commits an assault"
      ),
      "B23456",
      "Associated Prisoner",
    )

    private val ALL_HEARINGS_DTO =
      listOf(
        HearingSummaryDto(
          id = 1,
          dateTimeOfHearing = LocalDateTime.now(),
          dateTimeOfDiscovery = LocalDateTime.now(),
          adjudicationNumber = 123,
          prisonerNumber = "123"
        )
      )

    private val REPORTED_ADJUDICATION_DTO =
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
        offenceDetails = listOf(
          OffenceDto(
            offenceCode = 2,
            OffenceRuleDto(
              paragraphNumber = "3",
              paragraphDescription = "A paragraph description",
            )
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
      )
  }
}
