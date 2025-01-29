package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.OffenceDetailsRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Characteristic
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import java.time.LocalDateTime
import java.util.*

class DraftOffenceServiceTest : DraftAdjudicationTestBase() {

  private val incidentOffenceService = DraftOffenceService(
    draftAdjudicationRepository,
    offenceCodeLookupService,
    authenticationFacade,
  )

  private val draftAdjudicationEntity = DraftAdjudication(
    id = 1,
    prisonerNumber = "A12345",
    gender = Gender.MALE,
    agencyId = "MDI",
    incidentDetails = DraftAdjudicationServiceTest.incidentDetails(2L, locationUuid = UUID.fromString("0194ac90-2def-7c63-9f46-b3ccc911fdff"), clock),
    incidentRole = DraftAdjudicationServiceTest.incidentRoleWithNoValuesSet(),
  )

  @Test
  override fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
    whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

    Assertions.assertThatThrownBy {
      incidentOffenceService.setOffenceDetails(
        1,
        OffenceDetailsRequestItem(offenceCode = 1002),
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("DraftAdjudication not found for 1")
  }

  @Test
  fun `throws state exception if isYouthOffender is not set`() {
    whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudicationEntity))

    Assertions.assertThatThrownBy {
      incidentOffenceService.setOffenceDetails(
        1,
        OffenceDetailsRequestItem(offenceCode = 1002),
      )
    }.isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining(ValidationChecks.APPLICABLE_RULES.errorMessage)
  }

  @Test
  fun `throws bad request exception if offence code not valid`() {
    whenever(draftAdjudicationRepository.findById(any())).thenReturn(
      Optional.of(
        draftAdjudicationEntity.also {
          it.isYouthOffender = false
        },
      ),
    )

    Assertions.assertThatThrownBy {
      incidentOffenceService.setOffenceDetails(
        1,
        OffenceDetailsRequestItem(offenceCode = 2),
      )
    }.isInstanceOf(ValidationException::class.java)
      .hasMessageContaining("Invalid offence code 2")
  }

  @ParameterizedTest
  @CsvSource(
    "true",
    "false",
  )
  fun `adds the offence details to a draft adjudication`(
    isYouthOffender: Boolean,
  ) {
    val offenceDetailsToAdd = OffenceDetailsRequestItem(offenceCode = 1002)

    whenever(draftAdjudicationRepository.findById(any())).thenReturn(
      Optional.of(
        draftAdjudicationEntity.also {
          it.isYouthOffender = isYouthOffender
        },
      ),
    )

    whenever(draftAdjudicationRepository.save(any())).thenReturn(draftAdjudicationEntity)

    val draftAdjudication = incidentOffenceService.setOffenceDetails(1, offenceDetailsToAdd)

    assertThat(draftAdjudication)
      .extracting("id", "prisonerNumber")
      .contains(1L, "A12345")

    val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
    verify(draftAdjudicationRepository).save(argumentCaptor.capture())

    assertThat(argumentCaptor.value.offenceDetails.first().offenceCode).isEqualTo(1002)
  }

  @Test
  fun `edits the offence details of an existing draft adjudication`() {
    val existingOffenceDetails = mutableListOf(Offence(offenceCode = 1001))

    val existingDraftAdjudicationEntity = DraftAdjudication(
      id = 1,
      prisonerNumber = "A12345",
      gender = Gender.MALE,
      agencyId = "MDI",
      incidentDetails = IncidentDetails(
        locationId = 1,
        locationUuid = UUID.fromString("0194ac91-b762-7baf-a52e-725d34f05a78"),
        dateTimeOfIncident = LocalDateTime.now(clock),
        dateTimeOfDiscovery = LocalDateTime.now(clock).plusDays(1),
        handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
      ),
      incidentRole = DraftAdjudicationServiceTest.incidentRoleWithNoValuesSet(),
      offenceDetails = existingOffenceDetails,
      isYouthOffender = false,
    )

    whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(existingDraftAdjudicationEntity))

    whenever(draftAdjudicationRepository.save(any())).thenReturn(
      existingDraftAdjudicationEntity,
    )

    val draftAdjudication = incidentOffenceService.setOffenceDetails(1, OffenceDetailsRequestItem(offenceCode = 1002))

    assertThat(draftAdjudication)
      .extracting("id", "prisonerNumber")
      .contains(1L, "A12345")

    val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
    verify(draftAdjudicationRepository).save(argumentCaptor.capture())

    assertThat(argumentCaptor.value.offenceDetails.first().offenceCode).isEqualTo(1002)
  }

  @Test
  fun `gets all offence adult rules`() {
    val offenceRules = incidentOffenceService.getRules(isYouthOffender = false, gender = Gender.MALE, version = 1)

    assertThat(offenceRules.size).isEqualTo(offenceCodeLookupService.getAdultOffenceCodesByVersion(1).distinctBy { it.paragraph }.size)
  }

  @Test
  fun `gets all yoi offence rules`() {
    val offenceRules = incidentOffenceService.getRules(isYouthOffender = true, gender = Gender.MALE, version = 1)

    assertThat(offenceRules.size).isEqualTo(offenceCodeLookupService.getYouthOffenceCodesByVersion(1).distinctBy { it.paragraph }.size)
  }

  @Test
  fun `adds protected characteristics to the offence`() {
    val existingDraftAdjudicationEntity = DraftAdjudication(
      id = 1,
      prisonerNumber = "A12345",
      gender = Gender.MALE,
      agencyId = "MDI",
      incidentDetails = IncidentDetails(
        locationId = 1,
        locationUuid = UUID.fromString("0194ac91-b762-7baf-a52e-725d34f05a78"),
        dateTimeOfIncident = LocalDateTime.now(clock),
        dateTimeOfDiscovery = LocalDateTime.now(clock).plusDays(1),
        handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
      ),
      incidentRole = DraftAdjudicationServiceTest.incidentRoleWithNoValuesSet(),
      isYouthOffender = false,
    )

    whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(existingDraftAdjudicationEntity))
    whenever(draftAdjudicationRepository.save(any())).thenReturn(existingDraftAdjudicationEntity)
    val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)

    incidentOffenceService.setOffenceDetails(
      id = 1,
      offenceDetails = OffenceDetailsRequestItem(
        offenceCode = 1001,
        protectedCharacteristics = listOf(Characteristic.AGE),
      ),
    )
    verify(draftAdjudicationRepository).save(argumentCaptor.capture())

    assertThat(argumentCaptor.value.offenceDetails.first().protectedCharacteristics.first().characteristic).isEqualTo(
      Characteristic.AGE,
    )
  }
}
