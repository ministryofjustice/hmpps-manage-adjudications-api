package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.WitnessRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Witness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService

@Transactional
@Service
class DraftWitnessesService(
  draftAdjudicationRepository: DraftAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : DraftAdjudicationBaseService(
  draftAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {

  fun setWitnesses(id: Long, witnesses: List<WitnessRequestItem>): DraftAdjudicationDto {
    val draftAdjudication = find(id)
    val reporter = authenticationFacade.currentUsername!!

    draftAdjudication.witnesses.clear()
    draftAdjudication.witnesses.addAll(
      witnesses.map {
        Witness(
          code = it.code,
          firstName = it.firstName,
          lastName = it.lastName,
          reporter = reporter,
          username = it.username,
        )
      },
    )
    draftAdjudication.witnessesSaved = true

    return saveToDto(draftAdjudication)
  }
}
