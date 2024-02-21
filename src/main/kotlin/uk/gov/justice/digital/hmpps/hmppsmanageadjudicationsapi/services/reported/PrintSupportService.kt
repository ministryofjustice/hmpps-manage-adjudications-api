package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.Dis5DataModel
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.LastReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication.Companion.isCorrupted
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodes.Companion.containsNomisCode
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class PrintSupportService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {
  fun getDis5Data(chargeNumber: String): Dis5DataModel {
    val reportedAdjudication = findByChargeNumber(chargeNumber = chargeNumber)
    val currentEstablishment = reportedAdjudication.overrideAgencyId ?: reportedAdjudication.originatingAgencyId
    val otherChargesOnSentence = offenderChargesForPrintSupport(
      offenderBookingId = reportedAdjudication.offenderBookingId!!,
      chargeNumber = chargeNumber,
    )
    val previousAtCurrentEstablishmentCount = otherChargesOnSentence.count {
      it.originatingAgencyId == currentEstablishment || it.overrideAgencyId == currentEstablishment
    }
    val suspendedCutOff = LocalDate.now().minusDays(1)
    val suspendedPunishments = otherChargesOnSentence.flatMap { it.getPunishments() }.filter {
      !it.isCorrupted() && it.suspendedUntil?.isAfter(suspendedCutOff) == true
    }.toPunishments()

    val offenceCode = reportedAdjudication.offenceDetails.first().offenceCode
    val sameOffenceCharges = otherChargesOnSentence.filter {
      it.offenceDetails.matchesOffence(offenceCode = offenceCode) || it.offenceDetails.matchesLegacyOffence(offenceCode = offenceCode)
    }

    return Dis5DataModel(
      chargeNumber = reportedAdjudication.chargeNumber,
      dateOfIncident = reportedAdjudication.dateTimeOfIncident.toLocalDate(),
      dateOfDiscovery = reportedAdjudication.dateTimeOfDiscovery.toLocalDate(),
      previousCount = otherChargesOnSentence.size,
      previousAtCurrentEstablishmentCount = previousAtCurrentEstablishmentCount,
      suspendedPunishments = suspendedPunishments,
      sameOffenceCount = sameOffenceCharges.size,
      lastReportedOffence = sameOffenceCharges.maxByOrNull { it.dateTimeOfDiscovery }.toLastReportedOffence(),
    )
  }

  private fun ReportedAdjudication?.toLastReportedOffence(): LastReportedOffence? {
    this ?: return null

    return LastReportedOffence(
      dateOfIncident = this.dateTimeOfIncident.toLocalDate(),
      dateOfDiscovery = this.dateTimeOfDiscovery.toLocalDate(),
      chargeNumber = this.chargeNumber,
      statement = this.statement,
      punishments = this.getPunishments().toPunishments(),
    )
  }

  companion object {
    fun List<ReportedOffence>.matchesOffence(offenceCode: Int): Boolean =
      this.first().offenceCode != -0 && this.first().offenceCode == offenceCode

    fun List<ReportedOffence>.matchesLegacyOffence(offenceCode: Int): Boolean =
      this.first().offenceCode == 0 && this.first().nomisOffenceCode != null && offenceCode.containsNomisCode(this.first().nomisOffenceCode!!)
  }
}
