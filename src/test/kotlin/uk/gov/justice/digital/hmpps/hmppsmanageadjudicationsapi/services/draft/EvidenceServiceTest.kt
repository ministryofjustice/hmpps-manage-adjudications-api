package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.EvidenceRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Evidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import java.util.Optional
import javax.persistence.EntityNotFoundException

class EvidenceServiceTest : DraftAdjudicationTestBase() {

  private val evidenceService = DraftEvidenceService(
    draftAdjudicationRepository, offenceCodeLookupService, authenticationFacade
  )

  private val draftAdjudication =
    DraftAdjudication(
      id = 1,
      prisonerNumber = "A12345",
      gender = Gender.MALE,
      agencyId = "MDI",
      incidentDetails = DraftAdjudicationServiceTest.incidentDetails(2L, now),
      incidentRole = DraftAdjudicationServiceTest.incidentRoleWithAllValuesSet(),
      incidentStatement = IncidentStatement(
        statement = "Example statement",
        completed = false
      ),
      offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
      isYouthOffender = true,
      evidence = mutableListOf(
        Evidence(code = EvidenceCode.PHOTO, details = "details", reporter = "Fred")
      )
    )

  @BeforeEach
  fun init() {
    whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudication))
    whenever(draftAdjudicationRepository.save(any())).thenReturn(draftAdjudication)
    whenever(authenticationFacade.currentUsername).thenReturn("Fred")
  }

  @Test
  fun `add evidence to adjudication`() {
    val response = evidenceService.setEvidence(
      1,
      listOf(
        EvidenceRequestItem(code = EvidenceCode.PHOTO, details = "details")
      )
    )

    val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
    verify(draftAdjudicationRepository).save(argumentCaptor.capture())

    assertThat(argumentCaptor.value.evidence.size).isEqualTo(1)
    assertThat(argumentCaptor.value.evidence.first().code).isEqualTo(EvidenceCode.PHOTO)
    assertThat(argumentCaptor.value.evidence.first().details).isEqualTo("details")
    assertThat(argumentCaptor.value.evidence.first().reporter).isEqualTo("Fred")
    assertThat(argumentCaptor.value.evidenceSaved).isEqualTo(true)

    assertThat(response).isNotNull
  }

  @Test
  override fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
    whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

    Assertions.assertThatThrownBy {
      evidenceService.setEvidence(
        1,
        listOf(EvidenceRequestItem(code = EvidenceCode.PHOTO, details = "details"))
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("DraftAdjudication not found for 1")
  }
}
