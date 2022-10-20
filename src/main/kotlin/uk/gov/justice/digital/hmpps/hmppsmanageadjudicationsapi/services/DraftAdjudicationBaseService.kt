package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import javax.persistence.EntityNotFoundException

open class DraftAdjudicationBaseService(
  val draftAdjudicationRepository: DraftAdjudicationRepository,
  val offenceCodeLookupService: OffenceCodeLookupService,
) {

  fun find(id: Long): DraftAdjudication =
    draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

  fun findToDto(id: Long): DraftAdjudicationDto =
    draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }
      .toDto()

  fun saveToDto(draftAdjudication: DraftAdjudication): DraftAdjudicationDto =
    draftAdjudicationRepository.save(draftAdjudication).toDto()

  fun delete(draftAdjudication: DraftAdjudication) = draftAdjudicationRepository.delete(draftAdjudication)

  fun DraftAdjudication.toDto(): DraftAdjudicationDto =
    DraftAdjudicationDto(
      id = this.id!!,
      prisonerNumber = this.prisonerNumber,
      incidentStatement = this.incidentStatement?.toDto(),
      incidentDetails = this.incidentDetails.toDto(),
      incidentRole = this.incidentRole?.toDto(this.isYouthOffender!!),
      offenceDetails = this.offenceDetails?.map { it.toDto(offenceCodeLookupService, this.isYouthOffender!!) },
      adjudicationNumber = this.reportNumber,
      startedByUserId = this.reportNumber?.let { this.reportByUserId } ?: this.createdByUserId,
      isYouthOffender = this.isYouthOffender,
      damages = this.damages.map { it.toDto() },
      evidence = this.evidence.map { it.toDto() },
      witnesses = this.witnesses.map { it.toDto() },
      damagesSaved = this.damagesSaved,
      evidenceSaved = this.evidenceSaved,
      witnessesSaved = this.witnessesSaved
    )

  companion object {
    fun throwEntityNotFoundException(id: Long): Nothing =
      throw EntityNotFoundException("DraftAdjudication not found for $id")
  }
}
