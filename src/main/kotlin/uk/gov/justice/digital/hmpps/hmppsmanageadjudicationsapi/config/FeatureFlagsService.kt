package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service

@ConfigurationProperties(prefix = "feature.async")
class AsyncConfig(
 val chargeNumbers: Boolean,
 val adjudications: Boolean,
 val hearings: Boolean,
 val outcomes: Boolean,
 val punishments: Boolean,
 val witnesses: Boolean,
 val damages: Boolean,
 val evidence: Boolean,
)

@Service
class FeatureFlagsService(
  @Value("\${feature.nomis-source-of-truth:true}")
  val nomisSourceOfTruth: Boolean,
) {
  fun isNomisSourceOfTruth(): Boolean {
    return nomisSourceOfTruth
  }
}
