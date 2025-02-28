package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea

object HearingOutcomeTransformer {
  fun displayOutcomeCodeName(status: HearingOutcomeCode): String = when (status) {
    HearingOutcomeCode.NOMIS -> "Nomis"
    HearingOutcomeCode.ADJOURN -> "Adjourn the hearing for another reason"
    HearingOutcomeCode.COMPLETE -> "Hearing complete - add adjudication finding"
    HearingOutcomeCode.REFER_GOV -> "Refer this case to the governor"
    HearingOutcomeCode.REFER_POLICE -> "Refer this case to the police"
    HearingOutcomeCode.REFER_INAD -> "Refer this case to the independent adjudicator"
  }

  fun displayOutcomePleaName(status: HearingOutcomePlea): String = when (status) {
    HearingOutcomePlea.UNFIT -> "No plea - unfit to plea or attend"
    HearingOutcomePlea.GUILTY -> "Guilty"
    HearingOutcomePlea.ABSTAIN -> "No plea - abstained or refused to plea or attend"
    HearingOutcomePlea.NOT_ASKED -> "No plea - charge dismissed before the prisoner was asked"
    HearingOutcomePlea.NOT_GUILTY -> "Not guilty"
  }

  /**
   * If status is a string (maybe from an HTTP request), try to parse it.
   * If invalid, return null or any fallback behavior you prefer.
   */
  fun displayOutcomeCodeName(status: String): String? = try {
    val enumValue = HearingOutcomeCode.valueOf(status.uppercase())
    displayOutcomeCodeName(enumValue)
  } catch (ex: IllegalArgumentException) {
    // String didn't match any enum constant
    null
  }

  /**
   * If status is a string (maybe from an HTTP request), try to parse it.
   * If invalid, return null or any fallback behavior you prefer.
   */
  fun displayOutcomePleaName(status: String): String? = try {
    val enumValue = HearingOutcomePlea.valueOf(status.uppercase())
    displayOutcomePleaName(enumValue)
  } catch (ex: IllegalArgumentException) {
    // String didn't match any enum constant
    null
  }
}
