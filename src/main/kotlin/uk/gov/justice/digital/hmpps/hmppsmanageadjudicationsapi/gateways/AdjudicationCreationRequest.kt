package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

data class AdjudicationCreationRequest(
  val adjudicationNumber: String,
  val bookingId: Long,
)
