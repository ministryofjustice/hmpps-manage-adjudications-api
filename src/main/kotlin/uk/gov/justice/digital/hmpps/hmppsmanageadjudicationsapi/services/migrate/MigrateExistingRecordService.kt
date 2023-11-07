package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.MigrateResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigrateHearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.MigratePunishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentComment
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.createHearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.hasAdditionalOutcomesAndFinalOutcomeIsNotQuashed
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService.Companion.hasReducedSanctions
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService.Companion.latestOutcome
import java.time.LocalDateTime

@Service
class MigrateExistingRecordService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {
  fun accept(adjudicationMigrateDto: AdjudicationMigrateDto, existingAdjudication: ReportedAdjudication): MigrateResponse {
    if (adjudicationMigrateDto.prisoner.prisonerNumber != existingAdjudication.prisonerNumber) {
      existingAdjudication.prisonerNumber = adjudicationMigrateDto.prisoner.prisonerNumber
      log.warn("Prisoner different between nomis ${adjudicationMigrateDto.prisoner.prisonerNumber} and adjudications ${existingAdjudication.prisonerNumber}")
    }
    if (adjudicationMigrateDto.agencyId != existingAdjudication.originatingAgencyId) throw ExistingRecordConflictException("${existingAdjudication.originatingAgencyId} agency different between nomis and adjudications")

    while (existingAdjudication.offenceDetails.size > 1) {
      existingAdjudication.offenceDetails.removeLast()
    }

    existingAdjudication.offenderBookingId = adjudicationMigrateDto.bookingId
    existingAdjudication.statusBeforeMigration = existingAdjudication.status

    if (OffenceCodes.findByNomisCode(adjudicationMigrateDto.offence.offenceCode).none { it.uniqueOffenceCodes.contains(existingAdjudication.offenceDetails.first().offenceCode) }) {
      existingAdjudication.offenceDetails.first().update(adjudicationMigrateDto)
    }

    // remove duplicate not proceeds - bug in DPS using back on browser
    if (existingAdjudication.getOutcomes().all { it.code == OutcomeCode.NOT_PROCEED } && existingAdjudication.getOutcomes().size == 2) {
      existingAdjudication.removeOutcome(existingAdjudication.latestOutcome()!!)
    }

    // if dps is not proceed no hearing and nomis has hearings, remove dps outcome
    if (existingAdjudication.getOutcomes().size == 1 && existingAdjudication.latestOutcome()?.code == OutcomeCode.NOT_PROCEED && existingAdjudication.hearings.isEmpty() && adjudicationMigrateDto.hearings.isNotEmpty()) {
      existingAdjudication.removeOutcome(existingAdjudication.latestOutcome()!!)
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

  private fun ReportedAdjudication.addHearingsAndOutcomes(hearingsAndResultsAndOutcomes: Triple<List<Hearing>, List<Outcome>, PunishmentComment?>) {
    val hearingsAndResults = hearingsAndResultsAndOutcomes.first
    val outcomes = hearingsAndResultsAndOutcomes.second
    val punishmentComment = hearingsAndResultsAndOutcomes.third

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

    punishmentComment?.let {
      this.punishmentComments.add(punishmentComment)
    }
  }

  private fun ReportedAdjudication.processPhase1(adjudicationMigrateDto: AdjudicationMigrateDto) {
    val hearingsAndResultsAndOutcomes = adjudicationMigrateDto.hearings.sortedBy { it.hearingDateTime }.toHearingsAndResultsAndOutcomes(
      agencyId = adjudicationMigrateDto.agencyId,
      chargeNumber = this.chargeNumber,
      isYouthOffender = this.isYouthOffender,
      hasSanctions = adjudicationMigrateDto.punishments.isNotEmpty(),
      isActive = adjudicationMigrateDto.prisoner.currentAgencyId != null,
      hasADA = adjudicationMigrateDto.punishments.any { it.sanctionCode == OicSanctionCode.ADA.name },
      hasReducedSanctions = adjudicationMigrateDto.hasReducedSanctions(),
      usernameOnPunishment = adjudicationMigrateDto.punishments.firstOrNull()?.createdBy,
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
    val valid = adjudicationMigrateDto.hearings.validate(
      chargeNumber = this.chargeNumber,
      hasSanctions = adjudicationMigrateDto.punishments.isNotEmpty(),
      hasADA = adjudicationMigrateDto.punishments.any { it.sanctionCode == OicSanctionCode.ADA.name },
      isActive = adjudicationMigrateDto.prisoner.currentAgencyId != null,
      agency = this.originatingAgencyId,
    )

    val hearingsMarkedWithNomis = this.hearings.sortedBy { it.dateTimeOfHearing }.filter { it.hearingOutcome?.code == HearingOutcomeCode.NOMIS }

    hearingsMarkedWithNomis.forEachIndexed { hearingIndex, hearingOutcomeNomis ->
      val nomisHearing = adjudicationMigrateDto.hearings.firstOrNull { hearingOutcomeNomis.oicHearingId == it.oicHearingId && it.hearingResult != null }
      if (nomisHearing != null && !listOf("3871590", "3864251", "3899085", "3773547", "3892422", "3823250").contains(this.chargeNumber)) {
        val index = adjudicationMigrateDto.hearings.indexOf(nomisHearing)
        val hasAdditionalOutcomes =
          adjudicationMigrateDto.hearings.subList(index + 1, adjudicationMigrateDto.hearings.size).hasAdditionalOutcomesAndFinalOutcomeIsNotQuashed()
        val hasAdditionalHearings = index < adjudicationMigrateDto.hearings.size - 1
        val hasAdditionalHearingsWithoutResults = hasAdditionalHearings && adjudicationMigrateDto.hearings.subList(
          index + 1,
          adjudicationMigrateDto.hearings.size,
        ).all { it.hearingResult == null }

        val hearingOutcomeCode = nomisHearing.hearingResult!!.finding.mapToHearingOutcomeCode(
          hasAdditionalHearingOutcomes = hasAdditionalOutcomes || hearingIndex < hearingsMarkedWithNomis.size - 1,
          hasAdditionalHearingsInFutureWithoutResults = hasAdditionalHearingsWithoutResults && adjudicationMigrateDto.hearings.any { LocalDateTime.now().isBefore(it.hearingDateTime) },
          chargeNumber = this.chargeNumber,
          valid = valid,
        )

        hearingOutcomeNomis.hearingOutcome!!.adjudicator = nomisHearing.adjudicator ?: ""
        hearingOutcomeNomis.hearingOutcome!!.code = hearingOutcomeCode
        hearingOutcomeNomis.hearingOutcome!!.nomisOutcome = true
        if (hearingOutcomeCode == HearingOutcomeCode.ADJOURN) {
          hearingOutcomeNomis.hearingOutcome!!.details = nomisHearing.hearingResult.finding
          this.latestOutcome()?.let {
            this.removeOutcome(it)
          }
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
        nomisHearing?.let {
          hearingOutcomeNomis.hearingOutcome!!.details = "${it.commentText} - actual finding ${it.hearingResult?.finding}"
        }
      }
    }
  }

  private fun ReportedAdjudication.processPhase3(adjudicationMigrateDto: AdjudicationMigrateDto) {
    val hearings = this.hearings.sortedBy { it.dateTimeOfHearing }.filter { it.filterOutPreviousPhases() }
    hearings.forEachIndexed { index, hearing ->
      val nomisHearing = adjudicationMigrateDto.hearings.firstOrNull { it.oicHearingId == hearing.oicHearingId }
        ?: throw IgnoreAsPreprodRefreshOutofSyncException("${this.originatingAgencyId} ${this.chargeNumber} ${hearing.oicHearingId} hearing no longer exists in nomis")
      val nomisHearingResult = nomisHearing.hearingResult
        ?: if (hearing.hearingOutcome != null && hearing.hearingOutcome!!.code.shouldExistInNomis()) {
          throw IgnoreAsPreprodRefreshOutofSyncException("${this.originatingAgencyId} ${this.chargeNumber} ${hearing.oicHearingId} ${hearing.hearingOutcome?.code} hearing result no longer exists in nomis")
        } else {
          null
        }
      hearing.hearingOutcome?.let {
        nomisHearingResult?.let { nhr ->

          try {
            it.code.mapFinding(
              finding = nhr.finding,
              chargeNumber = this.chargeNumber,
              isLastOutcome = index == hearings.size - 1,
            )
          } catch (e: ExistingRecordConflictException) {
            when (it.code) {
              HearingOutcomeCode.ADJOURN ->
                when (nhr.finding) {
                  Finding.PROVED.name -> {
                    it.code = HearingOutcomeCode.COMPLETE
                    this.addOutcome(Outcome(code = OutcomeCode.CHARGE_PROVED, actualCreatedDate = LocalDateTime.now()))
                  }
                  Finding.NOT_PROCEED.name, Finding.DISMISSED.name -> {
                    it.code = HearingOutcomeCode.COMPLETE
                    this.addOutcome(Outcome(code = OutcomeCode.NOT_PROCEED, actualCreatedDate = LocalDateTime.now()))
                  }
                  Finding.D.name -> {
                    it.code = HearingOutcomeCode.COMPLETE
                    this.addOutcome(Outcome(code = OutcomeCode.DISMISSED, actualCreatedDate = LocalDateTime.now()))
                  }
                  Finding.REF_POLICE.name -> {
                    if (this.status != ReportedAdjudicationStatus.CHARGE_PROVED) {
                      throw e
                    } else {}
                  }
                  else -> throw e
                }
              HearingOutcomeCode.REFER_POLICE, HearingOutcomeCode.REFER_GOV, HearingOutcomeCode.REFER_INAD ->
                when (nhr.finding) {
                  Finding.PROVED.name -> {
                    it.code = HearingOutcomeCode.COMPLETE
                    this.latestOutcome()?.code = OutcomeCode.CHARGE_PROVED
                  }
                  Finding.NOT_PROCEED.name, Finding.DISMISSED.name, Finding.D.name -> {
                    this.addOutcome(Outcome(code = OutcomeCode.NOT_PROCEED, actualCreatedDate = LocalDateTime.now()))
                  }
                  Finding.PROSECUTED.name -> {
                    if (it.code == HearingOutcomeCode.REFER_POLICE) {
                      this.addOutcome(Outcome(code = OutcomeCode.PROSECUTION, actualCreatedDate = LocalDateTime.now()))
                    } else {
                      throw e
                    }
                  }
                  else -> throw e
                }
              else -> throw e
            }
          }
        }
      }

      hearing.update(nomisHearing)
      nomisHearingResult?.let {
        hearing.hearingOutcome?.update(nomisHearing)
      }
    }

    val newHearingsToReview = adjudicationMigrateDto.hearings.filter { this.filterNewHearings(it) }.toMutableList()

    newHearingsToReview.sortedBy { it.hearingDateTime }.forEach {
      if (this.getLatestHearing()?.dateTimeOfHearing?.isAfter(it.hearingDateTime) == true && it.hearingResult != null) {
        if (this.getLatestHearing()?.hearingOutcome != null) {
          val exception = ExistingRecordConflictException("${this.originatingAgencyId} $chargeNumber has a new hearing with result before latest with different outcome, nomis finding ${it.hearingResult.finding}")
          when (this.latestOutcome()?.code) {
            OutcomeCode.DISMISSED -> if (it.hearingResult.finding != Finding.D.name) {
              throw exception
            }
            OutcomeCode.NOT_PROCEED -> if (!listOf(Finding.DISMISSED.name, Finding.NOT_PROCEED.name).contains(it.hearingResult.finding)) {
              throw exception
            }
            OutcomeCode.CHARGE_PROVED -> if (it.hearingResult.finding != Finding.PROVED.name) {
              throw exception
            }
            OutcomeCode.REFER_POLICE -> if (it.hearingResult.finding != Finding.REF_POLICE.name) {
              throw exception
            }
            null -> if (this.getLatestHearing()?.hearingOutcome?.code == HearingOutcomeCode.ADJOURN) {
              this.hearings.remove(this.getLatestHearing()!!)
              return@forEach
            } else {
              throw exception
            }
            else -> throw exception
          }

          this.hearings.add(
            it.createHearing(
              isYouthOffender = this.isYouthOffender,
              agencyId = this.originatingAgencyId,
              chargeNumber = this.chargeNumber,
              hearingOutcome = createAdjourn(),
            ),
          )
          newHearingsToReview.remove(it)
          return@forEach
        } else {
          this.hearings.remove(this.getLatestHearing()!!)
        }
      }

      if (HearingOutcomeCode.COMPLETE == this.getLatestHearing()?.hearingOutcome?.code &&
        it.hearingResult?.finding != Finding.QUASHED.name
      ) {
        if (it.hearingResult == null) {
          newHearingsToReview.remove(it)
        } else {
          if (listOf(Finding.D.name, Finding.DISMISSED.name, Finding.NOT_PROCEED.name).contains(it.hearingResult.finding)) {
            if (adjudicationMigrateDto.punishments.isNotEmpty()) {
              throw ExistingRecordConflictException("${this.originatingAgencyId} $chargeNumber new hearing with negative result after completed ${it.hearingResult.finding} - sanctions present in nomis")
            } else {
              if (this.latestOutcome()?.code == OutcomeCode.CHARGE_PROVED) {
                this.removeOutcome(this.latestOutcome()!!)
                this.hearings.remove(this.getLatestHearing()!!)
              }
            }
          }
          if (it.hearingResult.finding == Finding.PROVED.name && this.latestOutcome()?.code == OutcomeCode.CHARGE_PROVED) {
            this.getLatestHearing()!!.hearingOutcome!!.code = HearingOutcomeCode.ADJOURN
            this.removeOutcome(this.latestOutcome()!!)
          }
          if (it.hearingResult.finding == Finding.APPEAL.name) {
            newHearingsToReview.remove(it)
            if (adjudicationMigrateDto.hasReducedSanctions()) {
              this.punishmentComments.add(PunishmentComment(comment = "Reduced on APPEAL", nomisCreatedBy = adjudicationMigrateDto.punishments.first().createdBy))
            } else {
              this.addOutcome(Outcome(code = OutcomeCode.QUASHED, actualCreatedDate = it.hearingDateTime))
            }
          }
        }
      }
    }
    val latestOutcome = this.latestOutcome()
    if (listOf(OutcomeCode.DISMISSED, OutcomeCode.NOT_PROCEED).contains(latestOutcome?.code) && newHearingsToReview.isNotEmpty() && this.hearings.isNotEmpty()) {
      if (newHearingsToReview.any { it.hearingResult?.finding == Finding.PROVED.name }) {
        this.getLatestHearing()?.hearingOutcome?.code = HearingOutcomeCode.ADJOURN
        this.getLatestHearing()?.hearingOutcome?.details = "Adjourned as nomis is now charge proved - previous outcome ${latestOutcome?.code}"
        this.removeOutcome(latestOutcome!!)
      }
    }

    this.addHearingsAndOutcomes(
      newHearingsToReview.sortedBy { it.hearingDateTime }
        .toHearingsAndResultsAndOutcomes(
          agencyId = adjudicationMigrateDto.agencyId,
          chargeNumber = this.chargeNumber,
          isYouthOffender = this.isYouthOffender,
          hasSanctions = adjudicationMigrateDto.punishments.isNotEmpty(),
          isActive = adjudicationMigrateDto.prisoner.currentAgencyId != null,
          hasADA = adjudicationMigrateDto.punishments.any { it.sanctionCode == OicSanctionCode.ADA.name },
          hasReducedSanctions = adjudicationMigrateDto.hasReducedSanctions(),
          usernameOnPunishment = adjudicationMigrateDto.punishments.firstOrNull()?.createdBy,
        ),
    )
    // we always clear existing ones now, and replace with NOMIS
    this.clearPunishments()
    this.processPunishments(adjudicationMigrateDto.punishments)
  }

  private fun ReportedAdjudication.filterNewHearings(nomisHearing: MigrateHearing): Boolean =
    this.hearings.none { it.oicHearingId == nomisHearing.oicHearingId } &&
      this.getOutcomes().none { it.oicHearingId == nomisHearing.oicHearingId }

  private fun Hearing.update(nomisHearing: MigrateHearing) {
    if (this.locationId != nomisHearing.locationId || this.dateTimeOfHearing != nomisHearing.hearingDateTime || this.oicHearingType != nomisHearing.oicHearingType) {
      this.locationId = nomisHearing.locationId
      this.dateTimeOfHearing = nomisHearing.hearingDateTime
      this.oicHearingType = nomisHearing.oicHearingType
    }
  }

  private fun HearingOutcome.update(nomisHearing: MigrateHearing) {
    if (this.adjudicator != nomisHearing.adjudicator) {
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

  private fun ReportedAdjudication.processPunishments(sanctions: List<MigratePunishment>) {
    val punishmentsAndComments = sanctions.toPunishments(finalOutcome = this.latestOutcome()?.code, usernameOnPunishment = sanctions.firstOrNull()?.createdBy)
    val punishments = punishmentsAndComments.first
    val punishmentComments = punishmentsAndComments.second

    punishmentComments.forEach { this.punishmentComments.add(it.also { punishmentComment -> punishmentComment.migrated = true }) }
    punishments.forEach { this.addPunishment(it.also { punishment -> punishment.migrated = true }) }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
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
        Finding.NOT_PROCEED.name, Finding.DISMISSED.name -> if (!listOf(HearingOutcomeCode.REFER_POLICE, HearingOutcomeCode.COMPLETE).contains(this)) throw ExistingRecordConflictException(msg)
        Finding.ADJOURNED.name -> HearingOutcomeCode.ADJOURN
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
