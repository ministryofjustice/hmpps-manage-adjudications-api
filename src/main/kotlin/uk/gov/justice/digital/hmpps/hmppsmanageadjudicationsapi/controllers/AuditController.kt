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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/adjudications-audit")
@Validated
class AuditController {
  @Autowired
  lateinit var auditService: AuditService

  @GetMapping("/draft")
  @Operation(summary = "Returns CSV for draft adjudications weekly report")
  @PreAuthorize("hasRole('ADJUDICATIONS_AUDIT')")
  fun getDraftAdjudicationReport(
    @RequestParam("historic") historic: Boolean?,
    response: HttpServletResponse
  ) {
    response.contentType = "text/csv"
    response.addHeader("Content-Disposition", "attachment; filename=\"draftAdjudications_${getReportDate()}.csv\"")
    auditService.getDraftAdjudicationReport(response.writer, historic)
  }

  @GetMapping("/reported")
  @Operation(summary = "Returns CSV for reported adjudications weekly report")
  @PreAuthorize("hasRole('ADJUDICATIONS_AUDIT')")
  fun getReportedAdjudicationReport(
    @RequestParam("historic") historic: Boolean?,
    response: HttpServletResponse
  ) {
    response.contentType = "text/csv"
    response.addHeader("Content-Disposition", "attachment; filename=\"reportedAdjudications_${getReportDate()}.csv\"")
    auditService.getReportedAdjudicationReport(response.writer, historic)
  }

  private fun getReportDate(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
}
