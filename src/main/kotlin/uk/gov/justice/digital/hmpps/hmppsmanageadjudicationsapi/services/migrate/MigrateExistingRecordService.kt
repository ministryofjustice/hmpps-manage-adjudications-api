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

    if (OffenceCodes.findByNomisCode(adjudicationMigrateDto.offence.offenceCode).none { it.uniqueOffenceCodes.contains(existingAdjudication.offenceDetails.first().offenceCode) }) {
      existingAdjudication.offenceDetails.first().nomisOffenceCode = adjudicationMigrateDto.offence.offenceCode
      existingAdjudication.offenceDetails.first().nomisOffenceDescription = adjudicationMigrateDto.offence.offenceDescription
      existingAdjudication.offenceDetails.first().offenceCode = OffenceCodes.MIGRATED_OFFENCE.uniqueOffenceCodes.first()
    }

    if (existingAdjudication.status == ReportedAdjudicationStatus.ACCEPTED) {
      existingAdjudication.processPhase1(adjudicationMigrateDto)
    } else if (existingAdjudication.hearings.multipleHearingsWithoutOutcomes() || existingAdjudication.hearings.containsNomisHearingOutcomeCode()) {
      existingAdjudication.processPhase2(adjudicationMigrateDto)
    }

    adjudicationMigrateDto.damages.toDamages().forEach { existingAdjudication.damages.add(it) }
    adjudicationMigrateDto.evidence.toEvidence().forEach { existingAdjudication.evidence.add(it) }
    adjudicationMigrateDto.witnesses.toWitnesses().forEach { existingAdjudication.witnesses.add(it) }

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

    hearingsAndResults.forEach { this.hearings.add(it) }
    outcomes.forEach { this.addOutcome(it) }

    this.processPunishments(adjudicationMigrateDto)
  }

  private fun ReportedAdjudication.processPhase2(adjudicationMigrateDto: AdjudicationMigrateDto) {
    this.hearings.filter { it.hearingOutcome?.code == HearingOutcomeCode.NOMIS }.forEach { nomisCode ->
      adjudicationMigrateDto.hearings.firstOrNull { nomisCode.oicHearingId == it.oicHearingId && it.hearingResult != null }?.let {
      }
    }
  }

  private fun ReportedAdjudication.processPunishments(adjudicationMigrateDto: AdjudicationMigrateDto) {
    val punishmentsAndComments = adjudicationMigrateDto.punishments.toPunishments()
    val punishments = punishmentsAndComments.first
    val punishmentComments = punishmentsAndComments.second

    punishmentComments.forEach { this.punishmentComments.add(it) }
    punishments.forEach { this.addPunishment(it) }
  }

  companion object {

    fun List<Hearing>.multipleHearingsWithoutOutcomes(): Boolean = this.all { it.hearingOutcome == null }

    fun List<Hearing>.containsNomisHearingOutcomeCode(): Boolean = this.any { it.hearingOutcome?.code == HearingOutcomeCode.NOMIS }
  }
}
