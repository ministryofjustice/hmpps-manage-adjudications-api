package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.IncidentRoleRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.OffenceDetailsRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToPublish
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToUpdate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.NomisAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

@Service
class DraftAdjudicationService(
  val draftAdjudicationRepository: DraftAdjudicationRepository,
  val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  val prisonApiGateway: PrisonApiGateway,
  val offenceCodeLookupService: OffenceCodeLookupService,
  val dateCalculationService: DateCalculationService,
  val authenticationFacade: AuthenticationFacade,
) {

  @Transactional
  fun deleteOrphanedDraftAdjudications() {
    draftAdjudicationRepository.deleteDraftAdjudicationByCreateDateTimeBeforeAndReportNumberIsNotNull(LocalDateTime.now().minusDays(1))
  }

  @Transactional
  fun startNewAdjudication(
    prisonerNumber: String,
    agencyId: String,
    locationId: Long,
    dateTimeOfIncident: LocalDateTime,
    incidentRole: IncidentRoleRequest
  ): DraftAdjudicationDto {
    val draftAdjudication = DraftAdjudication(
      prisonerNumber = prisonerNumber,
      agencyId = agencyId,
      incidentDetails = IncidentDetails(
        locationId = locationId,
        dateTimeOfIncident = dateTimeOfIncident,
        handoverDeadline = dateCalculationService.calculate48WorkingHoursFrom(dateTimeOfIncident)
      ),
      incidentRole = IncidentRole(
        roleCode = incidentRole.roleCode,
        associatedPrisonersNumber = incidentRole.associatedPrisonersNumber,
      ),
      reportNumber = null,
      reportByUserId = null,
    )
    return draftAdjudicationRepository
      .save(draftAdjudication)
      .toDto(offenceCodeLookupService)
  }

  fun getDraftAdjudicationDetails(id: Long): DraftAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    return draftAdjudication.toDto(offenceCodeLookupService)
  }

  @Transactional
  fun setOffenceDetails(id: Long, offenceDetails: List<OffenceDetailsRequestItem>): DraftAdjudicationDto {
    throwIfNoOffenceDetails(offenceDetails)

    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    val newValuesToStore = offenceDetails.map {
      Offence(
        offenceCode = it.offenceCode,
        paragraphCode = offenceCodeLookupService.getParagraphCode(it.offenceCode),
        victimPrisonersNumber = it.victimPrisonersNumber?.ifBlank { null },
        victimStaffUsername = it.victimStaffUsername?.ifBlank { null },
        victimOtherPersonsName = it.victimOtherPersonsName?.ifBlank { null },
      )
    }.toMutableList()
    if (draftAdjudication.offenceDetails != null) {
      draftAdjudication.offenceDetails!!.clear()
      draftAdjudication.offenceDetails!!.addAll(newValuesToStore)
    } else {
      draftAdjudication.offenceDetails = newValuesToStore
    }

    return draftAdjudicationRepository.save(draftAdjudication).toDto(offenceCodeLookupService)
  }

  @Transactional
  fun addIncidentStatement(id: Long, statement: String?, completed: Boolean?): DraftAdjudicationDto {
    throwIfStatementAndCompletedIsNull(statement, completed)

    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    if (draftAdjudication.incidentStatement != null)
      throw IllegalStateException("DraftAdjudication already includes the incident statement")

    draftAdjudication.incidentStatement = IncidentStatement(statement = statement, completed = completed)

    return draftAdjudicationRepository.save(draftAdjudication).toDto(offenceCodeLookupService)
  }

  @Transactional
  fun editIncidentDetails(
    id: Long,
    locationId: Long?,
    dateTimeOfIncident: LocalDateTime?,
    incidentRole: IncidentRoleRequest?,
    removeExistingOffences: Boolean,
  ): DraftAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    if (removeExistingOffences) {
      draftAdjudication.offenceDetails?.let { it.clear() }
    }

    locationId?.let { draftAdjudication.incidentDetails.locationId = it }
    dateTimeOfIncident?.let {
      draftAdjudication.incidentDetails.dateTimeOfIncident = it
      draftAdjudication.incidentDetails.handoverDeadline = dateCalculationService.calculate48WorkingHoursFrom(it)
    }
    incidentRole?.let {
      draftAdjudication.incidentRole.roleCode = it.roleCode
      draftAdjudication.incidentRole.associatedPrisonersNumber = it.associatedPrisonersNumber
    }

    return draftAdjudicationRepository
      .save(draftAdjudication)
      .toDto(offenceCodeLookupService)
  }

  @Transactional
  fun editIncidentStatement(id: Long, statement: String?, completed: Boolean?): DraftAdjudicationDto {
    throwIfStatementAndCompletedIsNull(statement, completed)

    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    if (draftAdjudication.incidentStatement == null)
      throw EntityNotFoundException("DraftAdjudication does not have any incident statement to update")

    statement?.let { draftAdjudication.incidentStatement?.statement = statement }
    completed?.let { draftAdjudication.incidentStatement?.completed = completed }

    return draftAdjudicationRepository.save(draftAdjudication).toDto(offenceCodeLookupService)
  }

  @Transactional
  fun completeDraftAdjudication(id: Long): ReportedAdjudicationDto {
    val draftAdjudication = draftAdjudicationRepository.findById(id).orElseThrow { throwEntityNotFoundException(id) }

    if (draftAdjudication.incidentStatement == null || draftAdjudication.incidentStatement!!.statement == null)
      throw IllegalStateException("Please include an incident statement before completing this draft adjudication")

    if (draftAdjudication.offenceDetails == null || draftAdjudication.offenceDetails!!.isEmpty())
      throw IllegalStateException("Please supply at least one set of offence details")

    val isNew = draftAdjudication.reportNumber == null
    val nomisAdjudication = saveToPrisonApi(draftAdjudication, isNew)
    val generatedReportedAdjudication = saveToReportedAdjudications(draftAdjudication, nomisAdjudication, isNew)

    draftAdjudicationRepository.delete(draftAdjudication)

    return generatedReportedAdjudication
      .toDto(offenceCodeLookupService)
  }

  fun getCurrentUsersInProgressDraftAdjudications(agencyId: String): List<DraftAdjudicationDto> {
    val username = authenticationFacade.currentUsername ?: return emptyList()

    return draftAdjudicationRepository.findDraftAdjudicationByAgencyIdAndCreatedByUserIdAndReportNumberIsNull(agencyId, username)
      .sortedBy { it.incidentDetails.dateTimeOfIncident }
      .map { it.toDto(offenceCodeLookupService) }
  }

  fun lookupRuleDetails(offenceCode: Int): OffenceRuleDetailsDto {
    return OffenceRuleDetailsDto(
      paragraphNumber = offenceCodeLookupService.getParagraphNumber(offenceCode),
      paragraphDescription = offenceCodeLookupService.getParagraphDescription(offenceCode),
    )
  }

  private fun saveToPrisonApi(draftAdjudication: DraftAdjudication, isNew: Boolean): NomisAdjudication {
    if (isNew) {
      return prisonApiGateway.publishAdjudication(
        AdjudicationDetailsToPublish(
          offenderNo = draftAdjudication.prisonerNumber,
          agencyId = draftAdjudication.agencyId,
          incidentTime = draftAdjudication.incidentDetails.dateTimeOfIncident,
          incidentLocationId = draftAdjudication.incidentDetails.locationId,
          statement = draftAdjudication.incidentStatement?.statement!!,
          offenceCodes = getNomisCodes(draftAdjudication.incidentRole, draftAdjudication.offenceDetails),
          connectedOffenderIds = getAssociatedOffenders(draftAdjudication.incidentRole.associatedPrisonersNumber),
          victimOffenderIds = getVictimOffenders(draftAdjudication.offenceDetails),
          victimStaffUsernames = getVictimStaffUsernames(draftAdjudication.offenceDetails),
        )
      )
    } else {
      return prisonApiGateway.updateAdjudication(
        draftAdjudication.reportNumber!!,
        AdjudicationDetailsToUpdate(
          incidentTime = draftAdjudication.incidentDetails.dateTimeOfIncident,
          incidentLocationId = draftAdjudication.incidentDetails.locationId,
          statement = draftAdjudication.incidentStatement?.statement!!,
          offenceCodes = getNomisCodes(draftAdjudication.incidentRole, draftAdjudication.offenceDetails),
          connectedOffenderIds = getAssociatedOffenders(draftAdjudication.incidentRole.associatedPrisonersNumber),
          victimOffenderIds = getVictimOffenders(draftAdjudication.offenceDetails),
          victimStaffUsernames = getVictimStaffUsernames(draftAdjudication.offenceDetails),
        )
      )
    }
  }

  private fun getNomisCodes(roleDetails: IncidentRole, offenceDetails: MutableList<Offence>?): List<String> {
    if (roleDetails.roleCode != null) { // Null means committed on own
      return offenceDetails?.map { offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(it.offenceCode) }
        ?: emptyList()
    }
    return offenceDetails?.flatMap { offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(it.offenceCode) }
      ?: emptyList()
  }

  private fun getAssociatedOffenders(associatedPrisonersNumber: String?): List<String> {
    if (associatedPrisonersNumber == null) {
      return emptyList()
    }
    return listOf(associatedPrisonersNumber)
  }

  private fun getVictimOffenders(offenceDetails: MutableList<Offence>?): List<String> {
    return offenceDetails?.mapNotNull { it.victimPrisonersNumber } ?: emptyList()
  }

  private fun getVictimStaffUsernames(offenceDetails: MutableList<Offence>?): List<String> {
    return offenceDetails?.mapNotNull { it.victimStaffUsername } ?: emptyList()
  }

  private fun saveToReportedAdjudications(
    draftAdjudication: DraftAdjudication,
    nomisAdjudication: NomisAdjudication,
    isNew: Boolean
  ): ReportedAdjudication {
    if (isNew) {
      return reportedAdjudicationRepository.save(
        ReportedAdjudication(
          bookingId = nomisAdjudication.bookingId,
          reportNumber = nomisAdjudication.adjudicationNumber,
          prisonerNumber = draftAdjudication.prisonerNumber,
          agencyId = draftAdjudication.agencyId,
          locationId = draftAdjudication.incidentDetails.locationId,
          dateTimeOfIncident = draftAdjudication.incidentDetails.dateTimeOfIncident,
          handoverDeadline = draftAdjudication.incidentDetails.handoverDeadline,
          incidentRoleCode = draftAdjudication.incidentRole.roleCode,
          incidentRoleAssociatedPrisonersNumber = draftAdjudication.incidentRole.associatedPrisonersNumber,
          offences = toReportedOffence(draftAdjudication.offenceDetails),
          statement = draftAdjudication.incidentStatement!!.statement!!
        )
      )
    }

    val previousReportedAdjudication = reportedAdjudicationRepository.findByReportNumber(nomisAdjudication.adjudicationNumber)
    previousReportedAdjudication?.let {
      it.bookingId = nomisAdjudication.bookingId
      it.reportNumber = nomisAdjudication.adjudicationNumber
      it.prisonerNumber = draftAdjudication.prisonerNumber
      it.agencyId = draftAdjudication.agencyId
      it.locationId = draftAdjudication.incidentDetails.locationId
      it.dateTimeOfIncident = draftAdjudication.incidentDetails.dateTimeOfIncident
      it.handoverDeadline = draftAdjudication.incidentDetails.handoverDeadline
      it.incidentRoleCode = draftAdjudication.incidentRole.roleCode
      it.incidentRoleAssociatedPrisonersNumber = draftAdjudication.incidentRole.associatedPrisonersNumber
      it.offences?.let { offence ->
        offence.clear()
        offence.addAll(toReportedOffence(draftAdjudication.offenceDetails))
      }
      it.statement = draftAdjudication.incidentStatement!!.statement!!
      return reportedAdjudicationRepository.save(it)
    } ?: ReportedAdjudicationService.throwEntityNotFoundException(nomisAdjudication.adjudicationNumber)
  }

  private fun toReportedOffence(draftOffences: MutableList<Offence>?): MutableList<ReportedOffence> {
    return (draftOffences ?: listOf()).map {
      ReportedOffence(
        offenceCode = it.offenceCode,
        paragraphCode = it.paragraphCode,
        victimPrisonersNumber = it.victimPrisonersNumber,
        victimStaffUsername = it.victimStaffUsername,
        victimOtherPersonsName = it.victimOtherPersonsName,
      )
    }.toMutableList()
  }

  private fun throwIfStatementAndCompletedIsNull(statement: String?, completed: Boolean?) {
    if (statement == null && completed == null)
      throw IllegalArgumentException("Please supply either a statement or the completed value")
  }

  private fun throwIfNoOffenceDetails(offenceDetails: List<OffenceDetailsRequestItem>) {
    if (offenceDetails.isEmpty())
      throw IllegalArgumentException("Please supply at least one set of offence details")
  }

  fun throwEntityNotFoundException(id: Long): Nothing =
    throw EntityNotFoundException("DraftAdjudication not found for $id")
}

