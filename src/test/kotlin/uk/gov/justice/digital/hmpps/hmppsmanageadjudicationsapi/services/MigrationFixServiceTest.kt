package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.atMost
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
     //na
  }

  @Test
  fun `ignores refer police if they have scheduled hearing` () {

  }

  @Test
  fun `ignores records if it has no refer police outcome` () {

  }

  //most important case.  fix first
  @Test
  fun `add missing next step for refer police - schedule a hearing, outcome should be charge proved and not adjourned, when details of hearing is PROVED` () {
    val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

    whenever(reportedAdjudicationRepository.findByMigratedIsFalseAndStatus(ReportedAdjudicationStatus.ADJOURNED)).thenReturn(
       listOf(
         entityBuilder.reportedAdjudication().also {
           it.status = ReportedAdjudicationStatus.ADJOURNED
           it.clearOutcomes()
           it.hearings.clear()
           it.hearings.add(
             Hearing(dateTimeOfHearing = LocalDateTime.now().minusDays(1), locationId = 1, oicHearingType = OicHearingType.GOV, agencyId = "", chargeNumber = "",
             hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "")
             )
           )
           it.hearings.add(
             Hearing(dateTimeOfHearing = LocalDateTime.now(), locationId = 1, oicHearingType = OicHearingType.GOV, agencyId = "", chargeNumber = "",
               hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = "", details = "PROVED")
             )
           )
           it.addOutcome(
             Outcome(code = OutcomeCode.REFER_POLICE)
           )
         },
       )
     )
    verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

    migrationFixService.repair()

    Assertions.assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(2)
    Assertions.assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.CHARGE_PROVED)
  }


}