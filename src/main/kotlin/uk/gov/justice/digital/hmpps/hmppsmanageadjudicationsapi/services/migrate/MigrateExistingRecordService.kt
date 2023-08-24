package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.MigrateResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateHearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigratePunishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePreMigrate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingPreMigrate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicSanctionCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Status
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodes
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.createAdditionalOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.createAdjourn
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.hasAdditionalOutcomesAndFinalOutcomeIsNotQuashed
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.mapToHearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.mapToOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toChargeMapping
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toDamages
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toHearingMappings
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toHearingsAndResultsAndOutcomes
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toPunishmentMappings
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toPunishments
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toWitnesses

@Transactional
@Service
class MigrateExistingRecordService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {
  fun accept(adjudicationMigrateDto: AdjudicationMigrateDto, existingAdjudication: ReportedAdjudication): MigrateResponse {
    if (adjudicationMigrateDto.prisoner.prisonerNumber != existingAdjudication.prisonerNumber) throw ExistingRecordConflictException("Prisoner different between nomis and adjudications")
    if (adjudicationMigrateDto.agencyId != existingAdjudication.originatingAgencyId) throw ExistingRecordConflictException("agency different between nomis and adjudications")

    existingAdjudication.offenderBookingId = adjudicationMigrateDto.bookingId
    existingAdjudication.statusBeforeMigration = existingAdjudication.status

    if (OffenceCodes.findByNomisCode(adjudicationMigrateDto.offence.offenceCode).none { it.uniqueOffenceCodes.contains(existingAdjudication.offenceDetails.first().offenceCode) }) {
      existingAdjudication.offenceDetails.first().update(adjudicationMigrateDto)
    }

    if (existingAdjudication.status == ReportedAdjudicationStatus.ACCEPTED) {
      existingAdjudication.processPhase1(adjudicationMigrateDto)
    } else if (existingAdjudication.hearings.containsNomisHearingOutcomeCode()) {
      existingAdjudication.processPhase2(adjudicationMigrateDto)
    }

    if (existingAdjudication.hearings.hasHearingWithoutResult()) {
      existingAdjudication.hearings.sortedBy { it.dateTimeOfHearing }.forEachIndexed { index, hearing ->
        if (index != existingAdjudication.hearings.size - 1 && hearing.hearingOutcome == null) {
          hearing.hearingOutcome = createAdjourn(null)
        }
      }
    }

    if (existingAdjudication.status != ReportedAdjudicationStatus.ACCEPTED) {
      existingAdjudication.processPhase3(adjudicationMigrateDto)
    }

    adjudicationMigrateDto.damages.toDamages().forEach {
      existingAdjudication.damages.add(it.also { reportedDamage -> reportedDamage.migrated = true })
    }

    adjudicationMigrateDto.evidence.toEvidence().forEach {
      existingAdjudication.evidence.add(it.also { reportedEvidence -> reportedEvidence.migrated = true })
    }

    adjudicationMigrateDto.witnesses.toWitnesses().forEach {
      existingAdjudication.witnesses.add(it.also { reportedWitness -> reportedWitness.migrated = true })
    }

    val saved = reportedAdjudicationRepository.save(existingAdjudication).also { it.calculateStatus() }

    return MigrateResponse(
      chargeNumberMapping = adjudicationMigrateDto.toChargeMapping(existingAdjudication.chargeNumber),
      hearingMappings = saved.hearings.toHearingMappings(),
      punishmentMappings = saved.getPunishments().toPunishmentMappings(adjudicationMigrateDto.bookingId),
    )
  }

  private fun ReportedAdjudication.addHearingsAndOutcomes(hearingsAndResultsAndOutcomes: Pair<List<Hearing>, List<Outcome>>) {
    val hearingsAndResults = hearingsAndResultsAndOutcomes.first
    val outcomes = hearingsAndResultsAndOutcomes.second

    hearingsAndResults.forEach { this.hearings.add(it.also { hearing -> hearing.migrated = true }) }
    outcomes.forEach { this.addOutcome(it.also { outcome -> outcome.migrated = true }) }
  }

  private fun ReportedAdjudication.processPhase1(adjudicationMigrateDto: AdjudicationMigrateDto) {
    val hearingsAndResultsAndOutcomes = adjudicationMigrateDto.hearings.sortedBy { it.hearingDateTime }.toHearingsAndResultsAndOutcomes(
      agencyId = adjudicationMigrateDto.agencyId,
      chargeNumber = this.chargeNumber,
    )

    this.addHearingsAndOutcomes(hearingsAndResultsAndOutcomes)
    this.processPunishments(adjudicationMigrateDto.punishments)
  }

