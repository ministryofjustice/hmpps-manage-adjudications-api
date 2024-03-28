package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

class OffenceCodeLookup {
  private val youthOffenceCodes = OffenceCodes.getYouthOffenceCodes()
  private val adultOffenceCodes = OffenceCodes.getAdultOffenceCodes()

  fun getOffenceCode(offenceCode: Int, isYouthOffender: Boolean): OffenceCodes =
    when (isYouthOffender) {
      true -> youthOffenceCodes.firstOrNull { it.uniqueOffenceCodes.contains(offenceCode) }
      false -> adultOffenceCodes.firstOrNull { it.uniqueOffenceCodes.contains(offenceCode) }
    } ?: OffenceCodes.MIGRATED_OFFENCE
}
