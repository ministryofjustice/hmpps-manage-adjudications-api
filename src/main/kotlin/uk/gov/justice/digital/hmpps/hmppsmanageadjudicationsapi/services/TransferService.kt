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
      ).filter { it.agencyId != agencyId }.forEach {
        log.info("transferring report ${it.reportNumber} from ${it.agencyId} to $agencyId")
        it.overrideAgencyId = agencyId
      }
    }
  }

  companion object {
    val transferableStatuses = listOf(
      ReportedAdjudicationStatus.UNSCHEDULED,
      ReportedAdjudicationStatus.SCHEDULED,
      ReportedAdjudicationStatus.ADJOURNED,
      ReportedAdjudicationStatus.REFER_POLICE,
      ReportedAdjudicationStatus.REFER_INAD,
    )

    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
