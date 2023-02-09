package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.CombinedOutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingOutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedDamageDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedEvidenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedWitnessDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.IncidentRoleRuleLookup
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import javax.persistence.EntityNotFoundException

open class ReportedDtoService(
  protected val offenceCodeLookupService: OffenceCodeLookupService,
) {
  protected fun ReportedAdjudication.toDto(): ReportedAdjudicationDto = ReportedAdjudicationDto(
    adjudicationNumber = reportNumber,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    incidentDetails = IncidentDetailsDto(
      locationId = locationId,
      dateTimeOfIncident = dateTimeOfIncident,
      dateTimeOfDiscovery = dateTimeOfDiscovery,
      handoverDeadline = handoverDeadline
    ),
    isYouthOffender = isYouthOffender,
    incidentRole = IncidentRoleDto(
      roleCode = incidentRoleCode,
      offenceRule = IncidentRoleRuleLookup.getOffenceRuleDetails(incidentRoleCode, isYouthOffender),
      associatedPrisonersNumber = incidentRoleAssociatedPrisonersNumber,
      associatedPrisonersName = incidentRoleAssociatedPrisonersName,
    ),
    offenceDetails = toReportedOffence(offenceDetails.first(), isYouthOffender, gender, offenceCodeLookupService),
    incidentStatement = IncidentStatementDto(
      statement = statement,
      completed = true,
    ),
    createdByUserId = createdByUserId!!,
    createdDateTime = createDateTime!!,
    reviewedByUserId = reviewUserId,
    damages = toReportedDamages(damages),
    evidence = toReportedEvidence(evidence),
    witnesses = toReportedWitnesses(witnesses),
    status = status,
    statusReason = statusReason,
    statusDetails = statusDetails,
    hearings = toHearings(hearings, outcomes.firstOrNull()), // TODO this needs to be fixed > must use correct index
    issuingOfficer = issuingOfficer,
    dateTimeOfIssue = dateTimeOfIssue,
    gender = gender,
    dateTimeOfFirstHearing = dateTimeOfFirstHearing,
    outcomes = outcomes.createCombinedOutcomes(),
  )

  protected fun List<Outcome>.createCombinedOutcomes(): List<CombinedOutcomeDto> {
    if (this.isEmpty()) return emptyList()

    val combinedOutcomes = mutableListOf<CombinedOutcomeDto>()
    val orderedOutcomes = this.sortedBy { it.createDateTime }.toMutableList()

    do {
      val outcome = orderedOutcomes.removeFirst()
      when (outcome.code) {
        OutcomeCode.REFER_POLICE, OutcomeCode.REFER_INAD -> {
          // a referral can only ever be followed by a referral outcome, or nothing (ie referral is current final state)
          val referralOutcome = orderedOutcomes.removeFirstOrNull()

          combinedOutcomes.add(
            CombinedOutcomeDto(
              outcome = outcome.toOutcomeDto(),
              referralOutcome = referralOutcome?.toOutcomeDto(),
            )
          )
        }
        else -> combinedOutcomes.add(
          CombinedOutcomeDto(
            outcome = outcome.toOutcomeDto()
          )
        )
      }
    } while (orderedOutcomes.isNotEmpty())

    return combinedOutcomes
  }

  private fun toReportedOffence(
    offence: ReportedOffence,
    isYouthOffender: Boolean,
    gender: Gender,
    offenceCodeLookupService: OffenceCodeLookupService
  ): OffenceDto =
    OffenceDto(
      offenceCode = offence.offenceCode,
      offenceRule = OffenceRuleDto(
        paragraphNumber = offenceCodeLookupService.getParagraphNumber(offence.offenceCode, isYouthOffender),
        paragraphDescription = offenceCodeLookupService.getParagraphDescription(offence.offenceCode, isYouthOffender, gender),
      ),
      victimPrisonersNumber = offence.victimPrisonersNumber,
      victimStaffUsername = offence.victimStaffUsername,
      victimOtherPersonsName = offence.victimOtherPersonsName,
    )

  private fun toReportedDamages(damages: MutableList<ReportedDamage>): List<ReportedDamageDto> =
    damages.map {
      ReportedDamageDto(
        code = it.code,
        details = it.details,
        reporter = it.reporter
      )
    }.toList()

  private fun toReportedEvidence(evidence: MutableList<ReportedEvidence>): List<ReportedEvidenceDto> =
    evidence.map {
      ReportedEvidenceDto(
        code = it.code,
        identifier = it.identifier,
        details = it.details,
        reporter = it.reporter
      )
    }.toList()

  private fun toReportedWitnesses(witnesses: MutableList<ReportedWitness>): List<ReportedWitnessDto> =
    witnesses.map {
      ReportedWitnessDto(
        code = it.code,
        firstName = it.firstName,
        lastName = it.lastName,
        reporter = it.reporter
      )
    }.toList()

  private fun toHearings(hearings: MutableList<Hearing>, outcome: Outcome?): List<HearingDto> =
    hearings.map {
      HearingDto(
        id = it.id,
        locationId = it.locationId,
        dateTimeOfHearing = it.dateTimeOfHearing,
        oicHearingType = it.oicHearingType,
        outcome = it.hearingOutcome?.toHearingOutcomeDto(outcome)
      )
    }.sortedBy { it.dateTimeOfHearing }.toList()

  private fun HearingOutcome.toHearingOutcomeDto(outcome: Outcome?): HearingOutcomeDto =
    HearingOutcomeDto(
      id = this.id,
      code = this.code,
      reason = this.reason,
      details = if (this.code.outcomeCode != null) outcome?.details else this.details,
      adjudicator = this.adjudicator,
      finding = this.finding,
      plea = this.plea,
    )

  private fun Outcome.toOutcomeDto(): OutcomeDto =
    OutcomeDto(
      id = this.id,
      code = this.code,
      details = this.details,
      reason = this.reason,
    )
}

open class ReportedAdjudicationBaseService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  protected val authenticationFacade: AuthenticationFacade,
) : ReportedDtoService(offenceCodeLookupService) {

  protected fun findByAdjudicationNumber(adjudicationNumber: Long) =
    reportedAdjudicationRepository.findByReportNumber(adjudicationNumber) ?: throwEntityNotFoundException(
      adjudicationNumber
    )
  protected fun saveToDto(reportedAdjudication: ReportedAdjudication): ReportedAdjudicationDto =
    reportedAdjudicationRepository.save(reportedAdjudication).toDto()

  protected fun findByReportNumberIn(adjudicationNumbers: List<Long>) = reportedAdjudicationRepository.findByReportNumberIn(adjudicationNumbers)

  companion object {
    fun throwEntityNotFoundException(id: Long): Nothing =
      throw EntityNotFoundException("ReportedAdjudication not found for $id")
  }
}
