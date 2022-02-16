package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto

class IncidentRoleRuleLookup {
  companion object {
    private val incidentRuleDetailByRole = mapOf(
      "25a" to Pair("25(a)", "Attempts to commit any of the foregoing offences:"),
      "25b" to Pair("25(b)", "Incites another prisoner to commit any of the foregoing offences:"),
      "25c" to Pair("25(c)", "Assists another prisoner to commit, or to attempt to commit, any of the foregoing offences:"),
    )

    fun getOffenceRuleDetails(incidentRoleCode: String?): OffenceRuleDetailsDto? {
      if (incidentRoleCode == null) {
        return null
      }
      return OffenceRuleDetailsDto(
        paragraphNumber = incidentRuleDetailByRole[incidentRoleCode]?.first ?: "",
        paragraphDescription = incidentRuleDetailByRole[incidentRoleCode]?.second ?: "",
      )
    }
  }
}
