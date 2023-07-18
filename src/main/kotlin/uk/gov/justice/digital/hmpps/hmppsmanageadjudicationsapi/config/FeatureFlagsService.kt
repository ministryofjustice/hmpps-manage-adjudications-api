package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class FeatureFlagsService(
  @Value("\${feature.async-mode:false}")
  val asyncMode: Boolean,
  @Value("\${feature.nomis-source-of-truth:true}")
  val nomisSourceOfTruth: Boolean,
  @Value("\${feature.create-adjudication-async-only}")
  val createAdjudicationAsyncOnly: Boolean,
) {

  fun isAsyncMode(): Boolean {
    return asyncMode
  }

  fun isLegacySyncMode(): Boolean {
    return !asyncMode
  }

  fun isNomisSourceOfTruth(): Boolean {
    return nomisSourceOfTruth
  }
}
