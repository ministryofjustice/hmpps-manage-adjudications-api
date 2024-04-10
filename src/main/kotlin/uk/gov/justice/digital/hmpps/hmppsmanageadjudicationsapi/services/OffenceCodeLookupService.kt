package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class OffenceCodeLookupService(
  @Value("\${service.offences.version}")
  val offencesVersion: Int,
) {
  val youthOffenceCodes: List<OffenceCodes>
    get() = getYouthOffenceCodes(offencesVersion)
  val adultOffenceCodes: List<OffenceCodes>
    get() = getAdultOffenceCodes(offencesVersion)

  fun getOffenceCode(offenceCode: Int, isYouthOffender: Boolean): OffenceCodes =
    when (isYouthOffender) {
      true -> youthOffenceCodes.firstOrNull { it.uniqueOffenceCodes.contains(offenceCode) }
      false -> adultOffenceCodes.firstOrNull { it.uniqueOffenceCodes.contains(offenceCode) }
    } ?: OffenceCodes.MIGRATED_OFFENCE

  private fun getAdultOffenceCodes(version: Int) = OffenceCodes.values().filter { it.nomisCode.startsWith("51:") && it.applicableVersions.contains(version) }
  private fun getYouthOffenceCodes(version: Int) = OffenceCodes.values().filter { it.nomisCode.startsWith("55:") && it.applicableVersions.contains(version) }
}