  private fun ReportedAdjudication.processPhase2(adjudicationMigrateDto: AdjudicationMigrateDto) {
    this.hearings.sortedBy { it.dateTimeOfHearing }.filter { it.hearingOutcome?.code == HearingOutcomeCode.NOMIS }.forEach { nomisCode ->
      val nomisHearing = adjudicationMigrateDto.hearings.firstOrNull { nomisCode.oicHearingId == it.oicHearingId && it.hearingResult != null }
        ?: throw ExistingRecordConflictException("${this.chargeNumber} has a NOMIS hearing outcome, and record no longer exists in NOMIS")

      val index = adjudicationMigrateDto.hearings.indexOf(nomisHearing)
      val hasAdditionalOutcomes = adjudicationMigrateDto.hearings.hasAdditionalOutcomesAndFinalOutcomeIsNotQuashed(index)
      val hasAdditionalHearings = index < adjudicationMigrateDto.hearings.size - 1

      val hearingOutcomeCode = nomisHearing.hearingResult!!.finding.mapToHearingOutcomeCode(hasAdditionalOutcomes)

      nomisCode.hearingOutcome!!.adjudicator = nomisHearing.adjudicator ?: ""
      nomisCode.hearingOutcome!!.code = hearingOutcomeCode
      nomisCode.hearingOutcome!!.nomisOutcome = true
      nomisHearing.hearingResult.mapToOutcome(hearingOutcomeCode)?.let {
        this.addOutcome(it.also { outcome -> outcome.migrated = true })
        nomisHearing.hearingResult.createAdditionalOutcome(hasAdditionalHearings)?.let { outcome ->
          this.addOutcome(outcome.also { o -> o.migrated = true })
        }
      }
    }
  }

  private fun ReportedAdjudication.processPhase3(adjudicationMigrateDto: AdjudicationMigrateDto) {
    this.hearings.sortedBy { it.dateTimeOfHearing }.filter { it.filterOutPreviousPhases() }.forEach { hearing ->
      val nomisHearing = adjudicationMigrateDto.hearings.firstOrNull { it.oicHearingId == hearing.oicHearingId }
        ?: throw ExistingRecordConflictException("${adjudicationMigrateDto.oicIncidentId} hearing no longer exists in nomis")
      val nomisHearingResult = nomisHearing.hearingResult
        ?: if (hearing.hearingOutcome != null) throw ExistingRecordConflictException("${adjudicationMigrateDto.oicIncidentId} hearing result no longer exists in nomis") else null
      hearing.hearingOutcome?.let {
        nomisHearingResult?.let { nhr ->
          it.code.mapFinding(nhr.finding, adjudicationMigrateDto.oicIncidentId)
        }
      }
      hearing.update(nomisHearing)
      nomisHearingResult?.let {
        hearing.hearingOutcome?.update(nomisHearing)
      }
    }

    adjudicationMigrateDto.hearings.sortedBy { it.hearingDateTime }.forEach { nomisHearing ->
      if (this.hearings.none { it.oicHearingId == nomisHearing.oicHearingId }) {
        if (listOf(HearingOutcomeCode.ADJOURN, HearingOutcomeCode.REFER_POLICE).contains(this.getLatestHearing()?.hearingOutcome?.code)) {
          this.addHearingsAndOutcomes(
            listOf(nomisHearing).toHearingsAndResultsAndOutcomes(
              agencyId = adjudicationMigrateDto.agencyId,
              chargeNumber = this.chargeNumber,
            ),
          )
        } else {
          // need to attempt to add, if its possible to add the new hearing(s)
          throw ExistingRecordConflictException("${adjudicationMigrateDto.oicIncidentId} has additional hearings and results in nomis")
        }
      }
    }

    when (this.getPunishments().isEmpty()) {
      true -> this.processPunishments(adjudicationMigrateDto.punishments)
      false -> this.processPunishments(this.getPunishments().update(adjudicationMigrateDto.punishments))
    }
  }

  private fun List<Punishment>.update(sanctions: List<MigratePunishment>): List<MigratePunishment> {
    val newPunishments = mutableListOf<MigratePunishment>()

    sanctions.forEach { sanction ->
      if (this.none { it.type == sanction.mapToPunishmentType(it.otherPrivilege) }) {
        newPunishments.add(sanction)
      } else {
        val matches = this.filter { it.type == sanction.mapToPunishmentType(it.otherPrivilege) }
        if (matches.size > 1) throw ExistingRecordConflictException("${sanction.sanctionCode} matches more than one punishment")
        matches.first().sanctionSeq = sanction.sanctionSeq
        // TODO("need to update punishment scheduled - part of discovery with John")
      }
    }

    return newPunishments
  }

