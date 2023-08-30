package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
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
import java.time.LocalDate
import java.time.LocalDateTime

open class ReportedDtoService(
  protected val offenceCodeLookupService: OffenceCodeLookupService,
) {

  protected fun ReportedAdjudication.toDto(activeCaseload: String? = null, consecutiveReportsAvailable: List<String>? = null): ReportedAdjudicationDto {
    val hearings = this.hearings.toHearings()
    val outcomes = this.getOutcomes().createCombinedOutcomes()
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
      punishments = this.getPunishments().toPunishments(consecutiveReportsAvailable),
      punishmentComments = this.punishmentComments.toPunishmentComments(),
      outcomeEnteredInNomis = hearings.any { it.outcome?.code == HearingOutcomeCode.NOMIS },
      overrideAgencyId = this.overrideAgencyId,
      originatingAgencyId = this.originatingAgencyId,
      transferableActionsAllowed = this.isActionable(activeCaseload),
    )
  }

  protected fun ReportedAdjudication.getOutcomeHistory(): List<OutcomeHistoryDto> =
    createOutcomeHistory(this.hearings.toHearings().toMutableList(), this.getOutcomes().createCombinedOutcomes().toMutableList())

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

  protected fun List<Outcome>.createCombinedOutcomes(): List<CombinedOutcomeDto> {
    if (this.isEmpty()) return emptyList()

    val combinedOutcomes = mutableListOf<CombinedOutcomeDto>()
    val orderedOutcomes = this.sortedBy { it.actualCreatedDate ?: it.createDateTime }.toMutableList()

    do {
      val outcome = orderedOutcomes.removeFirst()
      when (outcome.code) {
        OutcomeCode.REFER_POLICE, OutcomeCode.REFER_INAD, OutcomeCode.REFER_GOV -> {
          // a referral can only ever be followed by a referral outcome, or nothing (ie referral is current final state)
          val referralOutcome = orderedOutcomes.removeFirstOrNull()

          combinedOutcomes.add(
            CombinedOutcomeDto(
              outcome = outcome.toOutcomeDto(),
              referralOutcome = referralOutcome?.toOutcomeDto(),
            ),
          )
        }
        else -> combinedOutcomes.add(
          CombinedOutcomeDto(
            outcome = outcome.toOutcomeDto(),
          ),
        )
      }
    } while (orderedOutcomes.isNotEmpty())

    return combinedOutcomes
  }

  private fun ReportedAdjudication.toReportedOffence(
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
        oicHearingId = it.oicHearingId,
      )
    }.sortedBy { it.dateTimeOfHearing }.toList()

  private fun HearingOutcome.toHearingOutcomeDto(): HearingOutcomeDto =
    HearingOutcomeDto(
      id = this.id,
      code = this.code,
      reason = this.reason,
      details = this.details,
      adjudicator = this.adjudicator,
      plea = this.plea,
    )

  private fun Outcome.toOutcomeDto(): OutcomeDto =
    OutcomeDto(
      id = this.id,
      code = this.code,
      details = this.details,
      reason = this.reason,
      quashedReason = this.quashedReason,
    )

  private fun List<DisIssueHistory>.toDisIssueHistory(): List<DisIssueHistoryDto> =
    this.map {
      DisIssueHistoryDto(
        issuingOfficer = it.issuingOfficer,
        dateTimeOfIssue = it.dateTimeOfIssue,
      )
    }.sortedBy { it.dateTimeOfIssue }.toList()

  private fun List<Punishment>.toPunishments(consecutiveReportsAvailable: List<String>?): List<PunishmentDto> =
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
        consecutiveChargeNumber = it.consecutiveChargeNumber,
        consecutiveReportAvailable = isConsecutiveReportAvailable(it.consecutiveChargeNumber, consecutiveReportsAvailable),
        schedule = it.schedule.maxBy { latest -> latest.createDateTime ?: LocalDateTime.now() }.toPunishmentScheduleDto(),
      )
    }

  private fun isConsecutiveReportAvailable(consecutiveReportNumber: String?, consecutiveReportsAvailable: List<String>?): Boolean? {
    consecutiveReportNumber ?: return null
    consecutiveReportsAvailable ?: return null
    return consecutiveReportsAvailable.any { it == consecutiveReportNumber }
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
        createdByUserId = it.createdByUserId,
        dateTime = it.modifiedDateTime ?: it.createDateTime,
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

  protected fun saveToDto(reportedAdjudication: ReportedAdjudication): ReportedAdjudicationDto =
    reportedAdjudicationRepository.save(
      reportedAdjudication.also {
        it.lastModifiedAgencyId = authenticationFacade.activeCaseload
      },
    ).toDto(activeCaseload = authenticationFacade.activeCaseload)

  protected fun findByReportNumberIn(adjudicationNumbers: List<String>) = reportedAdjudicationRepository.findByChargeNumberIn(adjudicationNumbers)

  protected fun getReportsWithSuspendedPunishments(prisonerNumber: String) = reportedAdjudicationRepository.findByPrisonerNumberAndPunishmentsSuspendedUntilAfter(
    prisonerNumber = prisonerNumber,
    date = LocalDate.now().minusDays(1),
  )

  protected fun getReportsWithActiveAdditionalDays(prisonerNumber: String, punishmentType: PunishmentType) =
    reportedAdjudicationRepository.findByPrisonerNumberAndPunishmentsTypeAndPunishmentsSuspendedUntilIsNull(prisonerNumber, punishmentType)

  protected fun isLinkedToReport(consecutiveReportNumber: String, type: PunishmentType): Boolean =
    reportedAdjudicationRepository.findByPunishmentsConsecutiveChargeNumberAndPunishmentsType(consecutiveReportNumber, type).isNotEmpty()

  companion object {
    fun throwEntityNotFoundException(id: String): Nothing =
      throw EntityNotFoundException("ReportedAdjudication not found for $id")
  }
}
