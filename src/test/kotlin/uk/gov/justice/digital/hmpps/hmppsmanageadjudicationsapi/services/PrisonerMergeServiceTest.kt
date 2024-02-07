package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftAdjudicationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import java.time.LocalDateTime

class PrisonerMergeServiceTest : ReportedAdjudicationTestBase() {
  private val draftAdjudicationRepository: DraftAdjudicationRepository = mock()
  private val prisonerMergeService = PrisonerMergeService(reportedAdjudicationRepository, draftAdjudicationRepository)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }

  @Test
  fun `does not call repo if prisoner from is empty`() {
    prisonerMergeService.merge(
      prisonerFrom = null,
      prisonerTo = "TEST",
    )

    verify(reportedAdjudicationRepository, never()).findByPrisonerNumber(any())
  }

  @Test
  fun `does not call repo if prisoner to is empty`() {
    prisonerMergeService.merge(
      prisonerFrom = "TEST",
      prisonerTo = null,
    )

    verify(reportedAdjudicationRepository, never()).findByPrisonerNumber(any())
  }

  @Test
  fun `updates prisoner adjudications to prisoner to, from prisoner from`() {
    val fromReport = entityBuilder.reportedAdjudication(prisonerNumber = "FROM")
    whenever(reportedAdjudicationRepository.findByPrisonerNumber(any())).thenReturn(listOf(fromReport))

    prisonerMergeService.merge(
      prisonerFrom = "FROM",
      prisonerTo = "TO",
    )

    verify(reportedAdjudicationRepository, atLeastOnce()).findByPrisonerNumber("FROM")
    assertThat(fromReport.prisonerNumber).isEqualTo("TO")
  }

  @Test
  fun `updates associated prisoner number to prisoner to, from prisoner from`() {
    val fromReport = entityBuilder.reportedAdjudication(prisonerNumber = "RANDOM").also {
      it.incidentRoleAssociatedPrisonersNumber = "FROM"
    }
    whenever(reportedAdjudicationRepository.findByIncidentRoleAssociatedPrisonersNumber(any())).thenReturn(listOf(fromReport))

    prisonerMergeService.merge(
      prisonerFrom = "FROM",
      prisonerTo = "TO",
    )

    verify(reportedAdjudicationRepository, atLeastOnce()).findByIncidentRoleAssociatedPrisonersNumber("FROM")
    assertThat(fromReport.incidentRoleAssociatedPrisonersNumber).isEqualTo("TO")
  }
}

class PrisonerMergeServiceDraftTest : DraftAdjudicationTestBase() {
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository = mock()
  private val prisonerMergeService = PrisonerMergeService(reportedAdjudicationRepository, draftAdjudicationRepository)

  @Test
  fun `updates prisoner draft adjudications to prisoner to, from prisoner from`() {
    val fromReport = DraftAdjudication(
      agencyId = "",
      prisonerNumber = "FROM",
      gender = Gender.MALE,
      incidentDetails =
      IncidentDetails(dateTimeOfDiscovery = LocalDateTime.now(), dateTimeOfIncident = LocalDateTime.now(), handoverDeadline = LocalDateTime.now(), locationId = 1),
    )
    whenever(draftAdjudicationRepository.findByPrisonerNumber(any())).thenReturn(listOf(fromReport))

    prisonerMergeService.merge(
      prisonerFrom = "FROM",
      prisonerTo = "TO",
    )

    verify(draftAdjudicationRepository, atLeastOnce()).findByPrisonerNumber("FROM")
    assertThat(fromReport.prisonerNumber).isEqualTo("TO")
  }

  @Test
  fun `updates draft associated prisoner number to prisoner to, from prisoner from`() {
    val fromReport = DraftAdjudication(
      agencyId = "",
      prisonerNumber = "RANDOM",
      gender = Gender.MALE,
      incidentDetails =
      IncidentDetails(dateTimeOfDiscovery = LocalDateTime.now(), dateTimeOfIncident = LocalDateTime.now(), handoverDeadline = LocalDateTime.now(), locationId = 1),
      incidentRole = IncidentRole(associatedPrisonersNumber = "FROM", associatedPrisonersName = "", roleCode = ""),
    )
    whenever(draftAdjudicationRepository.findByIncidentRoleAssociatedPrisonersNumber(any())).thenReturn(listOf(fromReport))

    prisonerMergeService.merge(
      prisonerFrom = "FROM",
      prisonerTo = "TO",
    )

    verify(draftAdjudicationRepository, atLeastOnce()).findByIncidentRoleAssociatedPrisonersNumber("FROM")
    assertThat(fromReport.incidentRole!!.associatedPrisonersNumber).isEqualTo("TO")
  }

  override fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
    // na
  }
}
