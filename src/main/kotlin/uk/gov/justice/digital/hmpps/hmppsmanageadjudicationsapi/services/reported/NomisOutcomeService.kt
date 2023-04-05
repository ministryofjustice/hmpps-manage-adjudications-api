package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import javax.transaction.Transactional

@Transactional
@Service
class NomisOutcomeService(
  private val prisonApiGateway: PrisonApiGateway,
) {

  fun createHearingResultIfApplicable(hearing: Hearing?, outcome: Outcome): Long? {
    TODO("implement me")
  }

  fun amendHearingResultIfApplicable(hearing: Hearing?, outcome: Outcome) {
    TODO("implement me")
  }

  fun deleteHearingResultIfApplicable(hearing: Hearing?, outcome: Outcome) {
    TODO("implement me")
  }
}
