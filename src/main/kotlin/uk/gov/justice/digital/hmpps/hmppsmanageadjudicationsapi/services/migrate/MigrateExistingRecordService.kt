package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.ChargeNumberMapping
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.HearingMapping
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.MigrateResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.PunishmentMapping
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodes
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toHearingsAndResultsAndOutcomes
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.toPunishments

@Transactional
@Service
class MigrateExistingRecordService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {
  fun accept(adjudicationMigrateDto: AdjudicationMigrateDto, existingAdjudication: ReportedAdjudication): MigrateResponse {
    if (adjudicationMigrateDto.prisoner.prisonerNumber != existingAdjudication.prisonerNumber) throw UnableToMigrateException("Prisoner different between nomis and adjudications")
    if (adjudicationMigrateDto.agencyId != existingAdjudication.originatingAgencyId) throw UnableToMigrateException("agency different between nomis and adjudications")

    existingAdjudication.offenderBookingId = adjudicationMigrateDto.bookingId

    if (OffenceCodes.findByNomisCode(adjudicationMigrateDto.offence.offenceCode).none { it.uniqueOffenceCodes.contains(existingAdjudication.offenceDetails.first().offenceCode) }) {
      existingAdjudication.offenceDetails.first().nomisOffenceCode = adjudicationMigrateDto.offence.offenceCode
      existingAdjudication.offenceDetails.first().nomisOffenceDescription = adjudicationMigrateDto.offence.offenceDescription
      existingAdjudication.offenceDetails.first().offenceCode = OffenceCodes.MIGRATED_OFFENCE.uniqueOffenceCodes.first()
    }

    if (existingAdjudication.status == ReportedAdjudicationStatus.ACCEPTED) {
      existingAdjudication.processPhase1(adjudicationMigrateDto)
    }

    val saved = reportedAdjudicationRepository.save(existingAdjudication).also { it.calculateStatus() }

    return MigrateResponse(
      chargeNumberMapping = ChargeNumberMapping(
        oicIncidentId = adjudicationMigrateDto.oicIncidentId,
        chargeNumber = existingAdjudication.chargeNumber,
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

  private fun ReportedAdjudication.processPhase1(adjudicationMigrateDto: AdjudicationMigrateDto) {
    val punishmentsAndComments = adjudicationMigrateDto.punishments.toPunishments()
    val punishments = punishmentsAndComments.first
    val punishmentComments = punishmentsAndComments.second

    val hearingsAndResultsAndOutcomes = adjudicationMigrateDto.hearings.sortedBy { it.hearingDateTime }.toHearingsAndResultsAndOutcomes(
      agencyId = adjudicationMigrateDto.agencyId,
      chargeNumber = this.chargeNumber,
    )

    val hearingsAndResults = hearingsAndResultsAndOutcomes.first
    val outcomes = hearingsAndResultsAndOutcomes.second

    this.hearings = hearingsAndResults.toMutableList()
    this.punishmentComments = punishmentComments.toMutableList()
    punishments.forEach { this.addPunishment(it) }
    outcomes.forEach { this.addOutcome(it) }
  }
}
