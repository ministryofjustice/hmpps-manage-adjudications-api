package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
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
    code: HearingOutcomeCode,
    adjudicator: String,
    details: String,
  ): ReportedAdjudicationDto {
    TODO("implement me")
  }

  fun updateReferral(
    adjudicationNumber: Long,
    hearingId: Long,
    code: HearingOutcomeCode,
    adjudicator: String,
    details: String,
  ): ReportedAdjudicationDto {
    TODO("implement me")
  }
}
