package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Adjudication Summary for a prisoner")
data class AdjudicationSummary(
  @Schema(required = true, description = "Prisoner Booking Id")
  val bookingId: Long,

  @Schema(required = true, description = "Number of proven adjudications")
  val adjudicationCount: Int,

  @Schema(required = true, description = "List of awards / sanctions")
  val awards: List<Award>,
)

@Schema(description = "Adjudication award / sanction")
data class Award(
  @Schema(required = true, description = "Id of booking")
  val bookingId: Long,

  @Schema(required = true, description = "Type of award")
  val sanctionCode: String? = null,

  @Schema(description = "Award type description")
  val sanctionCodeDescription: String? = null,

  @Schema(description = "Number of months duration")
  val months: Int? = null,

  @Schema(description = "Number of days duration")
  val days: Int? = null,

  @Schema(description = "Compensation amount")
  val limit: BigDecimal? = null,

  @Schema(description = "Optional details")
  val comment: String? = null,

  @Schema(required = true, description = "Start of sanction")
  val effectiveDate: LocalDate? = null,

  @Schema(description = "Award status (ref domain OIC_SANCT_ST)")
  val status: String? = null,

  @Schema(description = "Award status description")
  val statusDescription: String? = null,

  @Schema(required = true, description = "Id of hearing")
  val hearingId: Long? = null,

  @Schema(required = true, description = "hearing record sequence number")
  val hearingSequence: Int? = null,
)

@Schema(description = "An overview of an adjudication")
data class Adjudication(
  @Schema(description = "Adjudication Number", example = "1234567")
  val adjudicationNumber: Long,

  @Schema(description = "Report Time", example = "2017-03-17T08:02:00")
  val reportTime: LocalDateTime,

  @Schema(description = "Agency Incident Id", example = "1484302")
  val agencyIncidentId: Long,

  @Schema(description = "Agency Id", example = "MDI")
  val agencyId: String?,

  @Schema(description = "Party Sequence", example = "1")
  val partySeq: Long,

  @Schema(description = "Charges made as part of the adjudication")
  val adjudicationCharges: List<AdjudicationCharge>? = null,
)

@Schema(description = "A charge which was made as part of an adjudication")
data class AdjudicationCharge(
  @Schema(description = "Charge Id", example = "1506763/1")
  val oicChargeId: String?,

  @Schema(description = "Offence Code", example = "51:22")
  val offenceCode: String? = null,

  @Schema(description = "Offence Description", example = "Disobeys any lawful order")
  val offenceDescription: String? = null,

  @Schema(description = "Offence Finding Code", example = "PROVED")
  val findingCode: String? = null,
)

data class AdjudicationResponse(
  val results: List<Adjudication>,
  val offences: List<AdjudicationOffence>,
  val agencies: List<Prison>,
)

data class AdjudicationSearchResponse(
  @Schema(description = "Search results")
  val results: Page<Adjudication>,

  @Schema(description = "A complete list of the type of offences that this offender has had adjudications for")
  val offences: List<AdjudicationOffence>,

  @Schema(description = "Complete list of agencies where this offender has had adjudications")
  val agencies: List<Prison>,
)

data class AdjudicationOffence(
  @Schema(description = "Offence Id", example = "8")
  val id: String? = null,
  @Schema(description = "Offence Code", example = "51:7")
  val code: String? = null,
  @Schema(description = "Offence Description", example = "Escapes or absconds from prison or from legal custody")
  val description: String? = null,
)

data class Prison(
  @Schema(required = true, description = "Agency identifier.", example = "MDI")
  val agencyId: String? = null,

  @Schema(required = true, description = "Agency description.", example = "Moorland (HMP & YOI)")
  val description: String? = null,

  @Schema(required = true, description = "Indicates the Agency is active", example = "true")
  val active: Boolean = true,
)

@Schema(name = "IndividualAdjudication", description = "Detail about an individual Adjudication")
data class AdjudicationDetail(
  @Schema(description = "Adjudication Number", example = "1234567")
  val adjudicationNumber: Long,

  @Schema(description = "Incident Time", example = "2017-03-17T08:02:00")
  val incidentTime: LocalDateTime? = null,

  @Schema(description = "Establishment", example = "Moorland (HMP & YOI)")
  val establishment: String? = null,

  @Schema(description = "Interior Location", example = "Wing A")
  val interiorLocation: String? = null,

  @Schema(description = "Incident Details", example = "Whilst conducting an intelligence cell search...")
  val incidentDetails: String? = null,

  @Schema(description = "Report Number", example = "1234567")
  val reportNumber: Long? = null,

  @Schema(description = "Report Type", example = "Governor's Report")
  val reportType: String? = null,

  @Schema(description = "Reporter First Name", example = "John")
  val reporterFirstName: String? = null,

  @Schema(description = "Reporter Last Name", example = "Smith")
  val reporterLastName: String? = null,

  @Schema(description = "Report Time", example = "2017-03-17T08:02:00")
  val reportTime: LocalDateTime? = null,

  @Schema(description = "Hearings")
  val hearings: List<Hearing>? = null,

)

