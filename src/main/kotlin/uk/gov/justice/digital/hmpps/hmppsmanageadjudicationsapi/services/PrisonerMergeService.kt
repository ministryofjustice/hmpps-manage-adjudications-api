package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository

@Transactional
@Service
class PrisonerMergeService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {

  fun merge(prisonerFrom: String?, prisonerTo: String?) {
    prisonerFrom ?: return
    prisonerTo ?: return

    reportedAdjudicationRepository.findByPrisonerNumber(prisonerNumber = prisonerFrom).forEach {
      log.info("prisonerMerge: updating ${it.chargeNumber} from $prisonerFrom to $prisonerTo")
      it.prisonerNumber = prisonerTo
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
