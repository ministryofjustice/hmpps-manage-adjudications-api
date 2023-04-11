package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService

@Transactional
@Service
class PunishmentsService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {

  fun create(
    adjudicationNumber: Long,
    punishments: List<PunishmentRequest>,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      it.status.validateCanAddPunishments()
    }

    punishments.forEach {
      when (it.type) {
        PunishmentType.PRIVILEGE -> {
          it.privilegeType ?: throw ValidationException("subtype missing for type PRIVILEGE")
          if (it.privilegeType == PrivilegeType.OTHER) {
            it.otherPrivilege ?: throw ValidationException("description missing for type PRIVILEGE - sub type OTHER")
          }
        }
        PunishmentType.EARNINGS -> it.stoppagePercentage ?: throw ValidationException("stoppage percentage missing for type EARNINGS")
        else -> {}
      }
      when (it.type) {
        PunishmentType.PROSPECTIVE_DAYS, PunishmentType.ADDITIONAL_DAYS -> {}
        else -> {
          it.suspendedUntil ?: it.startDate ?: it.endDate ?: throw ValidationException("missing all schedule data")
          it.suspendedUntil ?: it.startDate ?: throw ValidationException("missing start date for schedule")
          it.suspendedUntil ?: it.endDate ?: throw ValidationException("missing end date for schedule")
        }
      }

      reportedAdjudication.punishments.add(
        Punishment(
          type = it.type,
          privilegeType = it.privilegeType,
          otherPrivilege = it.otherPrivilege,
          stoppagePercentage = it.stoppagePercentage,
          schedule = mutableListOf(
            PunishmentSchedule(days = it.days, startDate = it.startDate, endDate = it.endDate, suspendedUntil = it.suspendedUntil),
          ),
        ),
      )
    }

    return saveToDto(reportedAdjudication)
  }

  companion object {
    fun ReportedAdjudicationStatus.validateCanAddPunishments() {
      if (this != ReportedAdjudicationStatus.CHARGE_PROVED) {
        throw ValidationException("status is not CHARGE_PROVED")
      }
    }
  }
}
