package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository

@Transactional
@Service
class PrisonerMergeService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  private val draftAdjudicationRepository: DraftAdjudicationRepository,
) {

  fun merge(prisonerFrom: String?, prisonerTo: String?) {
    prisonerFrom ?: return
    prisonerTo ?: return

    reportedAdjudicationRepository.findByPrisonerNumber(prisonerNumber = prisonerFrom).forEach {
      log.info("prisonerMerge: updating ${it.chargeNumber} from $prisonerFrom to $prisonerTo")
      it.prisonerNumber = prisonerTo
    }

    reportedAdjudicationRepository.findByIncidentRoleAssociatedPrisonersNumber(prisonerNumber = prisonerFrom).forEach {
      log.info("prisonerMerge: updating ${it.chargeNumber} associated prisoner from $prisonerFrom to $prisonerTo")
      it.incidentRoleAssociatedPrisonersNumber = prisonerTo
    }

    draftAdjudicationRepository.findByPrisonerNumber(prisonerNumber = prisonerFrom).forEach {
      log.info("prisonerMerge: updating draft ${it.id} from $prisonerFrom to $prisonerTo")
      it.prisonerNumber = prisonerTo
    }

    draftAdjudicationRepository.findByIncidentRoleAssociatedPrisonersNumber(prisonerNumber = prisonerFrom).forEach {
      log.info("prisonerMerge: updating draft ${it.id} associated prisoner from $prisonerFrom to $prisonerTo")
      it.incidentRole!!.associatedPrisonersNumber = prisonerTo
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
