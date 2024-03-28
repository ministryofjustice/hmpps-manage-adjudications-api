package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.EvidenceRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade

@Transactional
@Service
class EvidenceService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  authenticationFacade: AuthenticationFacade,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  authenticationFacade,
) {

  fun updateEvidence(chargeNumber: String, evidence: List<EvidenceRequestItem>): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    val reporter = authenticationFacade.currentUsername!!
    val toPreserve = reportedAdjudication.evidence.filter { it.reporter != reporter }

    reportedAdjudication.evidence.clear()
    reportedAdjudication.evidence.addAll(toPreserve)
    reportedAdjudication.evidence.addAll(
      evidence.filter { it.reporter == reporter }.map {
        ReportedEvidence(
          code = it.code,
          identifier = it.identifier,
          details = it.details,
          reporter = reporter,
        )
      },
    )

    return saveToDto(reportedAdjudication = reportedAdjudication, logLastModified = false)
  }
}
