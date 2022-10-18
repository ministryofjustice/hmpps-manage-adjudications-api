package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.EvidenceRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.EvidenceService
import javax.validation.Valid

@PreAuthorize("hasAuthority('SCOPE_write')")
@RestController
class EvidenceController : ReportedAdjudicationBaseController() {

  @Autowired
  lateinit var evidenceService: EvidenceService

  @PutMapping(value = ["/{adjudicationNumber}/evidence/edit"])
  @Operation(summary = "Updates the evidence for the reported adjudication.", description = "0 or more evidence to be supplied, only updates records owned by current user")
  @ResponseStatus(HttpStatus.OK)
  fun updateEvidence(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody @Valid evidenceRequest: EvidenceRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = evidenceService.updateEvidence(
      adjudicationNumber,
      evidenceRequest.evidence
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }
}
