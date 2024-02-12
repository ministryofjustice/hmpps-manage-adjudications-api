package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

class MigrationFixServiceTest : ReportedAdjudicationTestBase() {

  private val migrationFixService = MigrationFixService(reportedAdjudicationRepository)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }

  @Test
  fun `if we have multiple hearings at the same time, use the non adjourned status`() {
    val report = entityBuilder.reportedAdjudication().also {
      it.status = ReportedAdjudicationStatus.ADJOURNED
      it.addPunishment(
        Punishment(
          type = PunishmentType.ADDITIONAL_DAYS,
          schedule = mutableListOf(
            PunishmentSchedule(days = 0),
          ),
        ),
      )
      it.hearings.clear()
      it.hearings.add(
        Hearing(
          locationId = 1,
          oicHearingType = OicHearingType.INAD_ADULT,
          agencyId = "",
          chargeNumber = "",
          dateTimeOfHearing = LocalDate.now().atStartOfDay(),
          hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = ""),
        ),
      )
      it.hearings.add(
        Hearing(
          locationId = 1,
          oicHearingType = OicHearingType.INAD_ADULT,
          agencyId = "",
          chargeNumber = "",
          dateTimeOfHearing = LocalDate.now().atStartOfDay(),
          hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = ""),
        ),
      )
      it.addOutcome(
        Outcome(code = OutcomeCode.CHARGE_PROVED),
      )
    }
    whenever(reportedAdjudicationRepository.fixMigrationRecords()).thenReturn(
      listOf(report),
    )

    migrationFixService.repair()

    Assertions.assertThat(report.status).isEqualTo(ReportedAdjudicationStatus.CHARGE_PROVED)
  }

  @Test
  fun `no outcomes should with punishments should set status INVALID_OUTCOME`() {
    val report = entityBuilder.reportedAdjudication().also {
      it.status = ReportedAdjudicationStatus.ADJOURNED
      it.hearings.clear()
      it.addPunishment(
        Punishment(
          type = PunishmentType.ADDITIONAL_DAYS,
          schedule = mutableListOf(
            PunishmentSchedule(days = 0),
          ),
        ),
      )
      it.hearings.add(
        Hearing(
          locationId = 1,
          oicHearingType = OicHearingType.INAD_ADULT,
          agencyId = "",
          chargeNumber = "",
          dateTimeOfHearing = LocalDate.now().atStartOfDay(),
          hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = ""),
        ),
      )
      it.hearings.add(
        Hearing(
          locationId = 1,
          oicHearingType = OicHearingType.INAD_ADULT,
          agencyId = "",
          chargeNumber = "",
          dateTimeOfHearing = LocalDateTime.now(),
          hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = ""),
        ),
      )
    }
    whenever(reportedAdjudicationRepository.fixMigrationRecords()).thenReturn(
      listOf(report),
    )

    migrationFixService.repair()
    Assertions.assertThat(report.status).isEqualTo(ReportedAdjudicationStatus.INVALID_OUTCOME)
  }
}