  private fun Hearing.update(nomisHearing: MigrateHearing) {
    if (this.locationId != nomisHearing.locationId || this.dateTimeOfHearing != nomisHearing.hearingDateTime || this.oicHearingType != nomisHearing.oicHearingType) {
      this.hearingPreMigrate = HearingPreMigrate(dateTimeOfHearing = this.dateTimeOfHearing, locationId = this.locationId, oicHearingType = this.oicHearingType)
      this.locationId = nomisHearing.locationId
      this.dateTimeOfHearing = nomisHearing.hearingDateTime
      this.oicHearingType = nomisHearing.oicHearingType
    }
  }

  private fun HearingOutcome.update(nomisHearing: MigrateHearing) {
    if (this.adjudicator != nomisHearing.adjudicator) {
      this.hearingOutcomePreMigrate = HearingOutcomePreMigrate(code = this.code, adjudicator = this.adjudicator)
      this.adjudicator = nomisHearing.adjudicator ?: ""
    }
  }

  private fun ReportedOffence.update(adjudicationMigrateDto: AdjudicationMigrateDto) {
    this.nomisOffenceCode = adjudicationMigrateDto.offence.offenceCode
    this.nomisOffenceDescription = adjudicationMigrateDto.offence.offenceDescription
    this.actualOffenceCode = this.offenceCode
    this.offenceCode = OffenceCodes.MIGRATED_OFFENCE.uniqueOffenceCodes.first()
    this.migrated = true
  }

  private fun ReportedAdjudication.processPunishments(punishments: List<MigratePunishment>) {
    val punishmentsAndComments = punishments.toPunishments()
    val punishments = punishmentsAndComments.first
    val punishmentComments = punishmentsAndComments.second

    punishmentComments.forEach { this.punishmentComments.add(it.also { punishmentComment -> punishmentComment.migrated = true }) }
    punishments.forEach { this.addPunishment(it.also { punishment -> punishment.migrated = true }) }
  }

  companion object {
    fun Hearing.filterOutPreviousPhases(): Boolean = this.hearingOutcome?.nomisOutcome != true && this.hearingOutcome?.migrated != true
    fun List<Hearing>.hasHearingWithoutResult(): Boolean {
      val hearingsWithoutLast = this.sortedBy { it.dateTimeOfHearing }
      hearingsWithoutLast.toMutableList().removeLast()

      return hearingsWithoutLast.any { it.hearingOutcome == null }
    }

    fun List<Hearing>.containsNomisHearingOutcomeCode(): Boolean =
      this.any { it.hearingOutcome?.code == HearingOutcomeCode.NOMIS }

    fun HearingOutcomeCode.mapFinding(finding: String, id: Long) {
      val msg = "$id hearing result code has changed"
      when (finding) {
        Finding.D.name, Finding.PROVED.name, Finding.QUASHED.name -> if (this != HearingOutcomeCode.COMPLETE) throw ExistingRecordConflictException(msg)
        Finding.REF_POLICE.name, Finding.PROSECUTED.name -> if (this != HearingOutcomeCode.REFER_POLICE) throw ExistingRecordConflictException(msg)
        Finding.NOT_PROCEED.name -> if (!listOf(HearingOutcomeCode.REFER_POLICE, HearingOutcomeCode.COMPLETE).contains(this)) throw ExistingRecordConflictException(msg)
        else -> {} // to review later...
      }
    }

    fun MigratePunishment.mapToPunishmentType(otherPrivilege: String? = null): PunishmentType? =
      when (this.sanctionCode) {
        OicSanctionCode.CAUTION.name -> PunishmentType.CAUTION
        OicSanctionCode.REMACT.name -> PunishmentType.REMOVAL_ACTIVITY
        OicSanctionCode.EXTW.name -> PunishmentType.EXTRA_WORK
        OicSanctionCode.EXTRA_WORK.name -> PunishmentType.EXCLUSION_WORK
        OicSanctionCode.CC.name -> PunishmentType.CONFINEMENT
        OicSanctionCode.STOP_PCT.name -> PunishmentType.EARNINGS
        OicSanctionCode.REMWIN.name -> PunishmentType.REMOVAL_WING
        OicSanctionCode.ADA.name -> when (this.sanctionStatus) {
          Status.IMMEDIATE.name -> PunishmentType.ADDITIONAL_DAYS
          Status.PROSPECTIVE.name -> PunishmentType.PROSPECTIVE_DAYS
          else -> null
        }
        OicSanctionCode.OTHER.name -> if (this.compensationAmount != null) PunishmentType.DAMAGES_OWED else null
        OicSanctionCode.FORFEIT.name -> if (PrivilegeType.entries.map { it.name }.contains(this.comment) || this.comment == otherPrivilege) PunishmentType.PRIVILEGE else null
        else -> null
      }
  }
}
