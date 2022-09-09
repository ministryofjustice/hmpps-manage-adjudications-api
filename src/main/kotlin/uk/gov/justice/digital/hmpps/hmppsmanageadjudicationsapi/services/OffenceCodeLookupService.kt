package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service

@Service
class OffenceCodeLookupService {
  val offenceCodeParagraphs = OffenceCodeParagraphs()
  var adultOffenceDetailsByCode: Map<Int, OffenceCodeDetails> = AdultOffenceCodes.buildLookup()
  var youthOffenceDetailsByCode: Map<Int, OffenceCodeDetails> = YouthOffenceCodes.buildLookup()

  fun getCommittedOnOwnNomisOffenceCodes(offenceCode: Int, isYouthOffender: Boolean): String {
    return offenceDetails(offenceCode, isYouthOffender)?.nomisCode ?: OffenceCodeDefaults.DEFAULT_NOMIS_ID
  }

  fun getNotCommittedOnOwnNomisOffenceCode(offenceCode: Int, isYouthOffender: Boolean): String {
    return offenceDetails(offenceCode, isYouthOffender)?.notCommittedOnOwnNomisCode ?: OffenceCodeDefaults.DEFAULT_NOMIS_ID
  }

  fun getParagraphCode(offenceCode: Int, isYouthOffender: Boolean): String {
    return offenceDetails(offenceCode, isYouthOffender)?.paragraph?.paragraphCode ?: OffenceCodeDefaults.DEFAULT_PARAGRAPH_DATA
  }

  fun getParagraphNumber(offenceCode: Int, isYouthOffender: Boolean): String {
    return offenceDetails(offenceCode, isYouthOffender)?.paragraph?.paragraphNumber ?: OffenceCodeDefaults.DEFAULT_PARAGRAPH_DATA
  }

  fun getParagraphDescription(offenceCode: Int, isYouthOffender: Boolean): String {
    val offenceCodeDetails = offenceDetails(offenceCode, isYouthOffender) ?: return ""
    return offenceCodeParagraphs.getParagraphDescription(offenceCodeDetails.nomisCode)
  }

  private fun offenceDetails(offenceCode: Int, isYouthOffender: Boolean): OffenceCodeDetails? =
    when (isYouthOffender) {
      true -> { youthOffenceDetailsByCode[offenceCode] }
      false -> { adultOffenceDetailsByCode[offenceCode] }
    }
}

class OffenceCodeLookupBuilder {
  private var allCodeBuilders: MutableList<OffenceCodesBuilder> = mutableListOf()
  private var nomisPrefix: String? = null

  fun nomisPrefix(prefix: String): OffenceCodeLookupBuilder {
    nomisPrefix = prefix
    return this
  }

  fun code(code: Int): OffenceCodesBuilder {
    val codeLookupBuilder = OffenceCodesBuilder(this, code, nomisPrefix!!)
    allCodeBuilders.add(codeLookupBuilder)
    return codeLookupBuilder
  }

  fun build(): Map<Int, OffenceCodeDetails> {
    return allCodeBuilders.map { it.build() }.groupBy { it.offenceCode }
      .mapValues {
        if (it.value.size == 1) it.value[0] else throw RuntimeException("No single value provided for key ${it.key}")
      }
  }
}

class OffenceCodesBuilder(
  private val lookupBuilder: OffenceCodeLookupBuilder,
  private val offenceCode: Int,
  private val nomisPrefix: String,
) {
  private var nomisCode: String? = null
  private var notCommittedOnOwnNomisCode: String? = null
  private var paragraph: OffenceCodeParagraph? = null

  fun nomisCode(code: String): OffenceCodesBuilder {
    nomisCode = nomisPrefix + code
    return this
  }

  fun notCommittedOnOwnNomisCodes(code: String): OffenceCodesBuilder {
    notCommittedOnOwnNomisCode = nomisPrefix + code
    return this
  }

  fun paragraph(paragraphIn: OffenceCodeParagraph): OffenceCodeLookupBuilder {
    paragraph = paragraphIn
    return lookupBuilder
  }

  fun build(): OffenceCodeDetails {
    return OffenceCodeDetails(offenceCode, nomisCode!!, notCommittedOnOwnNomisCode!!, paragraph!!)
  }
}

data class OffenceCodeDetails(
  val offenceCode: Int,
  val nomisCode: String,
  val notCommittedOnOwnNomisCode: String,
  val paragraph: OffenceCodeParagraph,
)

data class OffenceCodeParagraph(
  val paragraphNumber: String,
  val paragraphCode: String = paragraphNumber,
)

object OffenceCodeDefaults {
  val DEFAULT_NOMIS_IDS = listOf<String>()
  val DEFAULT_NOMIS_ID = ""
  const val DEFAULT_PARAGRAPH_DATA = ""
}
