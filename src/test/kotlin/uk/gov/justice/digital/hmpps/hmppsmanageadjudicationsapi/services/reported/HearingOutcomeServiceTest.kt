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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.HearingOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException

class HearingOutcomeServiceTest : ReportedAdjudicationTestBase() {

  private var hearingOutcomeService = HearingOutcomeService(
    reportedAdjudicationRepository, offenceCodeLookupService, authenticationFacade
  )

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    Assertions.assertThatThrownBy {
      hearingOutcomeService.createHearingOutcome(
        adjudicationNumber = 1, adjudicator = "test", code = HearingOutcomeCode.REFER_POLICE
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      hearingOutcomeService.updateHearingOutcome(
        adjudicationNumber = 1, code = HearingOutcomeCode.ADJOURN, adjudicator = "test",
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      hearingOutcomeService.getHearingOutcomeForReferral(
        adjudicationNumber = 1, OutcomeCode.REFER_POLICE, 1,
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      hearingOutcomeService.deleteHearingOutcome(
        adjudicationNumber = 1, 1,
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
        }
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @Test
    fun `create hearing outcome`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingOutcomeService.createHearingOutcome(
        1, HearingOutcomeCode.REFER_POLICE, "test", HearingOutcomeAdjournReason.LEGAL_ADVICE, "details",
        HearingOutcomePlea.UNFIT
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("test")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isEqualTo("details")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.reason)
        .isEqualTo(HearingOutcomeAdjournReason.LEGAL_ADVICE)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea)
        .isEqualTo(HearingOutcomePlea.UNFIT)

      assertThat(response).isNotNull
    }

    @Test
    fun `throws an entity not found if the hearing for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication
          .also { it.hearings.clear() }
      )

      Assertions.assertThatThrownBy {
        hearingOutcomeService.createHearingOutcome(1, HearingOutcomeCode.REFER_POLICE, "testing",)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Hearing not found")
    }

    @CsvSource("COMPLETE", "ADJOURN")
    @ParameterizedTest
    fun `throws invalid state exception if plea is not present`(code: HearingOutcomeCode) {
      Assertions.assertThatThrownBy {
        hearingOutcomeService.createHearingOutcome(
          adjudicationNumber = 1L, adjudicator = "test", code = code,
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing mandatory field")
    }

    @ParameterizedTest
    @CsvSource("ADJOURN")
    fun `validation details for bad refer request`(code: HearingOutcomeCode) {
      Assertions.assertThatThrownBy {
        hearingOutcomeService.createHearingOutcome(
          adjudicationNumber = 1L, code = code, adjudicator = "test"
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing mandatory field")
    }

    @Test
    fun `validation of reason for adjourn`() {
      Assertions.assertThatThrownBy {
        hearingOutcomeService.createHearingOutcome(
          adjudicationNumber = 1L, code = HearingOutcomeCode.ADJOURN, adjudicator = "test", details = "details", plea = HearingOutcomePlea.ABSTAIN
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
          reason = HearingOutcomeAdjournReason.LEGAL_ADVICE,
          plea = HearingOutcomePlea.UNFIT,
          details = "details",
          adjudicator = "adjudicator",
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
          reason = HearingOutcomeAdjournReason.LEGAL_REPRESENTATION,
          plea = HearingOutcomePlea.ABSTAIN,
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
          plea = HearingOutcomePlea.ABSTAIN,
        )
      )
    }

    @ParameterizedTest
    @CsvSource("REFER_INAD", "REFER_POLICE")
    fun `update hearing outcomes to referral `(code: HearingOutcomeCode) {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingOutcomeService.updateHearingOutcome(
        adjudicationNumber = 1,
        code = code,
        adjudicator = "updated test",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("updated test")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(code)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isNull()
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.reason).isNull()
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea).isNull()

      assertThat(response).isNotNull
    }

    @Test
    fun `update hearing outcomes to adjourn `() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingOutcomeService.updateHearingOutcome(
        adjudicationNumber = 1,
        code = HearingOutcomeCode.ADJOURN,
        adjudicator = "updated test",
        details = "updated details",
        plea = HearingOutcomePlea.ABSTAIN,
        reason = HearingOutcomeAdjournReason.LEGAL_REPRESENTATION,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("updated test")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isEqualTo("updated details")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.reason)
        .isEqualTo(HearingOutcomeAdjournReason.LEGAL_REPRESENTATION)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea)
        .isEqualTo(HearingOutcomePlea.ABSTAIN)

      assertThat(response).isNotNull
    }

    @Test
    fun `update hearing outcomes to completed `() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingOutcomeService.updateHearingOutcome(
        adjudicationNumber = 1,
        code = HearingOutcomeCode.COMPLETE,
        adjudicator = "updated test",
        plea = HearingOutcomePlea.ABSTAIN,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("updated test")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isNull()
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.reason).isNull()
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea)
        .isEqualTo(HearingOutcomePlea.ABSTAIN)

      assertThat(response).isNotNull
    }

    @Test
    fun `throws an entity not found if the hearing for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication
          .also { it.hearings.clear() }
      )

      Assertions.assertThatThrownBy {
        hearingOutcomeService.updateHearingOutcome(1, HearingOutcomeCode.REFER_POLICE, "testing",)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Hearing not found")
    }

    @Test
    fun `throws an entity not found if the outcome of hearing for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.first().hearingOutcome = null
        }
      )

      Assertions.assertThatThrownBy {
        hearingOutcomeService.updateHearingOutcome(1, HearingOutcomeCode.REFER_POLICE, "testing",)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("outcome not found for hearing")
    }

    @CsvSource("COMPLETE", "ADJOURN")
    @ParameterizedTest
    fun `throws invalid state exception if plea is not present`(code: HearingOutcomeCode) {
      Assertions.assertThatThrownBy {
        hearingOutcomeService.updateHearingOutcome(
          adjudicationNumber = 1L, code = code, adjudicator = "test",
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing mandatory field")
    }

    @ParameterizedTest
    @CsvSource("ADJOURN")
    fun `validation of details for bad request`(code: HearingOutcomeCode) {
      Assertions.assertThatThrownBy {
        hearingOutcomeService.updateHearingOutcome(
          adjudicationNumber = 1L, code = code, adjudicator = "test"
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing mandatory field")
    }

    @Test
    fun `validation of reason for adjourn`() {
      Assertions.assertThatThrownBy {
        hearingOutcomeService.updateHearingOutcome(
          adjudicationNumber = 1L, code = HearingOutcomeCode.ADJOURN, adjudicator = "test"
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing mandatory field")
    }

    private fun updateExistingHearingOutcome(request: HearingOutcomeRequest) {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingOutcomeService.updateHearingOutcome(
        adjudicationNumber = 1,
        code = request.code,
        adjudicator = "updated test",
        reason = request.reason,
        details = request.details,
        plea = request.plea,
      )

      verify(reportedAdjudicationRepository, atLeastOnce()).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("updated test")
      request.code.outcomeCode ?: assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isEqualTo(request.details)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(request.code)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.reason).isEqualTo(request.reason)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea).isEqualTo(request.plea)

      assertThat(response).isNotNull
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
        }
      )
    }
    @Test
    fun `delete hearing outcome`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingOutcomeService.deleteHearingOutcome(
        2, 1,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNull()
      // TODO statuses...do on another ticket i think.
      assertThat(response).isNotNull
    }

    @Test
    fun `delete hearing outcome throws no outcome found for adjudication `() {
      Assertions.assertThatThrownBy {
        hearingOutcomeService.deleteHearingOutcome(1, 1)
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
        }
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
              adjudicator = "test"
            )
          }
        }
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
                id = 2
              )
            }
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
                id = 1
              )
            }
          )
        }
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
                id = 1
              )
            }
          )
        }
      )

      val result = hearingOutcomeService.getHearingOutcomeForReferral(1, OutcomeCode.REFER_POLICE, 1)

      assertThat(result).isNotNull
      assertThat(result!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(result.id).isEqualTo(1)
    }
  }
}
