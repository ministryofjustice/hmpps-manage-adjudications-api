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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.WitnessRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Witness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import java.util.Optional

class DraftWitnessesServiceTest : DraftAdjudicationTestBase() {

  private val witnessesService = DraftWitnessesService(
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
      incidentDetails = DraftAdjudicationServiceTest.incidentDetails(2L, now),
      incidentRole = DraftAdjudicationServiceTest.incidentRoleWithAllValuesSet(),
      incidentStatement = IncidentStatement(
        statement = "Example statement",
        completed = false,
      ),
      offenceDetails = mutableListOf(BASIC_OFFENCE_DETAILS_DB_ENTITY, FULL_OFFENCE_DETAILS_DB_ENTITY),
      isYouthOffender = true,
      witnesses = mutableListOf(
        Witness(code = WitnessCode.OFFICER, firstName = "prison", lastName = "officer", reporter = "Fred"),
      ),
    )

  @BeforeEach
  fun init() {
    whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudication))
    whenever(draftAdjudicationRepository.save(any())).thenReturn(draftAdjudication)
    whenever(authenticationFacade.currentUsername).thenReturn("Fred")
  }

  @Test
  fun `add witnesses to adjudication`() {
    val response = witnessesService.setWitnesses(
      1,
      listOf(
        WitnessRequestItem(code = WitnessCode.OFFICER, firstName = "prison", lastName = "officer"),
      ),
    )

    val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
    verify(draftAdjudicationRepository).save(argumentCaptor.capture())

    assertThat(argumentCaptor.value.witnesses.size).isEqualTo(1)
    assertThat(argumentCaptor.value.witnesses.first().code).isEqualTo(WitnessCode.OFFICER)
    assertThat(argumentCaptor.value.witnesses.first().firstName).isEqualTo("prison")
    assertThat(argumentCaptor.value.witnesses.first().lastName).isEqualTo("officer")
    assertThat(argumentCaptor.value.witnesses.first().reporter).isEqualTo("Fred")
    assertThat(argumentCaptor.value.witnessesSaved).isEqualTo(true)

    assertThat(response).isNotNull
  }

  @Test
  override fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
    whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

    Assertions.assertThatThrownBy {
      witnessesService.setWitnesses(
        1,
        listOf(WitnessRequestItem(code = WitnessCode.OFFICER, firstName = "prison", lastName = "officer")),
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("DraftAdjudication not found for 1")
  }
}
