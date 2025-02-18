package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import java.time.LocalDateTime

open class ReportedAdjudicationBaseService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  protected val offenceCodeLookupService: OffenceCodeLookupService,
  protected val authenticationFacade: AuthenticationFacade,
) {

  protected fun findByChargeNumber(chargeNumber: String): ReportedAdjudication {
    val reportedAdjudication =
      reportedAdjudicationRepository.findByChargeNumber(chargeNumber) ?: throwEntityNotFoundException(
        chargeNumber,
      )

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

  protected fun isLinkedToReportV2(consecutiveChargeNumber: String, types: List<PunishmentType>): Boolean =
    reportedAdjudicationRepository.findByPunishmentsConsecutiveToChargeNumberAndPunishmentsTypeInV2(consecutiveChargeNumber, types).isNotEmpty()

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
          isLinkedToReportV2(reportedAdjudication.chargeNumber, PunishmentType.additionalDays())
        }
      else -> false
    }

  protected fun getReportCountForProfile(offenderBookingId: Long, cutOff: LocalDateTime): Long =
    reportedAdjudicationRepository.activeChargeProvedForBookingId(
      bookingId = offenderBookingId,
      cutOff = cutOff,
    )

  protected fun offenderHasAdjudications(offenderBookingId: Long): Boolean = reportedAdjudicationRepository.existsByOffenderBookingId(
    offenderBookingId = offenderBookingId,
  )

  protected fun getActivatedPunishments(chargeNumber: String): List<Pair<String, Punishment>> =
    reportedAdjudicationRepository.findByPunishmentsActivatedByChargeNumber(chargeNumber = chargeNumber).map {
      it.getPunishments().filter { p -> p.activatedByChargeNumber == chargeNumber }
        .map { toPair -> Pair(it.chargeNumber, toPair) }
    }.flatten()

  protected fun deactivateActivatedPunishments(chargeNumber: String, idsToIgnore: List<Long>): Set<SuspendedPunishmentEvent> {
    val suspendedPunishmentEvents = mutableSetOf<SuspendedPunishmentEvent>()

    reportedAdjudicationRepository.findByPunishmentsActivatedByChargeNumber(chargeNumber = chargeNumber).forEach {
      it.getPunishments()
        .filter { p -> p.activatedByChargeNumber == chargeNumber && idsToIgnore.none { id -> id == it.id } && p.getSchedule().size > 1 }
        .forEach { punishmentToRestore ->

          punishmentToRestore.removeSchedule(punishmentToRestore.latestSchedule())
          punishmentToRestore.activatedByChargeNumber = null

          suspendedPunishmentEvents.add(
            SuspendedPunishmentEvent(
              agencyId = it.originatingAgencyId,
              chargeNumber = it.chargeNumber,
              status = it.status,
            ),
          )
        }
    }

    return suspendedPunishmentEvents
  }

  companion object {
    fun throwEntityNotFoundException(id: String): Nothing =
      throw EntityNotFoundException("ReportedAdjudication not found for $id")
  }
}
