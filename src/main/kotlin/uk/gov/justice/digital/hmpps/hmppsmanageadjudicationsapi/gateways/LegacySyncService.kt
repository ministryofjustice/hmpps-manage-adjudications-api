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

  fun createHearing(adjudicationNumber: Long, oicHearingRequest: OicHearingRequest): Long? {
    return if (!featureFlagsConfig.hearings) {
      legacyNomisGateway.createHearing(adjudicationNumber, oicHearingRequest)
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

  @Deprecated("to remove on completion of NN-5319")
  fun createSanction(adjudicationNumber: Long, sanction: OffenderOicSanctionRequest): Long? =
    legacyNomisGateway.createSanction(adjudicationNumber, sanction)

  @Deprecated("to remove on completion of NN-5319")
  fun deleteSanction(adjudicationNumber: Long, sanctionSeq: Long) =
    legacyNomisGateway.deleteSanction(adjudicationNumber, sanctionSeq)

  fun createSanctions(adjudicationNumber: Long, sanctions: List<OffenderOicSanctionRequest>) {
    if (!featureFlagsConfig.outcomes && !featureFlagsConfig.punishments) {
      legacyNomisGateway.createSanctions(adjudicationNumber, sanctions)
    }
  }

  fun updateSanctions(adjudicationNumber: Long, sanctions: List<OffenderOicSanctionRequest>) {
    if (!featureFlagsConfig.punishments) {
      legacyNomisGateway.updateSanctions(adjudicationNumber, sanctions)
    }
  }

  fun quashSanctions(adjudicationNumber: Long) {
    if (!featureFlagsConfig.punishments) {
      legacyNomisGateway.quashSanctions(adjudicationNumber)
    }
  }

  fun deleteSanctions(adjudicationNumber: Long) {
    if (!featureFlagsConfig.punishments) {
      legacyNomisGateway.deleteSanctions(adjudicationNumber)
    }
  }
}
