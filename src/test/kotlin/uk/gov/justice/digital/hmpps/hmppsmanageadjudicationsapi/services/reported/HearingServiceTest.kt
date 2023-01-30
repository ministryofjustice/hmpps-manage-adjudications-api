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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.HearingOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeFinding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeReason
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

    Assertions.assertThatThrownBy {
      hearingService.createHearingOutcome(
        adjudicationNumber = 1, hearingId = 1, adjudicator = "test", code = HearingOutcomeCode.REFER_POLICE
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      hearingService.updateHearingOutcome(
        adjudicationNumber = 1, hearingId = 1, code = HearingOutcomeCode.ADJOURN, adjudicator = "test",
      )
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

    @CsvSource("REJECTED", "NOT_PROCEED", "AWAITING_REVIEW", "REFER_POLICE")
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
          1,
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
        hearingService.amendHearing(1, 1, 1, LocalDateTime.now(), OicHearingType.GOV)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Invalid status transition")
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
        1,
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(prisonApiGateway, atLeastOnce()).deleteHearing(1235L, 3)

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.SCHEDULED)
      assertThat(argumentCaptor.value.dateTimeOfFirstHearing).isEqualTo(dateTimeOfHearing)
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

  @Nested
  inner class CreateHearingOutcome {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
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
    }

    @Test
    fun `create hearing outcome`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingService.createHearingOutcome(
        1, 1, "test", HearingOutcomeCode.REFER_POLICE, HearingOutcomeReason.TEST, "details",
        HearingOutcomeFinding.NOT_PROCEED_WITH, HearingOutcomePlea.TEST
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("test")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isEqualTo("details")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.reason).isEqualTo(HearingOutcomeReason.TEST)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.finding).isEqualTo(HearingOutcomeFinding.NOT_PROCEED_WITH)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea).isEqualTo(HearingOutcomePlea.TEST)

      assertThat(response).isNotNull
    }

    @Test
    fun `throws an entity not found if the hearing for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication
          .also { it.hearings.clear() }
      )

      Assertions.assertThatThrownBy {
        hearingService.createHearingOutcome(1, 1, "testing", HearingOutcomeCode.REFER_POLICE)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Hearing not found for 1")
    }

    @CsvSource("COMPLETE")
    @ParameterizedTest
    fun `throws invalid state exception if finding not present`(code: HearingOutcomeCode) {
      Assertions.assertThatThrownBy {
        hearingService.createHearingOutcome(
          adjudicationNumber = 1L, hearingId = 1L, adjudicator = "test", code = code, plea = HearingOutcomePlea.TEST,
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing mandatory field")
    }

    @CsvSource("COMPLETE", "ADJOURN")
    @ParameterizedTest
    fun `throws invalid state exception if plea is not present`(code: HearingOutcomeCode) {
      Assertions.assertThatThrownBy {
        hearingService.createHearingOutcome(
          adjudicationNumber = 1L, hearingId = 1L, adjudicator = "test", code = code,
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing mandatory field")
    }

    @ParameterizedTest
    @CsvSource("REFER_POLICE", "REFER_INAD", "ADJOURN")
    fun `validation details for bad refer request`(code: HearingOutcomeCode) {
      Assertions.assertThatThrownBy {
        hearingService.createHearingOutcome(
          adjudicationNumber = 1L, hearingId = 1L, code = code, adjudicator = "test"
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing mandatory field")
    }

    @Test
    fun `validation of reason for adjourn`() {
      Assertions.assertThatThrownBy {
        hearingService.createHearingOutcome(
          adjudicationNumber = 1L, hearingId = 1L, code = HearingOutcomeCode.ADJOURN, adjudicator = "test", details = "details", plea = HearingOutcomePlea.TEST2
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing mandatory field")
    }
  }

  @Nested
  inner class UpdateHearingOutcome {
    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.createdByUserId = ""
        it.createDateTime = LocalDateTime.now()
        it.hearings.first().hearingOutcome = HearingOutcome(
          code = HearingOutcomeCode.COMPLETE,
          reason = HearingOutcomeReason.TEST,
          plea = HearingOutcomePlea.TEST,
          details = "details",
          adjudicator = "adjudicator",
          finding = HearingOutcomeFinding.DISMISSED,
        )
      }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.SCHEDULED
        }
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @Test
    fun `update hearing outcomes for existing hearing outcome codes`() {
      updateExistingHearingOutcome(
        request = HearingOutcomeRequest(
          adjudicator = "",
          code = HearingOutcomeCode.ADJOURN,
          reason = HearingOutcomeReason.TEST2,
          plea = HearingOutcomePlea.TEST2,
          details = "updated details",
        )
      )
      updateExistingHearingOutcome(
        request = HearingOutcomeRequest(
          adjudicator = "",
          code = HearingOutcomeCode.REFER_POLICE,
          details = "updated details",
        )
      )
      updateExistingHearingOutcome(
        request = HearingOutcomeRequest(
          adjudicator = "",
          code = HearingOutcomeCode.REFER_INAD,
          details = "updated details",
        )
      )
      updateExistingHearingOutcome(
        request = HearingOutcomeRequest(
          adjudicator = "",
          code = HearingOutcomeCode.COMPLETE,
          plea = HearingOutcomePlea.TEST2,
          finding = HearingOutcomeFinding.PROVED
        )
      )
    }

    @ParameterizedTest
    @CsvSource("REFER_INAD", "REFER_POLICE")
    fun `update hearing outcomes to referral `(code: HearingOutcomeCode) {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingService.updateHearingOutcome(
        adjudicationNumber = 1,
        hearingId = 1,
        code = code,
        adjudicator = "updated test",
        details = "updated details",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("updated test")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isEqualTo("updated details")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(code)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.reason).isNull()
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.finding).isNull()
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea).isNull()

      assertThat(response).isNotNull
    }

    @Test
    fun `update hearing outcomes to adjourn `() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingService.updateHearingOutcome(
        adjudicationNumber = 1,
        hearingId = 1,
        code = HearingOutcomeCode.ADJOURN,
        adjudicator = "updated test",
        details = "updated details",
        plea = HearingOutcomePlea.TEST2,
        reason = HearingOutcomeReason.TEST2,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("updated test")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isEqualTo("updated details")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.reason).isEqualTo(HearingOutcomeReason.TEST2)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.finding).isNull()
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea).isEqualTo(HearingOutcomePlea.TEST2)

      assertThat(response).isNotNull
    }

    @Test
    fun `update hearing outcomes to completed `() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingService.updateHearingOutcome(
        adjudicationNumber = 1,
        hearingId = 1,
        code = HearingOutcomeCode.COMPLETE,
        adjudicator = "updated test",
        plea = HearingOutcomePlea.TEST2,
        finding = HearingOutcomeFinding.PROVED,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("updated test")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isNull()
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.reason).isNull()
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.finding).isEqualTo(HearingOutcomeFinding.PROVED)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea).isEqualTo(HearingOutcomePlea.TEST2)

      assertThat(response).isNotNull
    }

    @Test
    fun `throws an entity not found if the hearing for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication
          .also { it.hearings.clear() }
      )

      Assertions.assertThatThrownBy {
        hearingService.updateHearingOutcome(1, 1, HearingOutcomeCode.REFER_POLICE, "testing",)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Hearing not found for 1")
    }

    @Test
    fun `throws an entity not found if the outcome of hearing for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.first().hearingOutcome = null
        }
      )

      Assertions.assertThatThrownBy {
        hearingService.updateHearingOutcome(1, 1, HearingOutcomeCode.REFER_POLICE, "testing",)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("outcome not found for hearing 1")
    }

    @CsvSource("COMPLETE")
    @ParameterizedTest
    fun `throws invalid state exception if finding not present`(code: HearingOutcomeCode) {
      Assertions.assertThatThrownBy {
        hearingService.updateHearingOutcome(
          adjudicationNumber = 1L, hearingId = 1L, code = HearingOutcomeCode.ADJOURN, adjudicator = "test", plea = HearingOutcomePlea.TEST,
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing mandatory field")
    }

    @CsvSource("COMPLETE", "ADJOURN")
    @ParameterizedTest
    fun `throws invalid state exception if plea is not present`(code: HearingOutcomeCode) {
      Assertions.assertThatThrownBy {
        hearingService.updateHearingOutcome(
          adjudicationNumber = 1L, hearingId = 1L, code = HearingOutcomeCode.ADJOURN, adjudicator = "test",
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing mandatory field")
    }

    @ParameterizedTest
    @CsvSource("REFER_POLICE", "REFER_INAD", "ADJOURN")
    fun `validation of details for bad request`(code: HearingOutcomeCode) {
      Assertions.assertThatThrownBy {
        hearingService.updateHearingOutcome(
          adjudicationNumber = 1L, hearingId = 1L, code = code, adjudicator = "test"
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing mandatory field")
    }

    @Test
    fun `validation of reason for adjourn`() {
      Assertions.assertThatThrownBy {
        hearingService.updateHearingOutcome(
          adjudicationNumber = 1L, hearingId = 1L, code = HearingOutcomeCode.ADJOURN, adjudicator = "test"
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing mandatory field")
    }

    private fun updateExistingHearingOutcome(request: HearingOutcomeRequest) {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingService.updateHearingOutcome(
        adjudicationNumber = 1,
        hearingId = 1,
        code = request.code,
        adjudicator = "updated test",
        reason = request.reason,
        details = request.details,
        plea = request.plea,
        finding = request.finding
      )

      verify(reportedAdjudicationRepository, atLeastOnce()).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("updated test")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isEqualTo(request.details)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(request.code)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.reason).isEqualTo(request.reason)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.finding).isEqualTo(request.finding)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea).isEqualTo(request.plea)

      assertThat(response).isNotNull
    }
  }
}
