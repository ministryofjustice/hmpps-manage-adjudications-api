package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

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
