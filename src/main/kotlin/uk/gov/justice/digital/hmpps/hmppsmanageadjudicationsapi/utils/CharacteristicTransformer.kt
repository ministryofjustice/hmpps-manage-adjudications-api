package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Characteristic

object CharacteristicTransformer {
  fun displayName(status: Characteristic): String = when (status) {
    Characteristic.AGE -> "Age"
    Characteristic.DISABILITY -> "Disability"
    Characteristic.GENDER_REASSIGN -> "Gender reassignment"
    Characteristic.MARRIAGE_AND_CP -> "Marriage and civil partnership"
    Characteristic.PREGNANCY_AND_MAT -> "Pregnancy and maternity"
    Characteristic.RACE -> "Race"
    Characteristic.RELIGION -> "Religion or belief"
    Characteristic.SEX -> "Sex"
    Characteristic.SEX_ORIENTATION -> "Sexual orientation"
  }

  /**
   * If status is a string (maybe from an HTTP request), try to parse it.
   * If invalid, return null or any fallback behavior you prefer.
   */
  fun displayName(status: String): String? = try {
    val enumValue = Characteristic.valueOf(status.uppercase())
    displayName(enumValue)
  } catch (ex: IllegalArgumentException) {
    // String didn't match any enum constant
    null
  }
}
