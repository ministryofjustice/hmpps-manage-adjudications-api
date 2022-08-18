package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.DraftAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.IncidentRoleRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.JwtAuthHelper
import java.time.LocalDateTime

class IntegrationTestData(
  private val webTestClient: WebTestClient,
  private val jwtAuthHelper: JwtAuthHelper,
  private val prisonApiMockServer: PrisonApiMockServer
) {

  companion object {
    val BASIC_OFFENCE = OffenceTestDataSet(
      offenceCode = 1003,
      paragraphNumber = "1",
      paragraphDescription = "Commits any assault"
    )
    val FULL_OFFENCE = OffenceTestDataSet(
      offenceCode = 4001,
      paragraphNumber = "4",
      paragraphDescription = "Fights with any person",
      victimPrisonersNumber = "A1234AA",
      victimStaffUsername = "ABC12D",
      victimOtherPersonsName = "A. User",
    )
    val YOUTH_OFFENCE = OffenceTestDataSet(
      offenceCode = 4001,
      paragraphNumber = "5",
      paragraphDescription = "Fights with any person",
    )

    const val DEFAULT_ADJUDICATION_NUMBER = 1524242L
    const val DEFAULT_PRISONER_NUMBER = "AA1234A"
    const val DEFAULT_PRISONER_BOOKING_ID = 123L
    const val DEFAULT_AGENCY_ID = "MDI"
    const val DEFAULT_CREATED_USER_ID = "B_MILLS"
    val DEFAULT_DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 11, 12, 10, 0)
    const val DEFAULT_DATE_TIME_OF_INCIDENT_TEXT = "2010-11-12T10:00:00" // this is a friday
    const val DEFAULT_HANDOVER_DEADLINE_ISO_STRING = "2010-11-14T10:00:00"
    const val DEFAULT_INCIDENT_ROLE_CODE = "25a"
    const val DEFAULT_INCIDENT_ROLE_PARAGRAPH_NUMBER = "25(a)"
    const val DEFAULT_INCIDENT_ROLE_PARAGRAPH_DESCRIPTION = "Attempts to commit any of the foregoing offences:"
    const val DEFAULT_INCIDENT_ROLE_ASSOCIATED_PRISONER = "B2345BB"
    val DEFAULT_OFFENCES = listOf(FULL_OFFENCE, BASIC_OFFENCE)
    val DEFAULT_YOUTH_OFFENCES = listOf(YOUTH_OFFENCE)
    const val DEFAULT_STATEMENT = "A statement"
    val DEFAULT_REPORTED_DATE_TIME = DEFAULT_DATE_TIME_OF_INCIDENT.plusDays(1)
    const val DEFAULT_REPORTED_DATE_TIME_TEXT = "2010-11-13T10:00:00"
    val DEFAULT_EXPECTED_NOMIS_DATA = NomisOffenceTestDataSet(
      nomisCodes = listOf("51:4", "51:1A"), // 2 offences
      victimStaffUsernames = listOf("ABC12D"),
      victimPrisonersNumbers = listOf("A1234AA"),
    )
    val DEFAULT_DAMAGES = listOf(DamagesTestDataSet(code = DamageCode.CLEANING, details = "details"))
    val UPDATED_DAMAGES = listOf(DamagesTestDataSet(code = DamageCode.REDECORATION, details = "details"))

    const val UPDATED_DATE_TIME_OF_INCIDENT_TEXT = "2010-11-13T10:00:00" // 13 is saturday
    const val UPDATED_HANDOVER_DEADLINE_ISO_STRING = "2010-11-15T10:00:00"
    const val UPDATED_LOCATION_ID = 721899L
    const val UPDATED_INCIDENT_ROLE_CODE = "25b" // seems to be 25a now.
    const val UPDATED_INCIDENT_ROLE_PARAGRAPH_NUMBER = "25(b)"
    const val UPDATED_INCIDENT_ROLE_PARAGRAPH_DESCRIPTION =
      "Incites another prisoner to commit any of the foregoing offences:"
    const val UPDATED_INCIDENT_ROLE_ASSOCIATED_PRISONER = "C3456CC"
    val UPDATED_OFFENCES = listOf(BASIC_OFFENCE)
    const val UPDATED_STATEMENT = "updated test statement"
    val UPDATED_DATE_TIME_OF_INCIDENT = DEFAULT_DATE_TIME_OF_INCIDENT.plusDays(1)

    val DEFAULT_ADJUDICATION = AdjudicationIntTestDataSet(
      adjudicationNumber = DEFAULT_ADJUDICATION_NUMBER,
      prisonerNumber = DEFAULT_PRISONER_NUMBER,
      bookingId = DEFAULT_PRISONER_BOOKING_ID,
      agencyId = DEFAULT_AGENCY_ID,
      locationId = UPDATED_LOCATION_ID,
      dateTimeOfIncidentISOString = DEFAULT_DATE_TIME_OF_INCIDENT_TEXT,
      dateTimeOfIncident = DEFAULT_DATE_TIME_OF_INCIDENT,
      handoverDeadlineISOString = DEFAULT_HANDOVER_DEADLINE_ISO_STRING,
      isYouthOffender = false,
      incidentRoleCode = DEFAULT_INCIDENT_ROLE_CODE,
      incidentRoleParagraphNumber = DEFAULT_INCIDENT_ROLE_PARAGRAPH_NUMBER,
      incidentRoleParagraphDescription = DEFAULT_INCIDENT_ROLE_PARAGRAPH_DESCRIPTION,
      incidentRoleAssociatedPrisonersNumber = DEFAULT_INCIDENT_ROLE_ASSOCIATED_PRISONER,
      offences = DEFAULT_OFFENCES,
      statement = DEFAULT_STATEMENT,
      createdByUserId = DEFAULT_CREATED_USER_ID,
      damages = DEFAULT_DAMAGES
    )

    val UPDATED_ADJUDICATION = AdjudicationIntTestDataSet(
      adjudicationNumber = DEFAULT_ADJUDICATION_NUMBER,
      prisonerNumber = DEFAULT_PRISONER_NUMBER,
      bookingId = DEFAULT_PRISONER_BOOKING_ID,
      agencyId = DEFAULT_AGENCY_ID,
      locationId = UPDATED_LOCATION_ID,
      dateTimeOfIncidentISOString = UPDATED_DATE_TIME_OF_INCIDENT_TEXT,
      dateTimeOfIncident = UPDATED_DATE_TIME_OF_INCIDENT,
      handoverDeadlineISOString = UPDATED_HANDOVER_DEADLINE_ISO_STRING,
      isYouthOffender = true,
      incidentRoleCode = UPDATED_INCIDENT_ROLE_CODE,
      incidentRoleParagraphNumber = UPDATED_INCIDENT_ROLE_PARAGRAPH_NUMBER,
      incidentRoleParagraphDescription = UPDATED_INCIDENT_ROLE_PARAGRAPH_DESCRIPTION,
      incidentRoleAssociatedPrisonersNumber = UPDATED_INCIDENT_ROLE_ASSOCIATED_PRISONER,
      offences = UPDATED_OFFENCES,
      statement = UPDATED_STATEMENT,
      createdByUserId = DEFAULT_CREATED_USER_ID,
      damages = UPDATED_DAMAGES
    )

    val ADJUDICATION_1 = AdjudicationIntTestDataSet(
      adjudicationNumber = 456L,
      prisonerNumber = "BB2345B",
      bookingId = 31L,
      agencyId = "LEI",
      locationId = 11L,
      dateTimeOfIncidentISOString = "2020-12-13T08:00:00",
      dateTimeOfIncident = LocalDateTime.parse("2020-12-13T08:00:00"),
      handoverDeadlineISOString = "2020-12-15T08:00:00",
      isYouthOffender = false,
      incidentRoleCode = "25c",
      incidentRoleParagraphNumber = "25(c)",
      incidentRoleParagraphDescription = "Assists another prisoner to commit, or to attempt to commit, any of the foregoing offences:",
      incidentRoleAssociatedPrisonersNumber = "D4567DD",
      offences = DEFAULT_OFFENCES,
      statement = "Test statement",
      createdByUserId = "A_NESS",
      damages = DEFAULT_DAMAGES
    )

    val ADJUDICATION_2 = AdjudicationIntTestDataSet(
      adjudicationNumber = 567L,
      prisonerNumber = "CC2345C",
      bookingId = 32L,
      agencyId = "MDI",
      locationId = 12L,
      dateTimeOfIncidentISOString = "2020-12-14T09:00:00",
      dateTimeOfIncident = LocalDateTime.parse("2020-12-14T09:00:00"),
      handoverDeadlineISOString = "2020-12-16T09:00:00",
      isYouthOffender = true,
      incidentRoleCode = "25a",
      incidentRoleParagraphNumber = "29(a)",
      incidentRoleParagraphDescription = "Attempts to commit any of the foregoing offences:",
      incidentRoleAssociatedPrisonersNumber = "A5678AA",
      offences = DEFAULT_YOUTH_OFFENCES,
      statement = "Different test statement",
      createdByUserId = "P_NESS",
      damages = DEFAULT_DAMAGES
    )

    val ADJUDICATION_3 = AdjudicationIntTestDataSet(
      adjudicationNumber = 789L,
      prisonerNumber = "DD3456D",
      bookingId = 33L,
      agencyId = "MDI",
      locationId = 13L,
      dateTimeOfIncidentISOString = "2020-12-15T10:00:00",
      dateTimeOfIncident = LocalDateTime.parse("2020-12-15T10:00:00"),
      handoverDeadlineISOString = "2020-12-17T10:00:00",
      isYouthOffender = false,
      incidentRoleCode = "25c",
      incidentRoleParagraphNumber = "25(c)",
      incidentRoleParagraphDescription = "Assists another prisoner to commit, or to attempt to commit, any of the foregoing offences:",
      incidentRoleAssociatedPrisonersNumber = "D4567DD",
      offences = DEFAULT_OFFENCES,
      statement = "Another test statement",
      createdByUserId = "L_NESS",
      damages = DEFAULT_DAMAGES
    )

    val ADJUDICATION_4 = AdjudicationIntTestDataSet(
      adjudicationNumber = 1234L,
      prisonerNumber = "EE4567E",
      bookingId = 34L,
      agencyId = "MDI",
      locationId = 14L,
      dateTimeOfIncidentISOString = "2020-12-16T10:00:00",
      dateTimeOfIncident = LocalDateTime.parse("2020-12-16T10:00:00"),
      handoverDeadlineISOString = "2020-12-18T10:00:00",
      isYouthOffender = true,
      incidentRoleCode = "25a",
      incidentRoleParagraphNumber = "29(a)",
      incidentRoleParagraphDescription = "Attempts to commit any of the foregoing offences:",
      incidentRoleAssociatedPrisonersNumber = "A5678AA",
      offences = DEFAULT_YOUTH_OFFENCES,
      statement = "Yet another test statement",
      createdByUserId = "P_NESS",
      damages = DEFAULT_DAMAGES
    )

    val ADJUDICATION_5 = AdjudicationIntTestDataSet(
      adjudicationNumber = 2345L,
      prisonerNumber = "FF4567F",
      bookingId = 35L,
      agencyId = "LEI",
      locationId = 15L,
      dateTimeOfIncidentISOString = "2020-12-17T10:00:00",
      dateTimeOfIncident = LocalDateTime.parse("2020-12-17T10:00:00"),
      handoverDeadlineISOString = "2020-12-19T10:00:00",
      isYouthOffender = false,
      incidentRoleCode = "25a",
      incidentRoleParagraphNumber = "25(a)",
      incidentRoleParagraphDescription = "Attempts to commit any of the foregoing offences:",
      incidentRoleAssociatedPrisonersNumber = "A5678AA",
      offences = DEFAULT_OFFENCES,
      statement = "Keep on with the test statements",
      createdByUserId = "P_NESS",
      damages = DEFAULT_DAMAGES
    )
  }

  fun getDraftAdjudicationDetails(
    draftCreationData: DraftAdjudicationResponse,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): WebTestClient.ResponseSpec = webTestClient.get()
    .uri("/draft-adjudications/${draftCreationData.draftAdjudication.id}")
    .headers(headers)
    .exchange()

  fun startNewAdjudication(
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): DraftAdjudicationResponse {

    return webTestClient.post()
      .uri("/draft-adjudications")
      .headers(headers)
      .bodyValue(
        mapOf(
          "prisonerNumber" to testDataSet.prisonerNumber,
          "agencyId" to testDataSet.agencyId,
          "locationId" to testDataSet.locationId,
          "dateTimeOfIncident" to testDataSet.dateTimeOfIncident,
        )
      )
      .exchange()
      .expectStatus().is2xxSuccessful
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }

  fun setIncidentRole(
    draftCreationData: DraftAdjudicationResponse,
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()

  ): DraftAdjudicationResponse {
    return webTestClient.put()
      .uri("/draft-adjudications/${draftCreationData.draftAdjudication.id}/incident-role")
      .headers(headers)
      .bodyValue(
        mapOf(
          "incidentRole" to IncidentRoleRequest(
            testDataSet.incidentRoleCode,
          ),
          "removeExistingOffences" to true,
        )
      ).exchange()
      .expectStatus().is2xxSuccessful
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }

  fun setAssociatedPrisoner(
    draftCreationData: DraftAdjudicationResponse,
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): DraftAdjudicationResponse {

    return webTestClient.put()
      .uri("/draft-adjudications/${draftCreationData.draftAdjudication.id}/associated-prisoner")
      .headers(headers)
      .bodyValue(
        mapOf(
          "associatedPrisonersNumber" to testDataSet.incidentRoleAssociatedPrisonersNumber,
        ),
      ).exchange()
      .expectStatus().is2xxSuccessful
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }

  fun setApplicableRules(
    draftCreationData: DraftAdjudicationResponse,
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): DraftAdjudicationResponse {
    return webTestClient.put()
      .uri("/draft-adjudications/${draftCreationData.draftAdjudication.id}/applicable-rules")
      .headers(headers)
      .bodyValue(
        mapOf(
          "isYouthOffenderRule" to testDataSet.isYouthOffender,
        )
      )
      .exchange()
      .expectStatus().is2xxSuccessful
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }

  fun setOffenceDetails(
    draftCreationData: DraftAdjudicationResponse,
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): DraftAdjudicationResponse {
    return webTestClient.put()
      .uri("/draft-adjudications/${draftCreationData.draftAdjudication.id}/offence-details")
      .headers(headers)
      .bodyValue(
        mapOf(
          "offenceDetails" to testDataSet.offences,
        )
      )
      .exchange()
      .expectStatus().is2xxSuccessful
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }

  fun addIncidentStatement(
    draftCreationData: DraftAdjudicationResponse,
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): DraftAdjudicationResponse {
    return webTestClient.post()
      .uri("/draft-adjudications/${draftCreationData.draftAdjudication.id}/incident-statement")
      .headers(headers)
      .bodyValue(
        mapOf(
          "statement" to testDataSet.statement
        )
      )
      .exchange()
      .expectStatus().is2xxSuccessful
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }

  fun addDamages(
    draftCreationData: DraftAdjudicationResponse,
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): DraftAdjudicationResponse {
    return webTestClient.put()
      .uri("/draft-adjudications/${draftCreationData.draftAdjudication.id}/damages")
      .headers(headers)
      .bodyValue(
        mapOf(
          "damages" to testDataSet.damages,
        )
      )
      .exchange()
      .expectStatus().is2xxSuccessful
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }

  fun editIncidentDetails(
    draftAdjudicationResponse: DraftAdjudicationResponse,
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ) {
    webTestClient.put()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/incident-details")
      .headers(headers)
      .bodyValue(
        mapOf(
          "locationId" to testDataSet.locationId,
          "dateTimeOfIncident" to testDataSet.dateTimeOfIncidentISOString,
          "incidentRole" to IncidentRoleRequest("25b"),
        )
      )
      .exchange()
      .expectStatus().is2xxSuccessful
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }

  fun editIncidentStatement(
    draftCreationData: DraftAdjudicationResponse,
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): DraftAdjudicationResponse {
    return webTestClient.put()
      .uri("/draft-adjudications/${draftCreationData.draftAdjudication.id}/incident-statement")
      .headers(headers)
      .bodyValue(
        mapOf(
          "statement" to testDataSet.statement
        )
      )
      .exchange()
      .expectStatus().is2xxSuccessful
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }

  fun completeDraftAdjudication(
    draftCreationData: DraftAdjudicationResponse,
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): WebTestClient.ResponseSpec {
    prisonApiMockServer.stubPostAdjudicationCreationRequestData(testDataSet)

    return webTestClient.post()
      .uri("/draft-adjudications/${draftCreationData.draftAdjudication.id}/complete-draft-adjudication")
      .headers(headers)
      .exchange()
  }

  fun reportedAdjudicationStatus(
    reportedAdjudicationStatus: ReportedAdjudicationStatus,
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): WebTestClient.ResponseSpec {
    return webTestClient.put()
      .uri("/reported-adjudications/${testDataSet.adjudicationNumber}/status")
      .bodyValue(mapOf("status" to reportedAdjudicationStatus))
      .headers(headers)
      .exchange()
  }

  fun recallCompletedDraftAdjudication(
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): DraftAdjudicationResponse {
    return webTestClient.post()
      .uri("/reported-adjudications/${testDataSet.adjudicationNumber}/create-draft-adjudication")
      .headers(headers)
      .exchange()
      .expectStatus().is2xxSuccessful
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }

  fun setHeaders(
    contentType: MediaType = MediaType.APPLICATION_JSON,
    username: String? = "ITAG_USER",
    roles: List<String> = emptyList()
  ): (HttpHeaders) -> Unit = {
    it.setBearerAuth(jwtAuthHelper.createJwt(subject = username, roles = roles, scope = listOf("write")))
    it.contentType = contentType
  }
}
