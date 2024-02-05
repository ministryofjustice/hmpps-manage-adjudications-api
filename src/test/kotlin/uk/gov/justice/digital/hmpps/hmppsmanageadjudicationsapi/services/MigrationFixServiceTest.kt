package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import java.time.LocalDateTime

class MigrationFixServiceTest : ReportedAdjudicationTestBase() {

  private val migrationFixService = MigrationFixService(reportedAdjudicationRepository)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }

  @BeforeEach
  fun `init`() {
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(null)
    whenever(reportedAdjudicationRepository.findByMigratedIsFalseAndStatus(ReportedAdjudicationStatus.REFER_INAD)).thenReturn(
      emptyList(),
    )
  }

  @Test
  fun `adds missing next step`() {
    val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

    whenever(reportedAdjudicationRepository.findByMigratedIsFalseAndStatus(ReportedAdjudicationStatus.REFER_POLICE)).thenReturn(
      listOf(
        entityBuilder.reportedAdjudication().also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "")
          it.addOutcome(Outcome(code = OutcomeCode.REFER_POLICE, actualCreatedDate = LocalDateTime.now()))
          it.hearings.add(
            Hearing(
              dateTimeOfHearing = LocalDateTime.now().plusDays(1),
              agencyId = "",
              locationId = 1,
              oicHearingType = OicHearingType.GOV_ADULT,
              chargeNumber = "",
            ),
          )
        },
      ),
    )

    migrationFixService.repair()

    verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

    Assertions.assertThat(argumentCaptor.value.getOutcomes().sortedByDescending { it.getCreatedDateTime() }.first().code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
    Assertions.assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.SCHEDULED)
  }

  @Test
  fun `does not add missing next step`() {
    whenever(reportedAdjudicationRepository.findByMigratedIsFalseAndStatus(ReportedAdjudicationStatus.REFER_POLICE)).thenReturn(
      listOf(
        entityBuilder.reportedAdjudication().also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = "")
          it.addOutcome(Outcome(code = OutcomeCode.REFER_POLICE, actualCreatedDate = LocalDateTime.now()))
          it.hearings.add(
            Hearing(
              dateTimeOfHearing = LocalDateTime.now().plusDays(1),
              agencyId = "",
              locationId = 1,
              oicHearingType = OicHearingType.GOV_ADULT,
              chargeNumber = "",
              hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = ""),
            ),
          )
        },
      ),
    )

    migrationFixService.repair()

    verify(reportedAdjudicationRepository, never()).save(any())
  }
}
