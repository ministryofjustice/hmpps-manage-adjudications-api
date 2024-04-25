package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service

@Service
class OffenceCodeLookupService {
  private val youthOffenceCodes: List<OffenceCodes> = OffenceCodes.entries.filter { it.nomisCode.startsWith("55:") }
  private val adultOffenceCodes: List<OffenceCodes> = OffenceCodes.entries.filter { it.nomisCode.startsWith("51:") }

  fun getOffenceCode(offenceCode: Int, isYouthOffender: Boolean): OffenceCodes =
    when (isYouthOffender) {
      true -> youthOffenceCodes.firstOrNull { it.uniqueOffenceCodes.contains(offenceCode) }
      false -> adultOffenceCodes.firstOrNull { it.uniqueOffenceCodes.contains(offenceCode) }
    } ?: OffenceCodes.MIGRATED_OFFENCE

  fun getAdultOffenceCodesByVersion(version: Int) = adultOffenceCodes.filter { it.applicableVersions.contains(version) }
  fun getYouthOffenceCodesByVersion(version: Int) = youthOffenceCodes.filter { it.applicableVersions.contains(version) }
}
