package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions
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

  @Test
  fun `ignores refer police if they have scheduled hearing`() {
    whenever(reportedAdjudicationRepository.findByMigratedIsFalseAndStatus(any())).thenReturn(
      listOf(
        entityBuilder.reportedAdjudication().also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "")
          it.clearOutcomes()
          it.addOutcome(
            Outcome(code = OutcomeCode.REFER_POLICE),
          )
          it.addOutcome(
            Outcome(code = OutcomeCode.SCHEDULE_HEARING),
          )
        },
      ),
    )

    migrationFixService.repair()
    verify(reportedAdjudicationRepository, never()).save(any())
  }

  @Test
  fun `ignores records if it has no refer police outcome`() {
    whenever(reportedAdjudicationRepository.findByMigratedIsFalseAndStatus(any())).thenReturn(
      listOf(
        entityBuilder.reportedAdjudication(),
      ),
    )

    migrationFixService.repair()
    verify(reportedAdjudicationRepository, never()).save(any())
  }

  @Test
  fun `ignores a record if no hearing follows the police refer`() {
    whenever(reportedAdjudicationRepository.findByMigratedIsFalseAndStatus(any())).thenReturn(
      listOf(
        entityBuilder.reportedAdjudication().also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "")
          it.clearOutcomes()
          it.addOutcome(
            Outcome(code = OutcomeCode.REFER_POLICE),
          )
        },
      ),
    )

    migrationFixService.repair()
    verify(reportedAdjudicationRepository, never()).save(any())
  }

  @Test
  fun `ignores records if hearing outcome detail of later hearing is not PROVED`() {
    whenever(reportedAdjudicationRepository.findByMigratedIsFalseAndStatus(any())).thenReturn(
      listOf(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.ADJOURNED
          it.clearOutcomes()
          it.hearings.clear()
          it.hearings.add(
            Hearing(
              dateTimeOfHearing = LocalDateTime.now().minusDays(1),
              locationId = 1,
              oicHearingType = OicHearingType.GOV,
              agencyId = "",
              chargeNumber = "",
              hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = ""),
            ),
          )
          it.hearings.add(
            Hearing(
              dateTimeOfHearing = LocalDateTime.now(),
              locationId = 1,
              oicHearingType = OicHearingType.GOV,
              agencyId = "",
              chargeNumber = "",
              hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = "", details = "something else"),
            ),
          )
          it.addOutcome(
            Outcome(code = OutcomeCode.REFER_POLICE),
          )
        },
      ),
    )

    migrationFixService.repair()
    verify(reportedAdjudicationRepository, never()).save(any())
  }

  @Test
  fun `add missing next step for refer police - schedule a hearing, outcome should be charge proved and not adjourned, when details of hearing is PROVED`() {
    val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

    whenever(reportedAdjudicationRepository.findByMigratedIsFalseAndStatus(ReportedAdjudicationStatus.ADJOURNED)).thenReturn(
      listOf(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.ADJOURNED
          it.clearOutcomes()
          it.hearings.clear()
          it.hearings.add(
            Hearing(
              dateTimeOfHearing = LocalDateTime.now().minusDays(1),
              locationId = 1,
              oicHearingType = OicHearingType.GOV,
              agencyId = "",
              chargeNumber = "",
              hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = ""),
            ),
          )
          it.hearings.add(
            Hearing(
              dateTimeOfHearing = LocalDateTime.now(),
              locationId = 1,
              oicHearingType = OicHearingType.GOV,
              agencyId = "",
              chargeNumber = "",
              hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = "", details = "PROVED"),
            ),
          )
          it.addOutcome(
            Outcome(code = OutcomeCode.REFER_POLICE, actualCreatedDate = LocalDateTime.now()),
          )
        },
      ),
    )

    migrationFixService.repair()
    verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

    Assertions.assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(3)
    Assertions.assertThat(argumentCaptor.value.hearings[1].hearingOutcome?.code).isEqualTo(HearingOutcomeCode.COMPLETE)
    Assertions.assertThat(argumentCaptor.value.getOutcomes().sortedBy { it.getCreatedDateTime() }.first().code).isEqualTo(OutcomeCode.REFER_POLICE)
    Assertions.assertThat(argumentCaptor.value.getOutcomes().sortedBy { it.getCreatedDateTime() }[1].code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
    Assertions.assertThat(argumentCaptor.value.getOutcomes().sortedBy { it.getCreatedDateTime() }.last().code).isEqualTo(OutcomeCode.CHARGE_PROVED)

    Assertions.assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.CHARGE_PROVED)
  }
}
