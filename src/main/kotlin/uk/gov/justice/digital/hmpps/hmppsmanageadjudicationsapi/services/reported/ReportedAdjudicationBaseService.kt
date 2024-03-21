package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.CombinedOutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DisIssueHistoryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingOutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OutcomeHistoryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentCommentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedDamageDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedEvidenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedWitnessDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DisIssueHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentComment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.IncidentRoleRuleLookup
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodes
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsService.Companion.getSuspendedPunishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsService.Companion.latestSchedule
import java.time.LocalDate
import java.time.LocalDateTime

open class ReportedDtoService(
  protected val offenceCodeLookupService: OffenceCodeLookupService,
) {

  protected fun ReportedAdjudication.toDto(
    activeCaseload: String? = null,
    consecutiveReportsAvailable: List<String>? = null,
    hasLinkedAda: Boolean = false,
    linkedChargeNumbers: List<String> = emptyList(),
    isAlo: Boolean = false,
  ): ReportedAdjudicationDto {
    val hearings = this.hearings.toHearings()
    val outcomes = this.getOutcomes().createCombinedOutcomes(hasLinkedAda = hasLinkedAda)
    return ReportedAdjudicationDto(
      chargeNumber = chargeNumber,
      prisonerNumber = prisonerNumber,
      incidentDetails = IncidentDetailsDto(
        locationId = locationId,
        dateTimeOfIncident = dateTimeOfIncident,
        dateTimeOfDiscovery = dateTimeOfDiscovery,
        handoverDeadline = handoverDeadline,
      ),
      isYouthOffender = isYouthOffender,
      incidentRole = IncidentRoleDto(
        roleCode = incidentRoleCode,
        offenceRule = IncidentRoleRuleLookup.getOffenceRuleDetails(incidentRoleCode, isYouthOffender),
        associatedPrisonersNumber = incidentRoleAssociatedPrisonersNumber,
        associatedPrisonersName = incidentRoleAssociatedPrisonersName,
      ),
      offenceDetails = this.toReportedOffence(offenceCodeLookupService),
      incidentStatement = IncidentStatementDto(
        statement = statement,
        completed = true,
      ),
      createdByUserId = createdByUserId!!,
      createdDateTime = createDateTime!!,
      reviewedByUserId = reviewUserId,
      damages = this.damages.toReportedDamages(),
      evidence = this.evidence.toReportedEvidence(),
      witnesses = this.witnesses.toReportedWitnesses(),
      status = status,
      statusReason = statusReason,
      statusDetails = statusDetails,
      hearings = hearings,
      issuingOfficer = issuingOfficer,
      dateTimeOfIssue = dateTimeOfIssue,
      disIssueHistory = this.disIssueHistory.toDisIssueHistory(),
      gender = gender,
      dateTimeOfFirstHearing = dateTimeOfFirstHearing,
      outcomes = createOutcomeHistory(hearings.toMutableList(), outcomes.toMutableList()),
      punishments = this.getPunishments().toPunishments(consecutiveReportsAvailable, hasLinkedAda),
      punishmentComments = this.punishmentComments.toPunishmentComments(),
      outcomeEnteredInNomis = hearings.any { it.outcome?.code == HearingOutcomeCode.NOMIS },
      overrideAgencyId = this.overrideAgencyId,
      originatingAgencyId = this.originatingAgencyId,
      transferableActionsAllowed = this.isActionable(activeCaseload),
      createdOnBehalfOfOfficer = this.createdOnBehalfOfOfficer,
      createdOnBehalfOfReason = this.createdOnBehalfOfReason,
      linkedChargeNumbers = linkedChargeNumbers,
      canActionFromHistory = activeCaseload != null && isAlo && listOf(this.originatingAgencyId, this.overrideAgencyId).contains(activeCaseload),
    )
  }

  protected fun ReportedAdjudication.getOutcomeHistory(): List<OutcomeHistoryDto> =
    createOutcomeHistory(this.hearings.toHearings().toMutableList(), this.getOutcomes().createCombinedOutcomes(false).toMutableList())

  private fun createOutcomeHistory(hearings: MutableList<HearingDto>, outcomes: MutableList<CombinedOutcomeDto>): List<OutcomeHistoryDto> {
    if (hearings.isEmpty() && outcomes.isEmpty()) return listOf()
    if (outcomes.isEmpty()) return hearings.map { OutcomeHistoryDto(hearing = it) }
    if (hearings.isEmpty()) return outcomes.map { OutcomeHistoryDto(outcome = it) }

    val history = mutableListOf<OutcomeHistoryDto>()
    val referPoliceOutcomeCount = outcomes.count { it.outcome.code == OutcomeCode.REFER_POLICE }
    val referPoliceHearingOutcomeCount = hearings.count { it.outcome?.code == HearingOutcomeCode.REFER_POLICE }

    // special case.  if we have more refer police outcomes than hearing outcomes, it means the first action was to refer to police
    if (referPoliceOutcomeCount > referPoliceHearingOutcomeCount) {
      history.add(OutcomeHistoryDto(outcome = outcomes.removeFirst()))
    }

    do {
      val hearing = if (outcomes.firstOrNull().isScheduleHearing()) null else hearings.removeFirst()
      val outcome = if (hearing != null && hearing.hearingHasNoAssociatedOutcome()) null else outcomes.removeFirstOrNull()

      history.add(
        OutcomeHistoryDto(hearing = hearing, outcome = outcome),
      )
    } while (hearings.isNotEmpty())

    // quashed or referral when removing a next steps scheduled hearing, due to 1 more outcome than hearing
    outcomes.removeFirstOrNull()?.let {
      history.add(
        OutcomeHistoryDto(outcome = it),
      )
    }

    return history.toList()
  }

  protected fun List<Outcome>.createCombinedOutcomes(hasLinkedAda: Boolean): List<CombinedOutcomeDto> {
    if (this.isEmpty()) return emptyList()

    val combinedOutcomes = mutableListOf<CombinedOutcomeDto>()
    val orderedOutcomes = this.sortedBy { it.getCreatedDateTime() }.toMutableList()

    do {
      val outcome = orderedOutcomes.removeFirst()
      when (outcome.code) {
        OutcomeCode.REFER_POLICE, OutcomeCode.REFER_INAD, OutcomeCode.REFER_GOV -> {
          // a referral can only ever be followed by a referral outcome, or nothing (ie referral is current final state)
          val referralOutcome = orderedOutcomes.removeFirstOrNull()

          combinedOutcomes.add(
            CombinedOutcomeDto(
              outcome = outcome.toOutcomeDto(hasLinkedAda = hasLinkedAda && outcome.code == OutcomeCode.CHARGE_PROVED),
              referralOutcome = referralOutcome?.toOutcomeDto(false),
            ),
          )
        }
        else -> combinedOutcomes.add(
          CombinedOutcomeDto(
            outcome = outcome.toOutcomeDto(hasLinkedAda = hasLinkedAda && outcome.code == OutcomeCode.CHARGE_PROVED),
          ),
        )
      }
    } while (orderedOutcomes.isNotEmpty())

    return combinedOutcomes
  }

  protected fun ReportedAdjudication.toReportedOffence(
    offenceCodeLookupService: OffenceCodeLookupService,
  ): OffenceDto {
    val offence = this.offenceDetails.first()

    val offenceRuleDto = when (val offenceCode = offenceCodeLookupService.getOffenceCode(offenceCode = offence.offenceCode, isYouthOffender = this.isYouthOffender)) {
      OffenceCodes.MIGRATED_OFFENCE -> OffenceRuleDto(
        paragraphNumber = offence.nomisOffenceCode!!,
        paragraphDescription = offence.nomisOffenceDescription!!,
      )
      else -> OffenceRuleDto(
        paragraphNumber = offenceCode.paragraph,
        paragraphDescription = offenceCode.paragraphDescription.getParagraphDescription(this.gender),
        nomisCode = offenceCode.getNomisCode(),
        withOthersNomisCode = offenceCode.getNomisCodeWithOthers(),
      )
    }

    return OffenceDto(
      offenceCode = offence.offenceCode,
      offenceRule = offenceRuleDto,
      victimPrisonersNumber = offence.victimPrisonersNumber,
      victimStaffUsername = offence.victimStaffUsername,
      victimOtherPersonsName = offence.victimOtherPersonsName,
    )
  }

  private fun List<ReportedDamage>.toReportedDamages(): List<ReportedDamageDto> =
    this.map {
      ReportedDamageDto(
        code = it.code,
        details = it.details,
        reporter = it.reporter,
      )
    }.toList()

  private fun List<ReportedEvidence>.toReportedEvidence(): List<ReportedEvidenceDto> =
    this.map {
      ReportedEvidenceDto(
        code = it.code,
        identifier = it.identifier,
        details = it.details,
        reporter = it.reporter,
      )
    }.toList()

  private fun List<ReportedWitness>.toReportedWitnesses(): List<ReportedWitnessDto> =
    this.map {
      ReportedWitnessDto(
        code = it.code,
        firstName = it.firstName,
        lastName = it.lastName,
        reporter = it.reporter,
      )
    }.toList()

  private fun List<Hearing>.toHearings(): List<HearingDto> =
    this.map {
      HearingDto(
        id = it.id,
        locationId = it.locationId,
        dateTimeOfHearing = it.dateTimeOfHearing,
        oicHearingType = it.oicHearingType,
        outcome = it.hearingOutcome?.toHearingOutcomeDto(),
        agencyId = it.agencyId,
      )
    }.sortedBy { it.dateTimeOfHearing }.toList()

  private fun HearingOutcome.toHearingOutcomeDto(): HearingOutcomeDto =
    HearingOutcomeDto(
      id = this.id,
      code = this.code,
      reason = this.adjournReason,
      details = this.details,
      adjudicator = this.adjudicator,
      plea = this.plea,
    )

  private fun Outcome.toOutcomeDto(hasLinkedAda: Boolean): OutcomeDto =
    OutcomeDto(
      id = this.id,
      code = this.code,
      details = this.details,
      // added due to migration - not applicable for DPS app itself
      reason = this.notProceedReason ?: if (this.code == OutcomeCode.NOT_PROCEED) NotProceedReason.OTHER else null,
      quashedReason = this.quashedReason,
      referGovReason = this.referGovReason,
      canRemove = !hasLinkedAda,
    )

  private fun List<DisIssueHistory>.toDisIssueHistory(): List<DisIssueHistoryDto> =
    this.map {
      DisIssueHistoryDto(
        issuingOfficer = it.issuingOfficer,
        dateTimeOfIssue = it.dateTimeOfIssue,
      )
    }.sortedBy { it.dateTimeOfIssue }.toList()

  protected fun List<Punishment>.toPunishments(
    consecutiveReportsAvailable: List<String>? = null,
    hasLinkedAda: Boolean = false,
  ): List<PunishmentDto> =
    this.sortedBy { it.type }.map {
      PunishmentDto(
        id = it.id,
        type = it.type,
        privilegeType = it.privilegeType,
        otherPrivilege = it.otherPrivilege,
        stoppagePercentage = it.stoppagePercentage,
        damagesOwedAmount = it.amount,
        activatedFrom = it.activatedFromChargeNumber,
        activatedBy = it.activatedByChargeNumber,
        consecutiveChargeNumber = it.consecutiveToChargeNumber,
        canRemove = !(PunishmentType.additionalDays().contains(it.type) && hasLinkedAda),
        consecutiveReportAvailable = isConsecutiveReportAvailable(it.consecutiveToChargeNumber, consecutiveReportsAvailable),
        schedule = it.schedule.maxBy { latest -> latest.createDateTime ?: LocalDateTime.now() }.toPunishmentScheduleDto(),
      )
    }

  private fun isConsecutiveReportAvailable(consecutiveChargeNumber: String?, consecutiveReportsAvailable: List<String>?): Boolean? {
    consecutiveChargeNumber ?: return null
    consecutiveReportsAvailable ?: return null
    return consecutiveReportsAvailable.any { it == consecutiveChargeNumber }
  }

  private fun PunishmentSchedule.toPunishmentScheduleDto(): PunishmentScheduleDto =
    PunishmentScheduleDto(
      days = this.days,
      startDate = this.startDate,
      endDate = this.endDate,
      suspendedUntil = this.suspendedUntil,
    )

  private fun List<PunishmentComment>.toPunishmentComments(): List<PunishmentCommentDto> =
    this.map {
      PunishmentCommentDto(
        id = it.id,
        comment = it.comment,
        reasonForChange = it.reasonForChange,
        createdByUserId = it.nomisCreatedBy ?: it.createdByUserId,
        dateTime = it.actualCreatedDate ?: it.modifiedDateTime ?: it.createDateTime,
      )
    }.sortedByDescending { it.dateTime }.toList()

  private fun ReportedAdjudication.isActionable(activeCaseload: String?): Boolean? {
    activeCaseload ?: return null
    this.overrideAgencyId ?: return null
    return when (this.status) {
      ReportedAdjudicationStatus.REJECTED, ReportedAdjudicationStatus.ACCEPTED -> false
      ReportedAdjudicationStatus.AWAITING_REVIEW, ReportedAdjudicationStatus.RETURNED -> this.originatingAgencyId == activeCaseload
      ReportedAdjudicationStatus.SCHEDULED -> this.getLatestHearing()?.agencyId == activeCaseload
      else -> this.overrideAgencyId == activeCaseload
    }
  }

  companion object {
    fun HearingDto.hearingHasNoAssociatedOutcome() =
      this.outcome == null || this.outcome.code == HearingOutcomeCode.ADJOURN

    fun CombinedOutcomeDto?.isScheduleHearing() = this?.outcome?.code == OutcomeCode.SCHEDULE_HEARING
  }
}

