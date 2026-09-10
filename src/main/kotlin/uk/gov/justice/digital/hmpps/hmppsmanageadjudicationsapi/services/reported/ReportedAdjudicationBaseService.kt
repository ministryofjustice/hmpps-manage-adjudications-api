package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.LossOfVisitsChangeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.LossOfVisitsEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.toLossOfVisitsEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
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

  protected data class SuspendedPunishmentUpdates(
    val events: Set<SuspendedPunishmentEvent> = emptySet(),
    val lossOfVisitsEvents: List<LossOfVisitsEventDto> = emptyList(),
  )

  protected sealed interface ConsecutiveChainIssue {
    val chargeNumber: String
  }

  protected data class InvalidConsecutiveTarget(
    override val chargeNumber: String,
  ) : ConsecutiveChainIssue

  protected data class MissingConsecutiveTarget(
    override val chargeNumber: String,
  ) : ConsecutiveChainIssue

  protected data class ConsecutivePunishmentLoop(
    override val chargeNumber: String,
  ) : ConsecutiveChainIssue

  protected data class MissingConsecutiveSourceHearing(
    override val chargeNumber: String,
  ) : ConsecutiveChainIssue

  protected data class ConsecutiveTargetAlreadyHasDependent(
    override val chargeNumber: String,
    val dependentChargeNumber: String,
  ) : ConsecutiveChainIssue

  protected fun findByChargeNumber(chargeNumber: String): ReportedAdjudication = authorizeForActiveCaseload(
    reportedAdjudicationRepository.findByChargeNumber(chargeNumber) ?: throwEntityNotFoundException(chargeNumber),
  )

  /**
   * Consecutive-punishment changes are serialized per prisoner before an individual charge is
   * locked. This stable lock order prevents opposite chain edits from deadlocking and keeps chain
   * validation valid until the transaction completes.
   */
  protected fun findByChargeNumberForUpdate(chargeNumber: String): ReportedAdjudication {
    val prisonerNumber = reportedAdjudicationRepository.findPrisonerNumberByChargeNumber(chargeNumber)
      ?: throwEntityNotFoundException(chargeNumber)
    reportedAdjudicationRepository.lockConsecutivePunishmentOperationsForPrisoner(prisonerNumber)

    return authorizeForActiveCaseload(
      reportedAdjudicationRepository.findByChargeNumberForUpdate(chargeNumber)
        ?: throwEntityNotFoundException(chargeNumber),
    )
  }

  protected fun findConsecutiveReport(chargeNumber: String): ReportedAdjudication? = reportedAdjudicationRepository.findByChargeNumber(chargeNumber)

  /**
   * Validates every exact-type target reachable from a consecutive punishment. The source
   * prisoner's mutex must be held by the caller before invoking this function.
   */
  protected fun findConsecutiveChainIssue(
    source: ReportedAdjudication,
    punishmentType: PunishmentType,
    targetChargeNumber: String,
  ): ConsecutiveChainIssue? {
    val sourceHearingDate = source.getLatestHearing()?.dateTimeOfHearing?.toLocalDate()
      ?: return MissingConsecutiveSourceHearing(source.chargeNumber)
    val reports = mutableMapOf(source.chargeNumber to source)
    fun findReport(chargeNumber: String): ReportedAdjudication? = reports[chargeNumber]
      ?: findConsecutiveReport(chargeNumber)?.also { reports[chargeNumber] = it }

    findExactTypeLoop(source.chargeNumber, targetChargeNumber, punishmentType, ::findReport)?.let {
      return ConsecutivePunishmentLoop(it)
    }

    val toValidate = ArrayDeque<ConsecutiveTarget>().apply {
      add(ConsecutiveTarget(targetChargeNumber, source.chargeNumber))
    }
    val validated = mutableSetOf<String>()
    while (toValidate.isNotEmpty()) {
      val (chargeNumber, expectedDependentChargeNumber) = toValidate.removeFirst()
      if (!validated.add(chargeNumber)) continue

      val target = findReport(chargeNumber) ?: return MissingConsecutiveTarget(chargeNumber)
      val matchingPunishments = target.getPunishments().filter {
        it.type == punishmentType && it.getSuspendedUntil() == null
      }
      val targetIsEligible = target.prisonerNumber == source.prisonerNumber &&
        target.getLatestHearing()?.dateTimeOfHearing?.toLocalDate() == sourceHearingDate &&
        target.latestOutcome()?.code == OutcomeCode.CHARGE_PROVED &&
        matchingPunishments.isNotEmpty()
      if (!targetIsEligible) return InvalidConsecutiveTarget(chargeNumber)

      chargeProvedReportsConsecutiveTo(chargeNumber, listOf(punishmentType))
        .firstOrNull { it != expectedDependentChargeNumber }
        ?.let { return ConsecutiveTargetAlreadyHasDependent(chargeNumber, it) }

      matchingPunishments.mapNotNull { it.consecutiveToChargeNumber }.forEach {
        toValidate.add(ConsecutiveTarget(it, chargeNumber))
      }
    }

    return null
  }

  private fun findExactTypeLoop(
    sourceChargeNumber: String,
    targetChargeNumber: String,
    punishmentType: PunishmentType,
    findReport: (String) -> ReportedAdjudication?,
  ): String? {
    val visitState = mutableMapOf(sourceChargeNumber to ChainVisitState.VISITING)
    val stack = ArrayDeque<ChainFrame>().apply { addLast(ChainFrame(targetChargeNumber)) }

    while (stack.isNotEmpty()) {
      val frame = stack.last()
      val state = visitState[frame.chargeNumber]
      if (state == ChainVisitState.VISITING && frame.targets == null) {
        return frame.chargeNumber
      }
      if (state == ChainVisitState.VISITED) {
        stack.removeLast()
        continue
      }

      if (frame.targets == null) {
        visitState[frame.chargeNumber] = ChainVisitState.VISITING
        frame.targets = findReport(frame.chargeNumber)
          ?.getPunishments()
          ?.filter { it.type == punishmentType && it.getSuspendedUntil() == null }
          ?.mapNotNull { it.consecutiveToChargeNumber }
          ?.iterator()
          ?: emptyList<String>().iterator()
      }

      val targets = requireNotNull(frame.targets)
      if (targets.hasNext()) {
        val nextChargeNumber = targets.next()
        when (visitState[nextChargeNumber]) {
          ChainVisitState.VISITING -> return nextChargeNumber
          ChainVisitState.VISITED -> Unit
          null -> stack.addLast(ChainFrame(nextChargeNumber))
        }
      } else {
        visitState[frame.chargeNumber] = ChainVisitState.VISITED
        stack.removeLast()
      }
    }

    return null
  }

  private fun authorizeForActiveCaseload(reportedAdjudication: ReportedAdjudication): ReportedAdjudication {
    val overrideAgencyId = reportedAdjudication.overrideAgencyId ?: reportedAdjudication.originatingAgencyId

    if (listOf(reportedAdjudication.originatingAgencyId, overrideAgencyId)
        .none { it == authenticationFacade.activeCaseload }
    ) {
      throwEntityNotFoundException(reportedAdjudication.chargeNumber)
    }

    return reportedAdjudication
  }

  protected fun saveToDto(
    reportedAdjudication: ReportedAdjudication,
    logLastModified: Boolean = true,
  ): ReportedAdjudicationDto = reportedAdjudicationRepository.save(
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

  protected fun isLinkedToChargeProvedReport(
    consecutiveChargeNumber: String,
    types: List<PunishmentType>,
  ): Boolean = chargeProvedReportsConsecutiveTo(consecutiveChargeNumber, types).isNotEmpty()

  protected fun chargeProvedReportsConsecutiveTo(
    consecutiveChargeNumber: String,
    types: List<PunishmentType>,
  ): List<String> = reportedAdjudicationRepository.findChargeProvedReportsWithActiveConsecutivePunishments(
    consecutiveChargeNumber,
    types.map { it.name },
  ).map { it.chargeNumber }.sorted()

  protected fun findMultipleOffenceCharges(prisonerNumber: String, chargeNumber: String): List<String> = reportedAdjudicationRepository.findByPrisonerNumberAndChargeNumberStartsWith(
    prisonerNumber = prisonerNumber,
    chargeNumber = "${chargeNumber.substringBefore("-")}-",
  )
    .filter { it.chargeNumber != chargeNumber }.map { it.chargeNumber }
    .sortedBy { it }

  protected fun hasLinkedAda(reportedAdjudication: ReportedAdjudication): Boolean = when (reportedAdjudication.status) {
    ReportedAdjudicationStatus.CHARGE_PROVED ->
      if (reportedAdjudication.getPunishments().none { PunishmentType.additionalDays().contains(it.type) }) {
        false
      } else {
        isLinkedToChargeProvedReport(reportedAdjudication.chargeNumber, PunishmentType.additionalDays())
      }

    else -> false
  }

  protected fun getReportCountForProfile(offenderBookingId: Long, cutOff: LocalDateTime): Long = reportedAdjudicationRepository.activeChargeProvedForBookingId(
    bookingId = offenderBookingId,
    cutOff = cutOff,
  )

  protected fun offenderHasAdjudications(offenderBookingId: Long): Boolean = reportedAdjudicationRepository.existsByOffenderBookingId(
    offenderBookingId = offenderBookingId,
  )

  protected fun getActivatedPunishments(chargeNumber: String): List<Pair<String, Punishment>> = reportedAdjudicationRepository.findByPunishmentsActivatedByChargeNumber(chargeNumber = chargeNumber).map {
    it.getPunishments().filter { p -> p.activatedByChargeNumber == chargeNumber }
      .map { toPair -> Pair(it.chargeNumber, toPair) }
  }.flatten()

  protected fun deactivateActivatedPunishments(
    chargeNumber: String,
    prisonerNumber: String,
    idsToIgnore: List<Long>,
  ): SuspendedPunishmentUpdates {
    val updatedReports = linkedMapOf<String, ReportedAdjudication>()
    val reportsWithVisitsChanges = mutableSetOf<String>()

    reportedAdjudicationRepository.findByPunishmentsActivatedByChargeNumber(chargeNumber = chargeNumber)
      .associateBy { it.chargeNumber }
      .toSortedMap()
      .values
      .forEach { report ->
        if (report.prisonerNumber != prisonerNumber) {
          throw ValidationException(
            "Unable to deactivate punishments on ${report.chargeNumber}: prisoner does not match $chargeNumber",
          )
        }
        report.getPunishments()
          .filter { p -> p.activatedByChargeNumber == chargeNumber && idsToIgnore.none { id -> id == p.id } && p.getSchedule().size > 1 }
          .forEach { punishmentToRestore ->
            if (
              PunishmentType.additionalDays().contains(punishmentToRestore.type) &&
              chargeProvedReportsConsecutiveTo(report.chargeNumber, listOf(punishmentToRestore.type))
                .any { it != chargeNumber }
            ) {
              throw ValidationException(
                "Unable to deactivate: ${punishmentToRestore.type} on ${report.chargeNumber} is linked to another report",
              )
            }

            punishmentToRestore.removeSchedule(punishmentToRestore.latestSchedule())
            punishmentToRestore.activatedByChargeNumber = null

            updatedReports[report.chargeNumber] = report
            if (punishmentToRestore.type.isVisitsPunishment()) reportsWithVisitsChanges.add(report.chargeNumber)
          }
      }

    val events = updatedReports.values.map { report ->
      SuspendedPunishmentEvent(
        agencyId = report.originatingAgencyId,
        chargeNumber = report.chargeNumber,
        status = report.status,
      )
    }.toSet()

    return SuspendedPunishmentUpdates(
      events = events,
      lossOfVisitsEvents = updatedReports.values
        .filter { reportsWithVisitsChanges.contains(it.chargeNumber) }
        .map { it.toLossOfVisitsEvent(LossOfVisitsChangeType.UPDATED) },
    )
  }

  companion object {
    fun throwEntityNotFoundException(id: String): Nothing = throw EntityNotFoundException("ReportedAdjudication not found for $id")
  }

  private data class ChainFrame(
    val chargeNumber: String,
    var targets: Iterator<String>? = null,
  )

  private data class ConsecutiveTarget(
    val chargeNumber: String,
    val expectedDependentChargeNumber: String,
  )

  private enum class ChainVisitState { VISITING, VISITED }
}
