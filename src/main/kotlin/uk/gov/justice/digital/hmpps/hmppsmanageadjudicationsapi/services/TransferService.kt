package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository

@Transactional
@Service
class TransferService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {

  fun processTransferEvent(prisonerNumber: String?, agencyId: String?) {
    if (prisonerNumber != null && agencyId != null) {
      log.info("transfer event for $prisonerNumber to $agencyId")
      reportedAdjudicationRepository.findByPrisonerNumberAndStatusIn(
        prisonerNumber = prisonerNumber,
        statuses = transferableStatuses,
      ).forEach {
        log.info("transferring report ${it.chargeNumber} from ${it.originatingAgencyId} to $agencyId")
        if (it.originatingAgencyId != agencyId) {
          it.overrideAgencyId = agencyId
        } else {
          // we remove the lock, and the originating agency has full control again
          it.overrideAgencyId = null
        }
      }
    }
  }

  companion object {
    val transferableStatuses = listOf(
      ReportedAdjudicationStatus.AWAITING_REVIEW,
      ReportedAdjudicationStatus.UNSCHEDULED,
      ReportedAdjudicationStatus.SCHEDULED,
      ReportedAdjudicationStatus.ADJOURNED,
      ReportedAdjudicationStatus.REFER_POLICE,
      ReportedAdjudicationStatus.REFER_INAD,
      ReportedAdjudicationStatus.CHARGE_PROVED,
    )

    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
