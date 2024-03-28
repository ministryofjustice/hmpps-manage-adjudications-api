package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DamageRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Damage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService

@Transactional
@Service
class DraftDamagesService(
  draftAdjudicationRepository: DraftAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : DraftAdjudicationBaseService(
  draftAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {

  fun setDamages(id: Long, damages: List<DamageRequestItem>): DraftAdjudicationDto {
    val draftAdjudication = find(id)
    val reporter = authenticationFacade.currentUsername!!

    draftAdjudication.damages.clear()
    draftAdjudication.damages.addAll(
      damages.map {
        Damage(
          code = it.code,
          details = it.details,
          reporter = reporter,
        )
      },
    )
    draftAdjudication.damagesSaved = true

    return saveToDto(draftAdjudication)
  }
}
