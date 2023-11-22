package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class FeatureFlagsConfig(
  @Value("\${feature.nomis-source-of-truth.summary:true}")
  val nomisSourceOfTruthSummary: Boolean,
  @Value("\${feature.nomis-source-of-truth.adjudications:true}")
  val nomisSourceOfTruthAdjudications: Boolean,
  @Value("\${feature.nomis-source-of-truth.adjudication:true}")
  val nomisSourceOfTruthAdjudication: Boolean,
  @Value("\${feature.nomis-source-of-truth.hearing:true}")
  val nomisSourceOfTruthHearing: Boolean,
  @Value("\${feature.async.chargeNumbers}")
  val chargeNumbers: Boolean,
  @Value("\${feature.async.adjudications}")
  val adjudications: Boolean,
  @Value("\${feature.async.hearings}")
  val hearings: Boolean,
  @Value("\${feature.async.outcomes}")
  val outcomes: Boolean,
  @Value("\${feature.async.punishments}")
  val punishments: Boolean,
  @Value("\${feature.skip.existing}")
  val skipExistingRecords: Boolean,
)
