package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService

@Service
class LegacySyncService(
  private val legacyNomisGateway: LegacyNomisGateway,
  private val featureFlagsConfig: FeatureFlagsConfig,
  private val offenceCodeLookupService: OffenceCodeLookupService,
) {

  fun requestAdjudicationCreationData(): Long? {
    return if (!featureFlagsConfig.chargeNumbers) {
      legacyNomisGateway.requestAdjudicationCreationData()
    } else {
      return null
    }
  }

  fun publishAdjudication(reportedAdjudication: ReportedAdjudication) {
    if (!featureFlagsConfig.adjudications) {
      legacyNomisGateway.publishAdjudication(
        AdjudicationDetailsToPublish(
          offenderNo = reportedAdjudication.prisonerNumber,
          adjudicationNumber = reportedAdjudication.chargeNumber.toLong(),
          reporterName = reportedAdjudication.createdByUserId
            ?: throw EntityNotFoundException(
              "ReportedAdjudication creator name not set for reported adjudication number ${reportedAdjudication.chargeNumber}",
            ),
          reportedDateTime = reportedAdjudication.createDateTime
            ?: throw EntityNotFoundException(
              "ReportedAdjudication creation time not set for reported adjudication number ${reportedAdjudication.chargeNumber}",
            ),
          agencyId = reportedAdjudication.originatingAgencyId,
          incidentTime = reportedAdjudication.dateTimeOfDiscovery,
          incidentLocationId = reportedAdjudication.locationId,
          statement = reportedAdjudication.statement,
          offenceCodes = getNomisCodes(reportedAdjudication.incidentRoleCode, reportedAdjudication.offenceDetails, reportedAdjudication.isYouthOffender),
          connectedOffenderIds = getAssociatedOffenders(
            associatedPrisonersNumber = reportedAdjudication.incidentRoleAssociatedPrisonersNumber,
          ),
          victimOffenderIds = getVictimOffenders(
            prisonerNumber = reportedAdjudication.prisonerNumber,
            offenceDetails = reportedAdjudication.offenceDetails,
          ),
          victimStaffUsernames = getVictimStaffUsernames(reportedAdjudication.offenceDetails),
        ),
      )
    }
  }

  fun createHearing(adjudicationNumber: String, oicHearingRequest: OicHearingRequest): Long? {
    return if (!featureFlagsConfig.hearings) {
      legacyNomisGateway.createHearing(adjudicationNumber.toLong(), oicHearingRequest)
    } else {
      null
    }
  }

  fun amendHearing(adjudicationNumber: String, oicHearingId: Long?, oicHearingRequest: OicHearingRequest) {
    if (!featureFlagsConfig.hearings) {
      legacyNomisGateway.amendHearing(
        adjudicationNumber = adjudicationNumber.toLong(),
        oicHearingId = oicHearingId!!,
        oicHearingRequest = oicHearingRequest,
      )
    }
  }

  fun deleteHearing(adjudicationNumber: String, oicHearingId: Long?) {
    if (!featureFlagsConfig.hearings) {
      legacyNomisGateway.deleteHearing(
        adjudicationNumber = adjudicationNumber.toLong(),
        oicHearingId = oicHearingId!!,
      )
    }
  }

  fun createHearingResult(
    adjudicationNumber: String,
    oicHearingId: Long?,
    oicHearingResultRequest: OicHearingResultRequest,
  ) {
    if (!featureFlagsConfig.outcomes) {
      legacyNomisGateway.createHearingResult(
        adjudicationNumber = adjudicationNumber.toLong(),
        oicHearingId = oicHearingId!!,
        oicHearingResultRequest = oicHearingResultRequest,
      )
    }
  }

  fun amendHearingResult(
    adjudicationNumber: String,
    oicHearingId: Long?,
    oicHearingResultRequest: OicHearingResultRequest,
  ) {
    if (!featureFlagsConfig.outcomes) {
      legacyNomisGateway.amendHearingResult(
        adjudicationNumber = adjudicationNumber.toLong(),
        oicHearingId = oicHearingId!!,
        oicHearingResultRequest = oicHearingResultRequest,
      )
    }
  }

  fun deleteHearingResult(adjudicationNumber: String, oicHearingId: Long?) {
    if (!featureFlagsConfig.outcomes) {
      legacyNomisGateway.deleteHearingResult(adjudicationNumber = adjudicationNumber.toLong(), oicHearingId = oicHearingId!!)
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

  private fun getNomisCodes(roleCode: String?, offenceDetails: MutableList<ReportedOffence>?, isYouthOffender: Boolean): List<String> {
    if (roleCode != null) { // Null means committed on own
      return offenceDetails?.map { offenceCodeLookupService.getOffenceCode(it.offenceCode, isYouthOffender).getNomisCodeWithOthers() }
        ?: emptyList()
    }
    return offenceDetails?.map { offenceCodeLookupService.getOffenceCode(it.offenceCode, isYouthOffender).getNomisCode() }
      ?: emptyList()
  }

  private fun getAssociatedOffenders(associatedPrisonersNumber: String?): List<String> {
    if (associatedPrisonersNumber == null) {
      return emptyList()
    }
    return listOf(associatedPrisonersNumber)
  }

  private fun getVictimOffenders(prisonerNumber: String, offenceDetails: MutableList<ReportedOffence>?): List<String> {
    return offenceDetails?.filter { it.victimPrisonersNumber != prisonerNumber }?.mapNotNull { it.victimPrisonersNumber } ?: emptyList()
  }

  private fun getVictimStaffUsernames(offenceDetails: MutableList<ReportedOffence>?): List<String> {
    return offenceDetails?.mapNotNull { it.victimStaffUsername } ?: emptyList()
  }
}
