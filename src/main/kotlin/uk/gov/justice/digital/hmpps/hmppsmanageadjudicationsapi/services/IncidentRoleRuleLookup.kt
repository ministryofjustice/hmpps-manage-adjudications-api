package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto

class IncidentRoleRuleLookup {
  companion object {
    private const val ATTEMPTS_TO_COMMIT_OFFENCE = "25a"
    private const val INCITES_OTHER_TO_COMMIT_OFFENCE = "25b"
    private const val ASSISTS_OTHER_TO_COMMIT_OFFENCE = "25c"

    private val adultIncidentRuleDetailByRole = mapOf(
      ATTEMPTS_TO_COMMIT_OFFENCE to Pair("25(a)", "Attempts to commit any of the foregoing offences:"),
      INCITES_OTHER_TO_COMMIT_OFFENCE to Pair(
        "25(b)",
        "Incites another prisoner to commit any of the foregoing offences:",
      ),
      ASSISTS_OTHER_TO_COMMIT_OFFENCE to Pair(
        "25(c)",
        "Assists another prisoner to commit, or to attempt to commit, any of the foregoing offences:",
      ),
    )
    private val youthIncidentRuleDetailByRole = mapOf(
      ATTEMPTS_TO_COMMIT_OFFENCE to Pair("29(a)", "Attempts to commit any of the foregoing offences:"),
      INCITES_OTHER_TO_COMMIT_OFFENCE to Pair(
        "29(b)",
        "Incites another inmate to commit any of the foregoing offences:",
      ),
      ASSISTS_OTHER_TO_COMMIT_OFFENCE to Pair(
        "29(c)",
        "Assists another inmate to commit, or to attempt to commit, any of the foregoing offences: ",
      ),
    )

    fun associatedPrisonerInformationRequired(incidentRoleCode: String?): Boolean = INCITES_OTHER_TO_COMMIT_OFFENCE == incidentRoleCode || ASSISTS_OTHER_TO_COMMIT_OFFENCE == incidentRoleCode

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