@Schema(description = "An Adjudication Hearing")
data class Hearing(
  @Schema(description = "OIC Hearing ID", example = "1985937")
  val oicHearingId: Long? = null,

  @Schema(description = "Hearing Type", example = "Governor's Hearing Adult")
  val hearingType: String? = null,

  @Schema(description = "Hearing Time", example = "2017-03-17T08:30:00")
  val hearingTime: LocalDateTime? = null,

  @Schema(description = "Establishment", example = "Moorland (HMP & YOI)")
  val establishment: String? = null,

  @Schema(description = "Hearing Location", example = "Adjudication Room")
  val location: String? = null,

  @Schema(description = "Adjudicator First name", example = "Bob")
  val heardByFirstName: String? = null,

  @Schema(description = "Adjudicator Last name", example = "Smith")
  val heardByLastName: String? = null,

  @Schema(description = "Other Representatives", example = "Councillor Adams")
  val otherRepresentatives: String? = null,

  @Schema(description = "Comment", example = "The defendant conducted themselves in a manner...")
  val comment: String? = null,

  @Schema(description = "Hearing Results")
  val results: List<HearingResult>? = null,
)

@Schema(description = "A result from a hearing")
data class HearingResult(
  @Schema(description = "OIC Offence Code", example = "51:22")
  val oicOffenceCode: String? = null,

  @Schema(description = "Offence Type", example = "Prison Rule 51")
  val offenceType: String? = null,

  @Schema(description = "Offence Description", example = "Disobeys any lawful order")
  val offenceDescription: String? = null,

  @Schema(description = "Plea", example = "Guilty")
  val plea: String? = null,

  @Schema(description = "Finding", example = "Charge Proved")
  val finding: String? = null,

  val sanctions: List<Sanction>? = null,
)

@Schema(description = "An Adjudication Sanction")
data class Sanction(
  @Schema(description = "Sanction Type", example = "Stoppage of Earnings (amount)")
  val sanctionType: String? = null,

  @Schema(description = "Sanction Days", example = "14")
  val sanctionDays: Long? = null,

  @Schema(description = "Sanction Months", example = "1")
  val sanctionMonths: Long? = null,

  @Schema(description = "Compensation Amount", example = "50")
  val compensationAmount: Long? = null,

  @Schema(description = "Effective", example = "2017-03-22T00:00:00")
  val effectiveDate: LocalDateTime? = null,

  @Schema(description = "Sanction status", example = "Immediate")
  val status: String? = null,

  @Schema(description = "Status Date", example = "2017-03-22T00:00:00")
  val statusDate: LocalDateTime? = null,

  @Schema(description = "Comment", example = "14x LOTV, 14x LOGYM, 14x LOC, 14x LOA, 14x LOE 50%, 14x CC")
  val comment: String? = null,

  @Schema(description = "Sanction Seq", example = "1")
  val sanctionSeq: Long? = null,

  @Schema(description = "Consecutive Sanction Seq", example = "1")
  val consecutiveSanctionSeq: Long? = null,
)

@Schema(description = "Represents an adjudication hearing at the offender level.")
data class OffenderAdjudicationHearing(
  val agencyId: String,

  @Schema(description = "Display Prisoner Number (UK is NOMS ID)")
  val offenderNo: String,

  @Schema(description = "OIC Hearing ID", example = "1985937")
  val hearingId: Long,

  @Schema(description = "Hearing Type", example = "Governor's Hearing Adult")
  val hearingType: String?,

  @Schema(description = "Hearing Time", example = "2017-03-17T08:30:00")
  val startTime: LocalDateTime?,

  @Schema(description = "The internal location id of the hearing", example = "789448")
  val internalLocationId: Long,

  @Schema(description = "The internal location description of the hearing", example = "PVI-RES-MCASU-ADJUD")
  val internalLocationDescription: String?,

  @Schema(description = "The status of the hearing, SCH, COMP or EXP", example = "COMP")
  val eventStatus: String?,
)
