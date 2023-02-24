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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException

class OutcomeServiceTest : ReportedAdjudicationTestBase() {

  private var outcomeService = OutcomeService(
    reportedAdjudicationRepository, offenceCodeLookupService, authenticationFacade
  )

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    Assertions.assertThatThrownBy {
      outcomeService.createReferral(1, OutcomeCode.REFER_POLICE, "")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.createProsecution(1, "")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.createNotProceed(1, NotProceedReason.NOT_FAIR, "")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.getOutcomes(1)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.deleteOutcome(1, 1)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }

  @Nested
  inner class CreateOutcome {
    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.UNSCHEDULED
          it.createdByUserId = "test"
          it.createDateTime = LocalDateTime.now()
        }
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @ParameterizedTest
    @CsvSource("REJECTED", "AWAITING_REVIEW", "NOT_PROCEED", "REFER_INAD", "REFER_POLICE", "UNSCHEDULED")
    fun `create outcome throws exception if invalid state `(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = status
        }
      )

      val code = if (listOf(ReportedAdjudicationStatus.UNSCHEDULED, ReportedAdjudicationStatus.REFER_POLICE).contains(status))
        OutcomeCode.REFER_INAD else OutcomeCode.REFER_POLICE

      Assertions.assertThatThrownBy {
        outcomeService.createReferral(adjudicationNumber = 1, code = code, details = "details")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Invalid status transition")
    }

    @CsvSource("NOT_PROCEED", "PROSECUTION", "SCHEDULE_HEARING")
    @ParameterizedTest
    fun `throws exception if referral validation fails`(code: OutcomeCode) {
      Assertions.assertThatThrownBy {
        outcomeService.createReferral(
          adjudicationNumber = 1L, code = code, details = "details",
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("invalid referral type")
    }

    @ParameterizedTest
    @CsvSource("REFER_INAD", "REFER_POLICE")
    fun `create outcome throws exception if police referral outcome invalid state `(code: OutcomeCode) {
      assertTransition(code, OutcomeCode.REFER_POLICE)
    }

    @ParameterizedTest
    @CsvSource("REFER_INAD", "REFER_POLICE")
    fun `create outcome throws exception if inad referral outcome invalid state `(code: OutcomeCode) {
      assertTransition(code, OutcomeCode.REFER_INAD)
    }

    private fun assertTransition(codeFrom: OutcomeCode, codeTo: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.outcomes.add(
            Outcome(code = codeFrom).also { o -> o.createDateTime = LocalDateTime.now().plusDays(1) }
          )
          it.status = ReportedAdjudicationStatus.SCHEDULED
        }
      )

      Assertions.assertThatThrownBy {
        outcomeService.createReferral(adjudicationNumber = 1, code = codeTo, details = "details")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Invalid referral transition")
    }

    @Test
    fun `create not proceed `() {
      reportedAdjudication.status = ReportedAdjudicationStatus.UNSCHEDULED

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.createNotProceed(
        1235L,
        NotProceedReason.NOT_FAIR,
        "details",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.outcomes.first()).isNotNull
      assertThat(argumentCaptor.value.outcomes.first().code).isEqualTo(OutcomeCode.NOT_PROCEED)
      assertThat(argumentCaptor.value.outcomes.first().details).isEqualTo("details")
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.valueOf(OutcomeCode.NOT_PROCEED.name))
      assertThat(argumentCaptor.value.outcomes.first().reason).isEqualTo(NotProceedReason.NOT_FAIR)
      assertThat(response).isNotNull
    }

    @ParameterizedTest
    @CsvSource("UNSCHEDULED, REFER_POLICE", "SCHEDULED, REFER_INAD")
    fun `create referral`(codeFrom: ReportedAdjudicationStatus, code: OutcomeCode) {
      reportedAdjudication.status = codeFrom

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.createReferral(
        1235L,
        code,
        "details",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.outcomes.first()).isNotNull
      assertThat(argumentCaptor.value.outcomes.first().code).isEqualTo(code)
      assertThat(argumentCaptor.value.outcomes.first().details).isEqualTo("details")
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.valueOf(code.name))
      assertThat(response).isNotNull
    }
  }

  @Nested
  inner class DeleteOutcome {
    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
    private val reportedAdjudicationWithOutcome = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
      it.outcomes.add(
        Outcome(id = 1, code = OutcomeCode.REFER_INAD)
      )
    }
    private val reportedAdjudicationWithOutcomeAndNoHearings = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
      it.hearings.clear()
      it.outcomes.add(
        Outcome(id = 1, code = OutcomeCode.REFER_POLICE)
      )
    }

    private val reportedAdjudicationLatestOutcome = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
      it.hearings.clear()
      it.outcomes.add(
        Outcome(id = 1, code = OutcomeCode.NOT_PROCEED)
      )
    }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.findByReportNumber(2)).thenReturn(reportedAdjudicationWithOutcome)
      whenever(reportedAdjudicationRepository.findByReportNumber(3)).thenReturn(reportedAdjudicationWithOutcomeAndNoHearings)
      whenever(reportedAdjudicationRepository.findByReportNumber(4)).thenReturn(reportedAdjudicationLatestOutcome)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        reportedAdjudication.also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = "test"
        }
      )
    }

    @Test
    fun `delete outcome when we have hearings `() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.deleteOutcome(
        2, 1,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.outcomes).isEmpty()
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.SCHEDULED)

      assertThat(response).isNotNull
    }

    @Test
    fun `delete outcome when we have no hearings `() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.deleteOutcome(
        3, 1,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.outcomes).isEmpty()
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.UNSCHEDULED)

      assertThat(response).isNotNull
    }

    @Test
    fun `delete outcome throws no outcome found for adjudication `() {
      Assertions.assertThatThrownBy {
        outcomeService.deleteOutcome(1, 1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Outcome not found for 1")
    }

    @Test
    fun `delete latest outcome throws not found if no outcomes present `() {
      Assertions.assertThatThrownBy {
        outcomeService.deleteOutcome(1,)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Outcome not found for 1")
    }

    @ParameterizedTest
    @CsvSource("REFER_POLICE", "REFER_INAD", "SCHEDULE_HEARING", "PROSECUTION", "NOT_PROCEED")
    fun `throws invalid state if delete latest outcome is invalid type `(code: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
        reportedAdjudication
          .also {
            it.outcomes.add(Outcome(code = code).also { o -> o.createDateTime = LocalDateTime.now() })
          }
      )

      Assertions.assertThatThrownBy {
        outcomeService.deleteOutcome(1,)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Unable to delete via api - DEL/outcome")
    }

    @Test
    fun `delete latest outcome succeeds`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.deleteOutcome(
        4,
      )
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.outcomes).isEmpty()
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.UNSCHEDULED)

      assertThat(response).isNotNull
    }
  }

  @Nested
  inner class GetCombinedOutcomes {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
      it.outcomes.add(
        Outcome(
          code = OutcomeCode.REFER_POLICE,
        ).also { o -> o.createDateTime = LocalDateTime.now() }
      )
    }
    private val reportedAdjudication2 = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
      it.outcomes.add(
        Outcome(
          code = OutcomeCode.REFER_POLICE,
        ).also { o -> o.createDateTime = LocalDateTime.now() }
      )
      it.outcomes.add(
        Outcome(
          code = OutcomeCode.NOT_PROCEED,
        ).also { o -> o.createDateTime = LocalDateTime.now().plusDays(1) }
      )
    }
    private val reportedAdjudication3 = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
      it.outcomes.add(
        Outcome(
          code = OutcomeCode.REFER_INAD,
        ).also { o -> o.createDateTime = LocalDateTime.now() }
      )
      it.outcomes.add(
        Outcome(
          code = OutcomeCode.SCHEDULE_HEARING,
        ).also { o -> o.createDateTime = LocalDateTime.now() }
      )
      it.outcomes.add(
        Outcome(
          code = OutcomeCode.REFER_POLICE,
        ).also { o -> o.createDateTime = LocalDateTime.now().plusDays(2) }
      )
      it.outcomes.add(
        Outcome(
          code = OutcomeCode.NOT_PROCEED,
        ).also { o -> o.createDateTime = LocalDateTime.now().plusDays(3) }
      )
    }
    private val reportedAdjudicationNoOutcomes = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
      it.outcomes.clear()
    }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.findByReportNumber(2)).thenReturn(reportedAdjudication2)
      whenever(reportedAdjudicationRepository.findByReportNumber(3)).thenReturn(reportedAdjudication3)
      whenever(reportedAdjudicationRepository.findByReportNumber(4)).thenReturn(reportedAdjudicationNoOutcomes)
    }

    @Test
    fun `no outcomes`() {
      val result = outcomeService.getOutcomes(4)

      assertThat(result.isEmpty()).isEqualTo(true)
    }
    @Test
    fun `get outcomes without any referral outcomes`() {
      val result = outcomeService.getOutcomes(1)

      assertThat(result.size).isEqualTo(1)
      assertThat(result.first().outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.first().referralOutcome).isNull()
    }

    @Test
    fun `get outcomes with referral outcomes`() {
      val result = outcomeService.getOutcomes(2)

      assertThat(result.size).isEqualTo(1)
      assertThat(result.first().outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.first().referralOutcome).isNotNull
      assertThat(result.first().referralOutcome!!.code).isEqualTo(OutcomeCode.NOT_PROCEED)
    }

    @Test
    fun `get outcomes with multiple referral outcomes`() {
      val result = outcomeService.getOutcomes(3)

      assertThat(result.size).isEqualTo(2)
      assertThat(result.first().outcome.code).isEqualTo(OutcomeCode.REFER_INAD)
      assertThat(result.first().referralOutcome).isNotNull
      assertThat(result.first().referralOutcome!!.code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
      assertThat(result.last().outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.last().referralOutcome).isNotNull
      assertThat(result.last().referralOutcome!!.code).isEqualTo(OutcomeCode.NOT_PROCEED)
    }
  }
}
