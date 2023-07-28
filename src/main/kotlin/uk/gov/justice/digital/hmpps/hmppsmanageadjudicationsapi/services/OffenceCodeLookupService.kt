package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender

@Service
class OffenceCodeLookupService {
  private val youthOffenceCodes = OffenceCodes.getYouthOffenceCodes()
  private val adultOffenceCodes = OffenceCodes.getAdultOffenceCodes()

  fun getCommittedOnOwnNomisOffenceCodes(offenceCode: Int, isYouthOffender: Boolean): String = offenceDetails(offenceCode, isYouthOffender).getNomisCode()

  fun getNotCommittedOnOwnNomisOffenceCode(offenceCode: Int, isYouthOffender: Boolean): String = offenceDetails(offenceCode, isYouthOffender).getNomisCodeWithOthers()

  fun getParagraphNumber(offenceCode: Int, isYouthOffender: Boolean): String =
    offenceDetails(offenceCode, isYouthOffender).paragraph

  fun getParagraphDescription(offenceCode: Int, isYouthOffender: Boolean, gender: Gender = Gender.MALE): String =
    offenceDetails(offenceCode, isYouthOffender).paragraphDescription.getParagraphDescription(gender)

  private fun offenceDetails(offenceCode: Int, isYouthOffender: Boolean): OffenceCodes =
    when (isYouthOffender) {
      true -> youthOffenceCodes.firstOrNull { it.uniqueOffenceCodes.contains(offenceCode) } ?: OffenceCodes.DEFAULT
      false -> adultOffenceCodes.firstOrNull { it.uniqueOffenceCodes.contains(offenceCode) } ?: OffenceCodes.DEFAULT
    }
}
