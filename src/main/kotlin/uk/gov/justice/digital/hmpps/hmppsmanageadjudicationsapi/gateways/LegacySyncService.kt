package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsConfig

@Service
class LegacySyncService(
  private val legacyNomisGateway: LegacyNomisGateway,
  private val featureFlagsConfig: FeatureFlagsConfig,
) {

  fun requestAdjudicationCreationData(): Long? {
    return if (!featureFlagsConfig.chargeNumbers) {
      legacyNomisGateway.requestAdjudicationCreationData()
    } else {
      TODO("this is not implemented and needs design, no equivalent sync service endpoint, it will be handled outside of this service")
    }
  }

  fun publishAdjudication(adjudicationDetailsToPublish: AdjudicationDetailsToPublish) {
    if (!featureFlagsConfig.adjudications) {
      legacyNomisGateway.publishAdjudication(adjudicationDetailsToPublish)
    }
  }

  fun createHearing(adjudicationNumber: String, oicHearingRequest: OicHearingRequest): Long? {
    return if (!featureFlagsConfig.hearings) {
      legacyNomisGateway.createHearing(adjudicationNumber.toLong(), oicHearingRequest)
    } else {
      null
    }
  }

  fun amendHearing(adjudicationNumber: Long, oicHearingId: Long?, oicHearingRequest: OicHearingRequest) {
    if (!featureFlagsConfig.hearings) {
      legacyNomisGateway.amendHearing(
        adjudicationNumber = adjudicationNumber,
        oicHearingId = oicHearingId!!,
        oicHearingRequest = oicHearingRequest,
      )
    }
  }

  fun deleteHearing(adjudicationNumber: Long, oicHearingId: Long?) {
    if (!featureFlagsConfig.hearings) {
      legacyNomisGateway.deleteHearing(
        adjudicationNumber = adjudicationNumber,
        oicHearingId = oicHearingId!!,
      )
    }
  }

  fun createHearingResult(
    adjudicationNumber: Long,
    oicHearingId: Long?,
    oicHearingResultRequest: OicHearingResultRequest,
  ) {
    if (!featureFlagsConfig.outcomes) {
      legacyNomisGateway.createHearingResult(
        adjudicationNumber = adjudicationNumber,
        oicHearingId = oicHearingId!!,
        oicHearingResultRequest = oicHearingResultRequest,
      )
    }
  }

  fun amendHearingResult(
    adjudicationNumber: Long,
    oicHearingId: Long?,
    oicHearingResultRequest: OicHearingResultRequest,
  ) {
    if (!featureFlagsConfig.outcomes) {
      legacyNomisGateway.amendHearingResult(
        adjudicationNumber = adjudicationNumber,
        oicHearingId = oicHearingId!!,
        oicHearingResultRequest = oicHearingResultRequest,
      )
    }
  }

  fun deleteHearingResult(adjudicationNumber: Long, oicHearingId: Long?) {
    if (!featureFlagsConfig.outcomes) {
      legacyNomisGateway.deleteHearingResult(adjudicationNumber = adjudicationNumber, oicHearingId = oicHearingId!!)
    }
  }

  fun createSanctions(adjudicationNumber: String, sanctions: List<OffenderOicSanctionRequest>) {
    if (!featureFlagsConfig.punishments) {
      legacyNomisGateway.createSanctions(adjudicationNumber.toLong(), sanctions)
    }
  }

  fun updateSanctions(adjudicationNumber: String, sanctions: List<OffenderOicSanctionRequest>) {
    if (!featureFlagsConfig.punishments) {
      legacyNomisGateway.updateSanctions(adjudicationNumber.toLong(), sanctions)
    }
  }

  fun quashSanctions(adjudicationNumber: String) {
    if (!featureFlagsConfig.punishments) {
      legacyNomisGateway.quashSanctions(adjudicationNumber.toLong())
    }
  }

  fun deleteSanctions(adjudicationNumber: String) {
    if (!featureFlagsConfig.punishments) {
      legacyNomisGateway.deleteSanctions(adjudicationNumber.toLong())
    }
  }
}
