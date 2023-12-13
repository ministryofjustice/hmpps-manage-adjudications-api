package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationServiceTest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import java.time.LocalDateTime

class LegacySyncServiceTest {

  private val legacyNomisGateway: LegacyNomisGateway = mock()
  private val featureFlagsConfig: FeatureFlagsConfig = mock()
  private val offenceCodeLookupService = OffenceCodeLookupService()

  private val legacySyncService = LegacySyncService(
    legacyNomisGateway = legacyNomisGateway,
    featureFlagsConfig = featureFlagsConfig,
    offenceCodeLookupService = offenceCodeLookupService,
  )

  @ParameterizedTest
  @CsvSource(
    "true",
    "false",
  )
  fun `setting status with different roles submits to prison api with correct data`(
    committedOnOwn: Boolean,
  ) {
    val existingReportedAdjudication = existingReportedAdjudication(committedOnOwn, true, true)

    val returnedReportedAdjudication = existingReportedAdjudication.copy().also {
      it.status = ReportedAdjudicationStatus.UNSCHEDULED
    }
    returnedReportedAdjudication.createdByUserId = "A_USER"
    returnedReportedAdjudication.createDateTime = ReportedAdjudicationServiceTest.REPORTED_DATE_TIME

    var expectedConnectedOffenderIds: List<String> = emptyList()
    if (!committedOnOwn) {
      expectedConnectedOffenderIds = listOf(ReportedAdjudicationServiceTest.INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER)
    }

    val expectedVictimOffenderIds: List<String> = listOf("A1234AA")
    val expectedVictimStaffUsernames: List<String> = listOf("ABC12D")

    legacySyncService.publishAdjudication(returnedReportedAdjudication)

    val expectedAdjudicationToPublish = AdjudicationDetailsToPublish(
      offenderNo = "A12345",
      adjudicationNumber = 1235L,
      reporterName = "A_USER",
      reportedDateTime = ReportedAdjudicationServiceTest.REPORTED_DATE_TIME,
      agencyId = "MDI",
      incidentLocationId = 2L,
      incidentTime = ReportedAdjudicationTestBase.DATE_TIME_OF_INCIDENT.plusDays(1),
      statement = ReportedAdjudicationServiceTest.INCIDENT_STATEMENT,
      offenceCodes = if (committedOnOwn) {
        listOf(offenceCodeLookupService.getOffenceCode(1002, true).getNomisCode())
      } else {
        listOf(offenceCodeLookupService.getOffenceCode(1002, true).getNomisCodeWithOthers())
      },
      victimOffenderIds = expectedVictimOffenderIds,
      victimStaffUsernames = expectedVictimStaffUsernames,
      connectedOffenderIds = expectedConnectedOffenderIds,
    )
    verify(legacyNomisGateway).publishAdjudication(expectedAdjudicationToPublish)
  }

  @ParameterizedTest
  @CsvSource(
    "true",
    "false",
  )
  fun `setting status with different youth offender flag submits to prison api with correct data`(
    isYouthOffender: Boolean,
  ) {
    val existingReportedAdjudication = existingReportedAdjudication(false, isYouthOffender, false)

    val returnedReportedAdjudication = existingReportedAdjudication.copy().also {
      it.status = ReportedAdjudicationStatus.UNSCHEDULED
    }
    returnedReportedAdjudication.createdByUserId = "A_USER"
    returnedReportedAdjudication.createDateTime = ReportedAdjudicationServiceTest.REPORTED_DATE_TIME

    val expectedConnectedOffenderIds = listOf(ReportedAdjudicationServiceTest.INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER)
    val expectedVictimOffenderIds: List<String> = emptyList()
    val expectedVictimStaffUsernames: List<String> = emptyList()

    legacySyncService.publishAdjudication(returnedReportedAdjudication)

    val expectedAdjudicationToPublish = AdjudicationDetailsToPublish(
      offenderNo = "A12345",
      adjudicationNumber = 1235L,
      reporterName = "A_USER",
      reportedDateTime = ReportedAdjudicationServiceTest.REPORTED_DATE_TIME,
      agencyId = "MDI",
      incidentLocationId = 2L,
      incidentTime = ReportedAdjudicationTestBase.DATE_TIME_OF_INCIDENT.plusDays(1),
      statement = ReportedAdjudicationServiceTest.INCIDENT_STATEMENT,
      offenceCodes = listOf(offenceCodeLookupService.getOffenceCode(1002, isYouthOffender).getNomisCodeWithOthers()),
      victimOffenderIds = expectedVictimOffenderIds,
      victimStaffUsernames = expectedVictimStaffUsernames,
      connectedOffenderIds = expectedConnectedOffenderIds,
    )
    verify(legacyNomisGateway).publishAdjudication(expectedAdjudicationToPublish)
  }

