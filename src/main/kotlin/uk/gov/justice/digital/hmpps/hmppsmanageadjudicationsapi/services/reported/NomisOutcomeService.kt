package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import javax.transaction.Transactional

@Transactional
@Service
class NomisOutcomeService(
  private val prisonApiGateway: PrisonApiGateway,
) {

  fun createHearingResult(reportedAdjudication: ReportedAdjudication) {
    TODO("implement me")
  }

  fun amendHearingResult(reportedAdjudication: ReportedAdjudication) {
    TODO("implement me")
  }

  fun deleteHearingResult(reportedAdjudication: ReportedAdjudication) {
    TODO("implement me")
  }
}
