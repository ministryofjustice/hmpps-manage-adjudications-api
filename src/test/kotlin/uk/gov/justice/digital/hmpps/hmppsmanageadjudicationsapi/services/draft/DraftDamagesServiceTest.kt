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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DamageRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Damage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import java.util.*

class DraftDamagesServiceTest : DraftAdjudicationTestBase() {
  private val damagesService =
    DraftDamagesService(
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
      damages = mutableListOf(
        Damage(code = DamageCode.CLEANING, details = "details", reporter = "Fred"),
      ),
    )

  @BeforeEach
  fun init() {
    whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudication))
    whenever(draftAdjudicationRepository.save(any())).thenReturn(draftAdjudication)
    whenever(authenticationFacade.currentUsername).thenReturn("Fred")
  }

  @Test
  fun `add damages to adjudication`() {
    val response = damagesService.setDamages(
      1,
      listOf(
        DamageRequestItem(DamageCode.CLEANING, "details"),
      ),
    )

    val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
    verify(draftAdjudicationRepository).save(argumentCaptor.capture())

    assertThat(argumentCaptor.value.damages.size).isEqualTo(1)
    assertThat(argumentCaptor.value.damages.first().code).isEqualTo(DamageCode.CLEANING)
    assertThat(argumentCaptor.value.damages.first().details).isEqualTo("details")
    assertThat(argumentCaptor.value.damages.first().reporter).isEqualTo("Fred")
    assertThat(argumentCaptor.value.damagesSaved).isEqualTo(true)

    assertThat(response).isNotNull
  }

  @Test
  override fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
    whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

    Assertions.assertThatThrownBy {
      damagesService.setDamages(1, listOf(DamageRequestItem(DamageCode.CLEANING, "details")))
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("DraftAdjudication not found for 1")
  }
}
