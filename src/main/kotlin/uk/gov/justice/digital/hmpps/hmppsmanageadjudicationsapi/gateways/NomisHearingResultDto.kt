package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

enum class Finding {
  NOT_GUILTY, NOT_PROCEED, NOT_PROVEN, PROSECUTED, PROVED, QUASHED, REFUSED, REF_POLICE, S, UNFIT
}

enum class Plea {
  GUILTY, NOT_GUILTY, REFUSED, UNFIT
}

data class NomisHearingResultRequest(
  val plea: Plea,
  val finding: Finding,
  val adjudicator: String,
)
