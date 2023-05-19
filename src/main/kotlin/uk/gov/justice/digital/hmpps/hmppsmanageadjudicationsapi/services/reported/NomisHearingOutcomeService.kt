package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.HearingRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository

@Transactional
@Service
class NomisHearingOutcomeService(
  private val prisonApiGateway: PrisonApiGateway,
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  private val hearingRepository: HearingRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun checkForNomisHearingOutcomesAndUpdate() {
    log.info("Lock down record check: checking for NOMIS hearing results.")
    hearingRepository.findByHearingOutcomeIsNull().forEach { hearing ->
      if (prisonApiGateway.hearingOutcomesExistInNomis(
          adjudicationNumber = hearing.reportNumber,
          oicHearingId = hearing.oicHearingId,
        )
      ) {
        log.info("Adjudication hearing ${hearing.id} has results created in NOMIS, locking hearing outcomes")
        reportedAdjudicationRepository.findByReportNumber(hearing.reportNumber)?.let {
          it.hearings.filter { hearing -> hearing.hearingOutcome == null }.forEach { hearing ->
            hearing.hearingOutcome = HearingOutcome(code = HearingOutcomeCode.NOMIS, adjudicator = "")
          }
        }
      }
    }
  }
}
