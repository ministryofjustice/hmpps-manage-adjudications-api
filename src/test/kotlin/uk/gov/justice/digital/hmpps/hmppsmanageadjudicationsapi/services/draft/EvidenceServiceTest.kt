package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import jakarta.persistence.EntityNotFoundException
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import java.util.*

class EvidenceServiceTest : DraftAdjudicationTestBase() {

  private val evidenceService = DraftEvidenceService(
    draftAdjudicationRepository,
    offenceCodeLookupService,
    authenticationFacade,
  )

  private val draftAdjudication =
    DraftAdjudication(
      id = 1,
      prisonerNumber = "A12345",
      gender = Gender.MALE,
      agencyId = "MDI",
      incidentDetails = DraftAdjudicationServiceTest.incidentDetails(
        2L,
        locationUuid = UUID.fromString("0194ac90-2def-7c63-9f46-b3ccc911fdff"),
        now,
      ),
      incidentRole = DraftAdjudicationServiceTest.incidentRoleWithAllValuesSet(),
      incidentStatement = IncidentStatement(
        statement = "Example statement",
        completed = false,
      ),
      offenceDetails = mutableListOf(Offence(offenceCode = 1002)),
      isYouthOffender = true,
      evidence = mutableListOf(
        Evidence(code = EvidenceCode.PHOTO, details = "details", reporter = "Fred"),
      ),
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
        EvidenceRequestItem(code = EvidenceCode.PHOTO, details = "details"),
      ),
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
        listOf(EvidenceRequestItem(code = EvidenceCode.PHOTO, details = "details")),
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("DraftAdjudication not found for 1")
  }
}
