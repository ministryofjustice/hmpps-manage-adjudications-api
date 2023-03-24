package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
@RestController
class PunishmentsController() : ReportedAdjudicationBaseController()
