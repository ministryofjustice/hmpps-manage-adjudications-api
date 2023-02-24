package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
      hearingOutcomeService.createReferral(
        adjudicationNumber = 1, adjudicator = "test", code = HearingOutcomeCode.REFER_POLICE, details = ""
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
    fun `create a referral outcome`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingOutcomeService.createReferral(
        1, HearingOutcomeCode.REFER_POLICE, "test", "details",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("test")
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isEqualTo("details")

      assertThat(response).isNotNull
    }

    @Test
    fun `create an adjourn` () {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = hearingOutcomeService.createAdjourn(
        1, "test", HearingOutcomeAdjournReason.LEGAL_ADVICE, "details", HearingOutcomePlea.UNFIT
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
    fun `throws an entity not found if the hearing for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication
          .also { it.hearings.clear() }
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
          adjudicationNumber = 1L, code = HearingOutcomeCode.ADJOURN, adjudicator = "test", details = "details",
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
