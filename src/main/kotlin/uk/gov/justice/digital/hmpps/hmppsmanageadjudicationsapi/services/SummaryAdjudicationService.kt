package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.Adjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationDetail
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationSearchResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationSummary
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.Award
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.LegacyNomisGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsService.Companion.latestSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportsService.Companion.transferIgnoreStatuses
import java.time.LocalDate

@Service
class SummaryAdjudicationService(
  private val legacyNomisGateway: LegacyNomisGateway,
  private val featureFlagsConfig: FeatureFlagsConfig,
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {
  fun getAdjudication(prisonerNumber: String, chargeId: Long): AdjudicationDetail {
    return if (featureFlagsConfig.nomisSourceOfTruthAdjudication) {
      legacyNomisGateway.getAdjudicationDetailForPrisoner(prisonerNumber, chargeId)
    } else {
      // TODO: get data from this database!
      AdjudicationDetail(adjudicationNumber = chargeId)
    }
  }

  fun getAdjudications(
    prisonerNumber: String,
    offenceId: Long?,
    agencyId: String?,
    finding: Finding?,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    pageable: Pageable,
  ): AdjudicationSearchResponse {
    return if (featureFlagsConfig.nomisSourceOfTruthAdjudications) {
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
      response.body?.let {
        AdjudicationSearchResponse(
          results = PageImpl(it.results, PageRequest.of(pageOffset.toInt() / pageSize.toInt(), pageSize.toInt()), totalRecords.toLong()),
          offences = it.offences,
          agencies = it.agencies,
        )
      } ?: AdjudicationSearchResponse(results = Page.empty(), offences = listOf(), agencies = listOf())
    } else {
      val adjudications = when(agencyId) {
        null -> reportedAdjudicationRepository.findByPrisonerNumberAndDateTimeOfDiscoveryBetweenAndStatusIn(
          prisonerNumber = prisonerNumber,
          fromDate = (fromDate ?: minimumDate).atStartOfDay(),
          toDate = (toDate ?: maximumDate).atStartOfDay(),
          statuses = finding?.mapFindingToStatus() ?: allStatuses,
          pageable = pageable
        )
        else -> reportedAdjudicationRepository.findByPrisonerNumberAndAgencyAndDate(
          prisonerNumber = prisonerNumber,
          startDate = (fromDate ?: minimumDate).atStartOfDay(),
          endDate = (toDate ?: maximumDate).atStartOfDay(),
          statuses = finding?.mapFindingToStatus()?.map { it.name } ?: allStatuses.map { it.name },
          transferIgnoreStatuses = transferIgnoreStatuses.map { it.name },
          agencyId = agencyId,
          pageable = pageable
        )
      }

      return   AdjudicationSearchResponse(
        PageImpl(
        adjudications.content.map {
          it.mapToAdjudication()
        },
        PageRequest.of(pageable.offset.toInt() / pageable.pageSize, pageable.pageSize), adjudications.totalPages.toLong()
      ), offences =  listOf(), agencies = listOf() )


    }
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

    val minimumDate: LocalDate = LocalDate.of(2001, 1, 1)
    val maximumDate: LocalDate = LocalDate.of(2999, 1, 1)

    val allStatuses = ReportedAdjudicationStatus.values().filter { it != ReportedAdjudicationStatus.ACCEPTED }

    fun Finding.mapFindingToStatus(): List<ReportedAdjudicationStatus> = when(this) {
      Finding.PROVED, Finding.GUILTY -> listOf(ReportedAdjudicationStatus.CHARGE_PROVED)
      Finding.ADJOURNED -> listOf(ReportedAdjudicationStatus.ADJOURNED)
      Finding.D, Finding.NOT_GUILTY, Finding.NOT_PROVEN -> listOf(ReportedAdjudicationStatus.DISMISSED)
      Finding.NOT_PROCEED, Finding.DISMISSED -> listOf(ReportedAdjudicationStatus.NOT_PROCEED)
      Finding.REF_POLICE -> listOf(ReportedAdjudicationStatus.REFER_POLICE)
      Finding.QUASHED, Finding.APPEAL -> listOf(ReportedAdjudicationStatus.QUASHED)
      Finding.PROSECUTED -> listOf(ReportedAdjudicationStatus.PROSECUTION)
      else -> listOf(ReportedAdjudicationStatus.SCHEDULED, ReportedAdjudicationStatus.UNSCHEDULED, ReportedAdjudicationStatus.REFER_INAD, ReportedAdjudicationStatus.REFER_GOV)
    }

    fun ReportedAdjudication.mapToAdjudication(): Adjudication =
      Adjudication(
        adjudicationNumber = this.chargeNumber,
        reportTime = this.dateTimeOfDiscovery,
        agencyId = this.originatingAgencyId,
      )
  }
}
