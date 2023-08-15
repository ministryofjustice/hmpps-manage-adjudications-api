package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.MigrateResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodes
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.createAdditionalOutcome
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
      existingAdjudication.updateOffence(adjudicationMigrateDto)
    }

    if (existingAdjudication.status == ReportedAdjudicationStatus.ACCEPTED) {
      existingAdjudication.processPhase1(adjudicationMigrateDto)
    } else if (existingAdjudication.hearings.containsNomisHearingOutcomeCode()) {
      existingAdjudication.processPhase2(adjudicationMigrateDto)
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

  private fun ReportedAdjudication.processPhase1(adjudicationMigrateDto: AdjudicationMigrateDto) {
    val hearingsAndResultsAndOutcomes = adjudicationMigrateDto.hearings.sortedBy { it.hearingDateTime }.toHearingsAndResultsAndOutcomes(
      agencyId = adjudicationMigrateDto.agencyId,
      chargeNumber = this.chargeNumber,
    )

    val hearingsAndResults = hearingsAndResultsAndOutcomes.first
    val outcomes = hearingsAndResultsAndOutcomes.second

    hearingsAndResults.forEach { this.hearings.add(it.also { hearing -> hearing.migrated = true }) }
    outcomes.forEach { this.addOutcome(it.also { outcome -> outcome.migrated = true }) }

    this.processPunishments(adjudicationMigrateDto)
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
        this.addOutcome(it.also{ outcome -> outcome.migrated = true})
        nomisHearing.hearingResult.createAdditionalOutcome(hasAdditionalHearings)?.let { outcome ->
          this.addOutcome(outcome.also{o -> o.migrated = true})
        }
      }
    }
  }

  private fun ReportedAdjudication.updateOffence(adjudicationMigrateDto: AdjudicationMigrateDto) {
    this.offenceDetails.first().nomisOffenceCode = adjudicationMigrateDto.offence.offenceCode
    this.offenceDetails.first().nomisOffenceDescription = adjudicationMigrateDto.offence.offenceDescription
    this.offenceDetails.first().actualOffenceCode = this.offenceDetails.first().offenceCode
    this.offenceDetails.first().offenceCode = OffenceCodes.MIGRATED_OFFENCE.uniqueOffenceCodes.first()
    this.offenceDetails.first().migrated = true
  }

  private fun ReportedAdjudication.processPunishments(adjudicationMigrateDto: AdjudicationMigrateDto) {
    val punishmentsAndComments = adjudicationMigrateDto.punishments.toPunishments()
    val punishments = punishmentsAndComments.first
    val punishmentComments = punishmentsAndComments.second

    punishmentComments.forEach { this.punishmentComments.add(it.also { punishmentComment -> punishmentComment.migrated = true }) }
    punishments.forEach { this.addPunishment(it.also { punishment -> punishment.migrated = true }) }
  }

  companion object {
    fun List<Hearing>.multipleHearingsWithoutOutcomes(): Boolean = this.all { it.hearingOutcome == null }

    fun List<Hearing>.containsNomisHearingOutcomeCode(): Boolean =
      this.any { it.hearingOutcome?.code == HearingOutcomeCode.NOMIS }
  }
}
