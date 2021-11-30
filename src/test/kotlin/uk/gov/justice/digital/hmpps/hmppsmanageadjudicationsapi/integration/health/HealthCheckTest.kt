package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.CacheConfiguration
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

class HealthCheckTest : IntegrationTestBase() {

  @Autowired
  lateinit var cacheManager: CacheManager

  @BeforeEach
  fun beforeEach() {
    prisonApiMockServer.resetMappings()
    prisonApiMockServer.stubHealth()

    bankHolidayApiMockServer.resetMappings()
    bankHolidayApiMockServer.stubGetBankHolidays()

    oAuthMockServer.stubGrantToken()
  }

  @Test
  fun `Health page reports ok`() {
    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health info reports version`() {
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("components.healthInfo.details.version").value(
        Consumer<String> {
          assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
        }
      )
  }

  @Test
  fun `Health ping page is accessible`() {
    webTestClient.get()
      .uri("/health/ping")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `readiness reports ok`() {
    webTestClient.get()
      .uri("/health/readiness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `liveness reports ok`() {
    webTestClient.get()
      .uri("/health/liveness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health reports db info`() {
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("components.db.details.database").isEqualTo("PostgreSQL")
      .jsonPath("components.db.details.validationQuery").isEqualTo("isValid()")
  }

  @Test
  fun `Prison API and Bank Holiday API health reports UP and OK`() {
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("components.prisonApiHealthCheck.status").value(
        Consumer<String> {
          assertThat(it).isEqualTo("UP")
        }
      )
      .jsonPath("components.prisonApiHealthCheck.details.HttpStatus").value(
        Consumer<String> {
          assertThat(it).isEqualTo("OK")
        }
      )
      .jsonPath("components.bankHolidayApiHealthCheck.status").value(
        Consumer<String> {
          assertThat(it).isEqualTo("UP")
        }
      )
  }

  @Test
  fun `Prison API health reports DOWN and not OK if fails`() {
    prisonApiMockServer.stubHealthFailure()
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus()
      .isEqualTo(503)
      .expectBody()
      .jsonPath("components.prisonApiHealthCheck.status").value(
        Consumer<String> {
          assertThat(it).isEqualTo("DOWN")
        }
      )
  }

  @Test
  fun `Bank Holiday API reports DOWN and not OK if fails`() {
    cacheManager.getCache(CacheConfiguration.BANK_HOLIDAYS_CACHE_NAME)!!.invalidate()
    bankHolidayApiMockServer.stubGetBankHolidaysFailure()
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus()
      .isEqualTo(503)
      .expectBody()
      .jsonPath("components.bankHolidayApiHealthCheck.status").value(
        Consumer<String> {
          assertThat(it).isEqualTo("DOWN")
        }
      )
  }
}
