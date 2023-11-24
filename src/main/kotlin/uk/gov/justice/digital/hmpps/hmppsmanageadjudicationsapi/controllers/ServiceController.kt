package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.ActivePrisonConfig

@Schema(description = "active prison")
data class ActivePrison(
  @Schema(description = "agency id")
  val agency: String,
  @Schema(description = "is active or not")
  val active: Boolean,
)

@PreAuthorize("hasRole('VIEW_ADJUDICATIONS')")
@RestController
@RequestMapping("/service")
class ServiceController(
  private val activePrisonConfig: ActivePrisonConfig,
) {

  @GetMapping("/active")
  fun isAvailable(
    @RequestParam("agency", required = true) agency: String,
  ): ActivePrison {
    return ActivePrison(
      agency = agency,
      active = activePrisonConfig.isAvailable(
        agency = agency,
      ),
    )
  }
}
