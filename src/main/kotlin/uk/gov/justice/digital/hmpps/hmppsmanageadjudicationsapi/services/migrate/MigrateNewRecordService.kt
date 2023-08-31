package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.ChargeNumberMapping
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.HearingMapping
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.MigrateResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.PunishmentMapping
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DisIssued
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateHearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateHearingResult
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigratePrisoner
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigratePunishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.NomisGender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DisIssueHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentComment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicSanctionCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Plea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Status
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftAdjudicationService
import java.time.LocalDateTime

@Transactional
@Service
class MigrateNewRecordService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {
  fun accept(adjudicationMigrateDto: AdjudicationMigrateDto): MigrateResponse {
    val chargeNumber = adjudicationMigrateDto.getChargeNumber()
    val punishmentsAndComments = adjudicationMigrateDto.punishments.toPunishments()
    val punishments = punishmentsAndComments.first
    val punishmentComments = punishmentsAndComments.second
    val isYoi = adjudicationMigrateDto.offence.getIsYouthOffender()

    val hearingsAndResultsAndOutcomes = adjudicationMigrateDto.hearings.sortedBy { it.hearingDateTime }.toHearingsAndResultsAndOutcomes(
      agencyId = adjudicationMigrateDto.agencyId,
      chargeNumber = chargeNumber,
      isYoi = isYoi,
    )

    val hearingsAndResults = hearingsAndResultsAndOutcomes.first
    val outcomes = hearingsAndResultsAndOutcomes.second

    val disIssued = adjudicationMigrateDto.disIssued.toDisIssue()

    val reportedAdjudication = ReportedAdjudication(
      chargeNumber = chargeNumber,
      agencyIncidentId = adjudicationMigrateDto.agencyIncidentId,
      prisonerNumber = adjudicationMigrateDto.prisoner.prisonerNumber,
      offenderBookingId = adjudicationMigrateDto.bookingId,
      originatingAgencyId = adjudicationMigrateDto.agencyId,
      overrideAgencyId = adjudicationMigrateDto.getOverrideAgencyId(),
      dateTimeOfDiscovery = adjudicationMigrateDto.incidentDateTime,
      dateTimeOfIncident = adjudicationMigrateDto.incidentDateTime,
      incidentRoleCode = null,
      incidentRoleAssociatedPrisonersNumber = null,
      incidentRoleAssociatedPrisonersName = null,
      damages = adjudicationMigrateDto.damages.toDamages(),
      evidence = adjudicationMigrateDto.evidence.toEvidence(),
      witnesses = adjudicationMigrateDto.witnesses.toWitnesses(),
      disIssueHistory = disIssued.third,
      dateTimeOfIssue = disIssued.second,
      issuingOfficer = disIssued.first,
      status = ReportedAdjudicationStatus.UNSCHEDULED,
      handoverDeadline = DraftAdjudicationService.daysToActionFromIncident(adjudicationMigrateDto.incidentDateTime),
      gender = adjudicationMigrateDto.prisoner.getGender(),
      hearings = hearingsAndResults.toMutableList(),
      isYouthOffender = isYoi,
      locationId = adjudicationMigrateDto.locationId,
      outcomes = outcomes.toMutableList(),
      statement = adjudicationMigrateDto.statement,
      offenceDetails = mutableListOf(adjudicationMigrateDto.offence.getOffenceDetails()),
      migrated = true,
      punishments = punishments.toMutableList(),
      punishmentComments = punishmentComments.toMutableList(),
    ).also {
      it.calculateStatus()
    }

    val saved = reportedAdjudicationRepository.save(reportedAdjudication).also {
      it.createDateTime = adjudicationMigrateDto.reportedDateTime
      it.createdByUserId = adjudicationMigrateDto.reportingOfficer.username
    }

    return MigrateResponse(
      chargeNumberMapping = adjudicationMigrateDto.toChargeMapping(chargeNumber),
      hearingMappings = saved.hearings.toHearingMappings(),
      punishmentMappings = saved.getPunishments().toPunishmentMappings(adjudicationMigrateDto.bookingId),
    )
  }

