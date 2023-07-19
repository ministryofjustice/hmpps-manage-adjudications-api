package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.AsyncConfig
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsService

@Service
class LegacySyncService(
  private val legacyNomisGateway: LegacyNomisGateway,
  private val featureFlagsService: FeatureFlagsService,
  private val asyncConfig: AsyncConfig,
) {

  fun requestAdjudicationCreationData(): Long? {
    return if (!asyncConfig.chargeNumbers) {
      legacyNomisGateway.requestAdjudicationCreationData()
    } else {
      null
    }
  }

  fun publishAdjudication(adjudicationDetailsToPublish: AdjudicationDetailsToPublish) {
    if (!asyncConfig.adjudications) {
      legacyNomisGateway.publishAdjudication(adjudicationDetailsToPublish)
    }
  }

  fun createHearing(adjudicationNumber: Long, oicHearingRequest: OicHearingRequest): Long? {
    return if (asyncConfig.hearings) {
      null
    } else {
      legacyNomisGateway.createHearing(adjudicationNumber, oicHearingRequest)
    }
  }

  fun amendHearing(adjudicationNumber: Long, oicHearingId: Long?, oicHearingRequest: OicHearingRequest) {
    if (!asyncConfig.hearings) {
      legacyNomisGateway.amendHearing(
        adjudicationNumber = adjudicationNumber,
        oicHearingId = oicHearingId!!,
        oicHearingRequest = oicHearingRequest,
      )
    }
  }

  fun deleteHearing(adjudicationNumber: Long, oicHearingId: Long?) {
    if (!asyncConfig.hearings) {
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
    if (!asyncConfig.outcomes) {
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
    if (!asyncConfig.outcomes) {
      legacyNomisGateway.amendHearingResult(
        adjudicationNumber = adjudicationNumber,
        oicHearingId = oicHearingId!!,
        oicHearingResultRequest = oicHearingResultRequest,
      )
    }
  }

  fun deleteHearingResult(adjudicationNumber: Long, oicHearingId: Long?) {
    if (!asyncConfig.outcomes) {
      legacyNomisGateway.deleteHearingResult(adjudicationNumber = adjudicationNumber, oicHearingId = oicHearingId!!)
    }
  }

  fun createSanction(adjudicationNumber: Long, sanction: OffenderOicSanctionRequest): Long? {
    return if (featureFlagsService.isAsyncMode()) {
      null
    } else {
      legacyNomisGateway.createSanction(adjudicationNumber, sanction)
    }
  }

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
