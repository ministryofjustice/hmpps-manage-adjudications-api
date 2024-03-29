package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DamageRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService

@Transactional
@Service
class DamagesService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {
  fun updateDamages(chargeNumber: String, damages: List<DamageRequestItem>): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    val reporter = authenticationFacade.currentUsername!!
    val toPreserve = reportedAdjudication.damages.filter { it.reporter != reporter }

    reportedAdjudication.damages.clear()
    reportedAdjudication.damages.addAll(toPreserve)
    reportedAdjudication.damages.addAll(
      damages.filter { it.reporter == reporter }.map {
        ReportedDamage(
          code = it.code,
          details = it.details,
          reporter = reporter,
        )
      },
    )

    return saveToDto(reportedAdjudication = reportedAdjudication, logLastModified = false)
  }
}
