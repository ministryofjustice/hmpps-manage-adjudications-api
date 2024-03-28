package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.WitnessRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade

@Transactional
@Service
class WitnessesService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  authenticationFacade: AuthenticationFacade,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  authenticationFacade,
) {

  fun updateWitnesses(chargeNumber: String, witnesses: List<WitnessRequestItem>): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
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

    return saveToDto(reportedAdjudication = reportedAdjudication, logLastModified = false)
  }
}
