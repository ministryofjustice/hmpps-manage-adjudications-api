package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
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

  /*
      so only 1 is issue.  its on the second hearing.
      the rest will be fixed later.
   */
  @Test
  fun `fix ranby case`() {
    val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
      entityBuilder.reportedAdjudication().also {
        it.status = ReportedAdjudicationStatus.ADJOURNED
        it.clearOutcomes()
        it.hearings.clear()
        it.hearings.add(
          Hearing(
            dateTimeOfHearing = LocalDateTime.now().plusDays(3),
            locationId = 1,
            oicHearingType = OicHearingType.GOV,
            agencyId = "",
            chargeNumber = "",
            hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = "", details = "PROVED"),
          ),
        )
        it.hearings.add(
          Hearing(
            dateTimeOfHearing = LocalDateTime.now(),
            locationId = 1,
            oicHearingType = OicHearingType.GOV,
            agencyId = "",
            chargeNumber = "",
            hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = ""),
          ),
        )
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
        it.addOutcome(
          Outcome(code = OutcomeCode.REFER_POLICE, actualCreatedDate = LocalDateTime.now()),
        )
      },
    )

    migrationFixService.repair()
    verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

    Assertions.assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(3)
    Assertions.assertThat(argumentCaptor.value.hearings.sortedBy { it.dateTimeOfHearing }[2].hearingOutcome?.code).isEqualTo(HearingOutcomeCode.COMPLETE)
    Assertions.assertThat(argumentCaptor.value.getOutcomes().sortedBy { it.getCreatedDateTime() }.first().code).isEqualTo(OutcomeCode.REFER_POLICE)
    Assertions.assertThat(argumentCaptor.value.getOutcomes().sortedBy { it.getCreatedDateTime() }[1].code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
    Assertions.assertThat(argumentCaptor.value.getOutcomes().sortedBy { it.getCreatedDateTime() }.last().code).isEqualTo(OutcomeCode.CHARGE_PROVED)

    Assertions.assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.CHARGE_PROVED)
  }
}
