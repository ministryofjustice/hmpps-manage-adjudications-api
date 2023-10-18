package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

enum class Finding {
  APPEAL, D, DISMISSED, GUILTY, NOT_GUILTY, NOT_PROCEED, NOT_PROVEN, PROSECUTED, PROVED, QUASHED, REFUSED, REF_POLICE, S, UNFIT, ADJOURNED,
}

enum class Plea {
  GUILTY, NOT_GUILTY, REFUSED, UNFIT, NOT_ASKED,
}

data class OicHearingResultRequest(
  val pleaFindingCode: Plea,
  val findingCode: Finding,
  val adjudicator: String? = null,
)
