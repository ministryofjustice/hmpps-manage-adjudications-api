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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toDisIssue
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toHearingMappings
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toHearingsAndResultsAndOutcomes
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toPunishmentMappings
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toPunishments
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toWitnesses
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.validate

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
          hearing.hearingOutcome = createAdjourn()
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

    hearingsAndResults.forEach { hearingToAdd ->
      this.hearings.add(
        hearingToAdd.also {
          it.migrated = true
          if (this.getLatestHearing()?.dateTimeOfHearing?.isAfter(it.dateTimeOfHearing) == true && it.hearingOutcome == null) {
            it.hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = "")
          }
        },
      )
    }
    outcomes.forEach { this.addOutcome(it.also { outcome -> outcome.migrated = true }) }
  }

  private fun ReportedAdjudication.processPhase1(adjudicationMigrateDto: AdjudicationMigrateDto) {
    val hearingsAndResultsAndOutcomes = adjudicationMigrateDto.hearings.sortedBy { it.hearingDateTime }.toHearingsAndResultsAndOutcomes(
      agencyId = adjudicationMigrateDto.agencyId,
      chargeNumber = this.chargeNumber,
      isYouthOffender = this.isYouthOffender,
      hasSanctions = adjudicationMigrateDto.punishments.isNotEmpty(),
    )

    val disIssued = adjudicationMigrateDto.disIssued.toDisIssue()

    this.issuingOfficer = disIssued.first
    this.dateTimeOfIssue = disIssued.second
    disIssued.third.forEach {
      this.disIssueHistory.add(it.also { disIssueHistory -> disIssueHistory.migrated = true })
    }

    this.addHearingsAndOutcomes(hearingsAndResultsAndOutcomes)
    this.processPunishments(adjudicationMigrateDto.punishments)
  }

  private fun ReportedAdjudication.processPhase2(adjudicationMigrateDto: AdjudicationMigrateDto) {
    adjudicationMigrateDto.hearings.validate(this.chargeNumber, adjudicationMigrateDto.punishments.isNotEmpty())

    this.hearings.sortedBy { it.dateTimeOfHearing }.filter { it.hearingOutcome?.code == HearingOutcomeCode.NOMIS }.forEach { hearingOutcomeNomis ->
      val nomisHearing = adjudicationMigrateDto.hearings.firstOrNull { hearingOutcomeNomis.oicHearingId == it.oicHearingId && it.hearingResult != null }
      if (nomisHearing != null) {
        val index = adjudicationMigrateDto.hearings.indexOf(nomisHearing)
        val hasAdditionalOutcomes =
          adjudicationMigrateDto.hearings.hasAdditionalOutcomesAndFinalOutcomeIsNotQuashed(index)
        val hasAdditionalHearings = index < adjudicationMigrateDto.hearings.size - 1
        val hasAdditionalHearingsWithoutResults = hasAdditionalHearings && adjudicationMigrateDto.hearings.subList(
          index + 1,
          adjudicationMigrateDto.hearings.size,
        ).all { it.hearingResult == null }

        val hearingOutcomeCode = nomisHearing.hearingResult!!.finding.mapToHearingOutcomeCode(
          hasAdditionalHearingOutcomes = hasAdditionalOutcomes,
          hasAdditionalHearingsWithoutResults = hasAdditionalHearingsWithoutResults,
          chargeNumber = this.chargeNumber,
        )

        hearingOutcomeNomis.hearingOutcome!!.adjudicator = nomisHearing.adjudicator ?: ""
        hearingOutcomeNomis.hearingOutcome!!.code = hearingOutcomeCode
        hearingOutcomeNomis.hearingOutcome!!.nomisOutcome = true
        if (hearingOutcomeCode == HearingOutcomeCode.ADJOURN) {
          hearingOutcomeNomis.hearingOutcome!!.details = nomisHearing.hearingResult.finding
        }
        nomisHearing.hearingResult.mapToOutcome(hearingOutcomeCode)?.let {
          this.addOutcome(it.also { outcome -> outcome.migrated = true })
          nomisHearing.hearingResult.createAdditionalOutcome(hasAdditionalHearings)?.let { outcome ->
            this.addOutcome(outcome.also { o -> o.migrated = true })
          }
        }
      } else {
        hearingOutcomeNomis.hearingOutcome!!.adjudicator = ""
        hearingOutcomeNomis.hearingOutcome!!.code = HearingOutcomeCode.ADJOURN
        hearingOutcomeNomis.hearingOutcome!!.nomisOutcome = true
      }
    }
  }

  private fun ReportedAdjudication.processPhase3(adjudicationMigrateDto: AdjudicationMigrateDto) {
    val hearings = this.hearings.sortedBy { it.dateTimeOfHearing }.filter { it.filterOutPreviousPhases() }
    hearings.forEachIndexed { index, hearing ->
      val nomisHearing = adjudicationMigrateDto.hearings.firstOrNull { it.oicHearingId == hearing.oicHearingId }
        ?: throw ExistingRecordConflictException("${this.chargeNumber} ${hearing.oicHearingId} hearing no longer exists in nomis")
      val nomisHearingResult = nomisHearing.hearingResult
        ?: if (hearing.hearingOutcome != null && hearing.hearingOutcome!!.code.shouldExistInNomis()) {
          throw ExistingRecordConflictException("${this.chargeNumber} ${hearing.oicHearingId} ${hearing.hearingOutcome?.code} hearing result no longer exists in nomis")
        } else {
          null
        }
      hearing.hearingOutcome?.let {
        nomisHearingResult?.let { nhr ->
          it.code.mapFinding(
            finding = nhr.finding,
            chargeNumber = this.chargeNumber,
            isLastOutcome = index == hearings.size - 1,
          )
        }
      }

      hearing.update(nomisHearing)
      nomisHearingResult?.let {
        hearing.hearingOutcome?.update(nomisHearing)
      }
    }

    adjudicationMigrateDto.hearings.filter { this.filterNewHearings(it) }.sortedBy { it.hearingDateTime }.forEach {
      if (this.getLatestHearing()?.dateTimeOfHearing?.isAfter(it.hearingDateTime) == true && it.hearingResult != null) {
        throw ExistingRecordConflictException("$chargeNumber has a new hearing with result before latest ${it.hearingResult.finding}")
      }

      if (HearingOutcomeCode.COMPLETE == this.getLatestHearing()?.hearingOutcome?.code &&
        it.hearingResult?.finding != Finding.QUASHED.name
      ) {
        throw ExistingRecordConflictException("$chargeNumber has a new hearing after completed ${it.hearingResult?.finding}")
      }
    }

    this.addHearingsAndOutcomes(
      adjudicationMigrateDto.hearings.filter { this.filterNewHearings(it) }.sortedBy { it.hearingDateTime }
        .toHearingsAndResultsAndOutcomes(
          agencyId = adjudicationMigrateDto.agencyId,
          chargeNumber = this.chargeNumber,
          isYouthOffender = this.isYouthOffender,
          hasSanctions = adjudicationMigrateDto.punishments.isNotEmpty(),
        ),
    )

    when (this.getPunishments().isEmpty()) {
      true -> this.processPunishments(adjudicationMigrateDto.punishments)
      false -> this.processPunishments(this.getPunishments().update(adjudicationMigrateDto.punishments))
    }
  }

  private fun ReportedAdjudication.filterNewHearings(nomisHearing: MigrateHearing): Boolean =
    this.hearings.none { it.oicHearingId == nomisHearing.oicHearingId } &&
      this.getOutcomes().none { it.oicHearingId == nomisHearing.oicHearingId }

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
      hearingsWithoutLast.toMutableList().removeLastOrNull()

      return hearingsWithoutLast.any { it.hearingOutcome == null }
    }

    fun List<Hearing>.containsNomisHearingOutcomeCode(): Boolean =
      this.any { it.hearingOutcome?.code == HearingOutcomeCode.NOMIS }

    fun HearingOutcomeCode.mapFinding(finding: String, chargeNumber: String, isLastOutcome: Boolean) {
      val msg = "$chargeNumber hearing result code has changed $this vs $finding, is last outcome? $isLastOutcome"
      when (finding) {
        Finding.D.name, Finding.PROVED.name, Finding.APPEAL.name -> if (this != HearingOutcomeCode.COMPLETE) throw ExistingRecordConflictException(msg)
        Finding.REF_POLICE.name -> if (this != HearingOutcomeCode.REFER_POLICE) throw ExistingRecordConflictException(msg)
        Finding.NOT_PROCEED.name -> if (!listOf(HearingOutcomeCode.REFER_POLICE, HearingOutcomeCode.COMPLETE).contains(this)) throw ExistingRecordConflictException(msg)
        else -> throw ExistingRecordConflictException("$chargeNumber unsupported mapping $finding")
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

    fun HearingOutcomeCode.shouldExistInNomis(): Boolean =
      when (this) {
        HearingOutcomeCode.ADJOURN, HearingOutcomeCode.REFER_GOV, HearingOutcomeCode.REFER_INAD -> false
        else -> true
      }
  }
}
