package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.Test

class ReportedAdjudicationIntTest : IntegrationTestBase() {

  @Test
  fun `get reported adjudication details`() {
    oAuthMockServer.stubGrantToken()
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

    val firstDraftUserHeaders = setHeaders(username = IntTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val firstDraftCreationResponse = intTestData.startNewAdjudication(IntTestData.DEFAULT_ADJUDICATION, firstDraftUserHeaders)
    intTestData.addIncidentStatement(firstDraftCreationResponse, IntTestData.DEFAULT_ADJUDICATION, firstDraftUserHeaders)
    intTestData.completeDraftAdjudication(firstDraftCreationResponse, IntTestData.DEFAULT_ADJUDICATION, firstDraftUserHeaders)

    webTestClient.get()
      .uri("/reported-adjudications/1524242")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.adjudicationNumber").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
      .jsonPath("$.reportedAdjudication.prisonerNumber").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.prisonerNumber)
      .jsonPath("$.reportedAdjudication.bookingId").isEqualTo(1) // From prisonApi.stubPostAdjudication
      .jsonPath("$.reportedAdjudication.dateTimeReportExpires").isEqualTo(IntTestData.DEFAULT_HANDOVER_DEADLINE_ISO_STRING)
      .jsonPath("$.reportedAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.dateTimeOfIncidentISOString)
      .jsonPath("$.reportedAdjudication.incidentDetails.locationId").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.locationId)
      .jsonPath("$.reportedAdjudication.createdByUserId").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.createdByUserId)
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
  fun `return a page of reported adjudications completed by the current user`() {
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

    val firstDraftUserHeaders = setHeaders(username = IntTestData.ADJUDICATION_2.createdByUserId)
    val firstDraftCreationResponse = intTestData.startNewAdjudication(IntTestData.ADJUDICATION_2, firstDraftUserHeaders)
    intTestData.addIncidentStatement(firstDraftCreationResponse, IntTestData.ADJUDICATION_2, firstDraftUserHeaders)
    intTestData.completeDraftAdjudication(firstDraftCreationResponse, IntTestData.ADJUDICATION_2, firstDraftUserHeaders)

    val secondDraftUserHeaders = setHeaders(username = IntTestData.ADJUDICATION_3.createdByUserId)
    val secondDraftCreationResponse = intTestData.startNewAdjudication(IntTestData.ADJUDICATION_3, secondDraftUserHeaders)
    intTestData.addIncidentStatement(secondDraftCreationResponse, IntTestData.ADJUDICATION_3, secondDraftUserHeaders)
    intTestData.completeDraftAdjudication(secondDraftCreationResponse, IntTestData.ADJUDICATION_3, secondDraftUserHeaders)

    val thirdDraftUserHeaders = setHeaders(username = IntTestData.ADJUDICATION_4.createdByUserId)
    val thirdDraftCreationResponse = intTestData.startNewAdjudication(IntTestData.ADJUDICATION_4, thirdDraftUserHeaders)
    intTestData.addIncidentStatement(thirdDraftCreationResponse, IntTestData.ADJUDICATION_4, thirdDraftUserHeaders)
    intTestData.completeDraftAdjudication(thirdDraftCreationResponse, IntTestData.ADJUDICATION_4, thirdDraftUserHeaders)

    val fourthDraftUserHeaders = setHeaders(username = IntTestData.ADJUDICATION_5.createdByUserId)
    val fourthDraftCreationResponse = intTestData.startNewAdjudication(IntTestData.ADJUDICATION_5, fourthDraftUserHeaders)
    intTestData.addIncidentStatement(fourthDraftCreationResponse, IntTestData.ADJUDICATION_5, fourthDraftUserHeaders)
    intTestData.completeDraftAdjudication(fourthDraftCreationResponse, IntTestData.ADJUDICATION_5, fourthDraftUserHeaders)

    webTestClient.get()
      .uri("/reported-adjudications/my/agency/MDI?page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content[0].adjudicationNumber").isEqualTo(IntTestData.ADJUDICATION_4.adjudicationNumber)
      .jsonPath("$.content[0].prisonerNumber").isEqualTo(IntTestData.ADJUDICATION_4.prisonerNumber)
      .jsonPath("$.content[0].bookingId").isEqualTo(1) // From PrisonAPiMockServer.stubPostAdjudication
      .jsonPath("$.content[0].incidentDetails.dateTimeOfIncident")
      .isEqualTo(IntTestData.ADJUDICATION_4.dateTimeOfIncidentISOString)
      .jsonPath("$.content[0].incidentDetails.locationId").isEqualTo(IntTestData.ADJUDICATION_4.locationId)
      .jsonPath("$.content[0].createdByUserId").isEqualTo(IntTestData.ADJUDICATION_4.createdByUserId)
      .jsonPath("$.content[1].adjudicationNumber").isEqualTo(IntTestData.ADJUDICATION_2.adjudicationNumber)
      .jsonPath("$.content[1].prisonerNumber").isEqualTo(IntTestData.ADJUDICATION_2.prisonerNumber)
      .jsonPath("$.content[1].bookingId").isEqualTo(1) // From PrisonAPiMockServer.stubPostAdjudication
      .jsonPath("$.content[1].incidentDetails.dateTimeOfIncident")
      .isEqualTo(IntTestData.ADJUDICATION_2.dateTimeOfIncidentISOString)
      .jsonPath("$.content[1].incidentDetails.locationId").isEqualTo(IntTestData.ADJUDICATION_2.locationId)
      .jsonPath("$.content[1].createdByUserId").isEqualTo(IntTestData.ADJUDICATION_2.createdByUserId)
  }

