package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DraftAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.IncidentRoleRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ReportedAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
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
      paragraphNumber = "1(a)",
      paragraphDescription = "Commits any racially aggravated assault"
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
    const val DEFAULT_DATE_TIME_OF_DISCOVERY_TEXT = "2010-11-13T10:00:00"
    const val DEFAULT_DATE_TIME_OF_HEARING_TEXT = "2010-11-19T10:00:00"
    const val DEFAULT_HANDOVER_DEADLINE_ISO_STRING = "2010-11-15T10:00:00"
    const val DEFAULT_INCIDENT_ROLE_CODE = "25a"
    const val DEFAULT_INCIDENT_ROLE_PARAGRAPH_NUMBER = "25(a)"
    const val DEFAULT_INCIDENT_ROLE_PARAGRAPH_DESCRIPTION = "Attempts to commit any of the foregoing offences:"
    const val DEFAULT_INCIDENT_ROLE_ASSOCIATED_PRISONER = "B2345BB"
    val DEFAULT_OFFENCE = FULL_OFFENCE
    val DEFAULT_YOUTH_OFFENCE = YOUTH_OFFENCE
    const val DEFAULT_STATEMENT = "A statement"
    val DEFAULT_REPORTED_DATE_TIME = DEFAULT_DATE_TIME_OF_INCIDENT.plusDays(1)
    const val DEFAULT_REPORTED_DATE_TIME_TEXT = "2010-11-13T10:00:00"
    val DEFAULT_EXPECTED_NOMIS_DATA = NomisOffenceTestDataSet(
      nomisCodes = listOf("51:4"),
      victimStaffUsernames = listOf("ABC12D"),
      victimPrisonersNumbers = listOf("A1234AA"),
    )
    val DEFAULT_DAMAGES = listOf(DamagesTestDataSet(code = DamageCode.CLEANING, details = "details"))
    val UPDATED_DAMAGES = listOf(DamagesTestDataSet(code = DamageCode.REDECORATION, details = "details"))
    val DEFAULT_EVIDENCE = listOf(EvidenceTestDataSet(code = EvidenceCode.PHOTO, details = "details"))
    val UPDATED_EVIDENCE = listOf(EvidenceTestDataSet(code = EvidenceCode.BAGGED_AND_TAGGED, details = "details"))
    val DEFAULT_WITNESSES = listOf(WitnessTestDataSet(code = WitnessCode.OFFICER, firstName = "prison", lastName = "officer"))
    val UPDATED_WITNESSES = listOf(WitnessTestDataSet(code = WitnessCode.STAFF, firstName = "staff", lastName = "member"))

    const val UPDATED_DATE_TIME_OF_INCIDENT_TEXT = "2010-11-13T10:00:00" // 13 is saturday
    const val UPDATED_HANDOVER_DEADLINE_ISO_STRING = "2010-11-15T10:00:00"
    const val UPDATED_LOCATION_ID = 721899L
    const val UPDATED_INCIDENT_ROLE_CODE = "25b" // seems to be 25a now.
    const val UPDATED_INCIDENT_ROLE_PARAGRAPH_NUMBER = "25(b)"
    const val UPDATED_INCIDENT_ROLE_PARAGRAPH_DESCRIPTION =
      "Incites another prisoner to commit any of the foregoing offences:"
    const val UPDATED_INCIDENT_ROLE_ASSOCIATED_PRISONER = "C3456CC"
    val UPDATED_OFFENCE = BASIC_OFFENCE
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
      dateTimeOfDiscovery = DEFAULT_DATE_TIME_OF_INCIDENT.plusDays(1),
      dateTimeOfDiscoveryISOString = DEFAULT_DATE_TIME_OF_DISCOVERY_TEXT,
      handoverDeadlineISOString = DEFAULT_HANDOVER_DEADLINE_ISO_STRING,
      isYouthOffender = false,
      incidentRoleCode = DEFAULT_INCIDENT_ROLE_CODE,
      incidentRoleParagraphNumber = DEFAULT_INCIDENT_ROLE_PARAGRAPH_NUMBER,
      incidentRoleParagraphDescription = DEFAULT_INCIDENT_ROLE_PARAGRAPH_DESCRIPTION,
      incidentRoleAssociatedPrisonersNumber = DEFAULT_INCIDENT_ROLE_ASSOCIATED_PRISONER,
      offence = DEFAULT_OFFENCE,
      statement = DEFAULT_STATEMENT,
      createdByUserId = DEFAULT_CREATED_USER_ID,
      damages = DEFAULT_DAMAGES,
      evidence = DEFAULT_EVIDENCE,
      witnesses = DEFAULT_WITNESSES,
      dateTimeOfHearing = DEFAULT_DATE_TIME_OF_INCIDENT.plusWeeks(1),
      dateTimeOfHearingISOString = DEFAULT_DATE_TIME_OF_HEARING_TEXT,
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
      offence = UPDATED_OFFENCE,
      statement = UPDATED_STATEMENT,
      createdByUserId = DEFAULT_CREATED_USER_ID,
      damages = UPDATED_DAMAGES,
      evidence = UPDATED_EVIDENCE,
      witnesses = UPDATED_WITNESSES
    )

    val ADJUDICATION_1 = AdjudicationIntTestDataSet(
      adjudicationNumber = 456L,
      prisonerNumber = "BB2345B",
      bookingId = 31L,
      agencyId = "LEI",
      locationId = 11L,
      dateTimeOfIncidentISOString = "2020-12-13T08:00:00",
      dateTimeOfIncident = LocalDateTime.parse("2020-12-13T08:00:00"),
      dateTimeOfDiscovery = LocalDateTime.parse("2020-12-14T08:00:00"),
      dateTimeOfDiscoveryISOString = "2020-12-13T08:00:00",
      handoverDeadlineISOString = "2020-12-16T08:00:00",
      isYouthOffender = false,
      incidentRoleCode = "25c",
      incidentRoleParagraphNumber = "25(c)",
      incidentRoleParagraphDescription = "Assists another prisoner to commit, or to attempt to commit, any of the foregoing offences:",
      incidentRoleAssociatedPrisonersNumber = "D4567DD",
      offence = DEFAULT_OFFENCE,
      statement = "Test statement",
      createdByUserId = "A_NESS",
      damages = DEFAULT_DAMAGES,
      evidence = DEFAULT_EVIDENCE,
      witnesses = DEFAULT_WITNESSES
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
      offence = DEFAULT_YOUTH_OFFENCE,
      statement = "Different test statement",
      createdByUserId = "P_NESS",
      damages = DEFAULT_DAMAGES,
      evidence = DEFAULT_EVIDENCE,
      witnesses = DEFAULT_WITNESSES
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
      offence = DEFAULT_OFFENCE,
      statement = "Another test statement",
      createdByUserId = "L_NESS",
      damages = DEFAULT_DAMAGES,
      evidence = DEFAULT_EVIDENCE,
      witnesses = DEFAULT_WITNESSES
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
      offence = DEFAULT_YOUTH_OFFENCE,
      statement = "Yet another test statement",
      createdByUserId = "P_NESS",
      damages = DEFAULT_DAMAGES,
      evidence = DEFAULT_EVIDENCE,
      witnesses = DEFAULT_WITNESSES
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
      offence = DEFAULT_OFFENCE,
      statement = "Keep on with the test statements",
      createdByUserId = "P_NESS",
      damages = DEFAULT_DAMAGES,
      evidence = DEFAULT_EVIDENCE,
      witnesses = DEFAULT_WITNESSES
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
          "gender" to testDataSet.gender.name,
          "agencyId" to testDataSet.agencyId,
          "locationId" to testDataSet.locationId,
          "dateTimeOfIncident" to testDataSet.dateTimeOfIncident,
          "dateTimeOfDiscovery" to testDataSet.dateTimeOfDiscovery
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
          "offenceDetails" to testDataSet.offence,
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

  fun addEvidence(
    draftCreationData: DraftAdjudicationResponse,
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): DraftAdjudicationResponse {
    return webTestClient.put()
      .uri("/draft-adjudications/${draftCreationData.draftAdjudication.id}/evidence")
      .headers(headers)
      .bodyValue(
        mapOf(
          "evidence" to testDataSet.evidence,
        )
      )
      .exchange()
      .expectStatus().is2xxSuccessful
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }

  fun addWitnesses(
    draftCreationData: DraftAdjudicationResponse,
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): DraftAdjudicationResponse {
    return webTestClient.put()
      .uri("/draft-adjudications/${draftCreationData.draftAdjudication.id}/witnesses")
      .headers(headers)
      .bodyValue(
        mapOf(
          "witnesses" to testDataSet.witnesses,
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

  fun acceptReport(
    reportNumber: String,
  ): WebTestClient.ResponseSpec {
    return webTestClient.put()
      .uri("/reported-adjudications/$reportNumber/status")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "status" to ReportedAdjudicationStatus.UNSCHEDULED.name,
          "statusReason" to "status reason",
          "statusDetails" to "status details"
        )
      )
      .exchange()
  }

  fun issueReport(
    draftCreationData: DraftAdjudicationResponse,
    reportNumber: String,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): WebTestClient.ResponseSpec {
    return webTestClient.put()
      .uri("/reported-adjudications/$reportNumber/issue")
      .headers(headers)
      .bodyValue(
        mapOf(
          "dateTimeOfIssue" to draftCreationData.draftAdjudication.incidentDetails.dateTimeOfDiscovery.plusDays(1)
        )
      )
      .exchange()
  }

  fun createHearing(
    testDataSet: AdjudicationIntTestDataSet,
  ): ReportedAdjudicationResponse {
    return webTestClient.post()
      .uri("/reported-adjudications/${testDataSet.adjudicationNumber}/hearing")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to testDataSet.locationId,
          "dateTimeOfHearing" to testDataSet.dateTimeOfHearing!!,
          "oicHearingType" to OicHearingType.GOV.name,
        )
      )
      .exchange()
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }

  fun createHearingOutcome(
    adjudicationNumber: Long,
    hearingId: Long,
  ): ReportedAdjudicationResponse {
    return webTestClient.post()
      .uri("/reported-adjudications/$adjudicationNumber/hearing/$hearingId/outcome")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "code" to HearingOutcomeCode.ADJOURN.name,
          "details" to "details",
          "adjudicator" to "testing",
          "reason" to HearingOutcomeAdjournReason.LEGAL_ADVICE.name,
          "plea" to HearingOutcomePlea.UNFIT.name,
        )
      )
      .exchange()
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
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
