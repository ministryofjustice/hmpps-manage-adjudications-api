package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AuditService
import java.time.LocalDateTime

class AuditReportIntTest : IntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime(LocalDateTime.now())
  }

  @ParameterizedTest
  @CsvSource("true,false")
  fun `get the draft adjudication audit report`(historic: Boolean) {

    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val userHeaders = setHeaders(username = testAdjudication.createdByUserId)
    IntegrationTestScenarioBuilder(intTestData, this, userHeaders)
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()

    val result = webTestClient.get()
      .uri("/adjudications-audit/draft" + if (historic) "?historic=true" else "")
      .headers(setHeaders(roles = listOf("ADJUDICATIONS_AUDIT")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .returnResult()

    val csv = String(result.responseBody!!).split("\n")

    assertThat(csv[0]).isEqualTo(AuditService.DRAFT_ADJUDICATION_CSV_HEADERS)
    assertThat(csv[1].isNotEmpty()).isEqualTo(true)
  }

  @ParameterizedTest
  @CsvSource("true,false")
  fun `get the reported adjudication audit report`(historic: Boolean) {

    val testAdjudication = IntegrationTestData.DEFAULT_ADJUDICATION
    val intTestData = integrationTestData()
    val userHeaders = setHeaders(username = testAdjudication.createdByUserId)
    IntegrationTestScenarioBuilder(intTestData, this, userHeaders)
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addDamages()
      .addEvidence()
      .addWitnesses()
      .addIncidentStatement()
      .completeDraft()

    val result = webTestClient.get()
      .uri("/adjudications-audit/reported" + if (historic) "?historic=true" else "")
      .headers(setHeaders(roles = listOf("ADJUDICATIONS_AUDIT")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .returnResult()

    val csv = String(result.responseBody!!).split("\n")

    assertThat(csv[0]).isEqualTo(AuditService.REPORTED_ADJUDICATION_CSV_HEADERS)
    assertThat(csv[1].isNotEmpty()).isEqualTo(true)
  }
}
