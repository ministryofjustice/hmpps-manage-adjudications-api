package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ChargeWithSuspendedPunishments
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.Dis5PrintSupportDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.LastReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication.Companion.toPunishmentsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodes.Companion.containsNomisCode
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class PrintSupportQueryService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {
  fun offenderChargesForPrintSupport(offenderBookingId: Long, chargeNumber: String): List<ReportedAdjudication> = reportedAdjudicationRepository.findByOffenderBookingIdAndStatus(
    offenderBookingId = offenderBookingId,
    status = ReportedAdjudicationStatus.CHARGE_PROVED,
  ).filter { it.chargeNumber != chargeNumber }
}

@Transactional(readOnly = true)
@Service
class PrintSupportService(
  private val printSupportQueryService: PrintSupportQueryService,
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
    val otherChargesOnSentence = printSupportQueryService.offenderChargesForPrintSupport(
      offenderBookingId = reportedAdjudication.offenderBookingId!!,
      chargeNumber = chargeNumber,
    )
    val previousAtCurrentEstablishmentCount = otherChargesOnSentence.count {
      it.originatingAgencyId == currentEstablishment
    }
    val punishmentCutOff = LocalDate.now().minusDays(1)
    val chargesWithActiveSuspendedPunishments = otherChargesOnSentence.filter {
      it.getPunishments().any { punishment ->
        punishment.isActiveSuspended(punishmentCutOff) && !punishment.isCorrupted()
      }
    }.sortedBy { it.dateTimeOfDiscovery }

    val offenceCode = reportedAdjudication.offenceDetails.first().offenceCode
    val sameOffenceCharges = otherChargesOnSentence.filter {
      it.offenceDetails.matchesOffence(offenceCode = offenceCode) || it.offenceDetails.matchesLegacyOffence(offenceCode = offenceCode)
    }
    val existingPunishments = otherChargesOnSentence.flatMap { it.getPunishments() }.filter { punishment ->
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
          offenceDetails = it.offenceDetails.first().toDto(offenceCodeLookupService, it.isYouthOffender, it.gender),
          suspendedPunishments = it.getPunishments().filter { punishment ->
            punishment.isActiveSuspended(punishmentCutOff) && !punishment.isCorrupted()
          }.toPunishmentsDto(false).sortedBy { p -> p.schedule.suspendedUntil },
        )
      },
      sameOffenceCount = sameOffenceCharges.size,
      lastReportedOffence = sameOffenceCharges.maxByOrNull { it.dateTimeOfDiscovery }.toLastReportedOffence(),
      existingPunishments = existingPunishments.toPunishmentsDto(false).sortedBy { it.schedule.endDate },
    )
  }

  private fun ReportedAdjudication?.toLastReportedOffence(): LastReportedOffence? {
    this ?: return null

    return LastReportedOffence(
      dateOfIncident = this.dateTimeOfIncident.toLocalDate(),
      dateOfDiscovery = this.dateTimeOfDiscovery.toLocalDate(),
      chargeNumber = this.chargeNumber,
      statement = this.statement,
      punishments = this.getPunishments().toPunishmentsDto(false),
    )
  }

  companion object {

    fun List<ReportedOffence>.matchesOffence(offenceCode: Int): Boolean = this.first().offenceCode != -0 && this.first().offenceCode == offenceCode

    fun List<ReportedOffence>.matchesLegacyOffence(offenceCode: Int): Boolean = this.first().offenceCode == 0 && this.first().nomisOffenceCode != null && offenceCode.containsNomisCode(this.first().nomisOffenceCode!!)
  }
}
