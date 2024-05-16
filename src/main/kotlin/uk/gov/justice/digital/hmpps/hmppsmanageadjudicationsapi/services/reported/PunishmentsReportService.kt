package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ActivePunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdditionalDaysDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Measurement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsReportService.Companion.corruptedSuspendedCutOff
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsReportService.Companion.suspendedCutOff
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsService.Companion.latestSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationBaseService.Companion.isActive
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class PunishmentsReportQueryService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {
  fun getReportsWithSuspendedPunishments(prisonerNumber: String) = reportedAdjudicationRepository.findByStatusAndPrisonerNumberAndPunishmentsSuspendedUntilAfter(
    status = ReportedAdjudicationStatus.CHARGE_PROVED,
    prisonerNumber = prisonerNumber,
    date = suspendedCutOff,
  )

  @Deprecated(
    """this can be removed at some point in the near future ie after 1.9.2024 approximately. 
      Its purpose is to include nomis records where nomis allowed the user to incorrectly specify suspended until dates """,
  )
  fun getCorruptedReportsWithSuspendedPunishmentsInLast6Months(prisonerNumber: String) =
    reportedAdjudicationRepository.findByPrisonerNumberAndStatusInAndPunishmentsSuspendedUntilAfter(
      prisonerNumber = prisonerNumber,
      statuses = ReportedAdjudicationStatus.corruptedStatuses(),
      date = corruptedSuspendedCutOff,
    )

  fun getReportsWithActiveAdditionalDays(prisonerNumber: String, punishmentType: PunishmentType) =
    reportedAdjudicationRepository.findByStatusAndPrisonerNumberAndPunishmentsTypeAndPunishmentsSuspendedUntilIsNull(
      status = ReportedAdjudicationStatus.CHARGE_PROVED,
      prisonerNumber = prisonerNumber,
      punishmentType = punishmentType,
    )

  fun getReportsWithActivePunishments(offenderBookingId: Long): List<Pair<String, List<Punishment>>> =
    reportedAdjudicationRepository.findByStatusAndOffenderBookingIdAndPunishmentsSuspendedUntilIsNullAndPunishmentsScheduleEndDateIsAfter(
      status = ReportedAdjudicationStatus.CHARGE_PROVED,
      offenderBookingId = offenderBookingId,
      cutOff = LocalDate.now().minusDays(1),
    ).map { Pair(it.chargeNumber, it.getPunishments().filter { p -> p.isActive() }) }
}

