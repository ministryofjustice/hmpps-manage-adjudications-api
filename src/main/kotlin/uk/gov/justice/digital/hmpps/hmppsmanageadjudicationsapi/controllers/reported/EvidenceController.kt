package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.EvidenceRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.EvidenceService

@PreAuthorize("hasAuthority('SCOPE_write')")
@RestController
@Tag(name = "23. Evidence")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class EvidenceController(
  private val evidenceService: EvidenceService,
) : ReportedAdjudicationBaseController() {

  @PutMapping(value = ["/{adjudicationNumber}/evidence/edit"])
  @Operation(summary = "Updates the evidence for the reported adjudication.", description = "0 or more evidence to be supplied, only updates records owned by current user")
  @ResponseStatus(HttpStatus.OK)
  fun updateEvidence(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody @Valid
    evidenceRequest: EvidenceRequest,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = evidenceService.updateEvidence(
      adjudicationNumber,
      evidenceRequest.evidence,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }
}
