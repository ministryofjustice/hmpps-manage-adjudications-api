package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.EvidenceRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Evidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import javax.transaction.Transactional

@Transactional
@Service
class DraftEvidenceService(
  draftAdjudicationRepository: DraftAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  val authenticationFacade: AuthenticationFacade,
) : DraftAdjudicationBaseService(
  draftAdjudicationRepository, offenceCodeLookupService
) {

  fun setEvidence(id: Long, evidence: List<EvidenceRequestItem>): DraftAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }
    val reporter = authenticationFacade.currentUsername!!

    draftAdjudication.evidence.clear()
    draftAdjudication.evidence.addAll(
      evidence.map {
        Evidence(
          code = it.code,
          identifier = it.identifier,
          details = it.details,
          reporter = reporter
        )
      }
    )
    draftAdjudication.evidenceSaved = true

    return saveToDto(draftAdjudication)
  }
}
