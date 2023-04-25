package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import java.time.LocalDateTime

class HearingOutcomeServiceTest : ReportedAdjudicationTestBase() {

  private val hearingOutcomeService = HearingOutcomeService(
    reportedAdjudicationRepository,
    offenceCodeLookupService,
    authenticationFacade,
  )

  @Test
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    Assertions.assertThatThrownBy {
      hearingOutcomeService.createReferral(
        adjudicationNumber = 1,
        adjudicator = "test",
        code = HearingOutcomeCode.REFER_POLICE,
        details = "",
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      hearingOutcomeService.getHearingOutcomeForReferral(
        adjudicationNumber = 1,
        OutcomeCode.REFER_POLICE,
        1,
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      hearingOutcomeService.deleteHearingOutcome(
        adjudicationNumber = 1,
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      hearingOutcomeService.removeAdjourn(
        adjudicationNumber = 1,
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      hearingOutcomeService.amendHearingOutcome(
        adjudicationNumber = 1,
        outcomeCodeToAmend = HearingOutcomeCode.ADJOURN,
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      hearingOutcomeService.getCurrentStatusAndLatestOutcome(
        adjudicationNumber = 1,
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
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
        },
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @Test
    fun `create a referral outcome`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingOutcomeService.createReferral(
        1,
        HearingOutcomeCode.REFER_POLICE,
        "test",
        "details",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("test")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isEqualTo("details")

      assertThat(response).isNotNull
    }

    @Test
    fun `create an adjourn`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingOutcomeService.createAdjourn(
        1,
        "test",
        HearingOutcomeAdjournReason.LEGAL_ADVICE,
        "details",
        HearingOutcomePlea.UNFIT,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("test")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isEqualTo("details")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.reason)
        .isEqualTo(HearingOutcomeAdjournReason.LEGAL_ADVICE)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea)
        .isEqualTo(HearingOutcomePlea.UNFIT)

      assertThat(response).isNotNull
    }

    @Test
    fun `create a completed hearing`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingOutcomeService.createCompletedHearing(
        1,
        "test",
        HearingOutcomePlea.UNFIT,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("test")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea)
        .isEqualTo(HearingOutcomePlea.UNFIT)

      assertThat(response).isNotNull
    }

    @Test
    fun `throws an entity not found if the hearing for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication
          .also { it.hearings.clear() },
      )

      Assertions.assertThatThrownBy {
        hearingOutcomeService.createReferral(1, HearingOutcomeCode.REFER_POLICE, "testing", "")
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Hearing not found")
    }

    @Test
    fun `throws exception if referral validation fails`() {
      Assertions.assertThatThrownBy {
        hearingOutcomeService.createReferral(
          adjudicationNumber = 1L,
          code = HearingOutcomeCode.ADJOURN,
          adjudicator = "test",
          details = "details",
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("invalid referral type")
    }
  }

  @Nested
  inner class DeleteHearingOutcome {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
    private val reportedAdjudicationWithOutcome = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
      it.hearings.first().hearingOutcome =
        HearingOutcome(id = 1, code = HearingOutcomeCode.REFER_INAD, adjudicator = "test")
    }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.findByReportNumber(2)).thenReturn(reportedAdjudicationWithOutcome)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        reportedAdjudication.also {
          it.createdByUserId = "test"
          it.createDateTime = LocalDateTime.now()
        },
      )
    }

    @Test
    fun `delete hearing outcome`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingOutcomeService.deleteHearingOutcome(2)

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNull()
      assertThat(response).isNotNull
    }

    @Test
    fun `delete adjourn outcome`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
        reportedAdjudication.also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = "testing")
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingOutcomeService.removeAdjourn(1)

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNull()
      assertThat(response).isNotNull
    }

    @Test
    fun `delete hearing outcome throws no outcome found for adjudication `() {
      Assertions.assertThatThrownBy {
        hearingOutcomeService.deleteHearingOutcome(1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("outcome not found for hearing")
    }

    @CsvSource("REFER_POLICE", "REFER_INAD", "COMPLETE")
    @ParameterizedTest
    fun `remove adjourn throws exception if latest outcome is not an adjourn `(code: HearingOutcomeCode) {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
        reportedAdjudication.also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = code, adjudicator = "testing")
        },
      )

      Assertions.assertThatThrownBy {
        hearingOutcomeService.removeAdjourn(
          adjudicationNumber = 1L,
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("latest outcome is not an adjourn")
    }

    @Test
    fun `remove adjourn throws exception if no hearing `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
        reportedAdjudication.also {
          it.hearings.clear()
        },
      )

      Assertions.assertThatThrownBy {
        hearingOutcomeService.removeAdjourn(
          adjudicationNumber = 1L,
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Hearing not found")
    }

    @Test
    fun `remove adjourn throws exception if no outcome `() {
      Assertions.assertThatThrownBy {
        hearingOutcomeService.removeAdjourn(
          adjudicationNumber = 1L,
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("outcome not found for hearing")
    }
  }

  @Nested
  inner class GetHearingOutcomesForReferral {

    @Test
    fun `no hearing outcomes for referral`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
        entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
          it.hearings.forEach { h -> h.hearingOutcome = null }
        },
      )

      val result = hearingOutcomeService.getHearingOutcomeForReferral(1, OutcomeCode.REFER_POLICE, 0)

      assertThat(result).isNull()
    }

    @Test
    fun `one hearing outcome for referral`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
        entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
          it.hearings.forEach { h ->
            h.hearingOutcome = HearingOutcome(
              code = HearingOutcomeCode.REFER_POLICE,
              adjudicator = "test",
            )
          }
        },
      )

      val result = hearingOutcomeService.getHearingOutcomeForReferral(1, OutcomeCode.REFER_POLICE, 0)

      assertThat(result).isNotNull
      assertThat(result!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
    }

    @Test
    fun `multiple hearing outcomes for multiple referrals with correct date order`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
        entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
          it.hearings.clear()
          it.hearings.add(
            Hearing(
              dateTimeOfHearing = LocalDateTime.now().plusDays(1),
              oicHearingType = OicHearingType.GOV,
              agencyId = "1",
              reportNumber = 2,
              locationId = 1,
              oicHearingId = 1,
            ).also { h ->
              h.hearingOutcome = HearingOutcome(
                code = HearingOutcomeCode.REFER_POLICE,
                adjudicator = "test",
                id = 2,
              )
            },
          )
          it.hearings.add(
            Hearing(
              dateTimeOfHearing = LocalDateTime.now(),
              oicHearingType = OicHearingType.GOV,
              agencyId = "1",
              reportNumber = 2,
              locationId = 1,
              oicHearingId = 1,
            ).also { h ->
              h.hearingOutcome = HearingOutcome(
                code = HearingOutcomeCode.REFER_POLICE,
                adjudicator = "test",
                id = 1,
              )
            },
          )
        },
      )

      val result = hearingOutcomeService.getHearingOutcomeForReferral(1, OutcomeCode.REFER_POLICE, 1)

      assertThat(result).isNotNull
      assertThat(result!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(result.id).isEqualTo(2)
    }

    @Test
    fun `multiple hearing outcomes for multiple referrals where the first REFER_POLICE was without a hearing`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
        entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
          it.hearings.clear()
          it.hearings.add(
            Hearing(
              dateTimeOfHearing = LocalDateTime.now(),
              oicHearingType = OicHearingType.GOV,
              agencyId = "1",
              reportNumber = 2,
              locationId = 1,
              oicHearingId = 1,
            ).also { h ->
              h.hearingOutcome = HearingOutcome(
                code = HearingOutcomeCode.REFER_POLICE,
                adjudicator = "test",
                id = 1,
              )
            },
          )
        },
      )

      val result = hearingOutcomeService.getHearingOutcomeForReferral(1, OutcomeCode.REFER_POLICE, 1)

      assertThat(result).isNotNull
      assertThat(result!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(result.id).isEqualTo(1)
    }
  }

  @Nested
  inner class AmendHearingOutcome {
    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.hearings.clear()
        it.hearings.add(Hearing(dateTimeOfHearing = LocalDateTime.now(), oicHearingId = 1L, reportNumber = 1L, agencyId = "", oicHearingType = OicHearingType.GOV, locationId = 1L))
        it.createdByUserId = ""
        it.createDateTime = LocalDateTime.now()
      }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.SCHEDULED
        },
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @Test
    fun `throws entity not found if no hearing `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also { it.hearings.clear() },
      )
      Assertions.assertThatThrownBy {
        hearingOutcomeService.amendHearingOutcome(
          adjudicationNumber = 1,
          outcomeCodeToAmend = HearingOutcomeCode.ADJOURN,
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Hearing not found")
    }

    @Test
    fun `throws entity not found if no outcome `() {
      Assertions.assertThatThrownBy {
        hearingOutcomeService.amendHearingOutcome(
          adjudicationNumber = 1,
          outcomeCodeToAmend = HearingOutcomeCode.ADJOURN,
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("outcome not found for hearing")
    }

    @CsvSource("ADJOURN", "REFER_POLICE", "REFER_INAD", "COMPLETE")
    @ParameterizedTest
    fun `throws validation exception if the latest outcome is not of the correct type `(code: HearingOutcomeCode) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.first().hearingOutcome = HearingOutcome(adjudicator = "testing", code = HearingOutcomeCode.values().first { hoc -> hoc != code })
        },
      )
      Assertions.assertThatThrownBy {
        hearingOutcomeService.amendHearingOutcome(1, outcomeCodeToAmend = code)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("latest outcome is not of same type")
    }

    @CsvSource("REFER_POLICE", "REFER_INAD")
    @ParameterizedTest
    fun `amend referrals `(code: HearingOutcomeCode) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = code, adjudicator = "adjudicator", details = "details")
          it.outcomes.add(Outcome(code = code.outcomeCode!!, details = "details"))
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val response = hearingOutcomeService.amendHearingOutcome(
        adjudicationNumber = 1L,
        outcomeCodeToAmend = code,
        adjudicator = "updated adjudicator",
        details = "updated details",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(code)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("updated adjudicator")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea).isNull()
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.reason).isNull()
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isEqualTo("updated details")

      assertThat(response).isNotNull
    }

    @Test
    fun `amend adjourn`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.first().hearingOutcome = HearingOutcome(
            code = HearingOutcomeCode.ADJOURN,
            adjudicator = "adjudicator",
            details = "details",
            plea = HearingOutcomePlea.NOT_GUILTY,
            reason = HearingOutcomeAdjournReason.MCKENZIE,
          )
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val response = hearingOutcomeService.amendHearingOutcome(
        adjudicationNumber = 1L,
        adjudicator = "updated adjudicator",
        outcomeCodeToAmend = HearingOutcomeCode.ADJOURN,
        details = "updated details",
        plea = HearingOutcomePlea.GUILTY,
        adjournedReason = HearingOutcomeAdjournReason.HELP,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("updated adjudicator")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea).isEqualTo(HearingOutcomePlea.GUILTY)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.reason).isEqualTo(HearingOutcomeAdjournReason.HELP)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isEqualTo("updated details")

      assertThat(response).isNotNull
    }

    @Test
    fun `amend completed hearing`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.first().hearingOutcome = HearingOutcome(
            code = HearingOutcomeCode.COMPLETE,
            adjudicator = "adjudicator",
            plea = HearingOutcomePlea.NOT_GUILTY,
          )
          it.outcomes.add(Outcome(code = OutcomeCode.NOT_PROCEED, details = "details"))
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val response = hearingOutcomeService.amendHearingOutcome(
        adjudicationNumber = 1L,
        outcomeCodeToAmend = HearingOutcomeCode.COMPLETE,
        adjudicator = "updated adjudicator",
        plea = HearingOutcomePlea.GUILTY,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("updated adjudicator")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea).isEqualTo(HearingOutcomePlea.GUILTY)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.reason).isNull()
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isNull()

      assertThat(response).isNotNull
    }
  }

  @Nested
  inner class GetStatusAndLatestOutcome {

    @Test
    fun `throws entity not found when no hearing `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.DISMISSED
          it.hearings.clear()
        },
      )
      Assertions.assertThatThrownBy {
        hearingOutcomeService.getCurrentStatusAndLatestOutcome(
          adjudicationNumber = 1,
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Hearing not found")
    }

    @Test
    fun `throws entity not found when no hearing outcome`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.DISMISSED
        },
      )
      Assertions.assertThatThrownBy {
        hearingOutcomeService.getCurrentStatusAndLatestOutcome(
          adjudicationNumber = 1,
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("outcome not found for hearing")
    }

    @Test
    fun `returns current status and latest outcome `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.DISMISSED
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "test")
        },
      )

      val result = hearingOutcomeService.getCurrentStatusAndLatestOutcome(1L)

      assertThat(result.first).isEqualTo(ReportedAdjudicationStatus.DISMISSED)
      assertThat(result.second.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
    }
  }
}
