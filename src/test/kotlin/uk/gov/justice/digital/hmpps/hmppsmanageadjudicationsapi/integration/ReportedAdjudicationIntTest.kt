package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.CacheConfiguration
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidays
import java.util.concurrent.ConcurrentHashMap

class ReportedAdjudicationIntTest : IntegrationTestBase() {
  fun dataAPiHelpers(): DataAPiHelpers = DataAPiHelpers(webTestClient, setHeaders())

  @Autowired
  lateinit var cacheManager: CacheManager

  @Test
  fun `get reported adjudication details`() {
    prisonApiMockServer.stubGetAdjudication()
    bankHolidayApiMockServer.stubGetBankHolidays()
    oAuthMockServer.stubGrantToken()

    webTestClient.get()
      .uri("/reported-adjudications/1524242")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.adjudicationNumber").isEqualTo("1524242")
      .jsonPath("$.reportedAdjudication.prisonerNumber").isEqualTo("AA1234A")
      .jsonPath("$.reportedAdjudication.bookingId").isEqualTo("123")
      .jsonPath("$.reportedAdjudication.dateTimeReportExpires").isEqualTo("2021-10-27T09:03:11")
      .jsonPath("$.reportedAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo("2021-10-25T09:03:11")
      .jsonPath("$.reportedAdjudication.incidentDetails.locationId").isEqualTo(721850)
      .jsonPath("$.reportedAdjudication.createdByUserId").isEqualTo("A_SMITH")
  }

  @Test
  fun `get reported adjudication details utilises bank holiday cache`() {
    prisonApiMockServer.stubGetAdjudication()
    bankHolidayApiMockServer.stubGetBankHolidays()
    oAuthMockServer.stubGrantToken()

    webTestClient.get()
      .uri("/reported-adjudications/1524242")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful

    @Suppress("UNCHECKED_CAST") val nativeCache: ConcurrentHashMap<Any, Any> =
      cacheManager.getCache(CacheConfiguration.BANK_HOLIDAYS_CACHE_NAME)!!.nativeCache as ConcurrentHashMap<Any, Any>

    assertThat(nativeCache.size).isEqualTo(1)

    val holidays: BankHolidays = nativeCache.values.first() as BankHolidays
    assertThat(holidays.englandAndWales.events).isNotEmpty
  }

  @Test
  fun `get reported adjudication details with invalid adjudication number`() {
    prisonApiMockServer.stubGetAdjudicationWithInvalidNumber()
    oAuthMockServer.stubGrantToken()

    webTestClient.get()
      .uri("/reported-adjudications/1524242")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isNotFound
      .expectBody()
      .jsonPath("$.status").isEqualTo(404)
      .jsonPath("$.userMessage")
      .isEqualTo("Forwarded HTTP call response error: 404 Not Found from GET http://localhost:8979/api/adjudications/adjudication/1524242")
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

    prisonApiMockServer.stubGetValidAdjudicationsById(IntTestData.ADJUDICATION_2, IntTestData.ADJUDICATION_4)
    bankHolidayApiMockServer.stubGetBankHolidays()

    webTestClient.get()
      .uri("/reported-adjudications/my/agency/MDI?page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content[0].adjudicationNumber").isEqualTo(IntTestData.ADJUDICATION_4.adjudicationNumber)
      .jsonPath("$.content[0].prisonerNumber").isEqualTo(IntTestData.ADJUDICATION_4.prisonerNumber)
      .jsonPath("$.content[0].bookingId").isEqualTo("456")
      .jsonPath("$.content[0].incidentDetails.dateTimeOfIncident")
      .isEqualTo(IntTestData.ADJUDICATION_4.dateTimeOfIncidentISOString)
      .jsonPath("$.content[0].incidentDetails.locationId").isEqualTo(IntTestData.ADJUDICATION_4.locationId)
      .jsonPath("$.content[0].createdByUserId").isEqualTo(IntTestData.ADJUDICATION_4.createdByUserId)
      .jsonPath("$.content[1].adjudicationNumber").isEqualTo(IntTestData.ADJUDICATION_2.adjudicationNumber)
      .jsonPath("$.content[1].prisonerNumber").isEqualTo(IntTestData.ADJUDICATION_2.prisonerNumber)
      .jsonPath("$.content[1].bookingId").isEqualTo("123")
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
    intTestData.completeDraftAdjudication(firstDraftCreationResponse, IntTestData.ADJUDICATION_1)

    val secondDraftCreationResponse = intTestData.startNewAdjudication(IntTestData.ADJUDICATION_2)
    intTestData.addIncidentStatement(secondDraftCreationResponse, IntTestData.ADJUDICATION_2)
    intTestData.completeDraftAdjudication(secondDraftCreationResponse, IntTestData.ADJUDICATION_2)

    val thirdDraftCreationResponse = intTestData.startNewAdjudication(IntTestData.ADJUDICATION_3)
    intTestData.addIncidentStatement(thirdDraftCreationResponse, IntTestData.ADJUDICATION_3)
    intTestData.completeDraftAdjudication(thirdDraftCreationResponse, IntTestData.ADJUDICATION_3)

    prisonApiMockServer.stubGetValidAdjudicationsById(IntTestData.ADJUDICATION_2, IntTestData.ADJUDICATION_3)
    bankHolidayApiMockServer.stubGetBankHolidays()

    webTestClient.get()
      .uri("/reported-adjudications/agency/MDI?page=0&size=20")
      .headers(setHeaders(username = "NEW_USER", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content[0].adjudicationNumber").isEqualTo(IntTestData.ADJUDICATION_3.adjudicationNumber)
      .jsonPath("$.content[0].prisonerNumber").isEqualTo(IntTestData.ADJUDICATION_3.prisonerNumber)
      .jsonPath("$.content[0].bookingId").isEqualTo("456")
      .jsonPath("$.content[0].incidentDetails.dateTimeOfIncident")
      .isEqualTo(IntTestData.ADJUDICATION_3.dateTimeOfIncidentISOString)
      .jsonPath("$.content[0].incidentDetails.locationId").isEqualTo(IntTestData.ADJUDICATION_3.locationId)
      .jsonPath("$.content[0].createdByUserId").isEqualTo(IntTestData.ADJUDICATION_3.createdByUserId)
      .jsonPath("$.content[1].adjudicationNumber").isEqualTo(IntTestData.ADJUDICATION_2.adjudicationNumber)
      .jsonPath("$.content[1].prisonerNumber").isEqualTo(IntTestData.ADJUDICATION_2.prisonerNumber)
      .jsonPath("$.content[1].bookingId").isEqualTo("123")
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
    prisonApiMockServer.stubGetAdjudication()
    bankHolidayApiMockServer.stubGetBankHolidays()
    oAuthMockServer.stubGrantToken()

    webTestClient.post()
      .uri("/reported-adjudications/1524242/create-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.draftAdjudication.adjudicationNumber").isEqualTo("1524242")
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo("AA1234A")
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo("2021-10-25T09:03:11")
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(721850)
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").isEqualTo("2021-10-27T09:03:11")
      .jsonPath("$.draftAdjudication.incidentStatement.completed").isEqualTo(true)
      .jsonPath("$.draftAdjudication.incidentStatement.statement").isEqualTo("It keeps happening...")
      .jsonPath("$.draftAdjudication.createdByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudication.startedByUserId").isEqualTo("A_SMITH")
  }

  @Test
  fun `create draft from reported adjudication adds draft`() {
    prisonApiMockServer.stubGetAdjudication()
    bankHolidayApiMockServer.stubGetBankHolidays()
    oAuthMockServer.stubGrantToken()

    val createdDraft = dataAPiHelpers().createADraftFromAReportedAdjudication(1524242)

    dataAPiHelpers().getDraftAdjudicationDetails(createdDraft.draftAdjudication.id).expectStatus().isOk
  }

  @Test
  fun `create draft from reported adjudication with invalid adjudication number`() {
    prisonApiMockServer.stubGetAdjudicationWithInvalidNumber()
    oAuthMockServer.stubGrantToken()

    webTestClient.post()
      .uri("/reported-adjudications/1524242/create-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isNotFound
      .expectBody()
      .jsonPath("$.status").isEqualTo(404)
      .jsonPath("$.userMessage")
      .isEqualTo("Forwarded HTTP call response error: 404 Not Found from GET http://localhost:8979/api/adjudications/adjudication/1524242")
  }
}