fun DraftAdjudication.toDto(offenceCodeLookupService: OffenceCodeLookupService): DraftAdjudicationDto = DraftAdjudicationDto(
  id = this.id!!,
  prisonerNumber = this.prisonerNumber,
  incidentStatement = this.incidentStatement?.toDo(),
  incidentDetails = this.incidentDetails.toDto(),
  incidentRole = this.incidentRole.toDto(),
  offenceDetails = this.offenceDetails?.map { it.toDto(offenceCodeLookupService) },
  adjudicationNumber = this.reportNumber,
  startedByUserId = this.reportNumber?.let { this.reportByUserId } ?: this.createdByUserId,
)

fun IncidentDetails.toDto(): IncidentDetailsDto = IncidentDetailsDto(
  locationId = this.locationId,
  dateTimeOfIncident = this.dateTimeOfIncident,
  handoverDeadline = this.handoverDeadline,
)

fun IncidentRole.toDto(): IncidentRoleDto = IncidentRoleDto(
  roleCode = this.roleCode,
  offenceRule = IncidentRoleRuleLookup.getOffenceRuleDetails(this.roleCode),
  associatedPrisonersNumber = this.associatedPrisonersNumber,
)

fun Offence.toDto(offenceCodeLookupService: OffenceCodeLookupService): OffenceDetailsDto = OffenceDetailsDto(
  offenceCode = this.offenceCode,
  offenceRule = OffenceRuleDetailsDto(
    paragraphNumber = offenceCodeLookupService.getParagraphNumber(offenceCode),
    paragraphDescription = offenceCodeLookupService.getParagraphDescription(offenceCode),
  ),
  victimPrisonersNumber = this.victimPrisonersNumber,
  victimStaffUsername = this.victimStaffUsername,
  victimOtherPersonsName = this.victimOtherPersonsName,
)

fun IncidentStatement.toDo(): IncidentStatementDto = IncidentStatementDto(
  statement = this.statement!!,
  completed = this.completed,
)
