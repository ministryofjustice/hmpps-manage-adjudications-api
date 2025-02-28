package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.stereotype.Component

@Component
class ActivePrisonsInfo(
  private val activePrisonConfig: ActivePrisonConfig,
) : InfoContributor {
  override fun contribute(builder: Info.Builder?) {
    builder?.withDetail(
      "activeAgencies",
      if (activePrisonConfig.activePrisons.trim()
          .isEmpty()
      ) {
        emptyList()
      } else {
        activePrisonConfig.activePrisons.split(",")
      },
    )
  }
}
