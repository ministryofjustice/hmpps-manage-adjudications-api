package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.assertj.core.api.Assertions
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.HearingRepository
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException

class HearingServiceTest : ReportedAdjudicationTestBase() {
  private val hearingRepository: HearingRepository = mock()
  private val prisonApiGateway: PrisonApiGateway = mock()
  private var hearingService = HearingService(
    reportedAdjudicationRepository, offenceCodeLookupService, authenticationFacade, hearingRepository, prisonApiGateway
  )

  @Test
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(null)

    Assertions.assertThatThrownBy {
      hearingService.createHearing(1, 1, LocalDateTime.now(), OicHearingType.GOV)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      hearingService.amendHearing(1, 1, 1, LocalDateTime.now(), OicHearingType.GOV)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      hearingService.deleteHearing(1, 1)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }

  @Nested
  inner class CreateHearing {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.createdByUserId = ""
        it.createDateTime = LocalDateTime.now()
        it.hearings.clear()
      }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
      whenever(prisonApiGateway.createHearing(any(), any())).thenReturn(5L)
    }

    @ParameterizedTest
    @CsvSource("GOV_ADULT, true", "INAD_ADULT, true", "GOV_YOI, false", "INAD_YOI, false")
    fun `create a hearing uses wrong hearingType exception `(oicHearingType: OicHearingType, isYouthOffender: Boolean) {
      val now = LocalDateTime.now()

      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.isYouthOffender = isYouthOffender
        }
      )

      Assertions.assertThatThrownBy {
        hearingService.createHearing(
          1235L,
          1,
          now,
          oicHearingType,
        )
      }.isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("oic hearing type is not applicable for rule set")
    }

    @Test
    fun `create a hearing`() {
      val now = LocalDateTime.now()
      val response = hearingService.createHearing(
        1235L,
        1,
        now,
        OicHearingType.GOV_ADULT,
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(prisonApiGateway, atLeastOnce()).createHearing(
        1235L,
        OicHearingRequest(
          dateTimeOfHearing = now, hearingLocationId = 1, oicHearingType = OicHearingType.GOV_ADULT
        )
      )

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
      assertThat(argumentCaptor.value.hearings.first().locationId).isEqualTo(1)
      assertThat(argumentCaptor.value.hearings.first().dateTimeOfHearing).isEqualTo(now)
      assertThat(argumentCaptor.value.hearings.first().agencyId).isEqualTo(reportedAdjudication.agencyId)
      assertThat(argumentCaptor.value.hearings.first().reportNumber).isEqualTo(reportedAdjudication.reportNumber)
      assertThat(argumentCaptor.value.hearings.first().oicHearingId).isEqualTo(5)
      assertThat(argumentCaptor.value.hearings.first().oicHearingType).isEqualTo(OicHearingType.GOV_ADULT)

      assertThat(response).isNotNull
    }
  }

  @Nested
  inner class AmendHearing {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.createdByUserId = ""
        it.createDateTime = LocalDateTime.now()
      }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
      whenever(prisonApiGateway.createHearing(any(), any())).thenReturn(5L)
    }

    @ParameterizedTest
    @CsvSource("GOV_ADULT, true", "INAD_ADULT, true", "GOV_YOI, false", "INAD_YOI, false")
    fun `amend a hearing uses wrong hearingType exception `(oicHearingType: OicHearingType, isYouthOffender: Boolean) {
      val now = LocalDateTime.now()

      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.isYouthOffender = isYouthOffender
        }
      )

      Assertions.assertThatThrownBy {
        hearingService.amendHearing(
          1,
          1235L,
          1,
          now,
          oicHearingType,
        )
      }.isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("oic hearing type is not applicable for rule set")
    }

    @Test
    fun `amend a hearing`() {
      val now = LocalDateTime.now()
      val response = hearingService.amendHearing(
        1235L,
        1,
        2,
        now.plusDays(1),
        OicHearingType.INAD_ADULT,
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(prisonApiGateway, atLeastOnce()).amendHearing(
        1235L, 3,
        OicHearingRequest(
          dateTimeOfHearing = now.plusDays(1), hearingLocationId = 2, oicHearingType = OicHearingType.INAD_ADULT
        )
      )

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
      assertThat(argumentCaptor.value.hearings.first().locationId).isEqualTo(2)
      assertThat(argumentCaptor.value.hearings.first().dateTimeOfHearing).isEqualTo(now.plusDays(1))
      assertThat(argumentCaptor.value.hearings.first().agencyId).isEqualTo(reportedAdjudication.agencyId)
      assertThat(argumentCaptor.value.hearings.first().reportNumber).isEqualTo(reportedAdjudication.reportNumber)
      assertThat(argumentCaptor.value.hearings.first().oicHearingType).isEqualTo(OicHearingType.INAD_ADULT)

      assertThat(response).isNotNull
    }

    @Test
    fun `throws an entity not found if the hearing for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication
          .also { it.hearings.clear() }
      )

      Assertions.assertThatThrownBy {
        hearingService.amendHearing(1, 1, 1, LocalDateTime.now(), OicHearingType.GOV)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Hearing not found for 1")
    }
  }

  @Nested
  inner class DeleteHearing {
    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.createdByUserId = ""
        it.createDateTime = LocalDateTime.now()
      }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @Test
    fun `throws an entity not found if the hearing for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication
          .also { it.hearings.clear() }
      )

      Assertions.assertThatThrownBy {
        hearingService.deleteHearing(1, 1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Hearing not found for 1")
    }

    @Test
    fun `delete a hearing`() {
      val response = hearingService.deleteHearing(
        1235L,
        1,
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(prisonApiGateway, atLeastOnce()).deleteHearing(1235L, 3)

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(0)

      assertThat(response).isNotNull
    }
  }

  @Nested
  inner class AllHearings {
    val now = LocalDate.now()

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.createdByUserId = ""
        it.createDateTime = LocalDateTime.now()
      }

    @BeforeEach
    fun init() {
      whenever(
        hearingRepository.findByAgencyIdAndDateTimeOfHearingBetween(
          "MDI", now.atStartOfDay(),
          now.plusDays(1).atStartOfDay()
        )
      ).thenReturn(
        reportedAdjudication.hearings
      )
      whenever(reportedAdjudicationRepository.findByReportNumberIn(listOf(reportedAdjudication.reportNumber))).thenReturn(
        listOf(reportedAdjudication)
      )
    }

    @Test
    fun `get all hearings `() {

      val response = hearingService.getAllHearingsByAgencyIdAndDate(
        "MDI", LocalDate.now()
      )

      assertThat(response).isNotNull
      assertThat(response.size).isEqualTo(1)
      assertThat(response.first().adjudicationNumber).isEqualTo(reportedAdjudication.reportNumber)
      assertThat(response.first().prisonerNumber).isEqualTo(reportedAdjudication.prisonerNumber)
      assertThat(response.first().dateTimeOfHearing).isEqualTo(reportedAdjudication.hearings.first().dateTimeOfHearing)
      assertThat(response.first().dateTimeOfDiscovery).isEqualTo(reportedAdjudication.dateTimeOfDiscovery)
    }

    @Test
    fun `empty response test `() {
      whenever(
        hearingRepository.findByAgencyIdAndDateTimeOfHearingBetween(
          "LEI", now.atStartOfDay(), now.plusDays(1).atStartOfDay()
        )
      ).thenReturn(emptyList())

      val response = hearingService.getAllHearingsByAgencyIdAndDate(
        "LEI", now
      )

      assertThat(response).isNotNull
      assertThat(response.isEmpty()).isTrue
    }
  }
}
