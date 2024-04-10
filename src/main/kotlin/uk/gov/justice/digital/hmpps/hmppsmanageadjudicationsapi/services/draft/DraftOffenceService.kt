package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.OffenceDetailsRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodes

@Transactional
@Service
class DraftOffenceService(
  draftAdjudicationRepository: DraftAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : DraftAdjudicationBaseService(
  draftAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {

  fun getRule(offenceCode: Int, isYouthOffender: Boolean, gender: Gender): OffenceRuleDetailsDto {
    val offenceDetails = offenceCodeLookupService.getOffenceCode(
      offenceCode = offenceCode,
      isYouthOffender = isYouthOffender,
    )

    return OffenceRuleDetailsDto(
      paragraphNumber = offenceDetails.paragraph,
      paragraphDescription = offenceDetails.paragraphDescription.getParagraphDescription(gender),
    )
  }

  fun getRules(isYouthOffender: Boolean, gender: Gender): List<OffenceRuleDetailsDto> =
    when (isYouthOffender) {
      true -> offenceCodeLookupService.youthOffenceCodes
      false -> offenceCodeLookupService.adultOffenceCodes
    }.distinctBy { it.paragraph }.map {
      OffenceRuleDetailsDto(
        paragraphNumber = it.paragraph,
        paragraphDescription = it.paragraphDescription.getParagraphDescription(gender = gender),
      )
    }

  fun setOffenceDetails(id: Long, offenceDetails: OffenceDetailsRequestItem): DraftAdjudicationDto {
    val draftAdjudication = find(id)
    // NOTE: new flow sets isYouthOffender first, therefore if we do not have this set we must throw as .Dto requires it
    ValidationChecks.APPLICABLE_RULES.validate(draftAdjudication)
    OffenceCodes.validateOffenceCode(offenceDetails.offenceCode)

    val newValuesToStore =
      Offence(
        offenceCode = offenceDetails.offenceCode,
        victimPrisonersNumber = offenceDetails.victimPrisonersNumber?.ifBlank { null },
        victimStaffUsername = offenceDetails.victimStaffUsername?.ifBlank { null },
        victimOtherPersonsName = offenceDetails.victimOtherPersonsName?.ifBlank { null },
      )

    draftAdjudication.offenceDetails.clear()
    draftAdjudication.offenceDetails.add(newValuesToStore)

    return saveToDto(draftAdjudication)
  }
}
