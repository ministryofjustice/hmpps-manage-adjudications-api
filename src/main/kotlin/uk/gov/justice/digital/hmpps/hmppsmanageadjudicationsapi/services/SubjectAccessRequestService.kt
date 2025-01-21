package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.TransferService.Companion.log
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.DamageCodeTransformer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.EvidenceCodeTransformer
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
        val hearingDescription = OicHearingTypeTransformer.displayName(hearingItem.oicHearingType)
        hearingItem.oicHearingTypeDescription = hearingDescription

        //to do - add transformation for location id when that is resolved in an earlier ticket (NN-6007)
      }

      dto
    }
    return HmppsSubjectAccessRequestContent(content = dtos)
  }
}
