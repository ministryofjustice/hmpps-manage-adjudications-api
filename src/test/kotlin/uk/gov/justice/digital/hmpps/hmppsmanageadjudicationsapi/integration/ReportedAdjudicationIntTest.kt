package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DamageRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.EvidenceRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.WitnessRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.DEFAULT_REPORTED_DATE_TIME
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ReportedAdjudicationIntTest : IntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME)
  }

  @Test
  fun `get reported adjudication details`() {
    oAuthMockServer.stubGrantToken()

    val intTestData = integrationTestData()

    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, draftUserHeaders)

    draftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .addDamages()
      .addEvidence()
      .addWitnesses()
      .completeDraft()

    webTestClient.get()
      .uri("/reported-adjudications/1524242")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.adjudicationNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
      .jsonPath("$.reportedAdjudication.bookingId").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.bookingId)
      .jsonPath("$.reportedAdjudication.incidentDetails.dateTimeOfIncident")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfIncidentISOString)
      .jsonPath("$.reportedAdjudication.incidentDetails.dateTimeOfDiscovery")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfDiscoveryISOString)
      .jsonPath("$.reportedAdjudication.incidentDetails.locationId")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.locationId)
      .jsonPath("$.reportedAdjudication.incidentDetails.handoverDeadline")
      .isEqualTo(IntegrationTestData.DEFAULT_HANDOVER_DEADLINE_ISO_STRING)
      .jsonPath("$.reportedAdjudication.isYouthOffender")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.isYouthOffender)
      .jsonPath("$.reportedAdjudication.incidentRole.roleCode")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.incidentRoleCode)
      .jsonPath("$.reportedAdjudication.incidentRole.offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.incidentRoleParagraphNumber)
      .jsonPath("$.reportedAdjudication.incidentRole.offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.incidentRoleParagraphDescription)
      .jsonPath("$.reportedAdjudication.offenceDetails[0].offenceCode")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].offenceCode)
      .jsonPath("$.reportedAdjudication.offenceDetails[0].offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].paragraphNumber)
      .jsonPath("$.reportedAdjudication.offenceDetails[0].offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].paragraphDescription)
      .jsonPath("$.reportedAdjudication.offenceDetails[0].victimPrisonersNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].victimPrisonersNumber)
      .jsonPath("$.reportedAdjudication.offenceDetails[0].victimStaffUsername")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].victimStaffUsername)
      .jsonPath("$.reportedAdjudication.offenceDetails[0].victimOtherPersonsName")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].victimOtherPersonsName)
      .jsonPath("$.reportedAdjudication.offenceDetails[1].offenceCode")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[1].offenceCode)
      .jsonPath("$.reportedAdjudication.offenceDetails[1].offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[1].paragraphNumber)
      .jsonPath("$.reportedAdjudication.offenceDetails[1].offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[1].paragraphDescription)
      .jsonPath("$.reportedAdjudication.offenceDetails[1].victimPrisonersNumber").doesNotExist()
      .jsonPath("$.reportedAdjudication.offenceDetails[1].victimStaffUsername").doesNotExist()
      .jsonPath("$.reportedAdjudication.offenceDetails[1].victimOtherPersonsName").doesNotExist()
      .jsonPath("$.reportedAdjudication.offenceDetails[2]").doesNotExist()
      .jsonPath("$.reportedAdjudication.incidentStatement.statement")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.statement)
      .jsonPath("$.reportedAdjudication.incidentStatement.completed").isEqualTo(true)
      .jsonPath("$.reportedAdjudication.createdByUserId")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
      .jsonPath("$.reportedAdjudication.createdDateTime").isEqualTo(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME_TEXT)
      .jsonPath("$.reportedAdjudication.damages[0].code")
      .isEqualTo(DamageCode.CLEANING.name)
      .jsonPath("$.reportedAdjudication.damages[0].details")
      .isEqualTo("details")
      .jsonPath("$.reportedAdjudication.damages[0].reporter")
      .isEqualTo("B_MILLS")
      .jsonPath("$.reportedAdjudication.evidence[0].code")
      .isEqualTo(EvidenceCode.PHOTO.name)
      .jsonPath("$.reportedAdjudication.evidence[0].details")
      .isEqualTo("details")
      .jsonPath("$.reportedAdjudication.evidence[0].reporter")
      .isEqualTo("B_MILLS")
      .jsonPath("$.reportedAdjudication.witnesses[0].code")
      .isEqualTo(WitnessCode.OFFICER.name)
      .jsonPath("$.reportedAdjudication.witnesses[0].firstName")
      .isEqualTo("prison")
      .jsonPath("$.reportedAdjudication.witnesses[0].reporter")
      .isEqualTo("B_MILLS")
  }

  @Test
  fun `get reported adjudication details with invalid adjudication number`() {
    oAuthMockServer.stubGrantToken()

    webTestClient.get()
      .uri("/reported-adjudications/15242")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isNotFound
      .expectBody()
      .jsonPath("$.status").isEqualTo(404)
      .jsonPath("$.userMessage")
      .isEqualTo("Not found: ReportedAdjudication not found for 15242")
  }

  @ParameterizedTest
  @CsvSource(
    "2020-12-14, 2020-12-17, AWAITING_REVIEW, 3, 1234",
    "2020-12-15, 2020-12-15, AWAITING_REVIEW, 1, 789"
  )
  fun `return a page of reported adjudications for agency with filters`(
    startDate: String,
    endDate: String,
    reportedAdjudicationStatus: ReportedAdjudicationStatus,
    expectedCount: Int,
    adjudicationNumber: Long
  ) {

    initMyReportData()

    webTestClient.get()
      .uri("/reported-adjudications/agency/MDI?startDate=$startDate&endDate=$endDate&status=$reportedAdjudicationStatus&page=0&size=20")
      .headers(setHeaders(username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(expectedCount)
      .jsonPath("$.content[0].adjudicationNumber").isEqualTo(adjudicationNumber)
  }

  @ParameterizedTest
  @CsvSource(
    "2020-12-14, 2020-12-16, AWAITING_REVIEW, 2, 1234",
    "2020-12-14, 2020-12-14, AWAITING_REVIEW, 1, 567"
  )
  fun `return a page of reported adjudications completed by the current user with filters`(
    startDate: String,
    endDate: String,
    reportedAdjudicationStatus: ReportedAdjudicationStatus,
    expectedCount: Int,
    adjudicationNumber: Long
  ) {

    initMyReportData()

    webTestClient.get()
      .uri("/reported-adjudications/my/agency/MDI?startDate=$startDate&endDate=$endDate&status=$reportedAdjudicationStatus&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(expectedCount)
      .jsonPath("$.content[0].adjudicationNumber").isEqualTo(adjudicationNumber)
  }

  @Test
  fun `return a page of reported adjudications completed by the current user`() {
    initMyReportData()

    val startDate =
      IntegrationTestData.ADJUDICATION_2.dateTimeOfIncident.toLocalDate().format(DateTimeFormatter.ISO_DATE)
    val endDate = IntegrationTestData.ADJUDICATION_5.dateTimeOfIncident.toLocalDate().format(DateTimeFormatter.ISO_DATE)

    webTestClient.get()
      .uri("/reported-adjudications/my/agency/MDI?startDate=$startDate&endDate=$endDate&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content[0].adjudicationNumber").isEqualTo(IntegrationTestData.ADJUDICATION_4.adjudicationNumber)
      .jsonPath("$.content[1].adjudicationNumber").isEqualTo(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)
  }

  @Test
  fun `return a page of reported adjudications completed in the current agency`() {
    val intTestData = integrationTestData()

    val firstDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_1.createdByUserId)
    val firstDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, firstDraftUserHeaders)
    firstDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_1)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val secondDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_2.createdByUserId)
    val secondDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, secondDraftUserHeaders)
    secondDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_2)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val thirdDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_3.createdByUserId)
    val thirdDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, thirdDraftUserHeaders)
    thirdDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_3)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val startDate =
      IntegrationTestData.ADJUDICATION_1.dateTimeOfIncident.toLocalDate().format(DateTimeFormatter.ISO_DATE)
    val endDate = IntegrationTestData.ADJUDICATION_3.dateTimeOfIncident.toLocalDate().format(DateTimeFormatter.ISO_DATE)

    webTestClient.get()
      .uri("/reported-adjudications/agency/MDI?startDate=$startDate&endDate=$endDate&page=0&size=20")
      .headers(setHeaders(username = "NEW_USER", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content[0].adjudicationNumber").isEqualTo(IntegrationTestData.ADJUDICATION_3.adjudicationNumber)
      .jsonPath("$.content[1].adjudicationNumber").isEqualTo(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)
  }

  @Test
  fun `get 403 without the relevant role when attempting to return reported adjudications for a caseload`() {
    webTestClient.get()
      .uri("/reported-adjudications/agency/MDI?page=0&size=20")
      .headers(setHeaders(username = "NEW_USER"))
      .exchange()
      .expectStatus().isForbidden
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Access is denied")
  }

  @Test
  fun `create draft from reported adjudication returns expected result`() {
    oAuthMockServer.stubGrantToken()

    val intTestData = integrationTestData()

    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, draftUserHeaders)

    draftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .addDamages()
      .addEvidence()
      .addWitnesses()
      .completeDraft()

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/create-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.draftAdjudication.adjudicationNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
      .jsonPath("$.draftAdjudication.damages[0].code")
      .isEqualTo(DamageCode.CLEANING.name)
      .jsonPath("$.draftAdjudication.damages[0].details")
      .isEqualTo("details")
      .jsonPath("$.draftAdjudication.damages[0].reporter")
      .isEqualTo("B_MILLS")
      .jsonPath("$.draftAdjudication.evidence[0].code")
      .isEqualTo(EvidenceCode.PHOTO.name)
      .jsonPath("$.draftAdjudication.evidence[0].details")
      .isEqualTo("details")
      .jsonPath("$.draftAdjudication.evidence[0].reporter")
      .isEqualTo("B_MILLS")
      .jsonPath("$.draftAdjudication.witnesses[0].code")
      .isEqualTo(WitnessCode.OFFICER.name)
      .jsonPath("$.draftAdjudication.witnesses[0].firstName")
      .isEqualTo("prison")
      .jsonPath("$.draftAdjudication.witnesses[0].reporter")
      .isEqualTo("B_MILLS")
  }

  @Test
  fun `create draft from reported adjudication adds draft`() {
    oAuthMockServer.stubGrantToken()

    val intTestData = integrationTestData()

    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, draftUserHeaders)

    draftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val createdDraftDetails = intTestData.recallCompletedDraftAdjudication(IntegrationTestData.DEFAULT_ADJUDICATION)

    webTestClient.get()
      .uri("/draft-adjudications/${createdDraftDetails.draftAdjudication.id}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
  }

  @Test
  fun `create draft from reported adjudication with invalid adjudication number`() {
    oAuthMockServer.stubGrantToken()

    webTestClient.post()
      .uri("/reported-adjudications/1524242/create-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isNotFound
      .expectBody()
      .jsonPath("$.status").isEqualTo(404)
      .jsonPath("$.userMessage")
      .isEqualTo("Not found: ReportedAdjudication not found for 1524242")
  }

  @Test
  fun `transition from one state to another`() {
    oAuthMockServer.stubGrantToken()

    val intTestData = integrationTestData()

    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, draftUserHeaders)

    draftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/status")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "status" to ReportedAdjudicationStatus.RETURNED,
          "statusReason" to "status reason",
          "statusDetails" to "status details"
        )
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.RETURNED.toString())
      .jsonPath("$.reportedAdjudication.reviewedByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.reportedAdjudication.statusReason").isEqualTo("status reason")
      .jsonPath("$.reportedAdjudication.statusDetails").isEqualTo("status details")
  }

  @Test
  fun `accepted reports submit to Prison API`() {
    oAuthMockServer.stubGrantToken()
    prisonApiMockServer.stubPostAdjudication(IntegrationTestData.DEFAULT_ADJUDICATION)

    val intTestData = integrationTestData()

    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, draftUserHeaders)

    draftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/status")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "status" to ReportedAdjudicationStatus.ACCEPTED,
        )
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.ACCEPTED.toString())

    val expectedBody = mapOf(
      "offenderNo" to IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber,
      "adjudicationNumber" to IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber,
      "bookingId" to IntegrationTestData.DEFAULT_ADJUDICATION.bookingId,
      "reporterName" to IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId,
      "reportedDateTime" to DEFAULT_REPORTED_DATE_TIME,
      "agencyId" to IntegrationTestData.DEFAULT_ADJUDICATION.agencyId,
      "incidentLocationId" to IntegrationTestData.DEFAULT_ADJUDICATION.locationId,
      "incidentTime" to IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfDiscoveryISOString,
      "statement" to IntegrationTestData.DEFAULT_ADJUDICATION.statement,
      "offenceCodes" to IntegrationTestData.DEFAULT_EXPECTED_NOMIS_DATA.nomisCodes,
      "victimStaffUsernames" to IntegrationTestData.DEFAULT_EXPECTED_NOMIS_DATA.victimStaffUsernames,
      "victimOffenderIds" to IntegrationTestData.DEFAULT_EXPECTED_NOMIS_DATA.victimPrisonersNumbers,
      "connectedOffenderIds" to listOf(IntegrationTestData.DEFAULT_INCIDENT_ROLE_ASSOCIATED_PRISONER),
    )

    prisonApiMockServer.verifyPostAdjudication(objectMapper.writeValueAsString(expectedBody))
  }

  @Test
  fun `accepted reports request fails if Prison API call fails`() {
    oAuthMockServer.stubGrantToken()
    prisonApiMockServer.stubPostAdjudicationFailure()

    val intTestData = integrationTestData()

    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, draftUserHeaders)

    draftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/status")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "status" to ReportedAdjudicationStatus.ACCEPTED,
        )
      )
      .exchange()
      .expectStatus().is5xxServerError
  }

  @Test
  fun `get a 400 when trying to transition to an invalid state`() {
    oAuthMockServer.stubGrantToken()

    val intTestData = integrationTestData()

    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, draftUserHeaders)

    draftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()
      .reportedAdjudicationSetStatus(ReportedAdjudicationStatus.REJECTED)

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/status")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "status" to ReportedAdjudicationStatus.ACCEPTED,
        )
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("ReportedAdjudication 1524242 cannot transition from REJECTED to ACCEPTED")
  }

  @Test
  fun `update damages to the reported adjudication`() {
    val intTestData = integrationTestData()

    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, draftUserHeaders)

    draftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .addDamages()
      .addEvidence()
      .addWitnesses()
      .completeDraft()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/damages/edit")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "damages" to listOf(
            DamageRequestItem(
              code = DamageCode.ELECTRICAL_REPAIR, details = "details 2", reporter = "ITAG_ALO"
            ),
            DamageRequestItem(
              code = DamageCode.CLEANING, details = "details", reporter = "B_MILLS"
            )
          ),
        )
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.damages[0].code")
      .isEqualTo(DamageCode.CLEANING.name)
      .jsonPath("$.reportedAdjudication.damages[0].details")
      .isEqualTo("details")
      .jsonPath("$.reportedAdjudication.damages[0].reporter")
      .isEqualTo("B_MILLS")
      .jsonPath("$.reportedAdjudication.damages[1].code")
      .isEqualTo(DamageCode.ELECTRICAL_REPAIR.name)
      .jsonPath("$.reportedAdjudication.damages[1].details")
      .isEqualTo("details 2")
      .jsonPath("$.reportedAdjudication.damages[1].reporter")
      .isEqualTo("ITAG_ALO")
  }

  @Test
  fun `update evidence to the reported adjudication`() {
    val intTestData = integrationTestData()

    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, draftUserHeaders)

    draftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .addDamages()
      .addEvidence()
      .addWitnesses()
      .completeDraft()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/evidence/edit")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "evidence" to listOf(
            EvidenceRequestItem(
              code = EvidenceCode.BODY_WORN_CAMERA, details = "details 2", reporter = "ITAG_ALO"
            ),
            EvidenceRequestItem(
              code = EvidenceCode.PHOTO, details = "details", reporter = "B_MILLS"
            )
          ),
        )
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.evidence[0].code")
      .isEqualTo(EvidenceCode.PHOTO.name)
      .jsonPath("$.reportedAdjudication.evidence[0].details")
      .isEqualTo("details")
      .jsonPath("$.reportedAdjudication.evidence[0].reporter")
      .isEqualTo("B_MILLS")
      .jsonPath("$.reportedAdjudication.evidence[1].code")
      .isEqualTo(EvidenceCode.BODY_WORN_CAMERA.name)
      .jsonPath("$.reportedAdjudication.evidence[1].details")
      .isEqualTo("details 2")
      .jsonPath("$.reportedAdjudication.evidence[1].reporter")
      .isEqualTo("ITAG_ALO")
  }

  @Test
  fun `update witnesses to the reported adjudication`() {
    val intTestData = integrationTestData()

    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, draftUserHeaders)

    draftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .addDamages()
      .addEvidence()
      .addWitnesses()
      .completeDraft()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/witnesses/edit")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "witnesses" to listOf(
            WitnessRequestItem(
              code = WitnessCode.STAFF, firstName = "first", lastName = "last", reporter = "ITAG_ALO"
            ),
            WitnessRequestItem(
              code = WitnessCode.OFFICER, firstName = "first", lastName = "last", reporter = "B_MILLS"
            )
          ),
        )
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.witnesses[0].code")
      .isEqualTo(WitnessCode.OFFICER.name)
      .jsonPath("$.reportedAdjudication.witnesses[0].firstName")
      .isEqualTo("prison")
      .jsonPath("$.reportedAdjudication.witnesses[0].reporter")
      .isEqualTo("B_MILLS")
      .jsonPath("$.reportedAdjudication.witnesses[1].code")
      .isEqualTo(WitnessCode.STAFF.name)
      .jsonPath("$.reportedAdjudication.witnesses[1].firstName")
      .isEqualTo("first")
      .jsonPath("$.reportedAdjudication.witnesses[1].reporter")
      .isEqualTo("ITAG_ALO")
  }

  @Test
  fun `create a hearing `() {
    initDataForHearings()

    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 12, 10, 0)
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 1,
          "dateTimeOfHearing" to dateTimeOfHearing
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings[0].locationId")
      .isEqualTo(1)
      .jsonPath("$.reportedAdjudication.hearings[0].dateTimeOfHearing")
      .isEqualTo("2010-10-12T10:00:00")
      .jsonPath("$.reportedAdjudication.hearings[0].id").isNotEmpty
  }

  @Test
  fun `create hearing fails on prisonApi and does not create a hearing`() {
    initDataForHearings()

    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 12, 10, 0)

    prisonApiMockServer.stubCreateHearingFailure(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 1,
          "dateTimeOfHearing" to dateTimeOfHearing
        )
      )
      .exchange()
      .expectStatus().is5xxServerError

    webTestClient.get()
      .uri("/reported-adjudications/1524242")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)
  }

  @Test
  fun `amend a hearing `() {
    val intTestData = initDataForHearings()

    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubDeleteHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber, 100)

    val reportedAdjudication = intTestData.createHearing(
      IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber,
      IntegrationTestData.DEFAULT_ADJUDICATION,
      setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER"))
    )

    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 25, 10, 0)

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 2,
          "dateTimeOfHearing" to dateTimeOfHearing
        )
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings[0].locationId")
      .isEqualTo(2)
      .jsonPath("$.reportedAdjudication.hearings[0].dateTimeOfHearing")
      .isEqualTo("2010-10-25T10:00:00")
      .jsonPath("$.reportedAdjudication.hearings[0].id").isNotEmpty
  }

  @Test
  fun `delete a hearing `() {
    val intTestData = initDataForHearings()

    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    val reportedAdjudication = intTestData.createHearing(
      IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber,
      IntegrationTestData.DEFAULT_ADJUDICATION,
      setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER"))
    )

    prisonApiMockServer.stubDeleteHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber, 100)

    assert(reportedAdjudication.reportedAdjudication.hearings.size == 1)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id!!}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)
  }

  @Test
  fun `delete a hearing fails on prison api and does not delete record`() {
    val intTestData = initDataForHearings()

    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    val reportedAdjudication = intTestData.createHearing(
      IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber,
      IntegrationTestData.DEFAULT_ADJUDICATION,
      setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER"))
    )

    prisonApiMockServer.stubDeleteHearingFailure(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber, 100)

    assert(reportedAdjudication.reportedAdjudication.hearings.size == 1)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id!!}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().is5xxServerError

    webTestClient.get()
      .uri("/reported-adjudications/1524242")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(1)
  }

  @Test
  fun `get all hearings`() {
    val intTestData = initDataForHearings()

    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    val reportedAdjudication = intTestData.createHearing(
      IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber,
      IntegrationTestData.DEFAULT_ADJUDICATION,
      setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER"))
    )

    webTestClient.get()
      .uri("/reported-adjudications/hearings/agency/${IntegrationTestData.DEFAULT_ADJUDICATION.agencyId}?hearingDate=${reportedAdjudication.reportedAdjudication.hearings.first().dateTimeOfHearing.toLocalDate()}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.hearings.size()").isEqualTo(1)
      .jsonPath("$.hearings[0].id")
      .isEqualTo(reportedAdjudication.reportedAdjudication.hearings.first().id!!)
      .jsonPath("$.hearings[0].prisonerNumber")
      .isEqualTo(reportedAdjudication.reportedAdjudication.prisonerNumber)
      .jsonPath("$.hearings[0].adjudicationNumber")
      .isEqualTo(reportedAdjudication.reportedAdjudication.adjudicationNumber)
      .jsonPath("$.hearings[0].dateTimeOfDiscovery")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfDiscoveryISOString)
      .jsonPath("$.hearings[0].dateTimeOfHearing")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfHearingISOString)
  }

  private fun initMyReportData() {
    val intTestData = integrationTestData()

    val firstDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_2.createdByUserId)
    val firstDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, firstDraftUserHeaders)
    firstDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_2)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val secondDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_3.createdByUserId)
    val secondDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, secondDraftUserHeaders)
    secondDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_3)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val thirdDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_4.createdByUserId)
    val thirdDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, thirdDraftUserHeaders)
    thirdDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_4)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val fourthDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_5.createdByUserId)
    val fourthDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, fourthDraftUserHeaders)
    fourthDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_5)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()
  }

  private fun initDataForHearings(): IntegrationTestData {
    val intTestData = integrationTestData()
    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, draftUserHeaders)

    draftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .addDamages()
      .addEvidence()
      .addWitnesses()
      .completeDraft()

    return intTestData
  }
}
