package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeFinding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import javax.transaction.Transactional

@Transactional
@Service
class ReferralService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {
  private val outcomeService = OutcomeService(reportedAdjudicationRepository, offenceCodeLookupService, authenticationFacade)
  private val hearingOutcomeService = HearingOutcomeService(reportedAdjudicationRepository, offenceCodeLookupService, authenticationFacade)

  fun createReferral(
    adjudicationNumber: Long,
    hearingId: Long,
    adjudicator: String,
    code: HearingOutcomeCode,
    reason: HearingOutcomeAdjournReason? = null,
    details: String? = null,
    finding: HearingOutcomeFinding? = null,
    plea: HearingOutcomePlea? = null
  ): ReportedAdjudicationDto {
    TODO("implement me")
  }

}
