package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.pagination.PageRequest

data class ReportedAdjudicationRequest(
  val incidentLocationId: Long,
  val pageRequest: PageRequest,
  val adjudicationNumbers: Set<Long>,
)
