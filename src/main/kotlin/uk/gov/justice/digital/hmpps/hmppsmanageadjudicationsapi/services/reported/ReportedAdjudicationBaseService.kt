package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsService.Companion.getSuspendedPunishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsService.Companion.latestSchedule
import java.time.LocalDate
import java.time.LocalDateTime

open class ReportedAdjudicationBaseService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  protected val offenceCodeLookupService: OffenceCodeLookupService,
  protected val authenticationFacade: AuthenticationFacade,
) {

  protected fun findByChargeNumber(chargeNumber: String): ReportedAdjudication = findByChargeNumber(chargeNumber = chargeNumber, ignoreSecurityCheck = false)

  private fun findByChargeNumber(chargeNumber: String, ignoreSecurityCheck: Boolean): ReportedAdjudication {
    val reportedAdjudication =
      reportedAdjudicationRepository.findByChargeNumber(chargeNumber) ?: throwEntityNotFoundException(
        chargeNumber,
      )
    if (ignoreSecurityCheck) return reportedAdjudication

    val overrideAgencyId = reportedAdjudication.overrideAgencyId ?: reportedAdjudication.originatingAgencyId

    if (listOf(reportedAdjudication.originatingAgencyId, overrideAgencyId)
        .none { it == authenticationFacade.activeCaseload }
    ) {
      throwEntityNotFoundException(chargeNumber)
    }

    return reportedAdjudication
  }

  protected fun saveToDto(reportedAdjudication: ReportedAdjudication, logLastModified: Boolean = true): ReportedAdjudicationDto =
    reportedAdjudicationRepository.save(
      reportedAdjudication.also {
        if (logLastModified) it.lastModifiedAgencyId = authenticationFacade.activeCaseload
      },
    ).toDto(
      offenceCodeLookupService = offenceCodeLookupService,
      activeCaseload = authenticationFacade.activeCaseload,
    )

  protected fun getNextChargeNumber(agency: String): String {
    val next = reportedAdjudicationRepository.getNextChargeSequence("${agency}_CHARGE_SEQUENCE")

    return "$agency-${next.toString().padStart(6, '0')}"
  }

  protected fun findByChargeNumberIn(chargeNumbers: List<String>) = reportedAdjudicationRepository.findByChargeNumberIn(chargeNumbers)

  protected fun isLinkedToReport(consecutiveChargeNumber: String, types: List<PunishmentType>): Boolean =
    reportedAdjudicationRepository.findByPunishmentsConsecutiveToChargeNumberAndPunishmentsTypeIn(consecutiveChargeNumber, types).isNotEmpty()

  protected fun findMultipleOffenceCharges(prisonerNumber: String, chargeNumber: String): List<String> =
    reportedAdjudicationRepository.findByPrisonerNumberAndChargeNumberStartsWith(
      prisonerNumber = prisonerNumber,
      chargeNumber = "${chargeNumber.substringBefore("-")}-",
    )
      .filter { it.chargeNumber != chargeNumber }.map { it.chargeNumber }
      .sortedBy { it }

  protected fun hasLinkedAda(reportedAdjudication: ReportedAdjudication): Boolean =
    when (reportedAdjudication.status) {
      ReportedAdjudicationStatus.CHARGE_PROVED ->
        if (reportedAdjudication.getPunishments().none { PunishmentType.additionalDays().contains(it.type) }) {
          false
        } else {
          isLinkedToReport(reportedAdjudication.chargeNumber, PunishmentType.additionalDays())
        }
      else -> false
    }

  protected fun getReportCountForProfile(offenderBookingId: Long, cutOff: LocalDateTime): Long =
    reportedAdjudicationRepository.countByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
      bookingId = offenderBookingId,
      status = ReportedAdjudicationStatus.CHARGE_PROVED,
      cutOff = cutOff,
    )

  protected fun offenderHasAdjudications(offenderBookingId: Long): Boolean = reportedAdjudicationRepository.existsByOffenderBookingId(
    offenderBookingId = offenderBookingId,
  )

  protected fun PunishmentRequest.updateAndGetSuspendedPunishment(activatedBy: String): Punishment {
    val activatedFromReport = findByChargeNumber(chargeNumber = this.activatedFrom!!, ignoreSecurityCheck = true)
    return activatedFromReport.getPunishments().getSuspendedPunishment(this.id!!).also {
      it.activatedByChargeNumber = activatedBy
    }
  }

  protected fun getActivatedPunishments(chargeNumber: String): List<Pair<String, Punishment>> =
    reportedAdjudicationRepository.findByPunishmentsActivatedByChargeNumber(chargeNumber = chargeNumber).map {
      // a handful of records were amended to a state they can not be repaired so this check will persist.  Address again with UI updates
      it.getPunishments().filter { p -> p.activatedByChargeNumber == chargeNumber && p.suspendedUntil == null }
        .map { toPair -> Pair(it.chargeNumber, toPair) }
    }.flatten()

  protected fun deactivateActivatedPunishments(chargeNumber: String, idsToIgnore: List<Long>): Set<SuspendedPunishmentEvent> {
    val suspendedPunishmentEvents = mutableSetOf<SuspendedPunishmentEvent>()
    reportedAdjudicationRepository.findByPunishmentsActivatedByChargeNumber(chargeNumber = chargeNumber)
      .forEach {
        it.getPunishments()
          .filter { p -> p.activatedByChargeNumber == chargeNumber && idsToIgnore.none { id -> id == it.id } }
          .forEach { punishmentToRestore ->
            punishmentToRestore.activatedByChargeNumber = null
            val latestSchedule = punishmentToRestore.schedule.latestSchedule()
            // this check is required until the historic data is corrected
            if (punishmentToRestore.schedule.size > 1 && latestSchedule.suspendedUntil == null) {
              punishmentToRestore.schedule.remove(latestSchedule)
              punishmentToRestore.suspendedUntil = punishmentToRestore.schedule.latestSchedule().suspendedUntil
            }
            suspendedPunishmentEvents.add(
              SuspendedPunishmentEvent(agencyId = it.originatingAgencyId, chargeNumber = it.chargeNumber, status = it.status),
            )
          }
      }

    return suspendedPunishmentEvents
  }

  companion object {
    fun throwEntityNotFoundException(id: String): Nothing =
      throw EntityNotFoundException("ReportedAdjudication not found for $id")

    fun Punishment.isActive(): Boolean =
      this.suspendedUntil == null && this.schedule.latestSchedule().endDate?.isAfter(LocalDate.now().minusDays(1)) == true
  }
}
