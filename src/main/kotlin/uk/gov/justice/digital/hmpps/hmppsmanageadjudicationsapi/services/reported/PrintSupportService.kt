package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ChargeWithSuspendedPunishments
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.Dis5DataModel
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.LastReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
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
    val chargesWithActiveSuspendedPunishments = otherChargesOnSentence.filter {
      it.getPunishments().any {
          punishment ->
        punishment.isActiveSuspended(suspendedCutOff)
      }
    }.sortedBy { it.dateTimeOfDiscovery }

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
      chargesWithSuspendedPunishments = chargesWithActiveSuspendedPunishments.map {
        ChargeWithSuspendedPunishments(
          chargeNumber = it.chargeNumber,
          dateOfIncident = it.dateTimeOfIncident.toLocalDate(),
          dateOfDiscovery = it.dateTimeOfDiscovery.toLocalDate(),
          suspendedPunishments = it.getPunishments().filter {
              punishment ->
            punishment.isActiveSuspended(suspendedCutOff)
          }.toPunishments().sortedBy { p -> p.schedule.suspendedUntil },
        )
      },
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

    fun Punishment.isActiveSuspended(suspendedCutOff: LocalDate): Boolean =
      !this.isCorrupted() && this.suspendedUntil?.isAfter(suspendedCutOff) == true
    fun List<ReportedOffence>.matchesOffence(offenceCode: Int): Boolean =
      this.first().offenceCode != -0 && this.first().offenceCode == offenceCode

    fun List<ReportedOffence>.matchesLegacyOffence(offenceCode: Int): Boolean =
      this.first().offenceCode == 0 && this.first().nomisOffenceCode != null && offenceCode.containsNomisCode(this.first().nomisOffenceCode!!)
  }
}
