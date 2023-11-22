package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.util.CollectionUtils
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationDetail
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationSummary
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.Award
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.LegacyNomisGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

class SummaryAdjudicationServiceTest : ReportedAdjudicationTestBase() {

  private val legacyNomisGateway: LegacyNomisGateway = mock()
  private val featureFlagsConfig: FeatureFlagsConfig = mock()

  private val prisonerNumber = "A1234AB"

  private val summaryAdjudicationService = SummaryAdjudicationService(
    legacyNomisGateway,
    featureFlagsConfig,
    reportedAdjudicationRepository,
  )
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }

  @Nested
  inner class Legacy {

    @BeforeEach
    fun beforeEach() {
      whenever(featureFlagsConfig.nomisSourceOfTruthSummary).thenReturn(true)
      whenever(featureFlagsConfig.nomisSourceOfTruthAdjudication).thenReturn(true)
      whenever(featureFlagsConfig.nomisSourceOfTruthAdjudications).thenReturn(true)
      whenever(featureFlagsConfig.nomisSourceOfTruthHearing).thenReturn(true)
    }

    @Test
    fun `can paginate results from NOMIS`() {
      val today = LocalDate.now()

      val offenceId = 1L
      val agencyId = "MDI"
      val finding = Finding.PROVED
      val fromDate = today.minusDays(30)
      val toDate = today.minusDays(1)
      val pageable = PageRequest.of(1, 10)

      val headers = CollectionUtils.unmodifiableMultiValueMap(
        CollectionUtils.toMultiValueMap(
          mapOf(
            "Page-Offset" to listOf("10"),
            "Page-Limit" to listOf("10"),
            "Total-Records" to listOf("30"),
          ),
        ),
      )

      whenever(
        legacyNomisGateway.getAdjudicationsForPrisoner(
          prisonerNumber,
          offenceId,
          agencyId,
          finding,
          fromDate,
          toDate,
          pageable,
        ),
      ).thenReturn(
        ResponseEntity(
          AdjudicationResponse(
            results = listOf(),
            offences = listOf(),
            agencies = listOf(),
          ),
          headers,
          HttpStatusCode.valueOf(200),
        ),
      )

      val adjudications = summaryAdjudicationService.getAdjudications(
        prisonerNumber = prisonerNumber,
        offenceId = offenceId,
        agencyId = agencyId,
        finding = finding,
        fromDate = fromDate,
        toDate = toDate,
        pageable = pageable,
      )

      assertThat(adjudications.results.totalElements).isEqualTo(30)
      assertThat(adjudications.results.totalPages).isEqualTo(3)
      assertThat(adjudications.results.pageable.pageNumber).isEqualTo(1)
      assertThat(adjudications.results.numberOfElements).isEqualTo(0)
    }

    @Test
    fun `can get results for a booking`() {
      whenever(legacyNomisGateway.getAdjudicationsForPrisonerForBooking(1L, null, null)).thenReturn(
        AdjudicationSummary(
          bookingId = 1L,
          adjudicationCount = 2,
          awards = listOf(Award(bookingId = 1L, sanctionCode = "T3"), Award(bookingId = 1L, sanctionCode = "T1")),
        ),
      )
      val adjudicationSummary = summaryAdjudicationService.getAdjudicationSummary(1L, null, null)

      assertThat(adjudicationSummary.adjudicationCount).isEqualTo(2)
    }

    @Test
    fun `can get results for a prisoner number`() {
      val adjudicationNumber = 10000L
      whenever(legacyNomisGateway.getAdjudicationDetailForPrisoner(prisonerNumber = prisonerNumber, chargeId = 1L)).thenReturn(
        AdjudicationDetail(
          adjudicationNumber = adjudicationNumber,
        ),
      )
      val adjudicationDetail = summaryAdjudicationService.getAdjudication(prisonerNumber, 1L)

      Assertions.assertThat(adjudicationDetail.adjudicationNumber).isEqualTo(adjudicationNumber)
    }

    @Test
    fun `can get hearing results for prisoner numbers`() {
      val anotherPrisonerNumber = "A12345"
      val agencyId = "MDI"
      val today = LocalDate.now()
      val fromDate = today.minusDays(30)
      val toDate = today.minusDays(1)

      whenever(
        legacyNomisGateway.getOffenderAdjudicationHearings(
          prisonerNumbers = setOf(prisonerNumber, anotherPrisonerNumber),
          agencyId = agencyId,
          fromDate = fromDate,
          toDate = toDate,
          timeSlot = null,
        ),
      ).thenReturn(
        listOf(
          OffenderAdjudicationHearing(
            agencyId = agencyId,
            offenderNo = prisonerNumber,
            hearingId = 1,
            hearingType = null,
            startTime = null,
            internalLocationId = 123L,
            internalLocationDescription = null,
            eventStatus = null,
          ),
          OffenderAdjudicationHearing(
            agencyId = agencyId,
            offenderNo = anotherPrisonerNumber,
            hearingId = 1,
            hearingType = null,
            startTime = null,
            internalLocationId = 123L,
            internalLocationDescription = null,
            eventStatus = null,
          ),
        ),
      )
      val adjudicationHearings = summaryAdjudicationService.getOffenderAdjudicationHearings(
        prisonerNumbers = setOf(prisonerNumber, anotherPrisonerNumber),
        agencyId = agencyId,
        fromDate = fromDate,
        toDate = toDate,
        timeSlot = null,
      )

      assertThat(adjudicationHearings[0].offenderNo).isEqualTo(prisonerNumber)
      assertThat(adjudicationHearings[1].offenderNo).isEqualTo(anotherPrisonerNumber)
    }
  }

  @Nested
  inner class AdjudicationsSummary {

    private val basicData = entityBuilder.reportedAdjudication().also {
      it.offenderBookingId = 1L
      it.status = ReportedAdjudicationStatus.CHARGE_PROVED
      it.clearPunishments()
      it.addPunishment(
        Punishment(
          type = PunishmentType.CAUTION,
          schedule =
          mutableListOf(
            PunishmentSchedule(days = 0, startDate = LocalDate.now()).also {
              it.createDateTime = LocalDateTime.now()
            },
          ),
        ),
      )
      it.addPunishment(
        Punishment(
          type = PunishmentType.CAUTION,
          schedule =
          mutableListOf(
            PunishmentSchedule(
              days = 0,
              startDate = LocalDate.now().minusDays(1),
            ).also {
              it.createDateTime = LocalDateTime.now()
            },
          ),
        ),
      )
    }

    @BeforeEach
    fun init() {
      whenever(featureFlagsConfig.nomisSourceOfTruthSummary).thenReturn(false)
    }

    @Test
    fun `returns adjudication summary for prisoner - basic`() {
      whenever(
        reportedAdjudicationRepository.findByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
          1L,
          ReportedAdjudicationStatus.CHARGE_PROVED,
          LocalDate.now().minusMonths(3).atStartOfDay(),
        ),
      ).thenReturn(
        listOf(
          basicData,
          basicData,
        ),
      )

      val response = summaryAdjudicationService.getAdjudicationSummary(bookingId = 1L, awardCutoffDate = null, adjudicationCutoffDate = null)
      assertThat(response.adjudicationCount).isEqualTo(2)
      assertThat(response.awards.size).isEqualTo(2)
      assertThat(response.bookingId).isEqualTo(1L)

      response.awards.forEach {
        assertThat(it.months).isNull()
        assertThat(it.hearingSequence).isNull()
        assertThat(it.bookingId).isEqualTo(1)
        assertThat(it.effectiveDate).isEqualTo(LocalDate.now())
      }
    }

    @CsvSource("true", "false")
    @ParameterizedTest
    fun `suspended award should not return - original was filter in DPS, connect team not maintained it, but its better to be placed in api`(includeSuspended: Boolean) {
      basicData.also {
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = PunishmentType.PRIVILEGE,
            privilegeType = PrivilegeType.TV,
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      }

      whenever(
        reportedAdjudicationRepository.findByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
          1L,
          ReportedAdjudicationStatus.CHARGE_PROVED,
          LocalDate.now().minusMonths(3).atStartOfDay(),
        ),
      ).thenReturn(listOf(basicData))
      val response = summaryAdjudicationService.getAdjudicationSummary(bookingId = 1L, awardCutoffDate = null, adjudicationCutoffDate = null, includeSuspended = includeSuspended)

      if (includeSuspended) assertThat(response.awards).isNotEmpty else assertThat(response.awards.isEmpty())
    }

    @Test
    fun `with effective date`() {
      basicData.also {
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = PunishmentType.ADDITIONAL_DAYS,
            privilegeType = PrivilegeType.TV,
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, startDate = LocalDate.now().plusDays(1)),
            ),
          ),
        )
      }

      whenever(
        reportedAdjudicationRepository.findByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
          1L,
          ReportedAdjudicationStatus.CHARGE_PROVED,
          LocalDate.now().minusMonths(3).atStartOfDay(),
        ),
      ).thenReturn(listOf(basicData))
      val response = summaryAdjudicationService.getAdjudicationSummary(bookingId = 1L, awardCutoffDate = null, adjudicationCutoffDate = null)

      assertThat(response.awards.first().effectiveDate).isEqualTo(LocalDate.now().plusDays(1))
    }

    @CsvSource("DAMAGES_OWED", "EARNINGS")
    @ParameterizedTest
    fun `with amount`(punishmentType: PunishmentType) {
      basicData.also {
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = punishmentType,
            amount = if (punishmentType == PunishmentType.DAMAGES_OWED) 100.0 else null,
            stoppagePercentage = if (punishmentType == PunishmentType.EARNINGS) 100 else null,
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, startDate = LocalDate.now().plusDays(1)),
            ),
          ),
        )
      }

      whenever(
        reportedAdjudicationRepository.findByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
          1L,
          ReportedAdjudicationStatus.CHARGE_PROVED,
          LocalDate.now().minusMonths(3).atStartOfDay(),
        ),
      ).thenReturn(listOf(basicData))
      val response = summaryAdjudicationService.getAdjudicationSummary(bookingId = 1L, awardCutoffDate = null, adjudicationCutoffDate = null)

      assertThat(response.awards.first().limit!!.toDouble()).isEqualTo(100.0)
      assertThat(response.awards.first().comment).contains("100")
    }

    @Test
    fun `with privilege`() {
      basicData.also {
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = PunishmentType.PRIVILEGE,
            privilegeType = PrivilegeType.FACILITIES,
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, startDate = LocalDate.now().plusDays(1)),
            ),
          ),
        )
      }

      whenever(
        reportedAdjudicationRepository.findByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
          1L,
          ReportedAdjudicationStatus.CHARGE_PROVED,
          LocalDate.now().minusMonths(3).atStartOfDay(),
        ),
      ).thenReturn(listOf(basicData))
      val response = summaryAdjudicationService.getAdjudicationSummary(bookingId = 1L, awardCutoffDate = null, adjudicationCutoffDate = null)

      assertThat(response.awards.first().sanctionCodeDescription).contains(PrivilegeType.FACILITIES.name)
    }

    @Test
    fun `with other privilege`() {
      basicData.also {
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = PunishmentType.PRIVILEGE,
            privilegeType = PrivilegeType.OTHER,
            otherPrivilege = "playstation",
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, startDate = LocalDate.now().plusDays(1)),
            ),
          ),
        )
      }

      whenever(
        reportedAdjudicationRepository.findByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
          1L,
          ReportedAdjudicationStatus.CHARGE_PROVED,
          LocalDate.now().minusMonths(3).atStartOfDay(),
        ),
      ).thenReturn(listOf(basicData))
      val response = summaryAdjudicationService.getAdjudicationSummary(bookingId = 1L, awardCutoffDate = null, adjudicationCutoffDate = null)

      assertThat(response.awards.first().sanctionCodeDescription).contains("playstation")
    }
  }

  @Nested
  inner class Adjudications {

    @BeforeEach
    fun init() {
      whenever(featureFlagsConfig.nomisSourceOfTruthAdjudications).thenReturn(false)
    }

    @Test
    fun `returns pageable adjudications - verify calls jpa prisoner number and status and date` () {

      whenever(reportedAdjudicationRepository.findByPrisonerNumberAndDateTimeOfDiscoveryBetweenAndStatusIn(any(), any(), any(), any(), any())).thenReturn(
        Page.empty()
      )

      summaryAdjudicationService.getAdjudications(
        prisonerNumber = "12345", offenceId = null, agencyId = null, finding = null, fromDate = LocalDate.now(), toDate = LocalDate.now(), pageable = PageRequest.of(1, 10)
      )
      verify(reportedAdjudicationRepository, atLeastOnce()).findByPrisonerNumberAndDateTimeOfDiscoveryBetweenAndStatusIn(any(), any(), any(), any(), any())
    }

    @Test
    fun `returns pageable adjudications - verify custom query and dates` () {

      whenever(reportedAdjudicationRepository.findByPrisonerNumberAndAgencyAndDate(any(), any(), any(), any(), any(), any(), any())).thenReturn(
        Page.empty()
      )

      summaryAdjudicationService.getAdjudications(
        prisonerNumber = "12345", offenceId = null, agencyId = "MDI", finding = null, fromDate = LocalDate.now(), toDate = LocalDate.now(), pageable = PageRequest.of(1, 10)
      )

      verify(reportedAdjudicationRepository, atLeastOnce()).findByPrisonerNumberAndAgencyAndDate(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `returns content correctly ` () {
      whenever(reportedAdjudicationRepository.findByPrisonerNumberAndDateTimeOfDiscoveryBetweenAndStatusIn(any(), any(), any(), any(), any())).thenReturn(
        PageImpl(
          listOf(entityBuilder.reportedAdjudication()),
        ),
      )

      val adjudications = summaryAdjudicationService.getAdjudications(
        prisonerNumber = "12345", offenceId = null, agencyId = null, finding = null, fromDate = LocalDate.now(), toDate = LocalDate.now(), pageable = PageRequest.of(1, 10))

    }
  }
}
