package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.EvidenceRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationDomainEventType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.EvidenceService

@PreAuthorize("hasRole('VIEW_ADJUDICATIONS') and hasAuthority('SCOPE_write')")
@RestController
@Tag(name = "23. Evidence")
class EvidenceController(
  private val evidenceService: EvidenceService,
) : ReportedAdjudicationBaseController() {

  @PutMapping(value = ["/{chargeNumber}/evidence/edit"])
  @Operation(
    summary = "Updates the evidence for the reported adjudication.",
    description = "0 or more evidence to be supplied, only updates records owned by current user",
  )
  @ResponseStatus(HttpStatus.OK)
  fun updateEvidence(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody @Valid
    evidenceRequest: EvidenceRequest,
  ): ReportedAdjudicationResponse = eventPublishWrapper(
    events = listOf(
      EventRuleAndSupplier(
        eventRule = {
          listOf(
            ReportedAdjudicationStatus.RETURNED,
            ReportedAdjudicationStatus.AWAITING_REVIEW,
          ).none { s -> it.status == s }
        },
        eventSupplier = { AdjudicationDomainEventType.EVIDENCE_UPDATED },
      ),
    ),
    controllerAction = {
      evidenceService.updateEvidence(
        chargeNumber,
        evidenceRequest.evidence,
      )
    },
  )
}
