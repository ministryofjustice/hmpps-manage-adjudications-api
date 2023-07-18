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
  fun amendPunishmentsFromChargeProvedIfApplicable(adjudicationNumber: Long, caution: Boolean, damagesOwed: Boolean?, amount: Double?): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)

    handleCautionChange(reportedAdjudication = reportedAdjudication, caution = caution).run {
      damagesOwed?.run {
        handleDamagesOwedChange(reportedAdjudication = reportedAdjudication, amount = amount)
      }
    }

    return saveToDto(reportedAdjudication)
  }

  fun removeQuashedFinding(reportedAdjudication: ReportedAdjudication) {
    legacySyncService.deleteSanctions(adjudicationNumber = reportedAdjudication.reportNumber)

    reportedAdjudication.createSanctionAndAssignSanctionSeq(type = PunishmentType.CAUTION)
    reportedAdjudication.createSanctionAndAssignSanctionSeq(type = PunishmentType.DAMAGES_OWED)

    legacySyncService.createSanctions(
      adjudicationNumber = reportedAdjudication.reportNumber,
      sanctions = reportedAdjudication.mapToSanctions(),
    )
  }

  @Deprecated("to remove on completion of NN-5319")
  fun create(
    adjudicationNumber: Long,
    punishments: List<PunishmentRequest>,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      it.validateCanAddPunishments()
    }

    punishments.forEach {
      it.validateRequest(reportedAdjudication.getLatestHearing())
      if (it.activatedFrom != null) {
        reportedAdjudication.addPunishment(
          activateSuspendedPunishment(adjudicationNumber = adjudicationNumber, punishmentRequest = it),
        )
      } else {
        reportedAdjudication.addPunishment(createNewPunishment(punishmentRequest = it))
      }
    }

    legacySyncService.createSanctions(
      adjudicationNumber = adjudicationNumber,
      sanctions = reportedAdjudication.mapToSanctions(),
    )

    return saveToDto(reportedAdjudication)
  }

  fun createV2(
    adjudicationNumber: Long,
    punishments: List<PunishmentRequestV2>,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      it.validateCanAddPunishmentsV2()
    }

    punishments.validateCaution()

    punishments.forEach {
      it.validateRequestV2(reportedAdjudication.getLatestHearing())
      if (it.activatedFrom != null) {
        reportedAdjudication.addPunishment(
          activateSuspendedPunishmentV2(adjudicationNumber = adjudicationNumber, punishmentRequest = it),
        )
      } else {
        reportedAdjudication.addPunishment(createNewPunishmentV2(punishmentRequest = it))
      }
    }

    legacySyncService.createSanctions(
      adjudicationNumber = adjudicationNumber,
      sanctions = reportedAdjudication.mapToSanctions(),
    )

    return saveToDto(reportedAdjudication)
  }

  fun update(
    adjudicationNumber: Long,
    punishments: List<PunishmentRequest>,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      it.validateCanAddPunishments()
    }

    val idsToUpdate = punishments.filter { it.id != null }.map { it.id }
    reportedAdjudication.getPunishments().filterOutChargeProvedPunishments().filter { !idsToUpdate.contains(it.id) }.forEach { punishment ->
      punishment.type.consecutiveReportValidation(adjudicationNumber).let {
        punishment.deleted = true
      }
    }

    punishments.forEach { punishmentRequest ->
      punishmentRequest.validateRequest(reportedAdjudication.getLatestHearing())

      when (punishmentRequest.id) {
        null -> when (punishmentRequest.activatedFrom) {
          null -> reportedAdjudication.addPunishment(createNewPunishment(punishmentRequest = punishmentRequest))
          else -> reportedAdjudication.addPunishment(activateSuspendedPunishment(adjudicationNumber = adjudicationNumber, punishmentRequest = punishmentRequest))
        }
        else -> {
          when (punishmentRequest.activatedFrom) {
            null -> {
              val punishmentToAmend = reportedAdjudication.getPunishments().getPunishmentToAmend(punishmentRequest.id)
              when (punishmentToAmend.type) {
                punishmentRequest.type -> {
                  punishmentRequest.suspendedUntil?.let {
                    punishmentRequest.type.consecutiveReportValidation(adjudicationNumber)
                  }
                  updatePunishment(punishmentToAmend, punishmentRequest)
                }
                else -> {
                  punishmentToAmend.type.consecutiveReportValidation(adjudicationNumber).let {
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
                    adjudicationNumber = adjudicationNumber,
                    punishmentRequest = punishmentRequest,
                  ),
                )
              }
          }
        }
      }
    }

    legacySyncService.updateSanctions(
      adjudicationNumber = adjudicationNumber,
      sanctions = reportedAdjudication.mapToSanctions(),
    ).run {
      reportedAdjudication.getPunishments().firstOrNull { it.type == PunishmentType.DAMAGES_OWED }?.let {
        it.sanctionSeq = createPunishmentFromChargeProved(
          adjudicationNumber = reportedAdjudication.reportNumber,
          type = PunishmentType.DAMAGES_OWED,
          amount = it.amount!!,
        ).sanctionSeq
      }
    }

    return saveToDto(reportedAdjudication)
  }

  private fun PunishmentType.consecutiveReportValidation(adjudicationNumber: Long) {
    if (PunishmentType.additionalDays().contains(this)) {
      if (isLinkedToReport(adjudicationNumber, this)) {
        throw ValidationException("Unable to modify: $this is linked to another report")
      }
    }
  }

  fun getSuspendedPunishments(prisonerNumber: String, reportNumber: Long? = null): List<SuspendedPunishmentDto> {
    val reportsWithSuspendedPunishments = getReportsWithSuspendedPunishments(prisonerNumber = prisonerNumber)
    var includeAdditionalDays: Boolean? = null
    reportNumber?.let {
      includeAdditionalDays = includeAdditionalDays(it)
    }

    return reportsWithSuspendedPunishments.map {
      it.getPunishments().suspendedPunishmentsToActivate()
        .filter { punishment -> reportNumber == null || punishment.type.includeInSuspendedPunishments(includeAdditionalDays!!) }.map { punishment ->
          val schedule = punishment.schedule.latestSchedule()

          SuspendedPunishmentDto(
            reportNumber = it.reportNumber,
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

  fun getReportsWithAdditionalDays(prisonerNumber: String, punishmentType: PunishmentType): List<AdditionalDaysDto> {
    if (!PunishmentType.additionalDays().contains(punishmentType)) throw ValidationException("Punishment type must be ADDITIONAL_DAYS or PROSPECTIVE_DAYS")

    return getReportsWithActiveAdditionalDays(
      prisonerNumber = prisonerNumber,
      punishmentType = punishmentType,
    ).map {
      it.getPunishments().filter { punishment -> PunishmentType.additionalDays().contains(punishment.type) }.map { punishment ->
        val schedule = punishment.schedule.latestSchedule()

        AdditionalDaysDto(
          reportNumber = it.reportNumber,
          chargeProvedDate = it.getLatestHearing()?.dateTimeOfHearing?.toLocalDate()!!,
          punishment = PunishmentDto(
            id = punishment.id,
            type = punishment.type,
            schedule = PunishmentScheduleDto(days = schedule.days),
          ),
        )
      }
    }.flatten()
  }

  fun createPunishmentComment(
    adjudicationNumber: Long,
    punishmentComment: PunishmentCommentRequest,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)

    reportedAdjudication.punishmentComments.add(
      PunishmentComment(
        comment = punishmentComment.comment,
      ),
    )

    return saveToDto(reportedAdjudication)
  }

  fun updatePunishmentComment(
    adjudicationNumber: Long,
    punishmentComment: PunishmentCommentRequest,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    val punishmentCommentToUpdate = reportedAdjudication.punishmentComments.getPunishmentComment(punishmentComment.id!!)
      .also {
        it.createdByUserId?.validatePunishmentCommentAction(authenticationFacade.currentUsername!!)
      }

    punishmentCommentToUpdate.comment = punishmentComment.comment

    return saveToDto(reportedAdjudication)
  }

  fun deletePunishmentComment(
    adjudicationNumber: Long,
    punishmentCommentId: Long,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    val punishmentComment = reportedAdjudication.punishmentComments.getPunishmentComment(punishmentCommentId)
      .also {
        it.createdByUserId?.validatePunishmentCommentAction(authenticationFacade.currentUsername!!)
      }

    reportedAdjudication.punishmentComments.remove(punishmentComment)

    return saveToDto(reportedAdjudication)
  }

  private fun includeAdditionalDays(reportNumber: Long): Boolean {
    val reportedAdjudication = findByAdjudicationNumber(reportNumber)
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
        adjudicationNumber = reportedAdjudication.reportNumber,
        type = PunishmentType.DAMAGES_OWED,
        amount = amount,
      ),
    )
  }

  @Deprecated("to remove on completion of NN-5319")
  private fun deleteDamagesOwed(reportedAdjudication: ReportedAdjudication, punishment: Punishment) {
    legacySyncService.deleteSanction(
      adjudicationNumber = reportedAdjudication.reportNumber,
      sanctionSeq = punishment.sanctionSeq!!,
    ).run {
      punishment.deleted = true
    }
  }

  @Deprecated("to remove on completion of NN-5319")
  private fun amendDamagesOwed(reportedAdjudication: ReportedAdjudication, punishment: Punishment, amount: Double) {
    legacySyncService.deleteSanction(adjudicationNumber = reportedAdjudication.reportNumber, sanctionSeq = punishment.sanctionSeq!!).run {
      punishment.amount = amount
      punishment.sanctionSeq = legacySyncService.createSanction(
        adjudicationNumber = reportedAdjudication.reportNumber,
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
        adjudicationNumber = reportedAdjudication.reportNumber,
        type = PunishmentType.CAUTION,
      ),
    )
  }

  @Deprecated("to remove on completion of NN-5319")
  private fun amendToCaution(reportedAdjudication: ReportedAdjudication) {
    val preserveDamagesOwed = reportedAdjudication.getPunishments().firstOrNull { it.type == PunishmentType.DAMAGES_OWED }

    legacySyncService.deleteSanctions(adjudicationNumber = reportedAdjudication.reportNumber).run {
      reportedAdjudication.clearPunishments()
      createCaution(reportedAdjudication = reportedAdjudication)
    }.run {
      preserveDamagesOwed?.run {
        reportedAdjudication.addPunishment(
          createPunishmentFromChargeProved(
            adjudicationNumber = reportedAdjudication.reportNumber,
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
      adjudicationNumber = reportedAdjudication.reportNumber,
      sanctionSeq = punishment.sanctionSeq!!,
    ).run {
      punishment.deleted = true
    }
  }

  @Deprecated("to remove on completion of NN-5319")
  private fun createPunishmentFromChargeProved(adjudicationNumber: Long, type: PunishmentType, amount: Double? = null): Punishment =
    Punishment(
      type = type,
      amount = amount,
      schedule = mutableListOf(
        PunishmentSchedule(days = 0),
      ),
    ).also {
      it.sanctionSeq = legacySyncService.createSanction(
        adjudicationNumber = adjudicationNumber,
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
      consecutiveReportNumber = punishmentRequest.consecutiveReportNumber,
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
      consecutiveReportNumber = punishmentRequest.consecutiveReportNumber,
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

  @Deprecated("to remove on completion of NN-5319")
  private fun activateSuspendedPunishment(adjudicationNumber: Long, punishmentRequest: PunishmentRequest): Punishment {
    var suspendedPunishment: Punishment? = null
    punishmentRequest.id?.run {
      val activatedFromReport = findByAdjudicationNumber(punishmentRequest.activatedFrom!!)
      suspendedPunishment = activatedFromReport.getPunishments().getSuspendedPunishment(punishmentRequest.id).also {
        it.activatedBy = adjudicationNumber
      }
    }

    return cloneSuspendedPunishment(
      punishment = suspendedPunishment ?: createNewPunishment(punishmentRequest = punishmentRequest),
      days = punishmentRequest.days,
      startDate = punishmentRequest.startDate,
      endDate = punishmentRequest.endDate,
    ).also {
      it.activatedFrom = punishmentRequest.activatedFrom
    }
  }

  private fun activateSuspendedPunishmentV2(adjudicationNumber: Long, punishmentRequest: PunishmentRequestV2): Punishment {
    var suspendedPunishment: Punishment? = null
    punishmentRequest.id?.run {
      val activatedFromReport = findByAdjudicationNumber(punishmentRequest.activatedFrom!!)
      suspendedPunishment = activatedFromReport.getPunishments().getSuspendedPunishment(punishmentRequest.id).also {
        it.activatedBy = adjudicationNumber
      }
    }

    return cloneSuspendedPunishment(
      punishment = suspendedPunishment ?: createNewPunishmentV2(punishmentRequest = punishmentRequest),
      days = punishmentRequest.days!!,
      startDate = punishmentRequest.startDate,
      endDate = punishmentRequest.endDate,
    ).also {
      it.activatedFrom = punishmentRequest.activatedFrom
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
        adjudicationNumber = this.reportNumber,
        sanction = it.mapPunishmentToSanction(),
      )
    }
  }

  companion object {

    fun ReportedAdjudication.mapToSanctions(): List<OffenderOicSanctionRequest> =
      this.getPunishments().filterOutChargeProvedPunishments().map { it.mapPunishmentToSanction() }

    fun List<PunishmentSchedule>.latestSchedule() = this.maxBy { it.createDateTime!! }

    fun List<Punishment>.suspendedPunishmentsToActivate() =
      this.filter { p -> p.activatedFrom == null && p.activatedBy == null && p.schedule.latestSchedule().suspendedUntil != null }

    fun List<Punishment>.getSuspendedPunishment(id: Long): Punishment = this.firstOrNull { it.id == id } ?: throw EntityNotFoundException("suspended punishment not found")

    fun PunishmentSchedule.hasScheduleBeenUpdated(punishmentRequest: PunishmentRequest): Boolean =
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
  }
}
