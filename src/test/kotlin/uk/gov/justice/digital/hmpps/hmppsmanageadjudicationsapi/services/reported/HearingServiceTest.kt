package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.HearingRepository
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException

class HearingServiceTest : ReportedAdjudicationTestBase() {
  private val hearingRepository: HearingRepository = mock()
  private val prisonApiGateway: PrisonApiGateway = mock()
  private val hearingService = HearingService(
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
      hearingService.amendHearing(1, 1, LocalDateTime.now(), OicHearingType.GOV)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      hearingService.deleteHearing(1,)
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
        it.outcomes.clear()
      }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.UNSCHEDULED
        }
      )
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

    @CsvSource("REJECTED", "NOT_PROCEED", "AWAITING_REVIEW")
    @ParameterizedTest
    fun `hearing is in invalid state`(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = status
        }
      )

      Assertions.assertThatThrownBy {
        hearingService.createHearing(1, 1, LocalDateTime.now(), OicHearingType.GOV)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Invalid status transition")
    }

    @Test
    fun `create hearing throws validation exception as there is already a hearing without an outcome`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.add(
            Hearing(dateTimeOfHearing = LocalDateTime.now().minusDays(10), locationId = 1, oicHearingType = OicHearingType.GOV, oicHearingId = 1, agencyId = "", reportNumber = 1)
          )
        }
      )
      Assertions.assertThatThrownBy {
        hearingService.createHearing(1, 1, LocalDateTime.now(), OicHearingType.GOV)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Adjudication already has a hearing without outcome")
    }

    @Test
    fun `create hearing throws validation exception if the hearing date is before the previous hearing`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.add(
            Hearing(dateTimeOfHearing = LocalDateTime.now().plusDays(10), locationId = 1, oicHearingType = OicHearingType.GOV, oicHearingId = 1, agencyId = "", reportNumber = 1)
          )
        }
      )
      Assertions.assertThatThrownBy {
        hearingService.createHearing(1, 1, LocalDateTime.now().minusDays(10), OicHearingType.GOV)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("A hearing can not be before the previous hearing")
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
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.SCHEDULED)
      assertThat(argumentCaptor.value.hearings.first().oicHearingType).isEqualTo(OicHearingType.GOV_ADULT)
      assertThat(argumentCaptor.value.dateTimeOfFirstHearing).isEqualTo(now)

      assertThat(response).isNotNull
    }

    @CsvSource("REFER_POLICE", "REFER_INAD")
    @ParameterizedTest
    fun `create a SCHEDULE_HEARING outcome when creating a hearing if the previous outcome is a REFER_POLICE`(code: OutcomeCode) {
      val reportedAdjudicationReferPolice = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
        .also {
          it.createdByUserId = ""
          it.createDateTime = LocalDateTime.now()
          it.hearings.clear()
          it.outcomes.add(
            Outcome(code = if (code == OutcomeCode.REFER_INAD) OutcomeCode.REFER_POLICE else OutcomeCode.REFER_INAD).also {
              o ->
              o.createDateTime = LocalDateTime.now()
            }
          )
          it.outcomes.add(
            Outcome(code = code).also {
              o ->
              o.createDateTime = LocalDateTime.now().plusDays(1)
            }
          )
        }

      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudicationReferPolice.also {
          it.status = code.status
        }
      )

      hearingService.createHearing(
        1235L,
        1,
        LocalDateTime.now().plusDays(2),
        OicHearingType.GOV_ADULT,
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.outcomes.size).isEqualTo(3)
      assertThat(argumentCaptor.value.outcomes.last().code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
    }

    @Test
    fun `does not create a SCHEDULE_HEARING outcome when creating a hearing if the previous outcome is not a REFER`() {
      val reportedAdjudicationReferInad = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
        .also {
          it.createdByUserId = ""
          it.createDateTime = LocalDateTime.now()
          it.hearings.clear()
          it.outcomes.add(
            Outcome(code = OutcomeCode.NOT_PROCEED).also {
              o ->
              o.createDateTime = LocalDateTime.now()
            }
          )
        }

      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudicationReferInad.also {
          it.status = ReportedAdjudicationStatus.REFER_INAD
        }
      )

      hearingService.createHearing(
        1235L,
        1,
        LocalDateTime.now().plusDays(2),
        OicHearingType.GOV_ADULT,
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.outcomes.size).isEqualTo(1)
    }
  }

  @Nested
  inner class AmendHearing {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = LocalDateTime.now())
      .also {
        it.createdByUserId = ""
        it.createDateTime = LocalDateTime.now()
      }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.SCHEDULED
        }
      )
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
          now,
          oicHearingType,
        )
      }.isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("oic hearing type is not applicable for rule set")
    }

    @CsvSource("REJECTED", "NOT_PROCEED", "AWAITING_REVIEW", "REFER_POLICE")
    @ParameterizedTest
    fun `hearing is in invalid state`(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = status
        }
      )

      Assertions.assertThatThrownBy {
        hearingService.amendHearing(1, 1, LocalDateTime.now(), OicHearingType.GOV)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Invalid status transition")
    }

    @Test
    fun `amend hearing throws validation exception if the hearing date is before the previous hearing`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.add(
            Hearing(dateTimeOfHearing = LocalDateTime.now().plusDays(3), locationId = 1, oicHearingType = OicHearingType.GOV, oicHearingId = 1, agencyId = "", reportNumber = 1)
          )
        }
      )
      Assertions.assertThatThrownBy {
        hearingService.amendHearing(1, 1, LocalDateTime.now().plusDays(1), OicHearingType.GOV)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("A hearing can not be before the previous hearing")
    }

    @Test
    fun `amend a hearing`() {
      val now = LocalDateTime.now()
      val response = hearingService.amendHearing(
        1235L,
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
      assertThat(argumentCaptor.value.dateTimeOfFirstHearing).isEqualTo(now.plusDays(1))

      assertThat(response).isNotNull
    }

    @Test
    fun `throws an entity not found if the hearing for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication
          .also { it.hearings.clear() }
      )

      Assertions.assertThatThrownBy {
        hearingService.amendHearing(1, 1, LocalDateTime.now(), OicHearingType.GOV)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Hearing not found")
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
        hearingService.deleteHearing(1,)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Hearing not found")
    }

    @Test
    fun `throws invalid request if the hearing has an outcome associated to it `() {
    }

    @Test
    fun `delete a hearing`() {
      val response = hearingService.deleteHearing(
        1235L,
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(prisonApiGateway, atLeastOnce()).deleteHearing(1235L, 3)

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(0)

      assertThat(response).isNotNull
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.UNSCHEDULED)
      assertThat(argumentCaptor.value.dateTimeOfFirstHearing).isNull()
    }

    @Test
    fun `delete a hearing when there is more than one and ensure status is still SCHEDULED`() {
      val dateTimeOfHearing = LocalDateTime.now().plusDays(5)
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.add(
            Hearing(
              oicHearingId = 2L,
              dateTimeOfHearing = dateTimeOfHearing,
              locationId = 1L,
              agencyId = reportedAdjudication.agencyId,
              reportNumber = 1235L,
              oicHearingType = OicHearingType.GOV,
            )
          )
          it.status = ReportedAdjudicationStatus.SCHEDULED
        }
      )

      hearingService.deleteHearing(
        1235L,
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(prisonApiGateway, atLeastOnce()).deleteHearing(1235L, 2)

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.SCHEDULED)
      assertThat(argumentCaptor.value.dateTimeOfFirstHearing).isEqualTo(reportedAdjudication.hearings.first().dateTimeOfHearing)
    }

    @Test
    fun `delete hearing removes last outcome if it was SCHEDULE HEARING`() {

      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication()
          .also {
            it.hearings.add(
              Hearing(agencyId = "", locationId = 1L, oicHearingType = OicHearingType.INAD_ADULT, dateTimeOfHearing = LocalDateTime.now().plusDays(5), oicHearingId = 1L, reportNumber = 1L)
            )
            it.outcomes.add(Outcome(code = OutcomeCode.REFER_INAD).also { o -> o.createDateTime = LocalDateTime.now() })
            it.outcomes.add(Outcome(code = OutcomeCode.SCHEDULE_HEARING).also { o -> o.createDateTime = LocalDateTime.now().plusDays(1) })
          }
      )

      hearingService.deleteHearing(
        1235L,
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(prisonApiGateway, atLeastOnce()).deleteHearing(1235L, 1)

      assertThat(argumentCaptor.value.outcomes.size).isEqualTo(1)
      assertThat(argumentCaptor.value.outcomes.first().code).isEqualTo(OutcomeCode.REFER_INAD)
    }

    @CsvSource("COMPLETE", "REFER_POLICE", "REFER_INAD")
    @ParameterizedTest
    fun `delete hearing throws validation exception if linked to specific outcome `(code: HearingOutcomeCode) {

      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication()
          .also {
            it.hearings.clear()
            it.hearings.add(
              Hearing(
                agencyId = "", locationId = 1L, oicHearingType = OicHearingType.INAD_ADULT, dateTimeOfHearing = LocalDateTime.now().plusDays(5), oicHearingId = 1L, reportNumber = 1L,
                hearingOutcome = HearingOutcome(code = code, adjudicator = "")
              )
            )
          }
      )

      Assertions.assertThatThrownBy {
        hearingService.deleteHearing(1,)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Unable to delete hearing via api DEL/hearing")
    }
  }

  @Nested
  inner class AllHearings {
    val now = LocalDate.now()

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.createdByUserId = ""
        it.createDateTime = LocalDateTime.now()
        it.hearings.addAll(
          listOf(
            Hearing(
              id = 2,
              oicHearingId = 2L,
              dateTimeOfHearing = now.atStartOfDay().plusHours(5),
              locationId = 1L,
              agencyId = it.agencyId,
              reportNumber = it.reportNumber,
              oicHearingType = OicHearingType.GOV,
            ),
            Hearing(
              id = 3,
              oicHearingId = 2L,
              dateTimeOfHearing = now.atStartOfDay().plusHours(6),
              locationId = 1L,
              agencyId = it.agencyId,
              reportNumber = it.reportNumber,
              oicHearingType = OicHearingType.GOV,
            )
          )
        )
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
      whenever(
        reportedAdjudicationRepository.findByReportNumberIn(
          reportedAdjudication.hearings.map { it.reportNumber }
        )
      ).thenReturn(
        listOf(reportedAdjudication)
      )
    }

    @Test
    fun `get all hearings `() {

      val response = hearingService.getAllHearingsByAgencyIdAndDate(
        "MDI", LocalDate.now()
      )

      assertThat(response).isNotNull
      assertThat(response.size).isEqualTo(3)
      assertThat(response.first().adjudicationNumber).isEqualTo(reportedAdjudication.reportNumber)
      assertThat(response.first().prisonerNumber).isEqualTo(reportedAdjudication.prisonerNumber)
      assertThat(response.first().dateTimeOfHearing).isEqualTo(reportedAdjudication.hearings.first().dateTimeOfHearing)
      assertThat(response.first().dateTimeOfDiscovery).isEqualTo(reportedAdjudication.dateTimeOfDiscovery)
      assertThat(response.first().oicHearingType).isEqualTo(reportedAdjudication.hearings[0].oicHearingType)
      assertThat(response[1].id).isEqualTo(2)
      assertThat(response[2].id).isEqualTo(3)
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
