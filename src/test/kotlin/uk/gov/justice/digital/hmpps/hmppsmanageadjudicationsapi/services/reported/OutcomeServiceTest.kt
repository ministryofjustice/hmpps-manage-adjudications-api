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
      outcomeService.createOutcome(1, OutcomeCode.REFER_POLICE)
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

    Assertions.assertThatThrownBy {
      outcomeService.updateReferral(1, OutcomeCode.REFER_INAD, "updated")
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
    @CsvSource("REJECTED", "AWAITING_REVIEW", "NOT_PROCEED", "REFER_INAD", "REFER_POLICE")
    fun `create outcome throws exception if invalid state `(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = status
        }
      )

      val code = if (status == ReportedAdjudicationStatus.REFER_POLICE) OutcomeCode.REFER_INAD else OutcomeCode.REFER_POLICE

      Assertions.assertThatThrownBy {
        outcomeService.createOutcome(adjudicationNumber = 1, code = code, details = "details")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Invalid status transition")
    }

    @Test
    fun `validation exception if missing reason for not proceed`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)

      Assertions.assertThatThrownBy {
        outcomeService.createOutcome(adjudicationNumber = 1, code = OutcomeCode.NOT_PROCEED, details = "details")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("a reason is required")
    }

    @ParameterizedTest
    @CsvSource("REFER_POLICE", "NOT_PROCEED", "REFER_INAD")
    fun `validation exception if missing details`(code: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)

      Assertions.assertThatThrownBy {
        outcomeService.createOutcome(1, code)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("details are required")
    }

    @ParameterizedTest
    @CsvSource("REFER_POLICE", "NOT_PROCEED", "REFER_INAD")
    fun `create outcome`(code: OutcomeCode) {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.createOutcome(
        1235L,
        code,
        "details",
        NotProceedReason.RELEASED
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.outcomes.first()).isNotNull
      assertThat(argumentCaptor.value.outcomes.first().code).isEqualTo(code)
      assertThat(argumentCaptor.value.outcomes.first().details).isEqualTo("details")
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.valueOf(code.name))
      assertThat(argumentCaptor.value.outcomes.first().reason).isEqualTo(NotProceedReason.RELEASED)
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

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.findByReportNumber(2)).thenReturn(reportedAdjudicationWithOutcome)
      whenever(reportedAdjudicationRepository.findByReportNumber(3)).thenReturn(reportedAdjudicationWithOutcomeAndNoHearings)
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
        ).also { o -> o.createDateTime = LocalDateTime.now().plusDays(1) }
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

  @Nested
  inner class UpdateOutcome {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.outcomes.clear()
        it.outcomes.add(
          Outcome(id = 1, code = OutcomeCode.REFER_POLICE, details = "ref police")
        )
        it.outcomes.add(
          Outcome(id = 2, code = OutcomeCode.REFER_INAD, details = "ref inad").also {
            o ->
            o.createDateTime = LocalDateTime.now()
          }
        )
        it.outcomes.add(
          Outcome(id = 3, code = OutcomeCode.REFER_INAD, details = "ref inad").also {
            o ->
            o.createDateTime = LocalDateTime.now().plusDays(1)
          }
        )
      }

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

    @Test
    fun `throws entity not found if no matching referral`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.outcomes.removeFirst()
        }
      )

      Assertions.assertThatThrownBy {
        outcomeService.updateReferral(1, OutcomeCode.REFER_POLICE, "updated")
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Referral not found for 1235")
    }
    @CsvSource("REFER_POLICE", "REFER_INAD")
    @ParameterizedTest
    fun `update outcome details for referral`(code: OutcomeCode) {
      val id = if (code == OutcomeCode.REFER_INAD) 3L else 1L
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val response = outcomeService.updateReferral(1, code, code.name)

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.outcomes.first { it.id == id }.details).isEqualTo(code.name)
      assertThat(response).isNotNull
    }
  }
}
