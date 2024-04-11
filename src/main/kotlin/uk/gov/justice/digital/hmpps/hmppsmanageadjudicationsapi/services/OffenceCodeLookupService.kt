package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service

@Service
class OffenceCodeLookupService {
  private val youthOffenceCodes: List<OffenceCodes> = getYouthOffenceCodes()
  private val adultOffenceCodes: List<OffenceCodes> = getAdultOffenceCodes()

  fun getOffenceCode(offenceCode: Int, isYouthOffender: Boolean): OffenceCodes =
    when (isYouthOffender) {
      true -> youthOffenceCodes.firstOrNull { it.uniqueOffenceCodes.contains(offenceCode) }
      false -> adultOffenceCodes.firstOrNull { it.uniqueOffenceCodes.contains(offenceCode) }
    } ?: OffenceCodes.MIGRATED_OFFENCE

  fun getAdultOffenceCodesByVersion(version: Int) = adultOffenceCodes.filter { it.applicableVersions.contains(version) }
  fun getYouthOffenceCodesByVersion(version: Int) = youthOffenceCodes.filter { it.applicableVersions.contains(version) }

  private fun getAdultOffenceCodes() = OffenceCodes.values().filter { it.nomisCode.startsWith("51:") }
  private fun getYouthOffenceCodes() = OffenceCodes.values().filter { it.nomisCode.startsWith("55:") }
}
