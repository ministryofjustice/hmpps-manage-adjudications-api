package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentCommentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentComment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OffenderOicSanctionRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OffenderOicSanctionRequest.Companion.mapPunishmentToSanction
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import java.time.LocalDate

@Transactional
@Service
class PunishmentsService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
  private val prisonApiGateway: PrisonApiGateway,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {

  fun createPunishmentsFromChargeProvedIfApplicable(reportedAdjudication: ReportedAdjudication, caution: Boolean, amount: Double?) {
    if (caution) createCaution(reportedAdjudication = reportedAdjudication)

    amount?.run {
      createDamagesOwed(reportedAdjudication = reportedAdjudication, amount = amount)
    }
  }

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
    prisonApiGateway.deleteSanctions(adjudicationNumber = reportedAdjudication.reportNumber)

    reportedAdjudication.createSanctionAndAssignSanctionSeq(type = PunishmentType.CAUTION)
    reportedAdjudication.createSanctionAndAssignSanctionSeq(type = PunishmentType.DAMAGES_OWED)

    prisonApiGateway.createSanctions(
      adjudicationNumber = reportedAdjudication.reportNumber,
      sanctions = reportedAdjudication.mapToSanctions(),
    )
  }

  fun create(
    adjudicationNumber: Long,
    punishments: List<PunishmentRequest>,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      it.validateCanAddPunishments()
    }

    punishments.forEach {
      it.validateRequest()
      if (it.activatedFrom != null) {
        reportedAdjudication.addPunishment(
          activateSuspendedPunishment(adjudicationNumber = adjudicationNumber, punishmentRequest = it),
        )
      } else {
        reportedAdjudication.addPunishment(createNewPunishment(punishmentRequest = it))
      }
    }

    prisonApiGateway.createSanctions(
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

    val ids = punishments.filter { it.id != null }.map { it.id }
    reportedAdjudication.getPunishments().filterOutChargeProvedPunishments().filter { !ids.contains(it.id) }.forEach {
      it.deleted = true
    }

    punishments.forEach {
      it.validateRequest()

      when (it.id) {
        null -> when (it.activatedFrom) {
          null -> reportedAdjudication.addPunishment(createNewPunishment(punishmentRequest = it))
          else -> reportedAdjudication.addPunishment(activateSuspendedPunishment(adjudicationNumber = adjudicationNumber, punishmentRequest = it))
        }
        else -> {
          when (it.activatedFrom) {
            null -> {
              val punishmentToAmend = reportedAdjudication.getPunishments().getPunishmentToAmend(it.id)
              when (punishmentToAmend.type) {
                it.type -> updatePunishment(punishmentToAmend, it)
                else -> {
                  punishmentToAmend.deleted = true
                  reportedAdjudication.addPunishment(createNewPunishment(punishmentRequest = it))
                }
              }
            }
            else ->
              if (reportedAdjudication.getPunishments().getPunishment(it.id) == null) {
                reportedAdjudication.addPunishment(
                  activateSuspendedPunishment(
                    adjudicationNumber = adjudicationNumber,
                    punishmentRequest = it,
                  ),
                )
              }
          }
        }
      }
    }

    prisonApiGateway.updateSanctions(
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

  fun getSuspendedPunishments(prisonerNumber: String): List<SuspendedPunishmentDto> {
    val reportsWithSuspendedPunishments = getReportsWithSuspendedPunishments(prisonerNumber = prisonerNumber)

    return reportsWithSuspendedPunishments.map {
      it.getPunishments().suspendedPunishmentsToActivate().map { p ->
        val schedule = p.schedule.latestSchedule()

        SuspendedPunishmentDto(
          reportNumber = it.reportNumber,
          punishment = PunishmentDto(
            id = p.id,
            type = p.type,
            privilegeType = p.privilegeType,
            otherPrivilege = p.otherPrivilege,
            stoppagePercentage = p.stoppagePercentage,
            schedule = PunishmentScheduleDto(days = schedule.days, suspendedUntil = schedule.suspendedUntil),
          ),
        )
      }
    }.flatten()
  }

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

  private fun createDamagesOwed(reportedAdjudication: ReportedAdjudication, amount: Double) {
    reportedAdjudication.addPunishment(
      createPunishmentFromChargeProved(
        adjudicationNumber = reportedAdjudication.reportNumber,
        type = PunishmentType.DAMAGES_OWED,
        amount = amount,
      ),
    )
  }

  private fun deleteDamagesOwed(reportedAdjudication: ReportedAdjudication, punishment: Punishment) {
    prisonApiGateway.deleteSanction(
      adjudicationNumber = reportedAdjudication.reportNumber,
      sanctionSeq = punishment.sanctionSeq!!,
    ).run {
      punishment.deleted = true
    }
  }

  private fun amendDamagesOwed(reportedAdjudication: ReportedAdjudication, punishment: Punishment, amount: Double) {
    prisonApiGateway.deleteSanction(adjudicationNumber = reportedAdjudication.reportNumber, sanctionSeq = punishment.sanctionSeq!!).run {
      punishment.amount = amount
      punishment.sanctionSeq = prisonApiGateway.createSanction(
        adjudicationNumber = reportedAdjudication.reportNumber,
        sanction = punishment.mapPunishmentToSanction(),
      )
    }
  }

  private fun handleCautionChange(reportedAdjudication: ReportedAdjudication, caution: Boolean) {
    when (val cautionPunishment = reportedAdjudication.getPunishments().firstOrNull { it.type == PunishmentType.CAUTION }) {
      null -> if (caution) amendToCaution(reportedAdjudication = reportedAdjudication)
      else -> if (!caution) removeCaution(reportedAdjudication = reportedAdjudication, punishment = cautionPunishment)
    }
  }

  private fun createCaution(reportedAdjudication: ReportedAdjudication) {
    reportedAdjudication.addPunishment(
      createPunishmentFromChargeProved(
        adjudicationNumber = reportedAdjudication.reportNumber,
        type = PunishmentType.CAUTION,
      ),
    )
  }

  private fun amendToCaution(reportedAdjudication: ReportedAdjudication) {
    val preserveDamagesOwed = reportedAdjudication.getPunishments().firstOrNull { it.type == PunishmentType.DAMAGES_OWED }

    prisonApiGateway.deleteSanctions(adjudicationNumber = reportedAdjudication.reportNumber).run {
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

  private fun removeCaution(reportedAdjudication: ReportedAdjudication, punishment: Punishment) {
    prisonApiGateway.deleteSanction(
      adjudicationNumber = reportedAdjudication.reportNumber,
      sanctionSeq = punishment.sanctionSeq!!,
    ).run {
      punishment.deleted = true
    }
  }

  private fun createPunishmentFromChargeProved(adjudicationNumber: Long, type: PunishmentType, amount: Double? = null): Punishment =
    Punishment(
      type = type,
      amount = amount,
      schedule = mutableListOf(
        PunishmentSchedule(days = 0),
      ),
    ).also {
      it.sanctionSeq = prisonApiGateway.createSanction(
        adjudicationNumber = adjudicationNumber,
        sanction = it.mapPunishmentToSanction(),
      )
    }

  private fun createNewPunishment(punishmentRequest: PunishmentRequest): Punishment =
    Punishment(
      type = punishmentRequest.type,
      privilegeType = punishmentRequest.privilegeType,
      otherPrivilege = punishmentRequest.otherPrivilege,
      stoppagePercentage = punishmentRequest.stoppagePercentage,
      suspendedUntil = punishmentRequest.suspendedUntil,
      schedule = mutableListOf(
        PunishmentSchedule(
          days = punishmentRequest.days,
          startDate = punishmentRequest.startDate,
          endDate = punishmentRequest.endDate,
          suspendedUntil = punishmentRequest.suspendedUntil,
        ),
      ),
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
      it.sanctionSeq = prisonApiGateway.createSanction(
        adjudicationNumber = this.reportNumber,
        sanction = it.mapPunishmentToSanction(),
      )
    }
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

    fun PunishmentRequest.validateRequest() {
      when (this.type) {
        PunishmentType.PRIVILEGE -> {
          this.privilegeType ?: throw ValidationException("subtype missing for type PRIVILEGE")
          if (this.privilegeType == PrivilegeType.OTHER) {
            this.otherPrivilege ?: throw ValidationException("description missing for type PRIVILEGE - sub type OTHER")
          }
        }
        PunishmentType.EARNINGS -> this.stoppagePercentage ?: throw ValidationException("stoppage percentage missing for type EARNINGS")
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

    fun ReportedAdjudication.validateCanAddPunishments() {
      if (this.status != ReportedAdjudicationStatus.CHARGE_PROVED) {
        throw ValidationException("status is not CHARGE_PROVED")
      }
      if (this.getPunishments().any { it.type == PunishmentType.CAUTION }) {
        throw ValidationException("outcome is a caution - no further punishments can be added")
      }
    }
  }
}
