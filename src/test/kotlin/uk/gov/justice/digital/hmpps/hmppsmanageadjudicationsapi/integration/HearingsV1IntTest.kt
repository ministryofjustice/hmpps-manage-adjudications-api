package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ReportedAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import java.time.LocalDateTime

@Deprecated(message = "to be removed when outcomes goes live")
class HearingsV1IntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME)
  }

  @Test
  fun `create a hearing `() {
    initDataForHearingsV1()

    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 12, 10, 0)
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 1,
          "dateTimeOfHearing" to dateTimeOfHearing,
          "oicHearingType" to OicHearingType.GOV.name,
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
      .jsonPath("$.reportedAdjudication.hearings[0].locationId")
      .isEqualTo(1)
      .jsonPath("$.reportedAdjudication.hearings[0].dateTimeOfHearing")
      .isEqualTo("2010-10-12T10:00:00")
      .jsonPath("$.reportedAdjudication.hearings[0].id").isNotEmpty
      .jsonPath("$.reportedAdjudication.hearings[0].oicHearingType").isEqualTo(OicHearingType.GOV.name)
  }

  @Test
  fun `create a hearing illegal state on hearing type `() {
    initDataForHearingsV1()

    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 12, 10, 0)
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 1,
          "dateTimeOfHearing" to dateTimeOfHearing,
          "oicHearingType" to OicHearingType.GOV_YOI.name,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  fun `create hearing fails on prisonApi and does not create a hearing`() {
    initDataForHearingsV1()

    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 12, 10, 0)

    prisonApiMockServer.stubCreateHearingFailure(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 1,
          "dateTimeOfHearing" to dateTimeOfHearing,
          "oicHearingType" to OicHearingType.GOV.name,
        ),
      )
      .exchange()
      .expectStatus().is5xxServerError

    webTestClient.get()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)
  }

  @Test
  fun `amend a hearing `() {
    initDataForHearingsV1()
    val reportedAdjudication = createHearing()

    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 25, 10, 0)

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 3,
          "dateTimeOfHearing" to dateTimeOfHearing.plusDays(1),
          "oicHearingType" to OicHearingType.GOV_ADULT.name,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
      .jsonPath("$.reportedAdjudication.hearings[0].locationId")
      .isEqualTo(3)
      .jsonPath("$.reportedAdjudication.hearings[0].dateTimeOfHearing")
      .isEqualTo("2010-10-26T10:00:00")
      .jsonPath("$.reportedAdjudication.hearings[0].id").isNotEmpty
      .jsonPath("$.reportedAdjudication.hearings[0].oicHearingType").isEqualTo(OicHearingType.GOV_ADULT.name)
  }

  @Test
  fun `amend a hearing illegal state on hearing type `() {
    initDataForHearingsV1()
    val reportedAdjudication = createHearing()

    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 25, 10, 0)

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 3,
          "dateTimeOfHearing" to dateTimeOfHearing.plusDays(1),
          "oicHearingType" to OicHearingType.GOV_YOI.name,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  fun `amend a hearing fails on prison api and does not update the hearing `() {
    initDataForHearingsV1()
    val reportedAdjudication = createHearing()

    prisonApiMockServer.stubAmendHearingFailure(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 25, 10, 0)

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 3,
          "dateTimeOfHearing" to dateTimeOfHearing.plusDays(1),
          "oicHearingType" to OicHearingType.GOV_ADULT.name,
        ),
      )
      .exchange()
      .expectStatus().is5xxServerError

    webTestClient.get()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings[0].locationId")
      .isEqualTo(IntegrationTestData.UPDATED_LOCATION_ID)
      .jsonPath("$.reportedAdjudication.hearings[0].dateTimeOfHearing")
      .isEqualTo("2010-11-19T10:00:00")
      .jsonPath("$.reportedAdjudication.hearings[0].id").isNotEmpty
  }

  @Test
  fun `delete a hearing `() {
    initDataForHearingsV1()
    val reportedAdjudication = createHearing()

    prisonApiMockServer.stubDeleteHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    assert(reportedAdjudication.reportedAdjudication.hearings.size == 1)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id!!}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.UNSCHEDULED.name)
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)
  }

  @Test
  fun `delete a hearing fails on prison api and does not delete record`() {
    initDataForHearingsV1()
    val reportedAdjudication = createHearing()

    prisonApiMockServer.stubDeleteHearingFailure(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    assert(reportedAdjudication.reportedAdjudication.hearings.size == 1)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id!!}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().is5xxServerError

    webTestClient.get()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(1)
  }

  private fun initDataForHearingsV1(): IntegrationTestData {
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

    return intTestData
  }

  fun createHearing(): ReportedAdjudicationResponse {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    val intTestData = integrationTestData()

    return createHearing(
      IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber,
      IntegrationTestData.DEFAULT_ADJUDICATION,
      setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")),
    )
  }

  fun createHearing(
    adjudicationNumber: String,
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders(),
  ): ReportedAdjudicationResponse {
    return webTestClient.post()
      .uri("/reported-adjudications/$adjudicationNumber/hearing")
      .headers(headers)
      .bodyValue(
        mapOf(
          "locationId" to testDataSet.locationId,
          "dateTimeOfHearing" to testDataSet.dateTimeOfHearing!!,
          "oicHearingType" to OicHearingType.GOV.name,
        ),
      )
      .exchange()
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }
}