@Transactional(readOnly = true)
@Service
class PunishmentsReportService(
  private val punishmentsReportQueryService: PunishmentsReportQueryService,
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {
  fun getSuspendedPunishments(prisonerNumber: String, chargeNumber: String): List<SuspendedPunishmentDto> {
    val reportsWithSuspendedPunishments = punishmentsReportQueryService.getReportsWithSuspendedPunishments(prisonerNumber = prisonerNumber).toMutableList()
      .union(punishmentsReportQueryService.getCorruptedReportsWithSuspendedPunishmentsInLast6Months(prisonerNumber = prisonerNumber))
      .filter { it.chargeNumber != chargeNumber }

    val includeAdditionalDays = includeAdditionalDays(chargeNumber)

    return reportsWithSuspendedPunishments.map {
      val corrupted = ReportedAdjudicationStatus.corruptedStatuses().contains(it.status)
      val cutOff = if (corrupted) corruptedSuspendedCutOff else suspendedCutOff
      it.getPunishments().suspendedPunishmentsToActivate(cutOff)
        .filter { punishment -> punishment.type.includeInSuspendedPunishments(includeAdditionalDays) }.map { punishment ->
          val schedule = punishment.schedule.latestSchedule()

          SuspendedPunishmentDto(
            chargeNumber = it.chargeNumber,
            corrupted = corrupted,
            punishment = PunishmentDto(
              id = punishment.id,
              type = punishment.type,
              privilegeType = punishment.privilegeType,
              otherPrivilege = punishment.otherPrivilege,
              stoppagePercentage = punishment.stoppagePercentage,
              schedule = PunishmentScheduleDto(days = schedule.duration ?: 0, suspendedUntil = schedule.suspendedUntil, duration = schedule.duration, measurement = Measurement.DAYS),
            ),
          )
        }
    }.flatten()
  }

  fun getReportsWithAdditionalDays(chargeNumber: String, prisonerNumber: String, punishmentType: PunishmentType): List<AdditionalDaysDto> {
    if (!PunishmentType.additionalDays().contains(punishmentType)) throw ValidationException("Punishment type must be ADDITIONAL_DAYS or PROSPECTIVE_DAYS")

    val reportedAdjudication = findByChargeNumber(chargeNumber)

    return punishmentsReportQueryService.getReportsWithActiveAdditionalDays(
      prisonerNumber = prisonerNumber,
      punishmentType = punishmentType,
    ).filter { it.includeAdaWithSameHearingDateAndSeparateCharge(reportedAdjudication) }
      .map {
        it.getPunishments().filter { punishment -> punishment.type == punishmentType }.map { punishment ->
          val schedule = punishment.schedule.latestSchedule()

          AdditionalDaysDto(
            chargeNumber = it.chargeNumber,
            chargeProvedDate = it.getLatestHearing()?.dateTimeOfHearing?.toLocalDate()!!,
            punishment = PunishmentDto(
              id = punishment.id,
              type = punishment.type,
              consecutiveChargeNumber = punishment.consecutiveToChargeNumber,
              schedule = PunishmentScheduleDto(days = schedule.duration ?: 0, duration = schedule.duration, measurement = Measurement.DAYS),
            ),
          )
        }
      }.flatten()
  }

  fun getActivePunishments(offenderBookingId: Long): List<ActivePunishmentDto> =
    punishmentsReportQueryService.getReportsWithActivePunishments(offenderBookingId = offenderBookingId)
      .map { chargeAndPunishments ->
        chargeAndPunishments.second.map {
          val latestSchedule = it.schedule.latestSchedule()
          ActivePunishmentDto(
            punishmentType = it.type,
            privilegeType = it.privilegeType,
            otherPrivilege = it.otherPrivilege,
            chargeNumber = chargeAndPunishments.first,
            startDate = latestSchedule.startDate,
            lastDay = latestSchedule.endDate,
            duration = if (latestSchedule.duration == 0) null else latestSchedule.duration,
            measurement = if (latestSchedule.duration == 0) null else Measurement.DAYS,
            amount = it.amount,
            stoppagePercentage = it.stoppagePercentage,
            // TODO this should really be activated by charge number, set to null for now until UI is updated post design review
            activatedFrom = null,
          )
        }
      }.flatten().sortedByDescending { it.startDate }

  private fun includeAdditionalDays(chargeNumber: String): Boolean {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    return OicHearingType.inadTypes().contains(reportedAdjudication.getLatestHearing()?.oicHearingType)
  }

  companion object {

    val suspendedCutOff: LocalDate = LocalDate.now().minusDays(1)
    val corruptedSuspendedCutOff: LocalDate = LocalDate.now().minusMonths(6)

    fun PunishmentType.includeInSuspendedPunishments(includeAdditionalDays: Boolean): Boolean {
      return if (!PunishmentType.additionalDays().contains(this)) {
        true
      } else {
        includeAdditionalDays
      }
    }

    fun ReportedAdjudication.includeAdaWithSameHearingDateAndSeparateCharge(currentAdjudication: ReportedAdjudication): Boolean =
      this.getLatestHearing()?.dateTimeOfHearing?.toLocalDate() == currentAdjudication.getLatestHearing()?.dateTimeOfHearing?.toLocalDate() &&
        this.chargeNumber != currentAdjudication.chargeNumber

    fun List<Punishment>.suspendedPunishmentsToActivate(cutOff: LocalDate) =
      this.filter { it.isActiveSuspended(cutOff) }
  }
}
