package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ChargeWithSuspendedPunishments
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.Dis5PrintSupportDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.LastReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication.Companion.isCorrupted
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodes.Companion.containsNomisCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsService.Companion.latestSchedule
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
  fun getDis5Data(chargeNumber: String): Dis5PrintSupportDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber = chargeNumber)
    val currentEstablishment = reportedAdjudication.overrideAgencyId ?: reportedAdjudication.originatingAgencyId
    val otherChargesOnSentence = offenderChargesForPrintSupport(
      offenderBookingId = reportedAdjudication.offenderBookingId!!,
      chargeNumber = chargeNumber,
    )
    val previousAtCurrentEstablishmentCount = otherChargesOnSentence.count {
      it.originatingAgencyId == currentEstablishment || it.overrideAgencyId == currentEstablishment
    }
    val punishmentCutOff = LocalDate.now().minusDays(1)
    val chargesWithActiveSuspendedPunishments = otherChargesOnSentence.filter {
      it.getPunishments().any {
          punishment ->
        punishment.isActiveSuspended(punishmentCutOff)
      }
    }.sortedBy { it.dateTimeOfDiscovery }

    val offenceCode = reportedAdjudication.offenceDetails.first().offenceCode
    val sameOffenceCharges = otherChargesOnSentence.filter {
      it.offenceDetails.matchesOffence(offenceCode = offenceCode) || it.offenceDetails.matchesLegacyOffence(offenceCode = offenceCode)
    }
    val existingPunishments = otherChargesOnSentence.flatMap { it.getPunishments() }.filter {
        punishment ->
      punishment.isActivePunishment(punishmentCutOff)
    }

    return Dis5PrintSupportDto(
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
          offenceDetails = it.toReportedOffence(offenceCodeLookupService),
          suspendedPunishments = it.getPunishments().filter {
              punishment ->
            punishment.isActiveSuspended(punishmentCutOff)
          }.toPunishments().sortedBy { p -> p.schedule.suspendedUntil },
        )
      },
      sameOffenceCount = sameOffenceCharges.size,
      lastReportedOffence = sameOffenceCharges.maxByOrNull { it.dateTimeOfDiscovery }.toLastReportedOffence(),
      existingPunishments = existingPunishments.toPunishments().sortedBy { it.schedule.endDate },
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

    fun Punishment.isActiveSuspended(punishmentCutOff: LocalDate): Boolean =
      !this.isCorrupted() && this.suspendedUntil?.isAfter(punishmentCutOff) == true

    fun Punishment.isActivePunishment(punishmentCutOff: LocalDate): Boolean =
      PunishmentType.damagesAndCaution().none { it == this.type } && this.suspendedUntil == null && (
        this.schedule.latestSchedule().endDate?.isAfter(punishmentCutOff) == true || PunishmentType.additionalDays().contains(this.type)
        )

    fun List<ReportedOffence>.matchesOffence(offenceCode: Int): Boolean =
      this.first().offenceCode != -0 && this.first().offenceCode == offenceCode

    fun List<ReportedOffence>.matchesLegacyOffence(offenceCode: Int): Boolean =
      this.first().offenceCode == 0 && this.first().nomisOffenceCode != null && offenceCode.containsNomisCode(this.first().nomisOffenceCode!!)
  }
}
