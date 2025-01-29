package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.TransferService.Companion.log
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.DamageCodeTransformer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.EvidenceCodeTransformer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.HearingOutcomeTransformer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.OffenceCodeTransformer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.OicHearingTypeTransformer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.PrivilegeTypeTransformer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.PunishmentCommentTransformer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.PunishmentTypeTransformer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.ReportedAdjudicationStatusTransformer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.WitnessCodeTransformer
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

    val dtos = reported.map { adjudication ->
      val dto = adjudication.toDto(offenceCodeLookupService)

      val prisonerNumber = dto.prisonerNumber
      // Use cache or call the service
      val prisonerName = prisonerCache.getOrPut(prisonerNumber) {
        val prisonerDet = prisonerSearchService.getPrisonerDetail(prisonerNumber)
        prisonerDet?.firstName + " " + prisonerDet?.lastName
      }
      // Set the locationName back into incidentDetails
      dto.prisonerName = prisonerName

      // Retrieve the locationId from 'incidentDetails'
      val locationId = dto.incidentDetails?.locationId
      if (locationId != null) {
        // Use cache or call the service
        val locationName = locationCache.getOrPut(locationId) {
          // First, call the getNomisLocationDetail to find the DPS location ID
          val dpsLocationId = locationService.getNomisLocationDetail(locationId.toString())?.dpsLocationId
          // If dpsLocationId is non-null, call getLocationDetail and return its localName
          dpsLocationId?.let { id ->
            locationService.getLocationDetail(id)?.localName
          }
        }
        // Set the locationName back into incidentDetails
        dto.incidentDetails?.locationName = locationName
      }

      val statusDescription = ReportedAdjudicationStatusTransformer.displayName(dto.status)
      dto.statusDescription = statusDescription
      log.info("added status description for ${dto.status} to ${dto.statusDescription}")

      // Transform each piece of damages
      dto.damages.forEach { damageItem ->
        val damageDescription = DamageCodeTransformer.displayName(damageItem.code)
        damageItem.codeDescription = damageDescription
        log.info("Transformed evidence code ${damageItem.code} -> $damageDescription")
      }

      // Transform each piece of evidence
      dto.evidence.forEach { evidenceItem ->
        val evidenceDescription = EvidenceCodeTransformer.displayName(evidenceItem.code)
        evidenceItem.codeDescription = evidenceDescription
        log.info("Transformed evidence code ${evidenceItem.code} -> $evidenceDescription")
      }

      // Transform each witness
      dto.witnesses.forEach { witnessItem ->
        val witnessDescription = WitnessCodeTransformer.displayName(witnessItem.code)
        witnessItem.codeDescription = witnessDescription
        log.info("Transformed witness code ${witnessItem.code} -> $witnessDescription")
      }

      // Transform each punishment
      dto.punishments.forEach { punishmentItem ->
        val punishmentTypeDescription = PunishmentTypeTransformer.displayName(punishmentItem.type)
        punishmentItem.typeDescription = punishmentTypeDescription

        val privilegeTypeDescription = punishmentItem.privilegeType?.let { PrivilegeTypeTransformer.displayName(it) }
        punishmentItem.privilegeTypeDescription = privilegeTypeDescription
      }

      // Transform each punishmentComments
      dto.punishmentComments.forEach { punishmentCommentsItem ->
        val punishmentCommentDescription = punishmentCommentsItem.reasonForChange?.let {
          PunishmentCommentTransformer.displayName(it)
        }
        punishmentCommentsItem.reasonForChangeDescription = punishmentCommentDescription
      }

      // Transform each hearings
      dto.hearings.forEach { hearingItem ->
        val ociHearingDescription = OicHearingTypeTransformer.displayName(hearingItem.oicHearingType)
        hearingItem.oicHearingTypeDescription = ociHearingDescription

        val hearingOutcomeCodeDescription = hearingItem.outcome?.code?.let {
          HearingOutcomeTransformer.displayOutcomeCodeName(it)
        }
        hearingItem.outcome?.codeDescription = hearingOutcomeCodeDescription

        val hearingOutcomePleaDescription = hearingItem.outcome?.plea?.let {
          HearingOutcomeTransformer.displayOutcomePleaName(it)
        }
        hearingItem.outcome?.pleaDescription = hearingOutcomePleaDescription

        // to do - add transformation for location id when that is resolved in an earlier ticket (NN-6007)
      }

      // Transform each protectedCharacteristics
      val protectedCharacteristicsDescriptions = dto.offenceDetails.protectedCharacteristics.mapNotNull { characteristic ->
        CharacteristicTransformer.displayName(characteristic)
      }
      dto.offenceDetails.protectedCharacteristicsDescriptions = protectedCharacteristicsDescriptions

      // Transform each offence code
      val offenceCodeDescriptions = OffenceCodeTransformer.displayName(dto.offenceDetails.offenceCode)
      dto.offenceDetails.offenceCodeDescription = offenceCodeDescriptions

      dto
    }
    return HmppsSubjectAccessRequestContent(content = dtos)
  }
}
