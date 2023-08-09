package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.ChargeNumberMapping
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.HearingMapping
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.MigrateResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.PunishmentMapping
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateHearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateHearingResult
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigratePrisoner
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigratePunishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.NomisGender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicSanctionCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Plea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Status
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftAdjudicationService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.mapToOutcomeCode

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

    val hearingsAndResultsAndOutcomes = adjudicationMigrateDto.hearings.toHearingsAndResultsAndOutcomes(
      agencyId = adjudicationMigrateDto.agencyId,
      chargeNumber = chargeNumber,
    )

    val hearingsAndResults = hearingsAndResultsAndOutcomes.first
    val outcomes = hearingsAndResultsAndOutcomes.second

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
      disIssueHistory = mutableListOf(),
      status = ReportedAdjudicationStatus.UNSCHEDULED,
      handoverDeadline = DraftAdjudicationService.daysToActionFromIncident(adjudicationMigrateDto.incidentDateTime),
      gender = adjudicationMigrateDto.prisoner.getGender(),
      hearings = hearingsAndResults.toMutableList(),
      isYouthOffender = adjudicationMigrateDto.offence.getIsYouthOffender(),
      locationId = adjudicationMigrateDto.locationId,
      outcomes = outcomes.toMutableList(),
      statement = adjudicationMigrateDto.statement,
      offenceDetails = mutableListOf(adjudicationMigrateDto.offence.getOffenceDetails()),
      migrated = true,
      punishments = punishments.toMutableList(),
      punishmentComments = punishmentComments.toMutableList(),
    )

    val saved = reportedAdjudicationRepository.save(reportedAdjudication).also {
      it.createDateTime = adjudicationMigrateDto.reportedDateTime
      it.createdByUserId = adjudicationMigrateDto.reportingOfficer.username
    }

    return MigrateResponse(
      chargeNumberMapping = ChargeNumberMapping(
        oicIncidentId = adjudicationMigrateDto.oicIncidentId,
        chargeNumber = chargeNumber,
        offenceSequence = adjudicationMigrateDto.offenceSequence,
      ),
      hearingMappings = saved.hearings.map {
        HearingMapping(hearingId = it.id!!, oicHearingId = it.oicHearingId!!)
      },
      punishmentMappings = saved.getPunishments().map {
        PunishmentMapping(punishmentId = it.id!!, sanctionSeq = it.sanctionSeq!!, bookingId = adjudicationMigrateDto.bookingId)
      },
    )
  }

  companion object {

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
        )
      }.toMutableList()

    fun List<MigrateEvidence>.toEvidence(): MutableList<ReportedEvidence> =
      this.map {
        ReportedEvidence(
          code = it.evidenceCode,
          details = it.details,
          reporter = it.reporter,
        )
      }.toMutableList()

    fun List<MigrateWitness>.toWitnesses(): MutableList<ReportedWitness> =
      this.map {
        ReportedWitness(
          firstName = it.firstName,
          lastName = it.lastName,
          reporter = it.createdBy,
          code = it.witnessType,
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

    fun List<MigrateHearing>.toHearingsAndResultsAndOutcomes(agencyId: String, chargeNumber: String): Pair<List<Hearing>, List<Outcome>> {
      val hearingsAndResults = mutableListOf<Hearing>()
      val outcomes = mutableListOf<Outcome>()

      this.forEach {
          oicHearing ->
        val hearingOutcomeAndOutcome = when (oicHearing.hearingResult) {
          null -> null
          else -> {
            val hearingOutcomeCode = oicHearing.hearingResult.finding.mapToHearingOutcomeCode()

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
          }
        }

        hearingsAndResults.add(
          Hearing(
            dateTimeOfHearing = oicHearing.hearingDateTime,
            locationId = oicHearing.locationId,
            oicHearingType = oicHearing.oicHearingType,
            oicHearingId = oicHearing.oicHearingId,
            agencyId = agencyId,
            chargeNumber = chargeNumber,
            hearingOutcome = hearingOutcomeAndOutcome?.first,
          ),
        )
      }

      return Pair(hearingsAndResults, outcomes)
    }

    private fun MigrateHearingResult.mapToOutcome(hearingOutcomeCode: HearingOutcomeCode): Outcome? =
      when (hearingOutcomeCode) {
        HearingOutcomeCode.ADJOURN, HearingOutcomeCode.NOMIS -> null
        else -> Outcome(code = this.finding.mapToOutcomeCode(), actualCreatedDate = this.createdDateTime)
      }

    private fun String.mapToOutcomeCode(): OutcomeCode = when (this) {
      Finding.PROVED.name -> OutcomeCode.CHARGE_PROVED
      Finding.D.name -> OutcomeCode.DISMISSED
      Finding.NOT_PROCEED.name -> OutcomeCode.NOT_PROCEED
      Finding.REF_POLICE.name -> OutcomeCode.REFER_POLICE
      else -> throw UnableToMigrateException("issue with outcome code mapping $this")
    }

    private fun String.mapToHearingOutcomeCode(): HearingOutcomeCode = when (this) {
      Finding.PROVED.name, Finding.QUASHED.name, Finding.D.name, Finding.NOT_PROCEED.name -> HearingOutcomeCode.COMPLETE
      Finding.PROSECUTED.name, Finding.REF_POLICE.name -> HearingOutcomeCode.REFER_POLICE
      else -> HearingOutcomeCode.ADJOURN // for now we adjourn.  not appeal and so on.
    }

    private fun String.mapToPlea(): HearingOutcomePlea = when (this) {
      Plea.NOT_GUILTY.name -> HearingOutcomePlea.NOT_GUILTY
      Plea.GUILTY.name -> HearingOutcomePlea.GUILTY
      Plea.NOT_ASKED.name -> HearingOutcomePlea.NOT_ASKED
      Plea.UNFIT.name -> HearingOutcomePlea.UNFIT
      Plea.REFUSED.name -> HearingOutcomePlea.ABSTAIN
      else -> HearingOutcomePlea.NOT_ASKED // TO confirm with John
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
