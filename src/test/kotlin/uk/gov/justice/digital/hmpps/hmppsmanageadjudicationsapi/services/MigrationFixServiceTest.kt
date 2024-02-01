package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import java.time.LocalDateTime

class MigrationFixServiceTest : ReportedAdjudicationTestBase() {

  private val migrationFixService = MigrationFixService(reportedAdjudicationRepository)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }

  @Test
  fun `fix missing adjourn on first hearing`() {
    val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

    whenever(reportedAdjudicationRepository.getReportsMissingAdjourn()).thenReturn(
      listOf(
        entityBuilder.reportedAdjudication().also {
          it.hearings.first().hearingOutcome = null
          it.hearings.add(
            Hearing(
              dateTimeOfHearing = LocalDateTime.now().plusDays(1),
              agencyId = "",
              locationId = 1,
              oicHearingType = OicHearingType.GOV_ADULT,
              chargeNumber = "",
              hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = ""),
            ),
          )
        },
      ),
    )

    migrationFixService.repair()

    verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

    Assertions.assertThat(argumentCaptor.value.hearings.minByOrNull { it.dateTimeOfHearing }!!.hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
  }
}
