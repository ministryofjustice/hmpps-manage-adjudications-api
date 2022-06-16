package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto

class IncidentRoleRuleLookup {
  companion object {
    private val adultIncidentRuleDetailByRole = mapOf(
      "25a" to Pair("25(a)", "Attempts to commit any of the foregoing offences:"),
      "25b" to Pair("25(b)", "Incites another prisoner to commit any of the foregoing offences:"),
      "25c" to Pair("25(c)", "Assists another prisoner to commit, or to attempt to commit, any of the foregoing offences:"),
    )
    private val youthIncidentRuleDetailByRole = mapOf(
      "25a" to Pair("29(a)", "Attempts to commit any of the foregoing offences:"),
      "25b" to Pair("29(b)", "Incites another prisoner to commit any of the foregoing offences:"),
      "25c" to Pair("29(c)", "Assists another prisoner to commit, or to attempt to commit, any of the foregoing offences:"),
    )

    fun getOffenceRuleDetails(incidentRoleCode: String?, isYouthOffender: Boolean): OffenceRuleDetailsDto? {
      if (incidentRoleCode == null) {
        return null
      }
      if (isYouthOffender) {
        return OffenceRuleDetailsDto(
          paragraphNumber = youthIncidentRuleDetailByRole[incidentRoleCode]?.first ?: "",
          paragraphDescription = youthIncidentRuleDetailByRole[incidentRoleCode]?.second ?: "",
        )
      }
      return OffenceRuleDetailsDto(
        paragraphNumber = adultIncidentRuleDetailByRole[incidentRoleCode]?.first ?: "",
        paragraphDescription = adultIncidentRuleDetailByRole[incidentRoleCode]?.second ?: "",
      )
    }
  }
}
