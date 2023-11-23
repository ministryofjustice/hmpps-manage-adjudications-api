package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationDetail
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationSearchResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationSummary
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.Award
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.LegacyNomisGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsService.Companion.latestSchedule
import java.time.LocalDate

@Service
class SummaryAdjudicationService(
  private val legacyNomisGateway: LegacyNomisGateway,
  private val featureFlagsConfig: FeatureFlagsConfig,
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {
  fun getAdjudication(prisonerNumber: String, chargeId: Long): AdjudicationDetail =
    legacyNomisGateway.getAdjudicationDetailForPrisoner(prisonerNumber, chargeId)

  fun getAdjudications(
    prisonerNumber: String,
    offenceId: Long?,
    agencyId: String?,
    finding: Finding?,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    pageable: Pageable,
  ): AdjudicationSearchResponse {
    val response = legacyNomisGateway.getAdjudicationsForPrisoner(
      prisonerNumber,
      offenceId,
      agencyId,
      finding,
      fromDate,
      toDate,
      pageable,
    )

    val pageOffset = response.headers.getHeader("Page-Offset")
    val pageSize = response.headers.getHeader("Page-Limit")
    val totalRecords = response.headers.getHeader("Total-Records")
    return response.body?.let {
      AdjudicationSearchResponse(
        results = PageImpl(it.results, PageRequest.of(pageOffset.toInt() / pageSize.toInt(), pageSize.toInt()), totalRecords.toLong()),
        offences = it.offences,
        agencies = it.agencies,
      )
    } ?: AdjudicationSearchResponse(results = Page.empty(), offences = listOf(), agencies = listOf())
  }

  private fun HttpHeaders.getHeader(key: String) = this[key]?.get(0) ?: "0"

  @Transactional(readOnly = true)
  fun getAdjudicationSummary(
    bookingId: Long,
    awardCutoffDate: LocalDate?,
    adjudicationCutoffDate: LocalDate?,
    includeSuspended: Boolean = false,
  ): AdjudicationSummary {
    return if (featureFlagsConfig.nomisSourceOfTruthSummary) {
      legacyNomisGateway.getAdjudicationsForPrisonerForBooking(bookingId, awardCutoffDate, adjudicationCutoffDate)
    } else {
      val cutOff = adjudicationCutoffDate ?: LocalDate.now().minusMonths(3)
      val punishmentCutOff = awardCutoffDate ?: LocalDate.now().minusDays(1)
      val provenByOffenderBookingId = reportedAdjudicationRepository.findByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
        bookingId = bookingId,
        status = ReportedAdjudicationStatus.CHARGE_PROVED,
        cutOff = cutOff.atStartOfDay(),
      )
      return AdjudicationSummary(
        bookingId = bookingId,
        adjudicationCount = provenByOffenderBookingId.size,
        awards =
        provenByOffenderBookingId.map { it.getPunishments() }.flatten().filter {
          it.schedule.filterCutOff(punishmentCutOff) && it.filterSuspended(includeSuspended)
        }.map {
          val latestSchedule = it.schedule.latestSchedule()
          Award(
            bookingId = bookingId,
            status = it.getStatus(latestSchedule),
            statusDescription = it.getStatus(latestSchedule),
            effectiveDate = latestSchedule.startDate ?: latestSchedule.createDateTime?.toLocalDate(),
            days = latestSchedule.days,
            sanctionCode = it.type.name,
            sanctionCodeDescription = it.sanctionCodeDescription(),
            limit = it.amount?.toBigDecimal() ?: it.stoppagePercentage?.toBigDecimal(),
            comment = it.comment(),
          )
        },
      )
    }
  }

  fun getOffenderAdjudicationHearings(
    prisonerNumbers: Set<String>,
    agencyId: String,
    fromDate: LocalDate,
    toDate: LocalDate,
    timeSlot: TimeSlot?,
  ): List<OffenderAdjudicationHearing> {
    return if (featureFlagsConfig.nomisSourceOfTruthHearing) {
      legacyNomisGateway.getOffenderAdjudicationHearings(prisonerNumbers, agencyId, fromDate, toDate, timeSlot)
    } else {
      // TODO: get data from this database!
      listOf()
    }
  }

  companion object {
    fun Punishment.filterSuspended(includeSuspended: Boolean): Boolean = (!includeSuspended && this.suspendedUntil == null || includeSuspended)
    fun List<PunishmentSchedule>.filterCutOff(cutOff: LocalDate): Boolean = (this.latestSchedule().startDate ?: LocalDate.now()).isAfter(cutOff)
    fun Punishment.getStatus(latest: PunishmentSchedule): String =
      if (latest.suspendedUntil != null) "SUSPENDED" else if (this.type == PunishmentType.PROSPECTIVE_DAYS) "PROSPECTIVE" else "IMMEDIATE"

    fun Punishment.sanctionCodeDescription(): String = when (this.otherPrivilege) {
      null -> when (this.privilegeType) {
        null -> this.type.name
        else -> this.privilegeType!!.name
      }
      else -> "Loss of ${this.otherPrivilege!!}"
    }

    fun Punishment.comment(): String? = when (this.type) {
      PunishmentType.DAMAGES_OWED -> String.format("%.2f", this.amount!!)
      PunishmentType.EARNINGS -> "${this.stoppagePercentage}%"
      else -> null
    }
  }
}
