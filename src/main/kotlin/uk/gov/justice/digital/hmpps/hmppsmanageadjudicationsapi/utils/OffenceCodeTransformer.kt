package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodes

object OffenceCodeTransformer {
  /**
   * Returns the paragraph description (e.g. "Disobeys any lawful order")
   * for the given unique offence code (e.g. 22001),
   * or null if none matches.
   */
  fun displayName(uniqueOffenceCode: Int): String? {
    val match = OffenceCodes.values().find { offence ->
      offence.uniqueOffenceCodes.contains(uniqueOffenceCode)
    }
    return match?.paragraphDescription?.description
  }
}