open class ReportedAdjudicationBaseService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  protected val authenticationFacade: AuthenticationFacade,
) : ReportedDtoService(offenceCodeLookupService) {

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

  protected fun saveToDto(reportedAdjudication: ReportedAdjudication, logLastModified: Boolean = true): ReportedAdjudicationDto =
    reportedAdjudicationRepository.save(
      reportedAdjudication.also {
        if (logLastModified) it.lastModifiedAgencyId = authenticationFacade.activeCaseload
      },
    ).toDto(
      activeCaseload = authenticationFacade.activeCaseload,
      hasLinkedAda = hasLinkedAda(reportedAdjudication),
    )

  protected fun getNextChargeNumber(agency: String): String {
    val next = reportedAdjudicationRepository.getNextChargeSequence("${agency}_CHARGE_SEQUENCE")

    return "$agency-${next.toString().padStart(6, '0')}"
  }

  protected fun findByChargeNumberIn(chargeNumbers: List<String>) = reportedAdjudicationRepository.findByChargeNumberIn(chargeNumbers)

  protected fun getReportsWithSuspendedPunishments(prisonerNumber: String) = reportedAdjudicationRepository.findByStatusAndPrisonerNumberAndPunishmentsSuspendedUntilAfter(
    status = ReportedAdjudicationStatus.CHARGE_PROVED,
    prisonerNumber = prisonerNumber,
    date = LocalDate.now().minusDays(1),
  )

  protected fun getCorruptedReportsWithSuspendedPunishmentsInLast6Months(prisonerNumber: String) =
    reportedAdjudicationRepository.findByPrisonerNumberAndStatusInAndPunishmentsSuspendedUntilAfter(
      prisonerNumber = prisonerNumber,
      statuses = ReportedAdjudicationStatus.corruptedStatuses(),
      date = LocalDate.now().minusMonths(6),
    )

  protected fun getReportsWithActiveAdditionalDays(prisonerNumber: String, punishmentType: PunishmentType) =
    reportedAdjudicationRepository.findByStatusAndPrisonerNumberAndPunishmentsTypeAndPunishmentsSuspendedUntilIsNull(
      status = ReportedAdjudicationStatus.CHARGE_PROVED,
      prisonerNumber = prisonerNumber,
      punishmentType = punishmentType,
    )

  protected fun isLinkedToReport(consecutiveChargeNumber: String, types: List<PunishmentType>): Boolean =
    reportedAdjudicationRepository.findByPunishmentsConsecutiveToChargeNumberAndPunishmentsTypeIn(consecutiveChargeNumber, types).isNotEmpty()

  protected fun getReportsWithActivePunishments(offenderBookingId: Long): List<Pair<String, List<Punishment>>> =
    reportedAdjudicationRepository.findByStatusAndOffenderBookingIdAndPunishmentsSuspendedUntilIsNullAndPunishmentsScheduleEndDateIsAfter(
      status = ReportedAdjudicationStatus.CHARGE_PROVED,
      offenderBookingId = offenderBookingId,
      cutOff = LocalDate.now().minusDays(1),
    ).map { Pair(it.chargeNumber, it.getPunishments().filter { p -> p.isActive() }) }

  protected fun getReportCountForProfile(offenderBookingId: Long, cutOff: LocalDateTime): Long =
    reportedAdjudicationRepository.countByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
      bookingId = offenderBookingId,
      status = ReportedAdjudicationStatus.CHARGE_PROVED,
      cutOff = cutOff,
    )

  protected fun offenderHasAdjudications(offenderBookingId: Long): Boolean = reportedAdjudicationRepository.existsByOffenderBookingId(
    offenderBookingId = offenderBookingId,
  )

  protected fun offenderChargesForPrintSupport(offenderBookingId: Long, chargeNumber: String): List<ReportedAdjudication> =
    reportedAdjudicationRepository.findByOffenderBookingIdAndStatus(
      offenderBookingId = offenderBookingId,
      status = ReportedAdjudicationStatus.CHARGE_PROVED,
    ).filter { it.chargeNumber != chargeNumber }

  protected fun List<Punishment>.checkAndRemoveActivatedByLinks(activatedFrom: String) {
    this.getDistinctActivatedFromLinks().forEach {
      findByChargeNumber(chargeNumber = it, ignoreSecurityCheck = true).removeActivatedByLink(activatedFrom = activatedFrom)
    }
  }

  protected fun PunishmentRequest.updateAndGetSuspendedPunishment(activatedBy: String): Punishment {
    val activatedFromReport = findByChargeNumber(chargeNumber = this.activatedFrom!!, ignoreSecurityCheck = true)
    return activatedFromReport.getPunishments().getSuspendedPunishment(this.id!!).also {
      it.activatedByChargeNumber = activatedBy
    }
  }

  companion object {
    fun throwEntityNotFoundException(id: String): Nothing =
      throw EntityNotFoundException("ReportedAdjudication not found for $id")

    fun Punishment.isActive(): Boolean =
      this.suspendedUntil == null && this.schedule.latestSchedule().endDate?.isAfter(LocalDate.now().minusDays(1)) == true

    fun List<Punishment>.getDistinctActivatedFromLinks(): List<String> =
      this.filter { it.activatedFromChargeNumber != null }.map { it.activatedFromChargeNumber!! }.distinct()
  }
}