  companion object {

    fun List<DisIssued>.toDisIssue(): Triple<String?, LocalDateTime?, MutableList<DisIssueHistory>> {
      val history = this.sortedByDescending { it.dateTimeOfIssue }.toMutableList()
      val latest = history.removeFirstOrNull()
      val issuedBy = latest?.issuingOfficer
      val issuedOn = latest?.dateTimeOfIssue

      return Triple(
        issuedBy,
        issuedOn,
        history.map {
          DisIssueHistory(issuingOfficer = it.issuingOfficer, dateTimeOfIssue = it.dateTimeOfIssue)
        }.toMutableList(),
      )
    }

    fun createAdjourn(adjudicator: String?): HearingOutcome =
      HearingOutcome(
        code = HearingOutcomeCode.ADJOURN,
        adjudicator = adjudicator ?: "",
        plea = HearingOutcomePlea.NOT_ASKED,
        reason = HearingOutcomeAdjournReason.OTHER,
        details = "No hearing result created in NOMIS",
        migrated = true,
      )

    fun AdjudicationMigrateDto.toChargeMapping(chargeNumber: String) = ChargeNumberMapping(
      oicIncidentId = this.oicIncidentId,
      chargeNumber = chargeNumber,
      offenceSequence = this.offenceSequence,
    )

    fun List<Hearing>.toHearingMappings() = this.map {
      HearingMapping(hearingId = it.id!!, oicHearingId = it.oicHearingId!!)
    }

    fun List<Punishment>.toPunishmentMappings(offenderBookingId: Long) = this.map {
      PunishmentMapping(punishmentId = it.id!!, sanctionSeq = it.sanctionSeq, bookingId = offenderBookingId)
    }

    fun AdjudicationMigrateDto.getChargeNumber(): String = "${this.oicIncidentId}-${this.offenceSequence}"

    fun MigrateOffence.getIsYouthOffender(): Boolean = this.offenceCode.startsWith("55:")

    fun MigratePrisoner.getGender(): Gender =
      when (this.gender) {
        NomisGender.F.name -> Gender.FEMALE
        else -> Gender.MALE
      }

    fun AdjudicationMigrateDto.getOverrideAgencyId(): String? {
      this.prisoner.currentAgencyId ?: return null

      return if (this.agencyId != this.prisoner.currentAgencyId) this.prisoner.currentAgencyId else null
    }

    fun List<MigrateDamage>.toDamages(): MutableList<ReportedDamage> =
      this.map {
        ReportedDamage(
          code = it.damageType,
          details = it.details ?: "No recorded details",
          reporter = it.createdBy,
          repairCost = it.repairCost?.toDouble(),
          dateAdded = it.dateAdded,
        )
      }.toMutableList()

    fun List<MigrateEvidence>.toEvidence(): MutableList<ReportedEvidence> =
      this.map {
        ReportedEvidence(
          code = it.evidenceCode,
          details = it.details,
          reporter = it.reporter,
          dateAdded = it.dateAdded,
        )
      }.toMutableList()

    fun List<MigrateWitness>.toWitnesses(): MutableList<ReportedWitness> =
      this.map {
        ReportedWitness(
          firstName = it.firstName,
          lastName = it.lastName,
          reporter = it.createdBy,
          code = it.witnessType,
          username = it.username,
          comment = it.comment,
          dateAdded = it.dateAdded,
        )
      }.toMutableList()

    fun MigrateOffence.getOffenceDetails(): ReportedOffence {
      return ReportedOffence(
        offenceCode = 0,
        victimPrisonersNumber = null,
        victimStaffUsername = null,
        nomisOffenceCode = this.offenceCode,
        nomisOffenceDescription = this.offenceDescription,
      )
    }

    private fun OicHearingType.handleGov(isYoi: Boolean): OicHearingType =
      when (this) {
        OicHearingType.GOV -> if (isYoi) OicHearingType.GOV_YOI else OicHearingType.GOV_ADULT
        else -> this
      }

    fun List<MigrateHearing>.toHearingsAndResultsAndOutcomes(agencyId: String, chargeNumber: String, isYoi: Boolean): Pair<List<Hearing>, List<Outcome>> {
      this.validate()

      val hearingsAndResults = mutableListOf<Hearing>()
      val outcomes = mutableListOf<Outcome>()
      for ((index, oicHearing) in this.withIndex()) {
        val hasAdditionalHearings = index < this.size - 1
        val hasAdditionalHearingOutcomes = this.hasAdditionalOutcomesAndFinalOutcomeIsNotQuashed(index)

        val hearingOutcomeAndOutcome = when (oicHearing.hearingResult) {
          null -> if (hasAdditionalHearings) Pair(createAdjourn(oicHearing.adjudicator), null) else null
          else -> {
            val hearingOutcomeCode = oicHearing.hearingResult.finding.mapToHearingOutcomeCode(hasAdditionalHearingOutcomes)

            Pair(
              HearingOutcome(
                code = hearingOutcomeCode,
                adjudicator = oicHearing.adjudicator ?: "",
                plea = oicHearing.hearingResult.plea.mapToPlea(),
              ),
              oicHearing.hearingResult.mapToOutcome(hearingOutcomeCode),
            )
          }
        }

        hearingOutcomeAndOutcome?.second.let {
          it?.let { outcome ->
            outcomes.add(outcome)
            oicHearing.hearingResult!!.createAdditionalOutcome(hasAdditionalHearings)?.let { additionalOutcome ->
              outcomes.add(additionalOutcome)
            }
          }
        }

        hearingsAndResults.add(
          Hearing(
            dateTimeOfHearing = oicHearing.hearingDateTime,
            locationId = oicHearing.locationId,
            oicHearingType = oicHearing.oicHearingType.handleGov(isYoi),
            oicHearingId = oicHearing.oicHearingId,
            agencyId = agencyId,
            chargeNumber = chargeNumber,
            hearingOutcome = hearingOutcomeAndOutcome?.first,
            representative = oicHearing.representative,
            comment = oicHearing.commentText,
          ),
        )
      }

      return Pair(hearingsAndResults, outcomes)
    }

    fun List<MigratePunishment>.toPunishments(): Pair<List<Punishment>, List<PunishmentComment>> {
      val punishments = mutableListOf<Punishment>()
      val punishmentComments = mutableListOf<PunishmentComment>()

      this.forEach { sanction ->

        punishments.add(sanction.mapToPunishment())

        sanction.comment?.let {
          punishmentComments.add(PunishmentComment(comment = it))
        }
      }

      return Pair(punishments, punishmentComments)
    }

    fun List<MigrateHearing>.hasAdditionalOutcomesAndFinalOutcomeIsNotQuashed(index: Int): Boolean =
      index < this.size - 1 && this.none { it.hearingResult == null } && this.last().hearingResult?.finding != Finding.QUASHED.name

    /*
       Note: this is a placeholder, awaiting further discovery of nomis data to expand on rules
       Currently allows REF_POLICE and QUASHED to be processed, pending discovery
     */
    private fun List<MigrateHearing>.validate() {
      val listOfExceptionStatus = listOf(Finding.PROVED.name, Finding.D.name, Finding.NOT_PROCEED.name).toMutableList()
      if (this.count { it.hearingResult != null } < 2) return
      if (this.filter { it.hearingResult != null }.any { listOf(Finding.REF_POLICE.name, Finding.QUASHED.name).contains(it.hearingResult!!.finding) }) return

      val firstResult = this.first { it.hearingResult != null }

      listOfExceptionStatus.removeIf { it == firstResult.hearingResult!!.finding }

      if (this.filter { it.hearingResult != null }.any { listOfExceptionStatus.contains(it.hearingResult!!.finding) }) {
        throw UnableToMigrateException("Currently unable to migrate due to results structure")
      }
    }

    fun MigrateHearingResult.mapToOutcome(hearingOutcomeCode: HearingOutcomeCode): Outcome? =
      when (hearingOutcomeCode) {
        HearingOutcomeCode.ADJOURN, HearingOutcomeCode.NOMIS -> null
        else -> Outcome(code = this.finding.mapToOutcomeCode(), actualCreatedDate = this.createdDateTime)
      }

    private fun String.mapToOutcomeCode(): OutcomeCode = when (this) {
      Finding.PROVED.name, Finding.QUASHED.name -> OutcomeCode.CHARGE_PROVED
      Finding.D.name -> OutcomeCode.DISMISSED
      Finding.NOT_PROCEED.name -> OutcomeCode.NOT_PROCEED
      Finding.REF_POLICE.name, Finding.PROSECUTED.name -> OutcomeCode.REFER_POLICE
      else -> throw UnableToMigrateException("issue with outcome code mapping $this")
    }

    fun MigrateHearingResult.createAdditionalOutcome(hasAdditionalHearings: Boolean): Outcome? = when (this.finding) {
      Finding.QUASHED.name -> Outcome(code = OutcomeCode.QUASHED, actualCreatedDate = this.createdDateTime.plusMinutes(1))
      Finding.PROSECUTED.name -> Outcome(code = OutcomeCode.PROSECUTION, actualCreatedDate = this.createdDateTime.plusMinutes(1))
      Finding.REF_POLICE.name -> if (hasAdditionalHearings) Outcome(code = OutcomeCode.SCHEDULE_HEARING, actualCreatedDate = this.createdDateTime.plusMinutes(1)) else null
      else -> null
    }

    fun String.mapToHearingOutcomeCode(hasAdditionalHearingOutcomes: Boolean): HearingOutcomeCode = when (this) {
      Finding.QUASHED.name -> HearingOutcomeCode.COMPLETE // TODO further discovery around nomis UNQUASHED
      Finding.PROVED.name, Finding.D.name, Finding.NOT_PROCEED.name ->
        if (hasAdditionalHearingOutcomes) HearingOutcomeCode.ADJOURN else HearingOutcomeCode.COMPLETE
      Finding.PROSECUTED.name, Finding.REF_POLICE.name -> HearingOutcomeCode.REFER_POLICE
      else -> throw UnableToMigrateException("To confirm default with John, given appeals and other such statuses")
    }

    private fun String.mapToPlea(): HearingOutcomePlea = when (this) {
      Plea.NOT_GUILTY.name -> HearingOutcomePlea.NOT_GUILTY
      Plea.GUILTY.name -> HearingOutcomePlea.GUILTY
      Plea.NOT_ASKED.name -> HearingOutcomePlea.NOT_ASKED
      Plea.UNFIT.name -> HearingOutcomePlea.UNFIT
      Plea.REFUSED.name -> HearingOutcomePlea.ABSTAIN
      else -> TODO("TO confirm with John default case, and issue where result can also be a plea")
    }

    private fun MigratePunishment.mapToPunishment(): Punishment {
      val prospectiveStatuses = listOf(Status.PROSPECTIVE.name, Status.SUSP_PROSP.name)
      val typesWithoutDates = PunishmentType.additionalDays().plus(PunishmentType.CAUTION)
      val type = when (this.sanctionCode) {
        OicSanctionCode.ADA.name -> if (prospectiveStatuses.contains(this.sanctionStatus)) PunishmentType.PROSPECTIVE_DAYS else PunishmentType.ADDITIONAL_DAYS
        OicSanctionCode.EXTRA_WORK.name -> PunishmentType.EXCLUSION_WORK
        OicSanctionCode.EXTW.name -> PunishmentType.EXTRA_WORK
        OicSanctionCode.CAUTION.name -> PunishmentType.CAUTION
        OicSanctionCode.CC.name -> PunishmentType.CONFINEMENT
        OicSanctionCode.REMACT.name -> PunishmentType.REMOVAL_ACTIVITY
        OicSanctionCode.REMWIN.name -> PunishmentType.REMOVAL_WING
        OicSanctionCode.STOP_PCT.name -> PunishmentType.EARNINGS
        else -> PunishmentType.PRIVILEGE
      }

      val suspendedUntil = when (this.sanctionStatus) {
        Status.SUSPENDED.name, Status.SUSP_PROSP.name -> this.effectiveDate
        else -> null
      }

      val startDate = when (this.sanctionStatus) {
        Status.SUSPENDED.name, Status.SUSP_PROSP.name -> null
        else -> if (typesWithoutDates.contains(type)) null else this.effectiveDate
      }

      val endDate = when (this.sanctionStatus) {
        Status.SUSPENDED.name, Status.SUSP_PROSP.name -> null
        else -> if (typesWithoutDates.contains(type)) null else this.effectiveDate.plusDays((this.days ?: 0).toLong())
      }

      val stoppagePercentage = when (type) {
        PunishmentType.EARNINGS -> this.compensationAmount
        else -> null
      }

      val privilegeType = when (type) {
        PunishmentType.PRIVILEGE -> PrivilegeType.OTHER
        else -> null
      }

      val otherPrivilege = when (type) {
        PunishmentType.PRIVILEGE -> this.sanctionCode
        else -> null
      }

      return Punishment(
        type = type,
        nomisStatus = this.sanctionStatus,
        consecutiveChargeNumber = this.consecutiveChargeNumber,
        stoppagePercentage = stoppagePercentage?.toInt(),
        sanctionSeq = this.sanctionSeq,
        suspendedUntil = suspendedUntil,
        privilegeType = privilegeType,
        otherPrivilege = otherPrivilege,
        schedule = mutableListOf(
          PunishmentSchedule(days = this.days ?: 0, startDate = startDate, endDate = endDate, suspendedUntil = suspendedUntil),
        ),
      )
    }
  }
}
