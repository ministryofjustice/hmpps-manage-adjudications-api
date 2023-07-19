package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.AsyncConfig

@Service
class LegacySyncService(
  private val legacyNomisGateway: LegacyNomisGateway,
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
    return if (!asyncConfig.hearings) {
      legacyNomisGateway.createHearing(adjudicationNumber, oicHearingRequest)
    } else {
      null
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

  @Deprecated("to remove on completion of NN-5319")
  fun createSanction(adjudicationNumber: Long, sanction: OffenderOicSanctionRequest): Long? =
    legacyNomisGateway.createSanction(adjudicationNumber, sanction)

  @Deprecated("to remove on completion of NN-5319")
  fun deleteSanction(adjudicationNumber: Long, sanctionSeq: Long) =
    legacyNomisGateway.deleteSanction(adjudicationNumber, sanctionSeq)

  fun createSanctions(adjudicationNumber: Long, sanctions: List<OffenderOicSanctionRequest>) {
    if (!asyncConfig.outcomes && !asyncConfig.punishments) {
      legacyNomisGateway.createSanctions(adjudicationNumber, sanctions)
    }
  }

  fun updateSanctions(adjudicationNumber: Long, sanctions: List<OffenderOicSanctionRequest>) {
    if (!asyncConfig.punishments) {
      legacyNomisGateway.updateSanctions(adjudicationNumber, sanctions)
    }
  }

  fun quashSanctions(adjudicationNumber: Long) {
    if (!asyncConfig.punishments) {
      legacyNomisGateway.quashSanctions(adjudicationNumber)
    }
  }

  fun deleteSanctions(adjudicationNumber: Long) {
    if (!asyncConfig.punishments) {
      legacyNomisGateway.deleteSanctions(adjudicationNumber)
    }
  }
}
