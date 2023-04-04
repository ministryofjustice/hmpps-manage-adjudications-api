package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.WitnessRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import javax.transaction.Transactional

@Transactional
@Service
class WitnessesService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {

  fun updateWitnesses(adjudicationNumber: Long, witnesses: List<WitnessRequestItem>): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    val reporter = authenticationFacade.currentUsername!!
    val toPreserve = reportedAdjudication.witnesses.filter { it.reporter != reporter }

    reportedAdjudication.witnesses.clear()
    reportedAdjudication.witnesses.addAll(toPreserve)
    reportedAdjudication.witnesses.addAll(
      witnesses.filter { it.reporter == reporter }.map {
        ReportedWitness(
          code = it.code,
          firstName = it.firstName,
          lastName = it.lastName,
          reporter = reporter,
        )
      },
    )

    return saveToDto(reportedAdjudication)
  }
}
