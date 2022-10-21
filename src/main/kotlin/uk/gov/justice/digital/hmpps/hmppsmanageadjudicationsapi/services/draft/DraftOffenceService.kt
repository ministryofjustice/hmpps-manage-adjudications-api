package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.OffenceDetailsRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import javax.transaction.Transactional

@Transactional
@Service
class DraftOffenceService(
  draftAdjudicationRepository: DraftAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
) : DraftAdjudicationBaseService(
  draftAdjudicationRepository, offenceCodeLookupService
) {

  fun lookupRuleDetails(offenceCode: Int, isYouthOffender: Boolean): OffenceRuleDetailsDto {
    return OffenceRuleDetailsDto(
      paragraphNumber = offenceCodeLookupService.getParagraphNumber(offenceCode, isYouthOffender),
      paragraphDescription = offenceCodeLookupService.getParagraphDescription(offenceCode, isYouthOffender),
    )
  }

  fun setOffenceDetails(id: Long, offenceDetails: List<OffenceDetailsRequestItem>): DraftAdjudicationDto {
    throwIfEmpty(offenceDetails)

    val draftAdjudication = find(id)
    // NOTE: new flow sets isYouthOffender first, therefore if we do not have this set we must throw as .Dto requires it
    ValidationChecks.APPLICABLE_RULES.validate(draftAdjudication)

    val newValuesToStore = offenceDetails.map {
      Offence(
        offenceCode = it.offenceCode,
        victimPrisonersNumber = it.victimPrisonersNumber?.ifBlank { null },
        victimStaffUsername = it.victimStaffUsername?.ifBlank { null },
        victimOtherPersonsName = it.victimOtherPersonsName?.ifBlank { null },
      )
    }.toMutableList()

    draftAdjudication.offenceDetails.clear()
    draftAdjudication.offenceDetails.addAll(newValuesToStore)

    return saveToDto(draftAdjudication)
  }

  companion object {
    private fun throwIfEmpty(toTest: List<Any>) {
      if (toTest.isEmpty())
        throw IllegalArgumentException("Please supply at least one set of items")
    }
  }
}
