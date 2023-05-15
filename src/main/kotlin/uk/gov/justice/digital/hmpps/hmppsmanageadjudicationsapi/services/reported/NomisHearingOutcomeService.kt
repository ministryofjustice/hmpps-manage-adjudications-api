package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.transaction.Transactional
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

  fun checkForNomisHearingOutcomesAndUpdate() {
    hearingRepository.findByHearingOutcomeIsNull().forEach { hearing ->
      if (prisonApiGateway.doesHearingOutcomeExist(
          adjudicationNumber = hearing.reportNumber,
          oicHearingId = hearing.oicHearingId,
        )
      ) {
        val reportedAdjudication = reportedAdjudicationRepository.findByReportNumber(hearing.reportNumber)!!
        reportedAdjudication.hearings.filter { it.hearingOutcome == null }.forEach {
          it.hearingOutcome = HearingOutcome(code = HearingOutcomeCode.NOMIS, adjudicator = "")
        }
      }
    }
  }
}
