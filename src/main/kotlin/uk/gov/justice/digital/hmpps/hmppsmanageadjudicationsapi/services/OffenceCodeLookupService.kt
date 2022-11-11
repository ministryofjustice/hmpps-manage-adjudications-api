package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender

@Service
class OffenceCodeLookupService {
  val offenceCodeParagraphs = OffenceCodeParagraphs()
  var adultOffenceDetailsByCode: Map<Int, Triple<String, String, String>> = AdultOffenceCodes.buildLookup()
  var youthOffenceDetailsByCode: Map<Int, Triple<String, String, String>> = YouthOffenceCodes.buildLookup()

  fun getCommittedOnOwnNomisOffenceCodes(offenceCode: Int, isYouthOffender: Boolean): String = offenceDetails(offenceCode, isYouthOffender)?.nomisCode ?: OffenceCodeDefaults.DEFAULT_NOMIS_ID

  fun getNotCommittedOnOwnNomisOffenceCode(offenceCode: Int, isYouthOffender: Boolean): String = offenceDetails(offenceCode, isYouthOffender)?.notCommittedOnOwnNomisCode ?: OffenceCodeDefaults.DEFAULT_NOMIS_ID

  fun getParagraphNumber(offenceCode: Int, isYouthOffender: Boolean): String =
    offenceDetails(offenceCode, isYouthOffender)?.paragraphNumber ?: OffenceCodeDefaults.DEFAULT_PARAGRAPH_DATA

  fun getParagraphDescription(offenceCode: Int, isYouthOffender: Boolean, gender: Gender = Gender.MALE): String {
    val offenceCodeDetails = offenceDetails(offenceCode, isYouthOffender) ?: return Descriptions.DEFAULT.description
    return offenceCodeParagraphs.getParagraphDescription(offenceCodeDetails.nomisCode, gender)
  }

  private fun offenceDetails(offenceCode: Int, isYouthOffender: Boolean): OffenceCodeDetails? =
    when (isYouthOffender) {
      true -> youthOffenceDetailsByCode[offenceCode]?.let { OffenceCodeDetails(offenceCode = offenceCode, paragraphNumber = it.first, nomisCode = it.second, notCommittedOnOwnNomisCode = it.third) }
      false -> adultOffenceDetailsByCode[offenceCode]?.let { OffenceCodeDetails(offenceCode = offenceCode, paragraphNumber = it.first, nomisCode = it.second, notCommittedOnOwnNomisCode = it.third) }
    }
}

data class OffenceCodeDetails(
  val offenceCode: Int,
  val paragraphNumber: String,
  val nomisCode: String,
  val notCommittedOnOwnNomisCode: String
)

object OffenceCodeDefaults {
  const val DEFAULT_NOMIS_ID = ""
  const val DEFAULT_PARAGRAPH_DATA = ""
}
