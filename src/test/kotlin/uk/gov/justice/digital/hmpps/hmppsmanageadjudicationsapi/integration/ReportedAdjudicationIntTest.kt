package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
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
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    webTestClient.get()
      .uri("/reported-adjudications/1524242")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.adjudicationNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
      .jsonPath("$.reportedAdjudication.prisonerNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber)
      .jsonPath("$.reportedAdjudication.bookingId").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.bookingId)
      .jsonPath("$.reportedAdjudication.incidentDetails.dateTimeOfIncident")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfIncidentISOString)
      .jsonPath("$.reportedAdjudication.incidentDetails.locationId")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.locationId)
      .jsonPath("$.reportedAdjudication.incidentDetails.handoverDeadline")
      .isEqualTo(IntegrationTestData.DEFAULT_HANDOVER_DEADLINE_ISO_STRING)
      .jsonPath("$.reportedAdjudication.incidentRole.roleCode")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.incidentRoleCode)
      .jsonPath("$.reportedAdjudication.incidentRole.offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.incidentRoleParagraphNumber)
      .jsonPath("$.reportedAdjudication.incidentRole.offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.incidentRoleParagraphDescription)
      .jsonPath("$.reportedAdjudication.incidentRole.associatedPrisonersNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.incidentRoleAssociatedPrisonersNumber)
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
    status: ReportedAdjudicationStatus,
    expectedCount: Int,
    adjudicationNumber: Long
  ) {

    initMyReportData()

    webTestClient.get()
      .uri("/reported-adjudications/agency/MDI?startDate=$startDate&endDate=$endDate&status=$status&page=0&size=20")
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
    status: ReportedAdjudicationStatus,
    expectedCount: Int,
    adjudicationNumber: Long
  ) {

    initMyReportData()

    webTestClient.get()
      .uri("/reported-adjudications/my/agency/MDI?startDate=$startDate&endDate=$endDate&status=$status&page=0&size=20")
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
      .jsonPath("$.content[0].prisonerNumber").isEqualTo(IntegrationTestData.ADJUDICATION_4.prisonerNumber)
      .jsonPath("$.content[0].bookingId").isEqualTo(IntegrationTestData.ADJUDICATION_4.bookingId)
      .jsonPath("$.content[0].incidentDetails.dateTimeOfIncident")
      .isEqualTo(IntegrationTestData.ADJUDICATION_4.dateTimeOfIncidentISOString)
      .jsonPath("$.content[0].incidentDetails.locationId").isEqualTo(IntegrationTestData.ADJUDICATION_4.locationId)
      .jsonPath("$.content[0].incidentRole.roleCode").isEqualTo(IntegrationTestData.ADJUDICATION_4.incidentRoleCode)
      .jsonPath("$.content[0].incidentRole.offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.ADJUDICATION_4.incidentRoleParagraphNumber)
      .jsonPath("$.content[0].incidentRole.offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.ADJUDICATION_4.incidentRoleParagraphDescription)
      .jsonPath("$.content[0].incidentRole.associatedPrisonersNumber")
      .isEqualTo(IntegrationTestData.ADJUDICATION_4.incidentRoleAssociatedPrisonersNumber)
      .jsonPath("$.content[0].offenceDetails[0].offenceCode")
      .isEqualTo(IntegrationTestData.ADJUDICATION_4.offences[0].offenceCode)
      .jsonPath("$.content[0].offenceDetails[0].offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.ADJUDICATION_4.offences[0].paragraphNumber)
      .jsonPath("$.content[0].offenceDetails[0].offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.ADJUDICATION_4.offences[0].paragraphDescription)
      .jsonPath("$.content[0].offenceDetails[0].victimPrisonersNumber")
      .isEqualTo(IntegrationTestData.ADJUDICATION_4.offences[0].victimPrisonersNumber)
      .jsonPath("$.content[0].offenceDetails[0].victimStaffUsername")
      .isEqualTo(IntegrationTestData.ADJUDICATION_4.offences[0].victimStaffUsername)
      .jsonPath("$.content[0].offenceDetails[0].victimOtherPersonsName")
      .isEqualTo(IntegrationTestData.ADJUDICATION_4.offences[0].victimOtherPersonsName)
      .jsonPath("$.content[0].offenceDetails[1].offenceCode")
      .isEqualTo(IntegrationTestData.ADJUDICATION_4.offences[1].offenceCode)
      .jsonPath("$.content[0].offenceDetails[1].offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.ADJUDICATION_4.offences[1].paragraphNumber)
      .jsonPath("$.content[0].offenceDetails[1].offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.ADJUDICATION_4.offences[1].paragraphDescription)
      .jsonPath("$.content[0].offenceDetails[1].offenceCode")
      .isEqualTo(IntegrationTestData.ADJUDICATION_4.offences[1].offenceCode)
      .jsonPath("$.content[0].offenceDetails[1].victimPrisonersNumber").doesNotExist()
      .jsonPath("$.content[0].offenceDetails[1].victimStaffUsername").doesNotExist()
      .jsonPath("$.content[0].offenceDetails[1].victimOtherPersonsName").doesNotExist()
      .jsonPath("$.content[0].offenceDetails[2]").doesNotExist()
      .jsonPath("$.content[0].incidentStatement.statement").isEqualTo(IntegrationTestData.ADJUDICATION_4.statement)
      .jsonPath("$.content[0].incidentStatement.completed").isEqualTo(true)
      .jsonPath("$.content[0].createdByUserId").isEqualTo(IntegrationTestData.ADJUDICATION_4.createdByUserId)
      .jsonPath("$.content[0].createdDateTime").isEqualTo(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME_TEXT)
      .jsonPath("$.content[1].adjudicationNumber").isEqualTo(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)
  }

  @Test
  fun `return a page of reported adjudications completed in the current agency`() {
    val intTestData = integrationTestData()

    val firstDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_1.createdByUserId)
    val firstDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, firstDraftUserHeaders)
    firstDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_1)
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val secondDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_2.createdByUserId)
    val secondDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, secondDraftUserHeaders)
    secondDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_2)
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val thirdDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_3.createdByUserId)
    val thirdDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, thirdDraftUserHeaders)
    thirdDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_3)
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
      .jsonPath("$.content[0].prisonerNumber").isEqualTo(IntegrationTestData.ADJUDICATION_3.prisonerNumber)
      .jsonPath("$.content[0].bookingId").isEqualTo(IntegrationTestData.ADJUDICATION_3.bookingId)
      .jsonPath("$.content[0].incidentDetails.dateTimeOfIncident")
      .isEqualTo(IntegrationTestData.ADJUDICATION_3.dateTimeOfIncidentISOString)
      .jsonPath("$.content[0].incidentDetails.locationId").isEqualTo(IntegrationTestData.ADJUDICATION_3.locationId)
      .jsonPath("$.content[0].incidentRole.roleCode").isEqualTo(IntegrationTestData.ADJUDICATION_3.incidentRoleCode)
      .jsonPath("$.content[0].incidentRole.offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.ADJUDICATION_3.incidentRoleParagraphNumber)
      .jsonPath("$.content[0].incidentRole.offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.ADJUDICATION_3.incidentRoleParagraphDescription)
      .jsonPath("$.content[0].incidentRole.associatedPrisonersNumber")
      .isEqualTo(IntegrationTestData.ADJUDICATION_3.incidentRoleAssociatedPrisonersNumber)
      .jsonPath("$.content[0].offenceDetails[0].offenceCode")
      .isEqualTo(IntegrationTestData.ADJUDICATION_3.offences[0].offenceCode)
      .jsonPath("$.content[0].offenceDetails[0].offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.ADJUDICATION_3.offences[0].paragraphNumber)
      .jsonPath("$.content[0].offenceDetails[0].offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.ADJUDICATION_3.offences[0].paragraphDescription)
      .jsonPath("$.content[0].offenceDetails[0].victimPrisonersNumber")
      .isEqualTo(IntegrationTestData.ADJUDICATION_3.offences[0].victimPrisonersNumber)
      .jsonPath("$.content[0].offenceDetails[0].victimStaffUsername")
      .isEqualTo(IntegrationTestData.ADJUDICATION_3.offences[0].victimStaffUsername)
      .jsonPath("$.content[0].offenceDetails[0].victimOtherPersonsName")
      .isEqualTo(IntegrationTestData.ADJUDICATION_3.offences[0].victimOtherPersonsName)
      .jsonPath("$.content[0].offenceDetails[1].offenceCode")
      .isEqualTo(IntegrationTestData.ADJUDICATION_3.offences[1].offenceCode)
      .jsonPath("$.content[0].offenceDetails[1].offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.ADJUDICATION_3.offences[1].paragraphNumber)
      .jsonPath("$.content[0].offenceDetails[1].offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.ADJUDICATION_3.offences[1].paragraphDescription)
      .jsonPath("$.content[0].offenceDetails[1].offenceCode")
      .isEqualTo(IntegrationTestData.ADJUDICATION_3.offences[1].offenceCode)
      .jsonPath("$.content[0].offenceDetails[1].victimPrisonersNumber").doesNotExist()
      .jsonPath("$.content[0].offenceDetails[1].victimStaffUsername").doesNotExist()
      .jsonPath("$.content[0].offenceDetails[1].victimOtherPersonsName").doesNotExist()
      .jsonPath("$.content[0].offenceDetails[2]").doesNotExist()
      .jsonPath("$.content[0].incidentStatement.statement").isEqualTo(IntegrationTestData.ADJUDICATION_3.statement)
      .jsonPath("$.content[0].incidentStatement.completed").isEqualTo(true)
      .jsonPath("$.content[0].createdByUserId").isEqualTo(IntegrationTestData.ADJUDICATION_3.createdByUserId)
      .jsonPath("$.content[0].createdDateTime").isEqualTo(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME_TEXT)
      .jsonPath("$.content[1].adjudicationNumber").isEqualTo(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)
  }

  @Test
  fun `get 403 without the relevant role when attempting to return reported adjudications for a caseload`() {
    webTestClient.get()
      .uri("/reported-adjudications/agency/MDI?page=0&size=20")
      .headers(setHeaders(username = "NEW_USER"))
      .exchange()
      .expectStatus().is5xxServerError
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Unexpected error: Access is denied")
  }

  @Test
  fun `create draft from reported adjudication returns expected result`() {
    oAuthMockServer.stubGrantToken()

    val intTestData = integrationTestData()

    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, draftUserHeaders)

    draftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/create-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.draftAdjudication.adjudicationNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber)
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfIncidentISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.locationId")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.locationId)
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline")
      .isEqualTo(IntegrationTestData.DEFAULT_HANDOVER_DEADLINE_ISO_STRING)
      .jsonPath("$.draftAdjudication.incidentRole.roleCode")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.incidentRoleCode)
      .jsonPath("$.draftAdjudication.incidentRole.offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.incidentRoleParagraphNumber)
      .jsonPath("$.draftAdjudication.incidentRole.offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.incidentRoleParagraphDescription)
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.incidentRoleAssociatedPrisonersNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[0].offenceCode")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].offenceCode)
      .jsonPath("$.draftAdjudication.offenceDetails[0].offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].paragraphNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[0].offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].paragraphDescription)
      .jsonPath("$.draftAdjudication.offenceDetails[0].victimPrisonersNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].victimPrisonersNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[0].victimStaffUsername")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].victimStaffUsername)
      .jsonPath("$.draftAdjudication.offenceDetails[0].victimOtherPersonsName")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].victimOtherPersonsName)
      .jsonPath("$.draftAdjudication.offenceDetails[1].offenceCode")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[1].offenceCode)
      .jsonPath("$.draftAdjudication.offenceDetails[1].offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[1].paragraphNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[1].offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[1].paragraphDescription)
      .jsonPath("$.draftAdjudication.offenceDetails[1].victimPrisonersNumber").doesNotExist()
      .jsonPath("$.draftAdjudication.offenceDetails[1].victimStaffUsername").doesNotExist()
      .jsonPath("$.draftAdjudication.offenceDetails[1].victimOtherPersonsName").doesNotExist()
      .jsonPath("$.draftAdjudication.offenceDetails[2]").doesNotExist()
      .jsonPath("$.draftAdjudication.incidentStatement.completed").isEqualTo(true)
      .jsonPath("$.draftAdjudication.incidentStatement.statement")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.statement)
      .jsonPath("$.draftAdjudication.startedByUserId")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
  }

  @Test
  fun `create draft from reported adjudication adds draft`() {
    oAuthMockServer.stubGrantToken()

    val intTestData = integrationTestData()

    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, draftUserHeaders)

    draftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
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

  private fun initMyReportData() {
    val intTestData = integrationTestData()

    val firstDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_2.createdByUserId)
    val firstDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, firstDraftUserHeaders)
    firstDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_2)
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val secondDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_3.createdByUserId)
    val secondDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, secondDraftUserHeaders)
    secondDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_3)
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val thirdDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_4.createdByUserId)
    val thirdDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, thirdDraftUserHeaders)
    thirdDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_4)
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val fourthDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_5.createdByUserId)
    val fourthDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, fourthDraftUserHeaders)
    fourthDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_5)
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()
  }
}
