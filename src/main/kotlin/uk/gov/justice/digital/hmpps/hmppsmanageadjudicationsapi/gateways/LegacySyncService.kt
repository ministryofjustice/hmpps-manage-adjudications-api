package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsService

@Service
class LegacySyncService(
  private val legacyNomisGateway: LegacyNomisGateway,
  private val featureFlagsService: FeatureFlagsService,
) {

  fun requestAdjudicationCreationData(): Long? {
    return if (featureFlagsService.isLegacySyncMode()) {
      legacyNomisGateway.requestAdjudicationCreationData()
    } else {
      null
    }
  }

  fun publishAdjudication(adjudicationDetailsToPublish: AdjudicationDetailsToPublish) {
    if (featureFlagsService.isLegacySyncMode()) {
      legacyNomisGateway.publishAdjudication(adjudicationDetailsToPublish)
    }
  }

  fun createHearing(adjudicationNumber: Long, oicHearingRequest: OicHearingRequest): Long? {
    return if (featureFlagsService.isAsyncMode()) {
      null
    } else {
      legacyNomisGateway.createHearing(adjudicationNumber, oicHearingRequest)
    }
  }

  fun amendHearing(adjudicationNumber: Long, oicHearingId: Long?, oicHearingRequest: OicHearingRequest) {
    if (featureFlagsService.isLegacySyncMode()) {
      legacyNomisGateway.amendHearing(
        adjudicationNumber = adjudicationNumber,
        oicHearingId = oicHearingId!!,
        oicHearingRequest = oicHearingRequest,
      )
    }
  }

  fun deleteHearing(adjudicationNumber: Long, oicHearingId: Long?) {
    if (featureFlagsService.isLegacySyncMode()) {
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
    if (featureFlagsService.isLegacySyncMode()) {
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
    if (featureFlagsService.isLegacySyncMode()) {
      legacyNomisGateway.amendHearingResult(
        adjudicationNumber = adjudicationNumber,
        oicHearingId = oicHearingId!!,
        oicHearingResultRequest = oicHearingResultRequest,
      )
    }
  }

  fun deleteHearingResult(adjudicationNumber: Long, oicHearingId: Long?) {
    if (featureFlagsService.isLegacySyncMode()) {
      legacyNomisGateway.deleteHearingResult(adjudicationNumber = adjudicationNumber, oicHearingId = oicHearingId!!)
    }
  }

  @Deprecated("to remove on completion of NN-5319")
  fun createSanction(adjudicationNumber: Long, sanction: OffenderOicSanctionRequest): Long? {
    return if (featureFlagsService.isAsyncMode()) {
      null
    } else {
      legacyNomisGateway.createSanction(adjudicationNumber, sanction)
    }
  }

  @Deprecated("to remove on completion of NN-5319")
  fun deleteSanction(adjudicationNumber: Long, sanctionSeq: Long) {
    if (featureFlagsService.isLegacySyncMode()) {
      legacyNomisGateway.deleteSanction(adjudicationNumber, sanctionSeq)
    }
  }

  fun createSanctions(adjudicationNumber: Long, sanctions: List<OffenderOicSanctionRequest>) {
    if (featureFlagsService.isLegacySyncMode()) {
      legacyNomisGateway.createSanctions(adjudicationNumber, sanctions)
    }
  }

  fun updateSanctions(adjudicationNumber: Long, sanctions: List<OffenderOicSanctionRequest>) {
    if (featureFlagsService.isLegacySyncMode()) {
      legacyNomisGateway.updateSanctions(adjudicationNumber, sanctions)
    }
  }

  fun quashSanctions(adjudicationNumber: Long) {
    if (featureFlagsService.isLegacySyncMode()) {
      legacyNomisGateway.quashSanctions(adjudicationNumber)
    }
  }

  fun deleteSanctions(adjudicationNumber: Long) {
    if (featureFlagsService.isLegacySyncMode()) {
      legacyNomisGateway.deleteSanctions(adjudicationNumber)
    }
  }
}
