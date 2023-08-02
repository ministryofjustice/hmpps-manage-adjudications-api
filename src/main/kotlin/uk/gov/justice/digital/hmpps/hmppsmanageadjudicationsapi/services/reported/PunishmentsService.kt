package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentCommentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequestV2
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdditionalDaysDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentComment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.LegacySyncService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OffenderOicSanctionRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OffenderOicSanctionRequest.Companion.mapPunishmentToSanction
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.ForbiddenException
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import java.time.LocalDate

@Transactional
@Service
class PunishmentsService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
  private val legacySyncService: LegacySyncService,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {

  @Deprecated("to remove on completion of NN-5319")
  fun createPunishmentsFromChargeProvedIfApplicable(reportedAdjudication: ReportedAdjudication, caution: Boolean, amount: Double?) {
    if (caution) createCaution(reportedAdjudication = reportedAdjudication)

    amount?.run {
      createDamagesOwed(reportedAdjudication = reportedAdjudication, amount = amount)
    }
  }

  @Deprecated("to remove on completion of NN-5319")
  fun amendPunishmentsFromChargeProvedIfApplicable(adjudicationNumber: String, caution: Boolean, damagesOwed: Boolean?, amount: Double?): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(adjudicationNumber)

    handleCautionChange(reportedAdjudication = reportedAdjudication, caution = caution).run {
      damagesOwed?.run {
        handleDamagesOwedChange(reportedAdjudication = reportedAdjudication, amount = amount)
      }
    }

    return saveToDto(reportedAdjudication)
  }

  fun removeQuashedFinding(reportedAdjudication: ReportedAdjudication) {
    legacySyncService.deleteSanctions(adjudicationNumber = reportedAdjudication.chargeNumber.toLong())

    reportedAdjudication.createSanctionAndAssignSanctionSeq(type = PunishmentType.CAUTION)
    reportedAdjudication.createSanctionAndAssignSanctionSeq(type = PunishmentType.DAMAGES_OWED)

    legacySyncService.createSanctions(
      adjudicationNumber = reportedAdjudication.chargeNumber.toLong(),
      sanctions = reportedAdjudication.mapToSanctions(),
    )
  }

  @Deprecated("to remove on completion of NN-5319")
  fun create(
    chargeNumber: String,
    punishments: List<PunishmentRequest>,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber).also {
      it.validateCanAddPunishments()
    }

    punishments.forEach {
      it.validateRequest(reportedAdjudication.getLatestHearing())
      if (it.activatedFrom != null) {
        reportedAdjudication.addPunishment(
          activateSuspendedPunishment(chargeNumber = chargeNumber, punishmentRequest = it),
        )
      } else {
        reportedAdjudication.addPunishment(createNewPunishment(punishmentRequest = it))
      }
    }

    legacySyncService.createSanctions(
      adjudicationNumber = chargeNumber.toLong(),
      sanctions = reportedAdjudication.mapToSanctions(),
    )

    return saveToDto(reportedAdjudication)
  }

  fun createV2(
    chargeNumber: String,
    punishments: List<PunishmentRequestV2>,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber).also {
      it.validateCanAddPunishmentsV2()
    }

    punishments.validateCaution()

    punishments.forEach {
      it.validateRequestV2(reportedAdjudication.getLatestHearing())
      if (it.activatedFrom != null) {
        reportedAdjudication.addPunishment(
          activateSuspendedPunishmentV2(chargeNumber = chargeNumber, punishmentRequest = it),
        )
      } else {
        reportedAdjudication.addPunishment(createNewPunishmentV2(punishmentRequest = it))
      }
    }

    legacySyncService.createSanctions(
      adjudicationNumber = chargeNumber.toLong(),
      sanctions = reportedAdjudication.mapToSanctionsV2(),
    )

    return saveToDto(reportedAdjudication)
  }

  @Deprecated("to remove on completion of NN-5319")
  fun update(
    chargeNumber: String,
    punishments: List<PunishmentRequest>,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber).also {
      it.validateCanAddPunishments()
    }

    val idsToUpdate = punishments.filter { it.id != null }.map { it.id }
    reportedAdjudication.getPunishments().filterOutChargeProvedPunishments().filter { !idsToUpdate.contains(it.id) }.forEach { punishment ->
      punishment.type.consecutiveReportValidation(chargeNumber).let {
        punishment.deleted = true
      }
    }

    punishments.forEach { punishmentRequest ->
      punishmentRequest.validateRequest(reportedAdjudication.getLatestHearing())

      when (punishmentRequest.id) {
        null -> when (punishmentRequest.activatedFrom) {
          null -> reportedAdjudication.addPunishment(createNewPunishment(punishmentRequest = punishmentRequest))
          else -> reportedAdjudication.addPunishment(activateSuspendedPunishment(chargeNumber = chargeNumber, punishmentRequest = punishmentRequest))
        }
        else -> {
          when (punishmentRequest.activatedFrom) {
            null -> {
              val punishmentToAmend = reportedAdjudication.getPunishments().getPunishmentToAmend(punishmentRequest.id)
              when (punishmentToAmend.type) {
                punishmentRequest.type -> {
                  punishmentRequest.suspendedUntil?.let {
                    punishmentRequest.type.consecutiveReportValidation(chargeNumber)
                  }
                  updatePunishment(punishmentToAmend, punishmentRequest)
                }
                else -> {
                  punishmentToAmend.type.consecutiveReportValidation(chargeNumber).let {
                    punishmentToAmend.deleted = true
                    reportedAdjudication.addPunishment(createNewPunishment(punishmentRequest = punishmentRequest))
                  }
                }
              }
            }
            else ->
              if (reportedAdjudication.getPunishments().getPunishment(punishmentRequest.id) == null) {
                reportedAdjudication.addPunishment(
                  activateSuspendedPunishment(
                    chargeNumber = chargeNumber,
                    punishmentRequest = punishmentRequest,
                  ),
                )
              }
          }
        }
      }
    }

    legacySyncService.updateSanctions(
      adjudicationNumber = chargeNumber.toLong(),
      sanctions = reportedAdjudication.mapToSanctions(),
    ).run {
      reportedAdjudication.getPunishments().firstOrNull { it.type == PunishmentType.DAMAGES_OWED }?.let {
        it.sanctionSeq = createPunishmentFromChargeProved(
          chargeNumber = reportedAdjudication.chargeNumber,
          type = PunishmentType.DAMAGES_OWED,
          amount = it.amount!!,
        ).sanctionSeq
      }
    }

    return saveToDto(reportedAdjudication)
  }

  fun updateV2(
    chargeNumber: String,
    punishments: List<PunishmentRequestV2>,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber).also {
      it.validateCanAddPunishmentsV2()
    }

    punishments.validateCaution()

    val idsToUpdate = punishments.filter { it.id != null }.map { it.id }
    reportedAdjudication.getPunishments().filter { !idsToUpdate.contains(it.id) }.forEach { punishment ->
      punishment.type.consecutiveReportValidation(chargeNumber).let {
        punishment.deleted = true
      }
    }

    punishments.forEach { punishmentRequest ->
      punishmentRequest.validateRequestV2(reportedAdjudication.getLatestHearing())

      when (punishmentRequest.id) {
        null -> when (punishmentRequest.activatedFrom) {
          null -> reportedAdjudication.addPunishment(createNewPunishmentV2(punishmentRequest = punishmentRequest))
          else -> reportedAdjudication.addPunishment(activateSuspendedPunishmentV2(chargeNumber = chargeNumber, punishmentRequest = punishmentRequest))
        }
        else -> {
          when (punishmentRequest.activatedFrom) {
            null -> {
              val punishmentToAmend = reportedAdjudication.getPunishments().getPunishmentToAmend(punishmentRequest.id)
              when (punishmentToAmend.type) {
                punishmentRequest.type -> {
                  punishmentRequest.suspendedUntil?.let {
                    punishmentRequest.type.consecutiveReportValidation(chargeNumber)
                  }
                  updatePunishmentV2(punishmentToAmend, punishmentRequest)
                }
                else -> {
                  punishmentToAmend.type.consecutiveReportValidation(chargeNumber).let {
                    punishmentToAmend.deleted = true
                    reportedAdjudication.addPunishment(createNewPunishmentV2(punishmentRequest = punishmentRequest))
                  }
                }
              }
            }
            else ->
              if (reportedAdjudication.getPunishments().getPunishment(punishmentRequest.id) == null) {
                reportedAdjudication.addPunishment(
                  activateSuspendedPunishmentV2(
                    chargeNumber = chargeNumber,
                    punishmentRequest = punishmentRequest,
                  ),
                )
              }
          }
        }
      }
    }

    legacySyncService.updateSanctions(
      adjudicationNumber = chargeNumber.toLong(),
      sanctions = reportedAdjudication.mapToSanctionsV2(),
    )

    return saveToDto(reportedAdjudication)
  }

  private fun PunishmentType.consecutiveReportValidation(chargeNumber: String) {
    if (PunishmentType.additionalDays().contains(this)) {
      if (isLinkedToReport(chargeNumber, this)) {
        throw ValidationException("Unable to modify: $this is linked to another report")
      }
    }
  }

  fun getSuspendedPunishments(prisonerNumber: String, chargeNumber: String): List<SuspendedPunishmentDto> {
    val reportsWithSuspendedPunishments = getReportsWithSuspendedPunishments(prisonerNumber = prisonerNumber)
    val includeAdditionalDays = includeAdditionalDays(chargeNumber)

    return reportsWithSuspendedPunishments.map {
      it.getPunishments().suspendedPunishmentsToActivate()
        .filter { punishment -> punishment.type.includeInSuspendedPunishments(includeAdditionalDays) }.map { punishment ->
          val schedule = punishment.schedule.latestSchedule()

          SuspendedPunishmentDto(
            reportNumber = it.chargeNumber.toLong(),
            chargeNumber = it.chargeNumber,
            punishment = PunishmentDto(
              id = punishment.id,
              type = punishment.type,
              privilegeType = punishment.privilegeType,
              otherPrivilege = punishment.otherPrivilege,
              stoppagePercentage = punishment.stoppagePercentage,
              schedule = PunishmentScheduleDto(days = schedule.days, suspendedUntil = schedule.suspendedUntil),
            ),
          )
        }
    }.flatten()
  }

  fun getReportsWithAdditionalDays(chargeNumber: String, prisonerNumber: String, punishmentType: PunishmentType): List<AdditionalDaysDto> {
    if (!PunishmentType.additionalDays().contains(punishmentType)) throw ValidationException("Punishment type must be ADDITIONAL_DAYS or PROSPECTIVE_DAYS")

    val reportedAdjudication = findByChargeNumber(chargeNumber)

    return getReportsWithActiveAdditionalDays(
      prisonerNumber = prisonerNumber,
      punishmentType = punishmentType,
    ).filter { it.includeAdaWithSameHearingDateAndSeparateCharge(reportedAdjudication) }
      .map {
        it.getPunishments().filter { punishment -> punishment.type == punishmentType }.map { punishment ->
          val schedule = punishment.schedule.latestSchedule()

          AdditionalDaysDto(
            reportNumber = it.chargeNumber.toLong(),
            chargeNumber = it.chargeNumber,
            chargeProvedDate = it.getLatestHearing()?.dateTimeOfHearing?.toLocalDate()!!,
            punishment = PunishmentDto(
              id = punishment.id,
              type = punishment.type,
              consecutiveReportNumber = punishment.consecutiveChargeNumber?.toLong(),
              consecutiveChargeNumber = punishment.consecutiveChargeNumber,
              schedule = PunishmentScheduleDto(days = schedule.days),
            ),
          )
        }
      }.flatten()
  }

  fun createPunishmentComment(
    chargeNumber: String,
    punishmentComment: PunishmentCommentRequest,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)

    reportedAdjudication.punishmentComments.add(
      PunishmentComment(
        comment = punishmentComment.comment,
      ),
    )

    return saveToDto(reportedAdjudication)
  }

  fun updatePunishmentComment(
    chargeNumber: String,
    punishmentComment: PunishmentCommentRequest,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    val punishmentCommentToUpdate = reportedAdjudication.punishmentComments.getPunishmentComment(punishmentComment.id!!)
      .also {
        it.createdByUserId?.validatePunishmentCommentAction(authenticationFacade.currentUsername!!)
      }

    punishmentCommentToUpdate.comment = punishmentComment.comment

    return saveToDto(reportedAdjudication)
  }

  fun deletePunishmentComment(
    chargeNumber: String,
    punishmentCommentId: Long,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    val punishmentComment = reportedAdjudication.punishmentComments.getPunishmentComment(punishmentCommentId)
      .also {
        it.createdByUserId?.validatePunishmentCommentAction(authenticationFacade.currentUsername!!)
      }

    reportedAdjudication.punishmentComments.remove(punishmentComment)

    return saveToDto(reportedAdjudication)
  }

  private fun includeAdditionalDays(chargeNumber: String): Boolean {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    return OicHearingType.inadTypes().contains(reportedAdjudication.getLatestHearing()?.oicHearingType)
  }

  @Deprecated("to remove on completion of NN-5319")
  private fun handleDamagesOwedChange(reportedAdjudication: ReportedAdjudication, amount: Double?) {
    when (val damagesOwedPunishment = reportedAdjudication.getPunishments().firstOrNull { it.type == PunishmentType.DAMAGES_OWED }) {
      null -> if (amount != null) createDamagesOwed(reportedAdjudication = reportedAdjudication, amount = amount)
      else -> if (damagesOwedPunishment.amount != amount) {
        when (amount) {
          null -> deleteDamagesOwed(reportedAdjudication = reportedAdjudication, punishment = damagesOwedPunishment)
          else -> amendDamagesOwed(reportedAdjudication = reportedAdjudication, punishment = damagesOwedPunishment, amount = amount)
        }
      }
    }
  }

  @Deprecated("to remove on completion of NN-5319")
  private fun createDamagesOwed(reportedAdjudication: ReportedAdjudication, amount: Double) {
    reportedAdjudication.addPunishment(
      createPunishmentFromChargeProved(
        chargeNumber = reportedAdjudication.chargeNumber,
        type = PunishmentType.DAMAGES_OWED,
        amount = amount,
      ),
    )
  }

  @Deprecated("to remove on completion of NN-5319")
  private fun deleteDamagesOwed(reportedAdjudication: ReportedAdjudication, punishment: Punishment) {
    legacySyncService.deleteSanction(
      adjudicationNumber = reportedAdjudication.chargeNumber.toLong(),
      sanctionSeq = punishment.sanctionSeq!!,
    ).run {
      punishment.deleted = true
    }
  }

  @Deprecated("to remove on completion of NN-5319")
  private fun amendDamagesOwed(reportedAdjudication: ReportedAdjudication, punishment: Punishment, amount: Double) {
    legacySyncService.deleteSanction(adjudicationNumber = reportedAdjudication.chargeNumber.toLong(), sanctionSeq = punishment.sanctionSeq!!).run {
      punishment.amount = amount
      punishment.sanctionSeq = legacySyncService.createSanction(
        adjudicationNumber = reportedAdjudication.chargeNumber.toLong(),
        sanction = punishment.mapPunishmentToSanction(),
      )
    }
  }

  @Deprecated("to remove on completion of NN-5319")
  private fun handleCautionChange(reportedAdjudication: ReportedAdjudication, caution: Boolean) {
    when (val cautionPunishment = reportedAdjudication.getPunishments().firstOrNull { it.type == PunishmentType.CAUTION }) {
      null -> if (caution) amendToCaution(reportedAdjudication = reportedAdjudication)
      else -> if (!caution) removeCaution(reportedAdjudication = reportedAdjudication, punishment = cautionPunishment)
    }
  }

  @Deprecated("to remove on completion of NN-5319")
  private fun createCaution(reportedAdjudication: ReportedAdjudication) {
    reportedAdjudication.addPunishment(
      createPunishmentFromChargeProved(
        chargeNumber = reportedAdjudication.chargeNumber,
        type = PunishmentType.CAUTION,
      ),
    )
  }

  @Deprecated("to remove on completion of NN-5319")
  private fun amendToCaution(reportedAdjudication: ReportedAdjudication) {
    val preserveDamagesOwed = reportedAdjudication.getPunishments().firstOrNull { it.type == PunishmentType.DAMAGES_OWED }

    legacySyncService.deleteSanctions(adjudicationNumber = reportedAdjudication.chargeNumber.toLong()).run {
      reportedAdjudication.clearPunishments()
      createCaution(reportedAdjudication = reportedAdjudication)
    }.run {
      preserveDamagesOwed?.run {
        reportedAdjudication.addPunishment(
          createPunishmentFromChargeProved(
            chargeNumber = reportedAdjudication.chargeNumber,
            type = PunishmentType.DAMAGES_OWED,
            amount = preserveDamagesOwed.amount,
          ),
        )
      }
    }
  }

  @Deprecated("to remove on completion of NN-5319")
  private fun removeCaution(reportedAdjudication: ReportedAdjudication, punishment: Punishment) {
    legacySyncService.deleteSanction(
      adjudicationNumber = reportedAdjudication.chargeNumber.toLong(),
      sanctionSeq = punishment.sanctionSeq!!,
    ).run {
      punishment.deleted = true
    }
  }

  @Deprecated("to remove on completion of NN-5319")
  private fun createPunishmentFromChargeProved(chargeNumber: String, type: PunishmentType, amount: Double? = null): Punishment =
    Punishment(
      type = type,
      amount = amount,
      schedule = mutableListOf(
        PunishmentSchedule(days = 0),
      ),
    ).also {
      it.sanctionSeq = legacySyncService.createSanction(
        adjudicationNumber = chargeNumber.toLong(),
        sanction = it.mapPunishmentToSanction(),
      )
    }

  @Deprecated("to remove on completion of NN-5319")
  private fun createNewPunishment(punishmentRequest: PunishmentRequest): Punishment =
    Punishment(
      type = punishmentRequest.type,
      privilegeType = punishmentRequest.privilegeType,
      otherPrivilege = punishmentRequest.otherPrivilege,
      stoppagePercentage = punishmentRequest.stoppagePercentage,
      suspendedUntil = punishmentRequest.suspendedUntil,
      consecutiveChargeNumber = punishmentRequest.consecutiveReportNumber,
      schedule = mutableListOf(
        PunishmentSchedule(
          days = punishmentRequest.days,
          startDate = punishmentRequest.startDate,
          endDate = punishmentRequest.endDate,
          suspendedUntil = punishmentRequest.suspendedUntil,
        ),
      ),
    )

  private fun createNewPunishmentV2(punishmentRequest: PunishmentRequestV2): Punishment =
    Punishment(
      type = punishmentRequest.type,
      privilegeType = punishmentRequest.privilegeType,
      otherPrivilege = punishmentRequest.otherPrivilege,
      stoppagePercentage = punishmentRequest.stoppagePercentage,
      suspendedUntil = punishmentRequest.suspendedUntil,
      consecutiveChargeNumber = punishmentRequest.consecutiveReportNumber,
      amount = punishmentRequest.damagesOwedAmount,
      schedule = when (punishmentRequest.type) {
        PunishmentType.CAUTION, PunishmentType.DAMAGES_OWED -> mutableListOf(PunishmentSchedule(days = 0))
        else -> mutableListOf(
          PunishmentSchedule(
            days = punishmentRequest.days!!,
            startDate = punishmentRequest.startDate,
            endDate = punishmentRequest.endDate,
            suspendedUntil = punishmentRequest.suspendedUntil,
          ),
        )
      },
    )

  @Deprecated("to remove on completion of NN-5319")
  private fun updatePunishment(punishment: Punishment, punishmentRequest: PunishmentRequest) =
    punishment.let {
      it.privilegeType = punishmentRequest.privilegeType
      it.otherPrivilege = punishmentRequest.otherPrivilege
      it.stoppagePercentage = punishmentRequest.stoppagePercentage
      it.suspendedUntil = punishmentRequest.suspendedUntil
      if (it.schedule.latestSchedule().hasScheduleBeenUpdated(punishmentRequest)) {
        it.schedule.add(
          PunishmentSchedule(
            days = punishmentRequest.days,
            startDate = punishmentRequest.startDate,
            endDate = punishmentRequest.endDate,
            suspendedUntil = punishmentRequest.suspendedUntil,
          ),
        )
      }
    }

  private fun updatePunishmentV2(punishment: Punishment, punishmentRequest: PunishmentRequestV2) =
    punishment.let {
      it.privilegeType = punishmentRequest.privilegeType
      it.otherPrivilege = punishmentRequest.otherPrivilege
      it.stoppagePercentage = punishmentRequest.stoppagePercentage
      it.suspendedUntil = punishmentRequest.suspendedUntil
      if (it.schedule.latestSchedule().hasScheduleBeenUpdatedV2(punishmentRequest) && !PunishmentType.damagesAndCaution().contains(it.type)) {
        it.schedule.add(
          PunishmentSchedule(
            days = punishmentRequest.days!!,
            startDate = punishmentRequest.startDate,
            endDate = punishmentRequest.endDate,
            suspendedUntil = punishmentRequest.suspendedUntil,
          ),
        )
      }
    }

  @Deprecated("to remove on completion of NN-5319")
  private fun activateSuspendedPunishment(chargeNumber: String, punishmentRequest: PunishmentRequest): Punishment {
    var suspendedPunishment: Punishment? = null
    punishmentRequest.id?.run {
      val activatedFromReport = findByChargeNumber(punishmentRequest.activatedFrom!!)
      suspendedPunishment = activatedFromReport.getPunishments().getSuspendedPunishment(punishmentRequest.id).also {
        it.activatedByChargeNumber = chargeNumber
      }
    }

    return cloneSuspendedPunishment(
      punishment = suspendedPunishment ?: createNewPunishment(punishmentRequest = punishmentRequest),
      days = punishmentRequest.days,
      startDate = punishmentRequest.startDate,
      endDate = punishmentRequest.endDate,
    ).also {
      it.activatedFromChargeNumber = punishmentRequest.activatedFrom
    }
  }

  private fun activateSuspendedPunishmentV2(chargeNumber: String, punishmentRequest: PunishmentRequestV2): Punishment {
    var suspendedPunishment: Punishment? = null
    punishmentRequest.id?.run {
      val activatedFromReport = findByChargeNumber(punishmentRequest.activatedFrom!!)
      suspendedPunishment = activatedFromReport.getPunishments().getSuspendedPunishment(punishmentRequest.id).also {
        it.activatedByChargeNumber = chargeNumber
      }
    }

    return cloneSuspendedPunishment(
      punishment = suspendedPunishment ?: createNewPunishmentV2(punishmentRequest = punishmentRequest),
      days = punishmentRequest.days!!,
      startDate = punishmentRequest.startDate,
      endDate = punishmentRequest.endDate,
    ).also {
      it.activatedFromChargeNumber = punishmentRequest.activatedFrom
    }
  }
  private fun cloneSuspendedPunishment(punishment: Punishment, days: Int, startDate: LocalDate?, endDate: LocalDate?) = Punishment(
    type = punishment.type,
    privilegeType = punishment.privilegeType,
    otherPrivilege = punishment.otherPrivilege,
    stoppagePercentage = punishment.stoppagePercentage,
    schedule = mutableListOf(
      PunishmentSchedule(days = days, startDate = startDate, endDate = endDate),
    ),
  )

  private fun ReportedAdjudication.createSanctionAndAssignSanctionSeq(type: PunishmentType) {
    this.getPunishments().firstOrNull { it.type == type }?.let {
      it.sanctionSeq = legacySyncService.createSanction(
        adjudicationNumber = this.chargeNumber.toLong(),
        sanction = it.mapPunishmentToSanction(),
      )
    }
  }

  companion object {

    @Deprecated("to remove on completion of NN-5319")
    fun ReportedAdjudication.mapToSanctions(): List<OffenderOicSanctionRequest> =
      this.getPunishments().filterOutChargeProvedPunishments().map { it.mapPunishmentToSanction() }

    fun ReportedAdjudication.mapToSanctionsV2(): List<OffenderOicSanctionRequest> =
      this.getPunishments().map { it.mapPunishmentToSanction() }

    fun List<PunishmentSchedule>.latestSchedule() = this.maxBy { it.createDateTime!! }

    fun List<Punishment>.suspendedPunishmentsToActivate() =
      this.filter { p -> p.activatedFromChargeNumber == null && p.activatedByChargeNumber == null && p.schedule.latestSchedule().suspendedUntil != null }

    fun List<Punishment>.getSuspendedPunishment(id: Long): Punishment = this.firstOrNull { it.id == id } ?: throw EntityNotFoundException("suspended punishment not found")

    @Deprecated("to remove on completion of NN-5319")
    fun PunishmentSchedule.hasScheduleBeenUpdated(punishmentRequest: PunishmentRequest): Boolean =
      this.days != punishmentRequest.days || this.endDate != punishmentRequest.endDate || this.startDate != punishmentRequest.startDate ||
        this.suspendedUntil != punishmentRequest.suspendedUntil

    fun PunishmentSchedule.hasScheduleBeenUpdatedV2(punishmentRequest: PunishmentRequestV2): Boolean =
      this.days != punishmentRequest.days || this.endDate != punishmentRequest.endDate || this.startDate != punishmentRequest.startDate ||
        this.suspendedUntil != punishmentRequest.suspendedUntil

    fun List<Punishment>.getPunishment(id: Long): Punishment? =
      this.firstOrNull { it.id == id }
    fun List<Punishment>.getPunishmentToAmend(id: Long): Punishment =
      this.getPunishment(id) ?: throw EntityNotFoundException("Punishment $id is not associated with ReportedAdjudication")

    fun List<PunishmentComment>.getPunishmentComment(id: Long): PunishmentComment =
      this.firstOrNull { it.id == id } ?: throw EntityNotFoundException("Punishment comment id $id is not found")

    @Deprecated("to remove on completion of NN-5319")
    fun PunishmentRequest.validateRequest(latestHearing: Hearing?) {
      when (this.type) {
        PunishmentType.PRIVILEGE -> {
          this.privilegeType ?: throw ValidationException("subtype missing for type PRIVILEGE")
          if (this.privilegeType == PrivilegeType.OTHER) {
            this.otherPrivilege ?: throw ValidationException("description missing for type PRIVILEGE - sub type OTHER")
          }
        }
        PunishmentType.EARNINGS -> this.stoppagePercentage ?: throw ValidationException("stoppage percentage missing for type EARNINGS")
        PunishmentType.PROSPECTIVE_DAYS, PunishmentType.ADDITIONAL_DAYS -> if (!OicHearingType.inadTypes().contains(latestHearing?.oicHearingType)) throw ValidationException("Punishment ${this.type} is invalid as the punishment decision was not awarded by an independent adjudicator")
        else -> {}
      }
      when (this.type) {
        PunishmentType.PROSPECTIVE_DAYS, PunishmentType.ADDITIONAL_DAYS -> {}
        else -> {
          this.suspendedUntil ?: this.startDate ?: this.endDate ?: throw ValidationException("missing all schedule data")
          this.suspendedUntil ?: this.startDate ?: throw ValidationException("missing start date for schedule")
          this.suspendedUntil ?: this.endDate ?: throw ValidationException("missing end date for schedule")
        }
      }
    }

    fun PunishmentRequestV2.validateRequestV2(latestHearing: Hearing?) {
      when (this.type) {
        PunishmentType.DAMAGES_OWED -> this.damagesOwedAmount ?: throw ValidationException("amount missing for type DAMAGES_OWED")
        PunishmentType.PRIVILEGE -> {
          this.privilegeType ?: throw ValidationException("subtype missing for type PRIVILEGE")
          if (this.privilegeType == PrivilegeType.OTHER) {
            this.otherPrivilege ?: throw ValidationException("description missing for type PRIVILEGE - sub type OTHER")
          }
        }
        PunishmentType.EARNINGS -> this.stoppagePercentage ?: throw ValidationException("stoppage percentage missing for type EARNINGS")
        PunishmentType.PROSPECTIVE_DAYS, PunishmentType.ADDITIONAL_DAYS -> if (!OicHearingType.inadTypes().contains(latestHearing?.oicHearingType)) throw ValidationException("Punishment ${this.type} is invalid as the punishment decision was not awarded by an independent adjudicator")
        else -> {}
      }
      when (this.type) {
        PunishmentType.PROSPECTIVE_DAYS, PunishmentType.ADDITIONAL_DAYS, PunishmentType.DAMAGES_OWED, PunishmentType.CAUTION -> {}
        else -> {
          this.suspendedUntil ?: this.startDate ?: this.endDate ?: throw ValidationException("missing all schedule data")
          this.suspendedUntil ?: this.startDate ?: throw ValidationException("missing start date for schedule")
          this.suspendedUntil ?: this.endDate ?: throw ValidationException("missing end date for schedule")
        }
      }
    }

    @Deprecated("to remove on completion of NN-5319")
    fun ReportedAdjudication.validateCanAddPunishments() {
      if (this.status != ReportedAdjudicationStatus.CHARGE_PROVED) {
        throw ValidationException("status is not CHARGE_PROVED")
      }
      if (this.getPunishments().any { it.type == PunishmentType.CAUTION }) {
        throw ValidationException("outcome is a caution - no further punishments can be added")
      }
    }

    fun ReportedAdjudication.validateCanAddPunishmentsV2() {
      if (this.status != ReportedAdjudicationStatus.CHARGE_PROVED) {
        throw ValidationException("status is not CHARGE_PROVED")
      }
    }

    fun String.validatePunishmentCommentAction(username: String) {
      if (this != username) {
        throw ForbiddenException("Only $this can carry out action on punishment comment. attempt by $username")
      }
    }

    fun PunishmentType.includeInSuspendedPunishments(includeAdditionalDays: Boolean): Boolean {
      return if (!PunishmentType.additionalDays().contains(this)) {
        true
      } else {
        includeAdditionalDays
      }
    }

    fun List<PunishmentRequestV2>.validateCaution() {
      if (this.any { it.type == PunishmentType.CAUTION }) {
        if (!this.all { PunishmentType.damagesAndCaution().contains(it.type) }) {
          throw ValidationException("CAUTION can only include DAMAGES_OWED")
        }
      }
    }

    fun ReportedAdjudication.includeAdaWithSameHearingDateAndSeparateCharge(currentAdjudication: ReportedAdjudication): Boolean =
      this.getLatestHearing()?.dateTimeOfHearing?.toLocalDate() == currentAdjudication.getLatestHearing()?.dateTimeOfHearing?.toLocalDate() &&
        this.chargeNumber != currentAdjudication.chargeNumber
  }
}
