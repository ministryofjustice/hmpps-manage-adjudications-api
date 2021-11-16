package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

data class ReportedAdjudicationRequest(
  val agencyLocationId: String,
  val adjudicationIdsMask: Set<Long>,
)
