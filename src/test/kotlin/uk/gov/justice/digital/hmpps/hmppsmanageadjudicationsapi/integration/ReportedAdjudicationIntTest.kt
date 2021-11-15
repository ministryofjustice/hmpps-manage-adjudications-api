package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.CacheConfiguration
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidays
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

class ReportedAdjudicationIntTest : IntegrationTestBase() {
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
  fun `return all reported adjudications completed by the current user`() {
    prisonApiMockServer.stubGetAdjudications()
    bankHolidayApiMockServer.stubGetBankHolidays()
    prisonApiMockServer.stubPostAdjudication()

    val dataAPiHelpers = DataAPiHelpers(webTestClient, setHeaders(username = "NEW_USER"))
    dataAPiHelpers.createAndCompleteADraftAdjudication(LocalDateTime.parse("2021-10-25T09:03:11"))

    webTestClient.get()
      .uri("/reported-adjudications/my")
      .headers(setHeaders(username = "NEW_USER"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudications[0].adjudicationNumber").isEqualTo("1")
      .jsonPath("$.reportedAdjudications[0].prisonerNumber").isEqualTo("AA1234A")
      .jsonPath("$.reportedAdjudications[0].bookingId").isEqualTo("123")
      .jsonPath("$.reportedAdjudications[0].dateTimeReportExpires").isEqualTo("2021-10-27T09:03:11")
      .jsonPath("$.reportedAdjudications[0].incidentDetails.dateTimeOfIncident")
      .isEqualTo("2021-10-25T09:03:11")
      .jsonPath("$.reportedAdjudications[0].incidentDetails.locationId").isEqualTo(721850)
      .jsonPath("$.reportedAdjudications[1].adjudicationNumber").isEqualTo("2")
      .jsonPath("$.reportedAdjudications[1].prisonerNumber").isEqualTo("AA1234B")
      .jsonPath("$.reportedAdjudications[1].bookingId").isEqualTo("456")
      .jsonPath("$.reportedAdjudications[1].dateTimeReportExpires").isEqualTo("2021-10-27T09:03:11")
      .jsonPath("$.reportedAdjudications[1].incidentDetails.dateTimeOfIncident")
      .isEqualTo("2021-10-25T09:03:11")
      .jsonPath("$.reportedAdjudications[1].incidentDetails.locationId").isEqualTo(721850)
  }
}
