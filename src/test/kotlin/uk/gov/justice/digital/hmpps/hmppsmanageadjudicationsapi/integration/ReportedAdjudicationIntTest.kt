package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DamageRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.EvidenceRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.WitnessRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.DEFAULT_REPORTED_DATE_TIME
import java.time.LocalDateTime

class ReportedAdjudicationIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME)
  }

  @Test
  fun `get reported adjudication details`() {
    initDataForAccept(testData = IntegrationTestData.DEFAULT_ADJUDICATION.also { it.overrideAgencyId = "TJW" })

    webTestClient.get()
      .uri("/reported-adjudications/1524242")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.adjudicationNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
      .jsonPath("$.reportedAdjudication.overrideAgencyId").isEqualTo("TJW")
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
      .jsonPath("$.reportedAdjudication.offenceDetails.offenceCode")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offence.offenceCode)
      .jsonPath("$.reportedAdjudication.offenceDetails.offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offence.paragraphNumber)
      .jsonPath("$.reportedAdjudication.offenceDetails.offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offence.paragraphDescription)
      .jsonPath("$.reportedAdjudication.offenceDetails.victimPrisonersNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offence.victimPrisonersNumber)
      .jsonPath("$.reportedAdjudication.offenceDetails.victimStaffUsername")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offence.victimStaffUsername)
      .jsonPath("$.reportedAdjudication.offenceDetails.victimOtherPersonsName")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offence.victimOtherPersonsName)
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
      .jsonPath("$.reportedAdjudication.gender").isEqualTo(Gender.MALE.name)
      .jsonPath("$.reportedAdjudication.originatingAgencyId").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.agencyId)
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

  @Test
  fun `get 403 without the relevant role when attempting to return reported adjudications for a caseload`() {
    webTestClient.get()
      .uri("/reported-adjudications/reports?status=SCHEDULED&page=0&size=20")
      .headers(setHeaders(username = "NEW_USER"))
      .exchange()
      .expectStatus().isForbidden
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Access is denied")
  }

  @Test
  fun `create draft from reported adjudication returns expected result`() {
    oAuthMockServer.stubGrantToken()

    initDataForAccept(testData = IntegrationTestData.DEFAULT_ADJUDICATION.also { it.overrideAgencyId = "TJW" })

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/create-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.draftAdjudication.adjudicationNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
      .jsonPath("$.draftAdjudication.overrideAgencyId").isEqualTo("TJW")
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
    val intTestData = initDataForAccept()

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
    initDataForAccept()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/status")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "status" to ReportedAdjudicationStatus.RETURNED,
          "statusReason" to "status reason",
          "statusDetails" to "status details",
        ),
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
    prisonApiMockServer.stubPostAdjudication(IntegrationTestData.DEFAULT_ADJUDICATION)

    initDataForAccept()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/status")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "status" to ReportedAdjudicationStatus.UNSCHEDULED,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.UNSCHEDULED.toString())

    val expectedBody = mapOf(
      "offenderNo" to IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber,
      "adjudicationNumber" to IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber,
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
    initDataForAccept()

    prisonApiMockServer.stubPostAdjudicationFailure()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/status")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "status" to ReportedAdjudicationStatus.UNSCHEDULED,
        ),
      )
      .exchange()
      .expectStatus().is5xxServerError
  }

  @Test
  fun `get a 400 when trying to transition to an invalid state`() {
    oAuthMockServer.stubGrantToken()

    val intTestData = integrationTestData()

    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = draftUserHeaders,
    )

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
          "status" to ReportedAdjudicationStatus.UNSCHEDULED,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("ReportedAdjudication 1524242 cannot transition from ${ReportedAdjudicationStatus.REJECTED.name} to ${ReportedAdjudicationStatus.UNSCHEDULED.name}")
  }

  @Test
  fun `update damages to the reported adjudication`() {
    initDataForAccept()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/damages/edit")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "damages" to listOf(
            DamageRequestItem(
              code = DamageCode.ELECTRICAL_REPAIR,
              details = "details 2",
              reporter = "ITAG_ALO",
            ),
            DamageRequestItem(
              code = DamageCode.CLEANING,
              details = "details",
              reporter = "B_MILLS",
            ),
          ),
        ),
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
    initDataForAccept()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/evidence/edit")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "evidence" to listOf(
            EvidenceRequestItem(
              code = EvidenceCode.BODY_WORN_CAMERA,
              details = "details 2",
              reporter = "ITAG_ALO",
            ),
            EvidenceRequestItem(
              code = EvidenceCode.PHOTO,
              details = "details",
              reporter = "B_MILLS",
            ),
          ),
        ),
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
    initDataForAccept()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/witnesses/edit")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "witnesses" to listOf(
            WitnessRequestItem(
              code = WitnessCode.STAFF,
              firstName = "first",
              lastName = "last",
              reporter = "ITAG_ALO",
            ),
            WitnessRequestItem(
              code = WitnessCode.OFFICER,
              firstName = "first",
              lastName = "last",
              reporter = "B_MILLS",
            ),
          ),
        ),
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
  fun `set issued and then re-issue details for DIS form `() {
    prisonApiMockServer.stubPostAdjudication(IntegrationTestData.DEFAULT_ADJUDICATION)

    val intTestData = integrationTestData()

    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = draftUserHeaders,
    )

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
      .acceptReport(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber.toString())

    val dateTimeOfIssue = LocalDateTime.of(2022, 11, 29, 10, 0)

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/issue")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "dateTimeOfIssue" to dateTimeOfIssue,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.issuingOfficer").isEqualTo("ITAG_USER")
      .jsonPath("$.reportedAdjudication.dateTimeOfIssue").isEqualTo("2022-11-29T10:00:00")
      .jsonPath("$.reportedAdjudication.disIssueHistory.size()").isEqualTo(0)

    // re-issue DIS1/2

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/issue")
      .headers(setHeaders(username = IntegrationTestData.DEFAULT_CREATED_USER_ID))
      .bodyValue(
        mapOf(
          "dateTimeOfIssue" to dateTimeOfIssue.plusHours(1),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.issuingOfficer").isEqualTo("B_MILLS")
      .jsonPath("$.reportedAdjudication.dateTimeOfIssue").isEqualTo("2022-11-29T11:00:00")
      .jsonPath("$.reportedAdjudication.disIssueHistory[0].issuingOfficer").isEqualTo("ITAG_USER")
      .jsonPath("$.reportedAdjudication.disIssueHistory[0].dateTimeOfIssue").isEqualTo("2022-11-29T10:00:00")
  }
}
