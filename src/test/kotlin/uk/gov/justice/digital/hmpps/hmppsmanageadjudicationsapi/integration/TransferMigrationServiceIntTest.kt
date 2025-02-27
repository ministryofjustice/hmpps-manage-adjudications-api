package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.TestOAuth2Config
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.TransferMigrationChargeStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.DEFAULT_REPORTED_DATE_TIME
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.TransferMigrationChargesRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.PrisonerResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.PrisonerSearchService
import java.time.LocalDate
import java.time.LocalDateTime

@Import(TestOAuth2Config::class)
@ActiveProfiles("test")
class TransferMigrationServiceIntTest : SqsIntegrationTestBase() {

  @Autowired
  private lateinit var transferMigrationChargesRepository: TransferMigrationChargesRepository

  @Autowired
  private lateinit var reportedAdjudicationRepository: ReportedAdjudicationRepository

  @MockitoBean
  lateinit var prisonerSearchService: PrisonerSearchService

  @BeforeEach
  fun setUp() {
    setAuditTime(DEFAULT_REPORTED_DATE_TIME)
  }

  fun setContentHeader(
    contentType: MediaType = MediaType.APPLICATION_JSON,
  ): (HttpHeaders) -> Unit = {
    it.contentType = contentType
  }

  @Test
  fun `Populate charges and process migration`() {
    val testData = IntegrationTestData.getDefaultAdjudication(offenderBookingId = 1000)
    val scenario = initDataForUnScheduled(testData = testData).createHearing(dateTimeOfHearing = LocalDateTime.now().plusDays(1)).createChargeProved().createPunishments(
      startDate = LocalDate.now().plusDays(1),
    )
    whenever(prisonerSearchService.getPrisonerDetail(any())).thenReturn(PrisonerResponse("Bob", "Smith", "PSI"))

    webTestClient.post()
      .uri("/scheduled-tasks/migration-populate-charges")
      .headers(setContentHeader())
      .exchange()
      .expectStatus().is2xxSuccessful

    var charges = transferMigrationChargesRepository.findAll()
    assertThat(charges.toList().all { it.status == TransferMigrationChargeStatus.READY }).isTrue()

    webTestClient.post()
      .uri("/scheduled-tasks/migration-process-charges")
      .headers(setContentHeader())
      .exchange()
      .expectStatus().is2xxSuccessful

    charges = transferMigrationChargesRepository.findAll()
    assertThat(charges.toList().all { it.status == TransferMigrationChargeStatus.COMPLETE }).isTrue()

    val charge = reportedAdjudicationRepository.findByChargeNumber(scenario.getGeneratedChargeNumber())
    assertThat(charge!!.overrideAgencyId).isEqualTo("PSI")
  }
}
