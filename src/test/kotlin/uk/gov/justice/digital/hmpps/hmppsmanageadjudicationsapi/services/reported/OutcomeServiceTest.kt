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
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        reportedAdjudication.also {
          it.outcome = Outcome(code = OutcomeCode.REFER_POLICE)
        }
      )
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

      assertThat(argumentCaptor.value.outcome).isNotNull
      assertThat(argumentCaptor.value.outcome!!.code).isEqualTo(code)
      assertThat(argumentCaptor.value.outcome!!.details).isEqualTo("details")
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.valueOf(code.name))
      assertThat(argumentCaptor.value.outcome!!.reason).isEqualTo(NotProceedReason.RELEASED)
      assertThat(response).isNotNull
    }
  }

  @Nested
  inner class DeleteOutcome {

    @Test
    fun `delete outcome `() {
      TODO("implement me")
    }
  }
}
