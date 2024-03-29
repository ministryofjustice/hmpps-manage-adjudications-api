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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService.Companion.latestOutcome
import java.lang.IllegalStateException
import java.time.LocalDate
import java.time.LocalDateTime
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
  var locationId: Long,
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
) :
  BaseEntity() {
  fun transition(to: ReportedAdjudicationStatus, reason: String? = null, details: String? = null, reviewUserId: String? = null) {
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

  fun removeActivatedByLink(activatedFrom: String) {
    this.getPunishments().filter { p -> p.activatedByChargeNumber == activatedFrom }.forEach { p -> p.activatedByChargeNumber = null }
  }

  fun addPunishment(punishment: Punishment) = this.punishments.add(punishment)

  fun clearPunishments() = this.punishments.clear()

  private fun Hearing?.isAdjourn() = this?.hearingOutcome?.code == HearingOutcomeCode.ADJOURN

  companion object {
    fun ReportedAdjudication.isInvalidSuspended(): Boolean =
      this.latestOutcome()?.code == OutcomeCode.CHARGE_PROVED && this.getPunishments().any { it.isCorrupted() }
    fun ReportedAdjudication.isInvalidAda(): Boolean =
      listOf(OutcomeCode.CHARGE_PROVED, OutcomeCode.QUASHED).none { it == this.latestOutcome()?.code } && this.getPunishments().any { it.type == PunishmentType.ADDITIONAL_DAYS }
    fun ReportedAdjudication.isInvalidOutcome(): Boolean =
      this.getOutcomes().count { listOf(OutcomeCode.DISMISSED, OutcomeCode.CHARGE_PROVED, OutcomeCode.NOT_PROCEED).contains(it.code) } > 1 ||
        this.latestOutcome()?.code == OutcomeCode.PROSECUTION && this.getPunishments().isNotEmpty()
    fun ReportedAdjudication.isActivePrisoner(): Boolean = !this.migratedInactivePrisoner
    fun List<Outcome>.getOutcomeToRemove() = this.maxBy { it.getCreatedDateTime()!! }
    fun Punishment.isCorrupted(): Boolean =
      this.suspendedUntil != null && this.actualCreatedDate?.toLocalDate()?.isEqual(this.suspendedUntil) == true && this.actualCreatedDate?.toLocalDate()?.isAfter(LocalDate.now().minusMonths(6)) == true
  }
}

@Schema(description = "reported adjudication status codes")
enum class ReportedAdjudicationStatus {
  @Deprecated("this is no longer used and remains for historic purposes - enabling for int tests phase 1")
  ACCEPTED,
  REJECTED,
  AWAITING_REVIEW {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(UNSCHEDULED, REJECTED, RETURNED, AWAITING_REVIEW, ACCEPTED) // Accepted re-enabled for phase 1 testing
    }
  },
  RETURNED {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(AWAITING_REVIEW, UNSCHEDULED, REJECTED)
    }
  },
  UNSCHEDULED {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(SCHEDULED, REFER_POLICE, NOT_PROCEED)
    }
  },
  SCHEDULED {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(UNSCHEDULED, REFER_POLICE, REFER_INAD, REFER_GOV, DISMISSED, NOT_PROCEED, CHARGE_PROVED, ADJOURNED)
    }
  },
  REFER_POLICE {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(PROSECUTION, NOT_PROCEED, SCHEDULED)
    }
  },
  REFER_INAD {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(NOT_PROCEED, SCHEDULED, REFER_GOV)
    }
  },
  REFER_GOV {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(NOT_PROCEED, SCHEDULED)
    }
  },
  PROSECUTION,
  DISMISSED,
  NOT_PROCEED,
  ADJOURNED {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(SCHEDULED)
    }
  },
  CHARGE_PROVED {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(QUASHED)
    }
  },
  QUASHED,
  INVALID_OUTCOME,
  INVALID_SUSPENDED {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(CHARGE_PROVED, QUASHED)
    }
  },
  INVALID_ADA {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(CHARGE_PROVED, QUASHED)
    }
  }, ;

  open fun nextStates(): List<ReportedAdjudicationStatus> = listOf()
  fun canTransitionFrom(from: ReportedAdjudicationStatus): Boolean {
    val to = this
    return from.nextStates().contains(to)
  }
  fun canTransitionTo(to: ReportedAdjudicationStatus): Boolean {
    val from = this
    return from.nextStates().contains(to)
  }
  fun isAccepted(): Boolean {
    return this == UNSCHEDULED
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
