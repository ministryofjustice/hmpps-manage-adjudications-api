package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.TransferService.Companion.log
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.ReportedAdjudicationStatusTransformer
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import java.time.LocalTime

@Transactional(readOnly = true)
@Service
class SubjectAccessRequestService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  private val offenceCodeLookupService: OffenceCodeLookupService,
) : HmppsPrisonSubjectAccessRequestService {

  companion object {
    val minDate: LocalDate = LocalDate.EPOCH
    val maxDate: LocalDate = LocalDate.now()
  }

  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    val reported = reportedAdjudicationRepository.findByPrisonerNumberAndDateTimeOfDiscoveryBetween(
      prisonerNumber = prn,
      fromDate = (fromDate ?: minDate).atStartOfDay(),
      toDate = (toDate ?: maxDate).atTime(LocalTime.MAX),
    )
    if (reported.isEmpty()) return null

//    return HmppsSubjectAccessRequestContent(content = reported.map { it.toDto(offenceCodeLookupService) })
    val dtos = reported.map { adjudication ->
      val dto = adjudication.toDto(offenceCodeLookupService)

      val statusDescription = ReportedAdjudicationStatusTransformer.displayName(dto.status)
      dto.statusDescription = statusDescription
      log.info("added status description for $dto.status to $dto.statusDescription")
      dto
    }
    return HmppsSubjectAccessRequestContent(content = dtos)
  }
}
