package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Java6Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.OffenceDetailsRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import java.time.LocalDateTime
import java.util.Optional
import javax.persistence.EntityNotFoundException

class DraftOffenceServiceTest : DraftAdjudicationTestBase() {

  private val incidentOffenceService = DraftOffenceService(
    draftAdjudicationRepository, offenceCodeLookupService, authenticationFacade
  )

  private val draftAdjudicationEntity = DraftAdjudication(
    id = 1,
    prisonerNumber = "A12345",
    agencyId = "MDI",
    incidentDetails = DraftAdjudicationServiceTest.incidentDetails(2L, clock),
    incidentRole = DraftAdjudicationServiceTest.incidentRoleWithNoValuesSet(),
  )

  @Test
  override fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
    whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

    Assertions.assertThatThrownBy {
      incidentOffenceService.setOffenceDetails(
        1,
        listOf(BASIC_OFFENCE_DETAILS_REQUEST)
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
        listOf(BASIC_OFFENCE_DETAILS_REQUEST)
      )
    }.isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining(ValidationChecks.APPLICABLE_RULES.errorMessage)
  }

  @ParameterizedTest
  @CsvSource(
    "true",
    "false",
  )
  fun `adds the offence details to a draft adjudication`(
    isYouthOffender: Boolean,
  ) {
    var offenceDetailsToAdd = listOf(
      BASIC_OFFENCE_DETAILS_REQUEST,
      FULL_OFFENCE_DETAILS_REQUEST
    )
    var offenceDetailsToSave = mutableListOf(
      BASIC_OFFENCE_DETAILS_DB_ENTITY,
      FULL_OFFENCE_DETAILS_DB_ENTITY
    )
    var expectedOffenceDetailsResponse = listOf(
      BASIC_OFFENCE_DETAILS_RESPONSE_DTO,
      FULL_OFFENCE_DETAILS_RESPONSE_DTO
    )
    if (isYouthOffender) {
      offenceDetailsToAdd = listOf(YOUTH_OFFENCE_DETAILS_REQUEST)
      offenceDetailsToSave = mutableListOf(YOUTH_OFFENCE_DETAILS_DB_ENTITY)
      expectedOffenceDetailsResponse = listOf(YOUTH_OFFENCE_DETAILS_RESPONSE_DTO)
    }

    whenever(draftAdjudicationRepository.findById(any())).thenReturn(
      Optional.of(
        draftAdjudicationEntity.also {
          it.isYouthOffender = isYouthOffender
        }
      )
    )

    whenever(draftAdjudicationRepository.save(any())).thenReturn(
      draftAdjudicationEntity.copy(
        offenceDetails = offenceDetailsToSave
      )
    )

    val draftAdjudication = incidentOffenceService.setOffenceDetails(1, offenceDetailsToAdd)

    assertThat(draftAdjudication)
      .extracting("id", "prisonerNumber")
      .contains(1L, "A12345")

    assertThat(draftAdjudication.offenceDetails).isEqualTo(expectedOffenceDetailsResponse)

    val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
    verify(draftAdjudicationRepository).save(argumentCaptor.capture())

    Java6Assertions.assertThat(argumentCaptor.value.offenceDetails).isEqualTo(offenceDetailsToSave)
  }

  @Test
  fun `edits the offence details of an existing draft adjudication`() {
    val existingOffenceDetails = mutableListOf(Offence(offenceCode = 1))
    val offenceDetailsToUse = listOf(
      BASIC_OFFENCE_DETAILS_REQUEST,
      FULL_OFFENCE_DETAILS_REQUEST
    )
    val offenceDetailsToSave = mutableListOf(
      BASIC_OFFENCE_DETAILS_DB_ENTITY,
      FULL_OFFENCE_DETAILS_DB_ENTITY
    )
    val expectedOffenceDetailsResponse = listOf(
      BASIC_OFFENCE_DETAILS_RESPONSE_DTO,
      FULL_OFFENCE_DETAILS_RESPONSE_DTO
    )
    val existingDraftAdjudicationEntity = DraftAdjudication(
      id = 1,
      prisonerNumber = "A12345",
      agencyId = "MDI",
      incidentDetails = IncidentDetails(
        locationId = 1,
        dateTimeOfIncident = LocalDateTime.now(clock),
        dateTimeOfDiscovery = LocalDateTime.now(clock).plusDays(1),
        handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
      ),
      incidentRole = DraftAdjudicationServiceTest.incidentRoleWithNoValuesSet(),
      offenceDetails = existingOffenceDetails,
      isYouthOffender = false
    )

    whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(existingDraftAdjudicationEntity))

    whenever(draftAdjudicationRepository.save(any())).thenReturn(
      existingDraftAdjudicationEntity.copy(
        offenceDetails = offenceDetailsToSave
      )
    )

    val draftAdjudication = incidentOffenceService.setOffenceDetails(1, offenceDetailsToUse)

    assertThat(draftAdjudication)
      .extracting("id", "prisonerNumber")
      .contains(1L, "A12345")

    assertThat(draftAdjudication.offenceDetails).isEqualTo(expectedOffenceDetailsResponse)

    val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
    verify(draftAdjudicationRepository).save(argumentCaptor.capture())

    Java6Assertions.assertThat(argumentCaptor.value.offenceDetails).isEqualTo(offenceDetailsToSave)
  }

  @Test
  fun `treats empty strings as null values`() {
    val offenceDetailsToAdd = listOf(
      OffenceDetailsRequestItem(
        offenceCode = 2,
        victimPrisonersNumber = "",
        victimStaffUsername = "",
        victimOtherPersonsName = "",
      )
    )
    val offenceDetailsToSave = mutableListOf(
      Offence(
        offenceCode = 2,
      )
    )
    val expectedOffenceDetailsResponse = listOf(
      OffenceDetailsDto(
        offenceCode = 2,
        offenceRule = OffenceRuleDetailsDto(
          paragraphNumber = OFFENCE_CODE_2_PARAGRAPH_NUMBER,
          paragraphDescription = OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION,
        ),
      )
    )

    val draftAdjudicationEntity = DraftAdjudication(
      id = 1,
      prisonerNumber = "A12345",
      agencyId = "MDI",
      incidentDetails = DraftAdjudicationServiceTest.incidentDetails(2L, clock),
      incidentRole = DraftAdjudicationServiceTest.incidentRoleWithNoValuesSet(),
      isYouthOffender = false
    )

    whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.of(draftAdjudicationEntity))

    whenever(draftAdjudicationRepository.save(any())).thenReturn(
      draftAdjudicationEntity.copy(
        offenceDetails = offenceDetailsToSave
      )
    )

    val draftAdjudication = incidentOffenceService.setOffenceDetails(1, offenceDetailsToAdd)

    assertThat(draftAdjudication)
      .extracting("id", "prisonerNumber")
      .contains(1L, "A12345")

    assertThat(draftAdjudication.offenceDetails).isEqualTo(expectedOffenceDetailsResponse)

    val argumentCaptor = ArgumentCaptor.forClass(DraftAdjudication::class.java)
    verify(draftAdjudicationRepository).save(argumentCaptor.capture())

    Java6Assertions.assertThat(argumentCaptor.value.offenceDetails).isEqualTo(offenceDetailsToSave)
  }

  @Test
  fun `throws an IllegalArgumentException when no offence details are provided`() {
    Assertions.assertThatThrownBy {
      incidentOffenceService.setOffenceDetails(1, listOf())
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("Please supply at least one set of items")
  }
}
