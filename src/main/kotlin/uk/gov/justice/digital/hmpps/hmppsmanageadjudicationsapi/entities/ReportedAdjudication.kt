package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.ValidationException
import org.hibernate.validator.constraints.Length
import org.jetbrains.annotations.TestOnly
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.CombinedOutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OutcomeHistoryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.IncidentRoleRuleLookup
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService.Companion.latestOutcome
import java.lang.IllegalStateException
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "reported_adjudications")
data class ReportedAdjudication(
  override val id: Long? = null,
  var prisonerNumber: String,
  var offenderBookingId: Long? = null,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var gender: Gender,
  @field:Length(max = 16)
  var chargeNumber: String,
  var agencyIncidentId: Long? = null,
  var originatingAgencyId: String,
  var overrideAgencyId: String? = null,
  var lastModifiedAgencyId: String? = null,
  var locationId: Long? = null,
  var locationUuid: UUID,
  var dateTimeOfIncident: LocalDateTime,
  var dateTimeOfDiscovery: LocalDateTime,
  var handoverDeadline: LocalDateTime,
  var isYouthOffender: Boolean,
  var incidentRoleCode: String?,
  var incidentRoleAssociatedPrisonersNumber: String?,
  var incidentRoleAssociatedPrisonersName: String?,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var status: ReportedAdjudicationStatus,
  @Enumerated(EnumType.STRING)
  var statusBeforeMigration: ReportedAdjudicationStatus? = null,
  @field:Length(max = 128)
  var statusReason: String? = null,
  @field:Length(max = 4000)
  var statusDetails: String? = null,
  var statement: String,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var offenceDetails: MutableList<ReportedOffence>,
  var reviewUserId: String? = null,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var damages: MutableList<ReportedDamage>,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var evidence: MutableList<ReportedEvidence>,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var witnesses: MutableList<ReportedWitness>,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var hearings: MutableList<Hearing>,
  var issuingOfficer: String? = null,
  var dateTimeOfIssue: LocalDateTime? = null,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var disIssueHistory: MutableList<DisIssueHistory>,
  var dateTimeOfFirstHearing: LocalDateTime? = null,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  private var outcomes: MutableList<Outcome>,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  private var punishments: MutableList<Punishment>,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var punishmentComments: MutableList<PunishmentComment>,
  var migrated: Boolean = false,
  @field:Length(max = 32)
  var createdOnBehalfOfOfficer: String? = null,
  @field:Length(max = 4000)
  var createdOnBehalfOfReason: String? = null,
  var migratedInactivePrisoner: Boolean = false,
  var migratedSplitRecord: Boolean = false,
  var dateTimeResubmitted: LocalDateTime? = null,
) : BaseEntity() {
  fun transition(
    to: ReportedAdjudicationStatus,
    reason: String? = null,
    details: String? = null,
    reviewUserId: String? = null,
  ) {
    if (this.status.canTransitionTo(to)) {
      this.status = to
      this.statusReason = reason
      this.statusDetails = details
      this.reviewUserId = reviewUserId
    } else {
      throw IllegalStateException("ReportedAdjudication ${this.chargeNumber} cannot transition from ${this.status} to $to")
    }
  }

  fun calculateStatus() {
    this.status = when (this.getOutcomes().isEmpty()) {
      true ->
        when (this.hearings.isEmpty()) {
          true -> ReportedAdjudicationStatus.UNSCHEDULED
          false -> {
            if (this.getLatestHearing().isAdjourn()) {
              ReportedAdjudicationStatus.ADJOURNED
            } else {
              ReportedAdjudicationStatus.SCHEDULED
            }
          }
        }

      false -> {
        if (this.getLatestHearing().isAdjourn()) {
          ReportedAdjudicationStatus.ADJOURNED
        } else {
          if (this.isActivePrisoner() && this.isInvalidAda()) {
            ReportedAdjudicationStatus.INVALID_ADA
          } else if (this.isActivePrisoner() && this.isInvalidOutcome()) {
            ReportedAdjudicationStatus.INVALID_OUTCOME
          } else if (this.isActivePrisoner() && this.isInvalidSuspended()) {
            ReportedAdjudicationStatus.INVALID_SUSPENDED
          } else {
            this.getOutcomes().sortedByDescending { it.getCreatedDateTime() }.first().code.status
          }
        }
      }
    }
  }

  fun getLatestHearing(): Hearing? = this.hearings.maxByOrNull { it.dateTimeOfHearing }

  fun addOutcome(outcome: Outcome) = this.outcomes.add(outcome)

  fun getOutcomes() = this.outcomes.filter { it.deleted != true }

  fun getOutcomeToRemove() = this.getOutcomes().getOutcomeToRemove()

  @TestOnly
  fun clearOutcomes() = this.outcomes.clear()

  fun getPunishments() = this.punishments.filter { it.deleted != true }

  fun addPunishment(punishment: Punishment) = this.punishments.add(punishment)

  fun clearPunishments() = this.punishments.clear()

  private fun Hearing?.isAdjourn() = this?.hearingOutcome?.code == HearingOutcomeCode.ADJOURN

  fun toDto(
    offenceCodeLookupService: OffenceCodeLookupService,
    activeCaseload: String? = null,
    consecutiveReportsAvailable: List<String>? = null,
    hasLinkedAda: Boolean = false,
    linkedChargeNumbers: List<String> = emptyList(),
    isAlo: Boolean = false,
  ): ReportedAdjudicationDto {
    val hearings = this.toHearingsDto()
    val outcomes = this.getOutcomes().createCombinedOutcomes(hasLinkedAda = hasLinkedAda)
    return ReportedAdjudicationDto(
      chargeNumber = chargeNumber,
      prisonerNumber = prisonerNumber,
      incidentDetails = IncidentDetailsDto(
        locationId = locationId,
        locationUuid = locationUuid,
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
      offenceDetails = this.offenceDetails.first().toDto(offenceCodeLookupService, isYouthOffender, gender),
      incidentStatement = IncidentStatementDto(
        statement = statement,
        completed = true,
      ),
      createdByUserId = createdByUserId!!,
      createdDateTime = dateTimeResubmitted ?: createDateTime!!,
      reviewedByUserId = reviewUserId,
      damages = this.damages.map { it.toDto() },
      evidence = this.evidence.map { it.toDto() },
      witnesses = this.witnesses.map { it.toDto() },
      status = status,
      statusReason = statusReason,
      statusDetails = statusDetails,
      hearings = hearings,
      issuingOfficer = issuingOfficer,
      dateTimeOfIssue = dateTimeOfIssue,
      disIssueHistory = this.disIssueHistory.map { it.toDto() }.sortedBy { it.dateTimeOfIssue },
      gender = gender,
      dateTimeOfFirstHearing = dateTimeOfFirstHearing,
      outcomes = createOutcomeHistory(hearings.toMutableList(), outcomes.toMutableList()),
      punishments = this.getPunishments().toPunishmentsDto(hasLinkedAda, consecutiveReportsAvailable),
      punishmentComments = this.punishmentComments.map { it.toDto() }.sortedByDescending { it.dateTime },
      outcomeEnteredInNomis = hearings.any { it.outcome?.code == HearingOutcomeCode.NOMIS },
      overrideAgencyId = this.overrideAgencyId,
      originatingAgencyId = this.originatingAgencyId,
      transferableActionsAllowed = this.isActionable(activeCaseload),
      createdOnBehalfOfOfficer = this.createdOnBehalfOfOfficer,
      createdOnBehalfOfReason = this.createdOnBehalfOfReason,
      linkedChargeNumbers = linkedChargeNumbers,
      canActionFromHistory = activeCaseload != null &&
        isAlo &&
        listOf(
          this.originatingAgencyId,
          this.overrideAgencyId,
        ).contains(activeCaseload),
    )
  }

  companion object {
    fun ReportedAdjudication.isInvalidSuspended(): Boolean = this.latestOutcome()?.code == OutcomeCode.CHARGE_PROVED && this.getPunishments().any { it.isCorrupted() }

    fun ReportedAdjudication.isInvalidAda(): Boolean = listOf(
      OutcomeCode.CHARGE_PROVED,
      OutcomeCode.QUASHED,
    ).none { it == this.latestOutcome()?.code } &&
      this.getPunishments()
        .any { it.type == PunishmentType.ADDITIONAL_DAYS }

    fun ReportedAdjudication.isInvalidOutcome(): Boolean = this.getOutcomes().count {
      listOf(
        OutcomeCode.DISMISSED,
        OutcomeCode.CHARGE_PROVED,
        OutcomeCode.NOT_PROCEED,
      ).contains(it.code)
    } > 1 ||
      this.latestOutcome()?.code == OutcomeCode.PROSECUTION &&
      this.getPunishments().isNotEmpty()

    fun ReportedAdjudication.isActivePrisoner(): Boolean = !this.migratedInactivePrisoner
    fun List<Outcome>.getOutcomeToRemove() = this.maxBy { it.getCreatedDateTime()!! }

    fun ReportedAdjudication.toHearingsDto() = this.hearings.map { it.toDto() }.sortedBy { it.dateTimeOfHearing }
    fun ReportedAdjudication.isActionable(activeCaseload: String?): Boolean? {
      activeCaseload ?: return null
      this.overrideAgencyId ?: return null
      return when (this.status) {
        ReportedAdjudicationStatus.REJECTED, ReportedAdjudicationStatus.ACCEPTED -> false
        ReportedAdjudicationStatus.AWAITING_REVIEW, ReportedAdjudicationStatus.RETURNED -> this.originatingAgencyId == activeCaseload
        ReportedAdjudicationStatus.SCHEDULED -> this.getLatestHearing()?.agencyId == activeCaseload
        else -> this.overrideAgencyId == activeCaseload
      }
    }

    fun List<Punishment>.toPunishmentsDto(
      hasLinkedAda: Boolean,
      consecutiveReportsAvailable: List<String>? = null,
    ): MutableList<PunishmentDto> = this
      .sortedBy { it.type }
      .map { it.toDto(hasLinkedAda, consecutiveReportsAvailable) }
      .toMutableList()

    private fun HearingDto.hearingHasNoAssociatedOutcome() = this.outcome == null || this.outcome.code == HearingOutcomeCode.ADJOURN

    private fun CombinedOutcomeDto?.isScheduleHearing() = this?.outcome?.code == OutcomeCode.SCHEDULE_HEARING

    fun ReportedAdjudication.getOutcomeHistory(): List<OutcomeHistoryDto> = createOutcomeHistory(
      this.toHearingsDto().toMutableList(),
      this.getOutcomes().createCombinedOutcomes(false).toMutableList(),
    )

    fun createOutcomeHistory(
      hearings: MutableList<HearingDto>,
      outcomes: MutableList<CombinedOutcomeDto>,
    ): List<OutcomeHistoryDto> {
      if (hearings.isEmpty() && outcomes.isEmpty()) return listOf()
      if (outcomes.isEmpty()) return hearings.map { OutcomeHistoryDto(hearing = it) }
      if (hearings.isEmpty()) return outcomes.map { OutcomeHistoryDto(outcome = it) }

      val history = mutableListOf<OutcomeHistoryDto>()
      val referPoliceOutcomeCount = outcomes.count { it.outcome.code == OutcomeCode.REFER_POLICE }
      val referPoliceHearingOutcomeCount = hearings.count { it.outcome?.code == HearingOutcomeCode.REFER_POLICE }

      // special case.  if we have more refer police outcomes than hearing outcomes, it means the first action was to refer to police
      if (referPoliceOutcomeCount > referPoliceHearingOutcomeCount) {
        history.add(OutcomeHistoryDto(outcome = outcomes.removeFirstOrNull()!!))
      }

      do {
        val hearing = if (outcomes.firstOrNull().isScheduleHearing()) null else hearings.removeFirstOrNull()
        val outcome =
          if (hearing != null && hearing.hearingHasNoAssociatedOutcome()) null else outcomes.removeFirstOrNull()

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

    fun List<Outcome>.createCombinedOutcomes(hasLinkedAda: Boolean): List<CombinedOutcomeDto> {
      if (this.isEmpty()) return emptyList()

      val combinedOutcomes = mutableListOf<CombinedOutcomeDto>()
      val orderedOutcomes = this.sortedBy { it.getCreatedDateTime() }.toMutableList()

      do {
        val outcome = orderedOutcomes.removeFirstOrNull()!!
        when (outcome.code) {
          OutcomeCode.REFER_POLICE, OutcomeCode.REFER_INAD, OutcomeCode.REFER_GOV -> {
            // a referral can only ever be followed by a referral outcome, or nothing (ie referral is current final state)
            val referralOutcome = orderedOutcomes.removeFirstOrNull()

            combinedOutcomes.add(
              CombinedOutcomeDto(
                outcome = outcome.toDto(hasLinkedAda = hasLinkedAda && outcome.code == OutcomeCode.CHARGE_PROVED),
                referralOutcome = referralOutcome?.toDto(false),
              ),
            )
          }

          else -> combinedOutcomes.add(
            CombinedOutcomeDto(
              outcome = outcome.toDto(hasLinkedAda = hasLinkedAda && outcome.code == OutcomeCode.CHARGE_PROVED),
            ),
          )
        }
      } while (orderedOutcomes.isNotEmpty())

      return combinedOutcomes
    }
  }
}

@Schema(description = "reported adjudication status codes")
enum class ReportedAdjudicationStatus {
  @Deprecated("this is no longer used and remains for historic purposes - enabling for int tests phase 1")
  ACCEPTED,
  REJECTED,
  AWAITING_REVIEW {
    override fun nextStates(): List<ReportedAdjudicationStatus> = listOf(UNSCHEDULED, REJECTED, RETURNED, AWAITING_REVIEW)
  },
  RETURNED {
    override fun nextStates(): List<ReportedAdjudicationStatus> = listOf(AWAITING_REVIEW, UNSCHEDULED, REJECTED)
  },
  UNSCHEDULED {
    override fun nextStates(): List<ReportedAdjudicationStatus> = listOf(SCHEDULED, REFER_POLICE, NOT_PROCEED)
  },
  SCHEDULED {
    override fun nextStates(): List<ReportedAdjudicationStatus> = listOf(UNSCHEDULED, REFER_POLICE, REFER_INAD, REFER_GOV, DISMISSED, NOT_PROCEED, CHARGE_PROVED, ADJOURNED)
  },
  REFER_POLICE {
    override fun nextStates(): List<ReportedAdjudicationStatus> = listOf(PROSECUTION, NOT_PROCEED, SCHEDULED)
  },
  REFER_INAD {
    override fun nextStates(): List<ReportedAdjudicationStatus> = listOf(NOT_PROCEED, SCHEDULED, REFER_GOV)
  },
  REFER_GOV {
    override fun nextStates(): List<ReportedAdjudicationStatus> = listOf(NOT_PROCEED, SCHEDULED)
  },
  PROSECUTION,
  DISMISSED,
  NOT_PROCEED,
  ADJOURNED {
    override fun nextStates(): List<ReportedAdjudicationStatus> = listOf(SCHEDULED)
  },
  CHARGE_PROVED {
    override fun nextStates(): List<ReportedAdjudicationStatus> = listOf(QUASHED)
  },
  QUASHED,
  INVALID_OUTCOME,
  INVALID_SUSPENDED {
    override fun nextStates(): List<ReportedAdjudicationStatus> = listOf(CHARGE_PROVED, QUASHED)
  },
  INVALID_ADA {
    override fun nextStates(): List<ReportedAdjudicationStatus> = listOf(CHARGE_PROVED, QUASHED)
  },
  ;

  open fun nextStates(): List<ReportedAdjudicationStatus> = listOf()
  fun canTransitionFrom(from: ReportedAdjudicationStatus): Boolean {
    val to = this
    return from.nextStates().contains(to)
  }

  fun canTransitionTo(to: ReportedAdjudicationStatus): Boolean {
    val from = this
    return from.nextStates().contains(to)
  }

  companion object {
    fun issuableStatuses() = listOf(SCHEDULED, UNSCHEDULED)
    fun issuableStatusesForPrint() = listOf(SCHEDULED)

    fun corruptedStatuses() = listOf(INVALID_SUSPENDED, INVALID_OUTCOME, INVALID_ADA)

    fun ReportedAdjudicationStatus.validateTransition(vararg next: ReportedAdjudicationStatus) {
      if (this == INVALID_OUTCOME) return
      next.toList().forEach {
        if (this != it && !this.canTransitionTo(it)) throw ValidationException("Invalid status transition ${this.name} - ${it.name}")
      }
    }
  }
}
