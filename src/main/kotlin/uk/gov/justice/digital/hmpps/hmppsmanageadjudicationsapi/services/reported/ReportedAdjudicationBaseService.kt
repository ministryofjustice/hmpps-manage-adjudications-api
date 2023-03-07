package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.CombinedOutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DisIssueHistoryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingOutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OutcomeHistoryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedDamageDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedEvidenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedWitnessDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DisIssueHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
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
  protected fun ReportedAdjudication.toDto(): ReportedAdjudicationDto {
    val hearings = this.hearings.toHearings()
    val outcomes = this.outcomes.createCombinedOutcomes()
    return ReportedAdjudicationDto(
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
      damages = this.damages.toReportedDamages(),
      evidence = this.evidence.toReportedEvidence(),
      witnesses = this.witnesses.toReportedWitnesses(),
      status = status,
      statusReason = statusReason,
      statusDetails = statusDetails,
      hearings = hearings,
      issuingOfficer = issuingOfficer,
      dateTimeOfIssue = dateTimeOfIssue,
      disIssueHistory = this.disIssueHistory.toDisIssueHistory(),
      gender = gender,
      dateTimeOfFirstHearing = dateTimeOfFirstHearing,
      outcomes = createHistory(hearings.toMutableList(), outcomes.toMutableList())
    )
  }

  private fun createHistory(hearings: MutableList<HearingDto>, outcomes: MutableList<CombinedOutcomeDto>): List<OutcomeHistoryDto> {
    if (hearings.isEmpty() && outcomes.isEmpty()) return listOf()
    if (outcomes.isEmpty()) return hearings.map { OutcomeHistoryDto(hearing = it) }
    if (hearings.isEmpty()) return outcomes.map { OutcomeHistoryDto(outcome = it) }

    val history = mutableListOf<OutcomeHistoryDto>()
    val referPoliceOutcomeCount = outcomes.count { it.outcome.code == OutcomeCode.REFER_POLICE }
    val referPoliceHearingOutcomeCount = hearings.count { it.outcome?.code == HearingOutcomeCode.REFER_POLICE }

    // special case.  if we have more refer police outcomes than hearing outcomes, it means the first action was to refer to police
    if (referPoliceOutcomeCount > referPoliceHearingOutcomeCount)
      history.add(OutcomeHistoryDto(outcome = outcomes.removeFirst()))

    do {
      val hearing = hearings.removeFirst()
      val outcome = if (hearing.hearingHasNoAssociatedOutcome()) null else outcomes.removeFirstOrNull()

      history.add(
        OutcomeHistoryDto(hearing = hearing, outcome = outcome)
      )
    } while (hearings.isNotEmpty())

    // quashed will be left if it is present
    outcomes.removeFirstOrNull()?.let {
      history.add(
        OutcomeHistoryDto(outcome = it)
      )
    }

    return history.toList()
  }

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

  private fun List<ReportedDamage>.toReportedDamages(): List<ReportedDamageDto> =
    this.map {
      ReportedDamageDto(
        code = it.code,
        details = it.details,
        reporter = it.reporter
      )
    }.toList()

  private fun List<ReportedEvidence>.toReportedEvidence(): List<ReportedEvidenceDto> =
    this.map {
      ReportedEvidenceDto(
        code = it.code,
        identifier = it.identifier,
        details = it.details,
        reporter = it.reporter
      )
    }.toList()

  private fun List<ReportedWitness>.toReportedWitnesses(): List<ReportedWitnessDto> =
    this.map {
      ReportedWitnessDto(
        code = it.code,
        firstName = it.firstName,
        lastName = it.lastName,
        reporter = it.reporter
      )
    }.toList()

  private fun List<Hearing>.toHearings(): List<HearingDto> =
    this.map {
      HearingDto(
        id = it.id,
        locationId = it.locationId,
        dateTimeOfHearing = it.dateTimeOfHearing,
        oicHearingType = it.oicHearingType,
        outcome = it.hearingOutcome?.toHearingOutcomeDto()
      )
    }.sortedBy { it.dateTimeOfHearing }.toList()

  private fun HearingOutcome.toHearingOutcomeDto(): HearingOutcomeDto =
    HearingOutcomeDto(
      id = this.id,
      code = this.code,
      reason = this.reason,
      details = this.details,
      adjudicator = this.adjudicator,
      plea = this.plea,
    )

  private fun Outcome.toOutcomeDto(): OutcomeDto =
    OutcomeDto(
      id = this.id,
      code = this.code,
      details = this.details,
      reason = this.reason,
      amount = this.amount,
      caution = this.caution,
      quashedReason = this.quashedReason,
    )

  private fun List<DisIssueHistory>.toDisIssueHistory(): List<DisIssueHistoryDto> =
    this.map {
      DisIssueHistoryDto(
        issuingOfficer = it.issuingOfficer,
        dateTimeOfIssue = it.dateTimeOfIssue,
      )
    }.sortedBy { it.dateTimeOfIssue }.toList()

  companion object {
    fun HearingDto.hearingHasNoAssociatedOutcome() =
      this.outcome == null || this.outcome.code == HearingOutcomeCode.ADJOURN
  }
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
