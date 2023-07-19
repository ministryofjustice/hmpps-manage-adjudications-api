package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.util.CollectionUtils
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationDetail
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationSummary
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.Award
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ProvenAdjudicationsSummary
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.LegacyNomisGateway
import java.time.LocalDate

class SummaryAdjudicationServiceTest {

  private val legacyNomisGateway: LegacyNomisGateway = mock()
  private val featureFlagsConfig: FeatureFlagsConfig = mock()

  private val prisonerNumber = "A1234AB"

  private val summaryAdjudicationService = SummaryAdjudicationService(
    legacyNomisGateway,
    featureFlagsConfig,
  )

  @BeforeEach
  fun beforeEach() {
    whenever(featureFlagsConfig.nomisSourceOfTruth).thenReturn(true)
  }

  @Test
  fun `can paginate results from NOMIS`() {
    val today = LocalDate.now()

    val offenceId = 1L
    val prisonId = "MDI"
    val finding = Finding.PROVED
    val fromDate = today.minusDays(30)
    val toDate = today.minusDays(1)
    val pageable = PageRequest.of(1, 10)

    val headers = CollectionUtils.unmodifiableMultiValueMap(
      CollectionUtils.toMultiValueMap(
        mapOf(
          "Page-Offset" to listOf("10"),
          "Page-Limit" to listOf("10"),
          "Total-Records" to listOf("30"),
        ),
      ),
    )

    whenever(
      legacyNomisGateway.getAdjudicationsForPrisoner(
        prisonerNumber,
        offenceId,
        prisonId,
        finding,
        fromDate,
        toDate,
        pageable,
      ),
    ).thenReturn(
      ResponseEntity(
        AdjudicationResponse(
          results = listOf(),
          offences = listOf(),
          agencies = listOf(),
        ),
        headers,
        HttpStatusCode.valueOf(200),
      ),
    )

    val adjudications = summaryAdjudicationService.getAdjudications(
      prisonerNumber = prisonerNumber,
      offenceId = offenceId,
      prisonId = prisonId,
      finding = finding,
      fromDate = fromDate,
      toDate = toDate,
      pageable = pageable,
    )

    Assertions.assertThat(adjudications.results.totalElements).isEqualTo(30)
    Assertions.assertThat(adjudications.results.totalPages).isEqualTo(3)
    Assertions.assertThat(adjudications.results.pageable.pageNumber).isEqualTo(1)
    Assertions.assertThat(adjudications.results.numberOfElements).isEqualTo(0)
  }

  @Test
  fun `can get results for a booking`() {
    whenever(legacyNomisGateway.getAdjudicationsForPrisonerForBooking(1L, null, null)).thenReturn(
      AdjudicationSummary(
        bookingId = 1L,
        adjudicationCount = 2,
        awards = listOf(Award(bookingId = 1L, sanctionCode = "T3"), Award(bookingId = 1L, sanctionCode = "T1")),
      ),
    )
    val adjudicationSummary = summaryAdjudicationService.getAdjudicationSummary(1L, null, null)

    Assertions.assertThat(adjudicationSummary.adjudicationCount).isEqualTo(2)
  }

  @Test
  fun `can get results for a prisoner number`() {
    val adjudicationNumber = 10000L
    whenever(legacyNomisGateway.getAdjudicationDetailForPrisoner(prisonerNumber = prisonerNumber, chargeId = 1L)).thenReturn(
      AdjudicationDetail(
        adjudicationNumber = adjudicationNumber,
      ),
    )
    val adjudicationDetail = summaryAdjudicationService.getAdjudication(prisonerNumber, 1L)

    Assertions.assertThat(adjudicationDetail.adjudicationNumber).isEqualTo(adjudicationNumber)
  }

  @Test
  fun `can get proven adjudication results for a set of booking Ids`() {
    val bookingIds = listOf(1L, 2L)
    whenever(legacyNomisGateway.getProvenAdjudicationsForBookings(bookingIds, null, null)).thenReturn(
      listOf(
        ProvenAdjudicationsSummary(bookingId = 1L, provenAdjudicationCount = 2),
        ProvenAdjudicationsSummary(bookingId = 2L, provenAdjudicationCount = 1),
      ),
    )
    val provenAdjs = summaryAdjudicationService.getProvenAdjudicationsForBookings(bookingIds, null, null)

    Assertions.assertThat(provenAdjs).hasSize(2)
  }
}
