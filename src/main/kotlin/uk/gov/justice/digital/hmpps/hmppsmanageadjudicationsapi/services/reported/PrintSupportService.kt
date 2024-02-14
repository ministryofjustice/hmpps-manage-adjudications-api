package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.Dis5DataModel
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService

@Transactional(readOnly = true)
@Service
class PrintSupportService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {
  fun getDis5Data(chargeNumber: String): Dis5DataModel {
    val reportedAdjudication = findByChargeNumber(chargeNumber = chargeNumber)
    val currentEstablishment = reportedAdjudication.overrideAgencyId ?: reportedAdjudication.originatingAgencyId
    val otherChargesOnSentence = offenderChargesForPrintSupport(
      offenderBookingId = reportedAdjudication.offenderBookingId!!,
      chargeNumber = chargeNumber,
    )
    TODO("implement me")
  }
}
