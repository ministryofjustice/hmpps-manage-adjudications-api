package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

data class NomisAdjudicationCreationRequest(
  val adjudicationNumber: Long,
  val bookingId: Long,
)
