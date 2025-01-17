package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import java.time.LocalTime

@Transactional(readOnly = true)
@Service
class SubjectAccessRequestService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  private val offenceCodeLookupService: OffenceCodeLookupService,
  private val locationService: LocationService,
  private val prisonerSearchService: PrisonerSearchService,
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

    val locationCache = mutableMapOf<Long, String?>()
    val prisonerCache = mutableMapOf<String, String?>()

//    var sar_initial_response =  HmppsSubjectAccessRequestContent(content = reported.map { it.toDto(offenceCodeLookupService) })
    val dtos = reported.map { adjudication ->
      val dto = adjudication.toDto(offenceCodeLookupService)

      val prisonerNumber = dto.prisonerNumber
      // Use cache or call the service
      val prisonerName = prisonerCache.getOrPut(prisonerNumber) {
        prisonerSearchService.getPrisonerDetail(prisonerNumber)?.firstName + " " + prisonerSearchService.getPrisonerDetail(prisonerNumber)?.lastName
      }
      // Set the locationName back into incidentDetails
      dto.prisonerName = prisonerName

      // Retrieve the locationId from 'incidentDetails'
      val locationId = dto.incidentDetails?.locationId
      if (locationId != null) {
        // Use cache or call the service
        val locationName = locationCache.getOrPut(locationId) {
          locationService.getNomisLocationDetail(locationId.toString())?.label
        }
        // Set the locationName back into incidentDetails
        dto.incidentDetails?.locationName = locationName
      }

      dto
    }
    return HmppsSubjectAccessRequestContent(content = dtos)
  }
}
