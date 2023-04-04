package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.OffenceDetailsRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

interface TestsToImplement {
  fun `throws an entity not found if the draft adjudication for the supplied id does not exists`()
}

abstract class DraftAdjudicationTestBase : TestsToImplement {
  internal val draftAdjudicationRepository: DraftAdjudicationRepository = mock()
  internal val offenceCodeLookupService: OffenceCodeLookupService = mock()
  internal val authenticationFacade: AuthenticationFacade = mock()

  @BeforeEach
  fun beforeEach() {
    // Set up offence code mocks
    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(BASIC_OFFENCE_DETAILS_REQUEST.offenceCode, false)).thenReturn(
      OFFENCE_CODE_2_NOMIS_CODE_ON_OWN,
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(BASIC_OFFENCE_DETAILS_REQUEST.offenceCode, false)).thenReturn(
      OFFENCE_CODE_2_NOMIS_CODE_ASSISTED,
    )
    whenever(offenceCodeLookupService.getParagraphNumber(BASIC_OFFENCE_DETAILS_REQUEST.offenceCode, false)).thenReturn(OFFENCE_CODE_2_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(BASIC_OFFENCE_DETAILS_REQUEST.offenceCode, false)).thenReturn(OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION)

    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(FULL_OFFENCE_DETAILS_REQUEST.offenceCode, false)).thenReturn(
      OFFENCE_CODE_3_NOMIS_CODE_ON_OWN,
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(FULL_OFFENCE_DETAILS_REQUEST.offenceCode, false)).thenReturn(
      OFFENCE_CODE_3_NOMIS_CODE_ASSISTED,
    )
    whenever(offenceCodeLookupService.getParagraphNumber(FULL_OFFENCE_DETAILS_REQUEST.offenceCode, false)).thenReturn(OFFENCE_CODE_3_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(FULL_OFFENCE_DETAILS_REQUEST.offenceCode, false)).thenReturn(OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION)

    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(BASIC_OFFENCE_DETAILS_REQUEST.offenceCode, true)).thenReturn(
      YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ON_OWN,
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(BASIC_OFFENCE_DETAILS_REQUEST.offenceCode, true)).thenReturn(
      YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ASSISTED,
    )
    whenever(offenceCodeLookupService.getParagraphNumber(BASIC_OFFENCE_DETAILS_REQUEST.offenceCode, true)).thenReturn(YOUTH_OFFENCE_CODE_2_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(BASIC_OFFENCE_DETAILS_REQUEST.offenceCode, true)).thenReturn(
      YOUTH_OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION,
    )

    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(FULL_OFFENCE_DETAILS_REQUEST.offenceCode, true)).thenReturn(
      YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ON_OWN,
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(FULL_OFFENCE_DETAILS_REQUEST.offenceCode, true)).thenReturn(
      YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ASSISTED,
    )
    whenever(offenceCodeLookupService.getParagraphNumber(FULL_OFFENCE_DETAILS_REQUEST.offenceCode, true)).thenReturn(YOUTH_OFFENCE_CODE_3_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(FULL_OFFENCE_DETAILS_REQUEST.offenceCode, true)).thenReturn(
      YOUTH_OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION,
    )
  }

  companion object {
    internal val clock: Clock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault())

    val now = LocalDateTime.now()

    val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
    val DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE = LocalDateTime.of(2010, 10, 14, 10, 0)

    val INCIDENT_ROLE_CODE = "25a"
    val INCIDENT_ROLE_PARAGRAPH_NUMBER = "25(a)"
    val INCIDENT_ROLE_PARAGRAPH_DESCRIPTION = "Attempts to commit any of the foregoing offences:"
    val INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER = "B23456"
    val INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME = "Associated Prisoner"

    private const val OFFENCE_CODE_2_NOMIS_CODE_ON_OWN = "5b"
    private const val OFFENCE_CODE_2_NOMIS_CODE_ASSISTED = "25z"
    const val OFFENCE_CODE_2_PARAGRAPH_NUMBER = "5(b)"
    const val OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION = "A paragraph description"
    private const val OFFENCE_CODE_3_NOMIS_CODE_ON_OWN = "5f"
    private const val OFFENCE_CODE_3_NOMIS_CODE_ASSISTED = "25f"
    private const val OFFENCE_CODE_3_PARAGRAPH_NUMBER = "6(a)"
    private const val OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION = "Another paragraph description"

    private const val YOUTH_OFFENCE_CODE_2_PARAGRAPH_NUMBER = "7(b)"
    private const val YOUTH_OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION = "A youth paragraph description"
    private const val YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ON_OWN = "7b"
    private const val YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ASSISTED = "29z"
    private const val YOUTH_OFFENCE_CODE_3_PARAGRAPH_NUMBER = "17(b)"
    private const val YOUTH_OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION = "Another youth paragraph description"
    private const val YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ON_OWN = "17b"
    private const val YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ASSISTED = "29f"

    val YOUTH_OFFENCE_DETAILS_REQUEST = OffenceDetailsRequestItem(offenceCode = 1001)
    val YOUTH_OFFENCE_DETAILS_RESPONSE_DTO = OffenceDetailsDto(
      offenceCode = YOUTH_OFFENCE_DETAILS_REQUEST.offenceCode,
      offenceRule = OffenceRuleDetailsDto(
        paragraphNumber = YOUTH_OFFENCE_CODE_2_PARAGRAPH_NUMBER,
        paragraphDescription = YOUTH_OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION,
      ),
    )

    val YOUTH_OFFENCE_DETAILS_DB_ENTITY = Offence(
      offenceCode = YOUTH_OFFENCE_DETAILS_RESPONSE_DTO.offenceCode,
    )

    val BASIC_OFFENCE_DETAILS_INVALID_REQUEST = OffenceDetailsRequestItem(offenceCode = 2)
    val BASIC_OFFENCE_DETAILS_REQUEST = OffenceDetailsRequestItem(offenceCode = 1001)
    val BASIC_OFFENCE_DETAILS_RESPONSE_DTO = OffenceDetailsDto(
      offenceCode = BASIC_OFFENCE_DETAILS_REQUEST.offenceCode,
      offenceRule = OffenceRuleDetailsDto(
        paragraphNumber = OFFENCE_CODE_2_PARAGRAPH_NUMBER,
        paragraphDescription = OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION,
      ),
    )
    val BASIC_OFFENCE_DETAILS_DB_ENTITY = Offence(
      offenceCode = BASIC_OFFENCE_DETAILS_RESPONSE_DTO.offenceCode,
    )

    val FULL_OFFENCE_DETAILS_REQUEST = OffenceDetailsRequestItem(
      offenceCode = 1002,
      victimPrisonersNumber = "A1234AA",
      victimStaffUsername = "ABC12D",
      victimOtherPersonsName = "A name",
    )

    val FULL_OFFENCE_DETAILS_RESPONSE_DTO = OffenceDetailsDto(
      offenceCode = FULL_OFFENCE_DETAILS_REQUEST.offenceCode,
      offenceRule = OffenceRuleDetailsDto(
        paragraphNumber = OFFENCE_CODE_3_PARAGRAPH_NUMBER,
        paragraphDescription = OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION,
      ),
      victimPrisonersNumber = FULL_OFFENCE_DETAILS_REQUEST.victimPrisonersNumber,
      victimStaffUsername = FULL_OFFENCE_DETAILS_REQUEST.victimStaffUsername,
      victimOtherPersonsName = FULL_OFFENCE_DETAILS_REQUEST.victimOtherPersonsName,
    )

    val FULL_OFFENCE_DETAILS_DB_ENTITY = Offence(
      offenceCode = FULL_OFFENCE_DETAILS_RESPONSE_DTO.offenceCode,
      victimPrisonersNumber = FULL_OFFENCE_DETAILS_RESPONSE_DTO.victimPrisonersNumber,
      victimStaffUsername = FULL_OFFENCE_DETAILS_RESPONSE_DTO.victimStaffUsername,
      victimOtherPersonsName = FULL_OFFENCE_DETAILS_RESPONSE_DTO.victimOtherPersonsName,
    )
  }
}
