package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import io.swagger.v3.oas.annotations.Operation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AuditService
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/adjudications-audit")
@Validated
class AuditController {
  @Autowired
  lateinit var auditService: AuditService

  @GetMapping("/draft")
  @Operation(summary = "Returns CSV for draft adjudications weekly report")
  @PreAuthorize("hasAuthority('AUDIT')")
  fun getDraftAdjudicationReport(
    @RequestParam("historic") historic: Boolean?,
    response: HttpServletResponse
  ) {
    response.contentType = "text/csv"
    response.addHeader("Content-Disposition", "attachment; filename=\"draftAdjudications.csv\"")
    auditService.getDraftAdjudicationReport(response.writer, historic)
  }

  @GetMapping("/reported")
  @Operation(summary = "Returns CSV for reported adjudications weekly report")
  @PreAuthorize("hasAuthority('AUDIT')")
  fun getReportedAdjudicationReport(
    @RequestParam("historic") historic: Boolean?,
    response: HttpServletResponse
  ) {
    response.contentType = "text/csv"
    response.addHeader("Content-Disposition", "attachment; filename=\"reportedAdjudications.csv\"")
    auditService.getReportedAdjudicationReport(response.writer, historic)
  }
}
