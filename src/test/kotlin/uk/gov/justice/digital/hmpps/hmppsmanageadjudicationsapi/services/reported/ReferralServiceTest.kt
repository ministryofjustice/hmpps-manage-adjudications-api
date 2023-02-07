package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.assertj.core.api.Assertions
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException

class ReferralServiceTest : ReportedAdjudicationTestBase() {

  private var referralService = ReferralService(
    reportedAdjudicationRepository, offenceCodeLookupService, authenticationFacade
  )

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    Assertions.assertThatThrownBy {
      referralService.createReferral(1, 1, HearingOutcomeCode.REFER_POLICE, "test", "details")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      referralService.updateReferral(1, 1, HearingOutcomeCode.REFER_POLICE, "test", "details")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }

  @Nested
  inner class CreateReferral {

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
    fun `create outcome and hearing outcome for referral`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = referralService.createReferral(
        1, 1, HearingOutcomeCode.REFER_POLICE, "test", "details",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("test")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isEqualTo("details")
      assertThat(argumentCaptor.value.outcome!!.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(argumentCaptor.value.outcome!!.details).isEqualTo("details")
      assertThat(argumentCaptor.value.status).isEqualTo(OutcomeCode.REFER_POLICE)

      assertThat(response).isNotNull
    }
  }

  @Nested
  inner class UpdateReferral {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.createdByUserId = ""
        it.createDateTime = LocalDateTime.now()
        it.hearings.first().hearingOutcome = HearingOutcome(
          code = HearingOutcomeCode.REFER_POLICE,
          details = "details",
          adjudicator = "adjudicator",
        )
        it.outcome = Outcome(
          code = OutcomeCode.REFER_POLICE,
          details = "details"
        )
        it.status = ReportedAdjudicationStatus.REFER_POLICE
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
    fun `updates outcome and hearing outcome for referral`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = referralService.updateReferral(
        1, 1, HearingOutcomeCode.REFER_INAD, "test 2", "details 2",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("test 2")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.REFER_INAD)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isEqualTo("details 2")
      assertThat(argumentCaptor.value.outcome!!.code).isEqualTo(OutcomeCode.REFER_INAD)
      assertThat(argumentCaptor.value.outcome!!.details).isEqualTo("details 2")
      assertThat(argumentCaptor.value.status).isEqualTo(OutcomeCode.REFER_INAD)

      assertThat(response).isNotNull
    }
  }
}
