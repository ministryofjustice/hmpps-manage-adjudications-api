package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class FeatureFlagsService(
  @Value("\${feature.emit-events-for-adjudications:false}")
  val emitEventsForAdjudications: Boolean,
) {

  fun isEmitEventsForAdjudications(): Boolean {
    return emitEventsForAdjudications
  }
}
