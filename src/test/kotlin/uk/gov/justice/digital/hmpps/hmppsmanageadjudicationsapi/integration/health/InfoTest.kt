package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.SqsIntegrationTestBase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class InfoTest : SqsIntegrationTestBase() {

  @Test
  fun `is service active`() {
    webTestClient.get()
      .uri("/service/active?agency=MDI")
      .headers(setHeaders())
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.active").isEqualTo("true")
  }

  @Test
  fun `Info page is accessible`() {
    webTestClient.get()
      .uri("/info")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("app.name").isEqualTo("Hmpps Manage Adjudications Api")
  }

  @Test
  fun `Info page reports version`() {
    webTestClient.get().uri("/info")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("build.version").value<String> {
        assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
      }
  }
}
