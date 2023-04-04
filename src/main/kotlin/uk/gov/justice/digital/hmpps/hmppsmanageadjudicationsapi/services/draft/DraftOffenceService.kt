package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.OffenceDetailsRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodes
import javax.transaction.Transactional

@Transactional
@Service
class DraftOffenceService(
  draftAdjudicationRepository: DraftAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
) : DraftAdjudicationBaseService(
  draftAdjudicationRepository,
  offenceCodeLookupService,
) {

  fun lookupRuleDetails(offenceCode: Int, isYouthOffender: Boolean, gender: Gender): OffenceRuleDetailsDto {
    return OffenceRuleDetailsDto(
      paragraphNumber = offenceCodeLookupService.getParagraphNumber(offenceCode, isYouthOffender),
      paragraphDescription = offenceCodeLookupService.getParagraphDescription(offenceCode, isYouthOffender, gender),
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
