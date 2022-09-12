package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service

@Service
class OffenceCodeLookupService {
  val offenceCodeParagraphs = OffenceCodeParagraphs()
  var adultOffenceDetailsByCode: Map<Int, Triple<String, String, String>> = AdultOffenceCodes.buildLookup()
  var youthOffenceDetailsByCode: Map<Int, Triple<String, String, String>> = YouthOffenceCodes.buildLookup()

  fun getCommittedOnOwnNomisOffenceCodes(offenceCode: Int, isYouthOffender: Boolean): String = offenceDetails(offenceCode, isYouthOffender)?.nomisCode ?: OffenceCodeDefaults.DEFAULT_NOMIS_ID

  fun getNotCommittedOnOwnNomisOffenceCode(offenceCode: Int, isYouthOffender: Boolean): String = offenceDetails(offenceCode, isYouthOffender)?.notCommittedOnOwnNomisCode ?: OffenceCodeDefaults.DEFAULT_NOMIS_ID

  fun getParagraphCode(offenceCode: Int, isYouthOffender: Boolean): String = offenceDetails(offenceCode, isYouthOffender)?.paragraphCode ?: OffenceCodeDefaults.DEFAULT_PARAGRAPH_DATA

  fun getParagraphNumber(offenceCode: Int, isYouthOffender: Boolean): String =
    offenceDetails(offenceCode, isYouthOffender)?.paragraphNumber ?: OffenceCodeDefaults.DEFAULT_PARAGRAPH_DATA

  fun getParagraphDescription(offenceCode: Int, isYouthOffender: Boolean): String {
    val offenceCodeDetails = offenceDetails(offenceCode, isYouthOffender) ?: return Descriptions.DEFAULT.description
    return offenceCodeParagraphs.getParagraphDescription(offenceCodeDetails.nomisCode)
  }

  private fun offenceDetails(offenceCode: Int, isYouthOffender: Boolean): OffenceCodeDetails? =
    when (isYouthOffender) {
      true -> {
        val res = youthOffenceDetailsByCode[offenceCode]
        if (res == null) null else OffenceCodeDetails(offenceCode = offenceCode, paragraphNumber = res.first, nomisCode = res.second, notCommittedOnOwnNomisCode = res.third)
      }
      false -> {
        val res = adultOffenceDetailsByCode[offenceCode]
        if (res == null) null else OffenceCodeDetails(offenceCode = offenceCode, paragraphNumber = res.first, nomisCode = res.second, notCommittedOnOwnNomisCode = res.third)
      }
    }
}

data class OffenceCodeDetails(
  val offenceCode: Int,
  val paragraphNumber: String,
  val nomisCode: String,
  val notCommittedOnOwnNomisCode: String,
  val paragraphCode: String = paragraphNumber.replace("(", "").replace(")", ""),
)

object OffenceCodeDefaults {
  const val DEFAULT_NOMIS_ID = ""
  const val DEFAULT_PARAGRAPH_DATA = ""
}
