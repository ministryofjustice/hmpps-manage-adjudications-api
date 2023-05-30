package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.LegacyNomisGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.HearingRepository
import java.time.LocalDateTime

class NomisHearingOutcomeServiceTest : ReportedAdjudicationTestBase() {

  private val legacyNomisGateway: LegacyNomisGateway = mock()
  private val hearingRepository: HearingRepository = mock()
  private val nomisHearingOutcomeService = NomisHearingOutcomeService(
    legacyNomisGateway,
    reportedAdjudicationRepository,
    hearingRepository,
  )

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // not applicable
  }

  @Test
  fun `updates hearings where hearing outcome record exists in nomis `() {
    val reportedAdjudication = entityBuilder.reportedAdjudication().also {
      it.hearings.first().oicHearingId = 1L
      it.hearings.add(
        Hearing(
          reportNumber = 1L,
          dateTimeOfHearing = LocalDateTime.now(),
          locationId = 1,
          agencyId = "",
          oicHearingId = 3L,
          oicHearingType = OicHearingType.GOV,
          hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = ""),
        ),
      )
    }

    whenever(hearingRepository.findByHearingOutcomeIsNull()).thenReturn(
      listOf(
        Hearing(reportNumber = 1L, dateTimeOfHearing = LocalDateTime.now(), locationId = 1, agencyId = "", oicHearingId = 1L, oicHearingType = OicHearingType.GOV),
        Hearing(reportNumber = 2L, dateTimeOfHearing = LocalDateTime.now(), locationId = 1, agencyId = "", oicHearingId = 2L, oicHearingType = OicHearingType.GOV),
      ),
    )

    whenever(reportedAdjudicationRepository.findByReportNumber(1L)).thenReturn(reportedAdjudication)

    whenever(legacyNomisGateway.hearingOutcomesExistInNomis(1L, 1L)).thenReturn(true)
    whenever(legacyNomisGateway.hearingOutcomesExistInNomis(2L, 2L)).thenReturn(false)

    nomisHearingOutcomeService.checkForNomisHearingOutcomesAndUpdate()

    verify(hearingRepository, atLeastOnce()).findByHearingOutcomeIsNull()
    verify(legacyNomisGateway, atLeastOnce()).hearingOutcomesExistInNomis(any(), any())

    val updatedHearing = reportedAdjudication.hearings.first { it.oicHearingId == 1L }

    assertThat(updatedHearing.hearingOutcome).isNotNull
    assertThat(updatedHearing.hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.NOMIS)
    assertThat(updatedHearing.hearingOutcome!!.adjudicator).isEqualTo("")
  }
}