  @Test
  fun `create adjudication removes offender from multiple roles for prison api `() {
    val now = LocalDateTime.now()
    val reportedAdjudication = ReportedAdjudicationTestBase.entityBuilder.reportedAdjudication().also {
      it.createdByUserId = ""
      it.createDateTime = now
      it.incidentRoleAssociatedPrisonersNumber = null
      it.offenceDetails.first().victimPrisonersNumber = "A12345"
      it.offenceDetails.first().victimStaffUsername = null
      it.offenceDetails.first().victimOtherPersonsName = null
    }

    legacySyncService.publishAdjudication(reportedAdjudication)

    verify(legacyNomisGateway, atLeastOnce()).publishAdjudication(
      adjudicationDetailsToPublish = AdjudicationDetailsToPublish(
        offenderNo = reportedAdjudication.prisonerNumber,
        adjudicationNumber = reportedAdjudication.chargeNumber.toLong(),
        reporterName = "",
        reportedDateTime = now,
        agencyId = reportedAdjudication.originatingAgencyId,
        incidentLocationId = reportedAdjudication.locationId,
        incidentTime = reportedAdjudication.dateTimeOfDiscovery,
        statement = reportedAdjudication.statement,
        offenceCodes = listOf(offenceCodeLookupService.getOffenceCode(1002, false).getNomisCodeWithOthers()),
        victimStaffUsernames = emptyList(),
        victimOffenderIds = emptyList(),
        connectedOffenderIds = emptyList(),
      ),
    )
  }

  @Test
  fun `create adjudication removes duplicate when associate is victim `() {
    val now = LocalDateTime.now()
    val reportedAdjudication = ReportedAdjudicationTestBase.entityBuilder.reportedAdjudication().also {
      it.createdByUserId = ""
      it.createDateTime = now
      it.incidentRoleAssociatedPrisonersNumber = "A12347"
      it.offenceDetails.first().victimPrisonersNumber = "A12347"
      it.offenceDetails.first().victimStaffUsername = null
      it.offenceDetails.first().victimOtherPersonsName = null
    }

    legacySyncService.publishAdjudication(reportedAdjudication)

    verify(legacyNomisGateway, atLeastOnce()).publishAdjudication(
      adjudicationDetailsToPublish = AdjudicationDetailsToPublish(
        offenderNo = reportedAdjudication.prisonerNumber,
        adjudicationNumber = reportedAdjudication.chargeNumber.toLong(),
        reporterName = "",
        reportedDateTime = now,
        agencyId = reportedAdjudication.originatingAgencyId,
        incidentLocationId = reportedAdjudication.locationId,
        incidentTime = reportedAdjudication.dateTimeOfDiscovery,
        statement = reportedAdjudication.statement,
        offenceCodes = listOf(offenceCodeLookupService.getOffenceCode(1002, false).getNomisCodeWithOthers()),
        victimStaffUsernames = emptyList(),
        victimOffenderIds = emptyList(),
        connectedOffenderIds = listOf("A12347"),
      ),
    )
  }

  private fun existingReportedAdjudication(
    committedOnOwn: Boolean,
    isYouthOffender: Boolean,
    withVictims: Boolean,
  ): ReportedAdjudication {
    var incidentRoleCode: String? = null
    var incidentRoleAssociatedPrisonersNumber: String? = null
    var incidentRoleAssociatedPrisonersName: String? = null
    if (!committedOnOwn) {
      incidentRoleCode = ReportedAdjudicationServiceTest.INCIDENT_ROLE_CODE
      incidentRoleAssociatedPrisonersNumber = ReportedAdjudicationServiceTest.INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER
      incidentRoleAssociatedPrisonersName = ReportedAdjudicationServiceTest.INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME
    }

    val offenceDetails = if (withVictims) {
      mutableListOf(
        ReportedOffence(
          offenceCode = 1002,
          victimPrisonersNumber = "A1234AA",
          victimStaffUsername = "ABC12D",
          victimOtherPersonsName = "A name",
        ),
      )
    } else {
      mutableListOf(
        ReportedOffence(offenceCode = 1002),
      )
    }

    val reportedAdjudication = ReportedAdjudicationTestBase.entityBuilder.reportedAdjudication(dateTime = ReportedAdjudicationTestBase.DATE_TIME_OF_INCIDENT)
      .also {
        it.offenceDetails = offenceDetails
        it.isYouthOffender = isYouthOffender
        it.incidentRoleCode = incidentRoleCode
        it.incidentRoleAssociatedPrisonersName = incidentRoleAssociatedPrisonersName
        it.incidentRoleAssociatedPrisonersNumber = incidentRoleAssociatedPrisonersNumber
      }
    // Add audit information
    reportedAdjudication.createdByUserId = "A_USER"
    reportedAdjudication.createDateTime = ReportedAdjudicationServiceTest.REPORTED_DATE_TIME
    return reportedAdjudication
  }
}
