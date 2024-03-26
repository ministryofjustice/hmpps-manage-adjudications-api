package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedDtoService
import java.time.LocalDate
import java.time.LocalTime

class NoContentFoundException : RuntimeException()
class MissingPRN : RuntimeException()

@Transactional(readOnly = true)
@Service
class SubjectAccessRequestService(
  offenceCodeLookupService: OffenceCodeLookupService,
  val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  val objectMapper: ObjectMapper,
) : ReportedDtoService(
  offenceCodeLookupService,
) {
  fun getSubjectAccessRequest(prn: String, fromDate: LocalDate?, toDate: LocalDate?): JsonNode {
    val reported = reportedAdjudicationRepository.findByPrisonerNumberAndDateTimeOfDiscoveryBetween(
      prisonerNumber = prn,
      fromDate = (fromDate ?: minDate).atStartOfDay(),
      toDate = (toDate ?: maxDate).atTime(LocalTime.MAX),
    )

    if (reported.isEmpty()) throw NoContentFoundException()

    return objectMapper.readTree(objectMapper.writeValueAsString(reported.map { it.toDto() }))
  }

  companion object {
    val minDate: LocalDate = LocalDate.of(1901, 1, 1)
    val maxDate: LocalDate = LocalDate.of(2999, 1, 1)
  }
}
