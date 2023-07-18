package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsService

@Service
class LegacySyncService(
  private val legacyNomisGateway: LegacyNomisGateway,
  private val featureFlagsService: FeatureFlagsService,
) {

  fun requestAdjudicationCreationData(): Long? {
    return legacyNomisGateway.requestAdjudicationCreationData()
  }

  fun publishAdjudication(adjudicationDetailsToPublish: AdjudicationDetailsToPublish) {
    if (featureFlagsService.isLegacySyncMode()) {
      legacyNomisGateway.publishAdjudication(adjudicationDetailsToPublish)
    }
  }

  fun createHearing(adjudicationNumber: Long, oicHearingRequest: OicHearingRequest): Long? {
    return legacyNomisGateway.createHearing(adjudicationNumber, oicHearingRequest)
  }

  fun amendHearing(adjudicationNumber: Long, oicHearingId: Long?, oicHearingRequest: OicHearingRequest) {
    legacyNomisGateway.amendHearing(
      adjudicationNumber = adjudicationNumber,
      oicHearingId = oicHearingId!!,
      oicHearingRequest = oicHearingRequest,
    )
  }

  fun deleteHearing(adjudicationNumber: Long, oicHearingId: Long?) {
    legacyNomisGateway.deleteHearing(
      adjudicationNumber = adjudicationNumber,
      oicHearingId = oicHearingId!!,
    )
  }

  fun createHearingResult(
    adjudicationNumber: Long,
    oicHearingId: Long?,
    oicHearingResultRequest: OicHearingResultRequest,
  ) {
    legacyNomisGateway.createHearingResult(
      adjudicationNumber = adjudicationNumber,
      oicHearingId = oicHearingId!!,
      oicHearingResultRequest = oicHearingResultRequest,
    )
  }

  fun amendHearingResult(
    adjudicationNumber: Long,
    oicHearingId: Long?,
    oicHearingResultRequest: OicHearingResultRequest,
  ) {
    legacyNomisGateway.amendHearingResult(
      adjudicationNumber = adjudicationNumber,
      oicHearingId = oicHearingId!!,
      oicHearingResultRequest = oicHearingResultRequest,
    )
  }

  fun deleteHearingResult(adjudicationNumber: Long, oicHearingId: Long?) {
    legacyNomisGateway.deleteHearingResult(adjudicationNumber = adjudicationNumber, oicHearingId = oicHearingId!!)
  }

  fun createSanction(adjudicationNumber: Long, sanction: OffenderOicSanctionRequest): Long? {
    return legacyNomisGateway.createSanction(adjudicationNumber, sanction)
  }

  fun deleteSanction(adjudicationNumber: Long, sanctionSeq: Long) {
    legacyNomisGateway.deleteSanction(adjudicationNumber, sanctionSeq)
  }

  fun createSanctions(adjudicationNumber: Long, sanctions: List<OffenderOicSanctionRequest>) {
    legacyNomisGateway.createSanctions(adjudicationNumber, sanctions)
  }

  fun updateSanctions(adjudicationNumber: Long, sanctions: List<OffenderOicSanctionRequest>) {
    legacyNomisGateway.updateSanctions(adjudicationNumber, sanctions)
  }

  fun quashSanctions(adjudicationNumber: Long) {
    legacyNomisGateway.quashSanctions(adjudicationNumber)
  }

  fun deleteSanctions(adjudicationNumber: Long) {
    legacyNomisGateway.deleteSanctions(adjudicationNumber)
  }
}