  @Test
  fun `return a page of reported adjudications completed in the current agency`() {
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

    val firstDraftCreationResponse = intTestData.startNewAdjudication(IntTestData.ADJUDICATION_1)
    intTestData.addIncidentStatement(firstDraftCreationResponse, IntTestData.ADJUDICATION_1)
    intTestData.completeDraftAdjudication(
      firstDraftCreationResponse,
      IntTestData.ADJUDICATION_1,
      setHeaders(username = IntTestData.ADJUDICATION_1.createdByUserId)
    )

    val secondDraftCreationResponse = intTestData.startNewAdjudication(IntTestData.ADJUDICATION_2)
    intTestData.addIncidentStatement(secondDraftCreationResponse, IntTestData.ADJUDICATION_2)
    intTestData.completeDraftAdjudication(
      secondDraftCreationResponse,
      IntTestData.ADJUDICATION_2,
      setHeaders(username = IntTestData.ADJUDICATION_2.createdByUserId)
    )

    val thirdDraftCreationResponse = intTestData.startNewAdjudication(IntTestData.ADJUDICATION_3)
    intTestData.addIncidentStatement(thirdDraftCreationResponse, IntTestData.ADJUDICATION_3)
    intTestData.completeDraftAdjudication(
      thirdDraftCreationResponse,
      IntTestData.ADJUDICATION_3,
      setHeaders(username = IntTestData.ADJUDICATION_3.createdByUserId)
    )

    webTestClient.get()
      .uri("/reported-adjudications/agency/MDI?page=0&size=20")
      .headers(setHeaders(username = "NEW_USER", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content[0].adjudicationNumber").isEqualTo(IntTestData.ADJUDICATION_3.adjudicationNumber)
      .jsonPath("$.content[0].prisonerNumber").isEqualTo(IntTestData.ADJUDICATION_3.prisonerNumber)
      .jsonPath("$.content[0].bookingId").isEqualTo(1) // From PrisonAPiMockServer.stubPostAdjudication
      .jsonPath("$.content[0].incidentDetails.dateTimeOfIncident")
      .isEqualTo(IntTestData.ADJUDICATION_3.dateTimeOfIncidentISOString)
      .jsonPath("$.content[0].incidentDetails.locationId").isEqualTo(IntTestData.ADJUDICATION_3.locationId)
      .jsonPath("$.content[0].createdByUserId").isEqualTo(IntTestData.ADJUDICATION_3.createdByUserId)
      .jsonPath("$.content[1].adjudicationNumber").isEqualTo(IntTestData.ADJUDICATION_2.adjudicationNumber)
      .jsonPath("$.content[1].prisonerNumber").isEqualTo(IntTestData.ADJUDICATION_2.prisonerNumber)
      .jsonPath("$.content[1].bookingId").isEqualTo(1) // From PrisonAPiMockServer.stubPostAdjudication
      .jsonPath("$.content[1].incidentDetails.dateTimeOfIncident")
      .isEqualTo(IntTestData.ADJUDICATION_2.dateTimeOfIncidentISOString)
      .jsonPath("$.content[1].incidentDetails.locationId").isEqualTo(IntTestData.ADJUDICATION_2.locationId)
      .jsonPath("$.content[1].createdByUserId").isEqualTo(IntTestData.ADJUDICATION_2.createdByUserId)
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
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

    val firstDraftUserHeaders = setHeaders(username = IntTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val firstDraftCreationResponse = intTestData.startNewAdjudication(IntTestData.DEFAULT_ADJUDICATION, firstDraftUserHeaders)
    intTestData.addIncidentStatement(firstDraftCreationResponse, IntTestData.DEFAULT_ADJUDICATION, firstDraftUserHeaders)
    intTestData.completeDraftAdjudication(firstDraftCreationResponse, IntTestData.DEFAULT_ADJUDICATION, firstDraftUserHeaders)

    webTestClient.post()
      .uri("/reported-adjudications/${IntTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/create-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.draftAdjudication.adjudicationNumber").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.prisonerNumber)
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.dateTimeOfIncidentISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.locationId)
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").isEqualTo(IntTestData.DEFAULT_HANDOVER_DEADLINE_ISO_STRING)
      // To be added
      // .jsonPath("$.draftAdjudication.incidentRole.roleCode").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.incidentRoleCode)
      // .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.incidentRoleAssociatedPrisonersNumber)
      .jsonPath("$.draftAdjudication.incidentStatement.completed").isEqualTo(true)
      .jsonPath("$.draftAdjudication.incidentStatement.statement").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.statement)
      .jsonPath("$.draftAdjudication.startedByUserId").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.createdByUserId)
  }

  @Test
  fun `create draft from reported adjudication adds draft`() {
    oAuthMockServer.stubGrantToken()
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

    val firstDraftUserHeaders = setHeaders(username = IntTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val firstDraftCreationResponse = intTestData.startNewAdjudication(IntTestData.DEFAULT_ADJUDICATION, firstDraftUserHeaders)
    intTestData.addIncidentStatement(firstDraftCreationResponse, IntTestData.DEFAULT_ADJUDICATION, firstDraftUserHeaders)
    intTestData.completeDraftAdjudication(firstDraftCreationResponse, IntTestData.DEFAULT_ADJUDICATION, firstDraftUserHeaders)

    val createdDraftDetails = intTestData.recallCompletedDraftAdjudication(IntTestData.DEFAULT_ADJUDICATION)

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
}
