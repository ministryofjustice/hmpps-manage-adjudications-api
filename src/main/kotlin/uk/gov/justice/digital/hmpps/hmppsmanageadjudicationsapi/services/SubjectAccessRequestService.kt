package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedDtoService

class NoContentFoundException : RuntimeException()

@Transactional(readOnly = true)
@Service
class SubjectAccessRequestService(
  offenceCodeLookupService: OffenceCodeLookupService,
  val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  val objectMapper: ObjectMapper,
) : ReportedDtoService(
  offenceCodeLookupService,
) {
  fun getSubjectAccessRequest(prn: String): JsonNode {
    val reported = reportedAdjudicationRepository.findByPrisonerNumber(prisonerNumber = prn)
    if (reported.isEmpty()) throw NoContentFoundException()

    return objectMapper.readTree(objectMapper.writeValueAsString(reported.map { it.toDto() }))
  }
}
