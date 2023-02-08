package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeFinding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import java.time.LocalDateTime

class HearingsIntTest : IntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME)
  }

  @Test
  fun `create a hearing `() {
    initDataForHearings()

    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 12, 10, 0)
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 1,
          "dateTimeOfHearing" to dateTimeOfHearing,
          "oicHearingType" to OicHearingType.GOV.name,
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
      .jsonPath("$.reportedAdjudication.hearings[0].locationId")
      .isEqualTo(1)
      .jsonPath("$.reportedAdjudication.hearings[0].dateTimeOfHearing")
      .isEqualTo("2010-10-12T10:00:00")
      .jsonPath("$.reportedAdjudication.hearings[0].id").isNotEmpty
      .jsonPath("$.reportedAdjudication.hearings[0].oicHearingType").isEqualTo(OicHearingType.GOV.name)
  }

  @Test
  fun `create a hearing illegal state on hearing type `() {
    initDataForHearings()

    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 12, 10, 0)
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 1,
          "dateTimeOfHearing" to dateTimeOfHearing,
          "oicHearingType" to OicHearingType.GOV_YOI.name,
        )
      )
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  fun `create hearing fails on prisonApi and does not create a hearing`() {
    initDataForHearings()

    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 12, 10, 0)

    prisonApiMockServer.stubCreateHearingFailure(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 1,
          "dateTimeOfHearing" to dateTimeOfHearing,
          "oicHearingType" to OicHearingType.GOV.name,
        )
      )
      .exchange()
      .expectStatus().is5xxServerError

    webTestClient.get()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)
  }

  @Test
  fun `amend a hearing `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    val reportedAdjudication = initDataForHearings().createHearing()

    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber, 100)
    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 25, 10, 0)

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 3,
          "dateTimeOfHearing" to dateTimeOfHearing.plusDays(1),
          "oicHearingType" to OicHearingType.GOV_ADULT.name,
        )
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
      .jsonPath("$.reportedAdjudication.hearings[0].locationId")
      .isEqualTo(3)
      .jsonPath("$.reportedAdjudication.hearings[0].dateTimeOfHearing")
      .isEqualTo("2010-10-26T10:00:00")
      .jsonPath("$.reportedAdjudication.hearings[0].id").isNotEmpty
      .jsonPath("$.reportedAdjudication.hearings[0].oicHearingType").isEqualTo(OicHearingType.GOV_ADULT.name)
  }

  @Test
  fun `amend a hearing illegal state on hearing type `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    val reportedAdjudication = initDataForHearings().createHearing()

    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber, 100)
    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 25, 10, 0)

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 3,
          "dateTimeOfHearing" to dateTimeOfHearing.plusDays(1),
          "oicHearingType" to OicHearingType.GOV_YOI.name,
        )
      )
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  fun `amend a hearing fails on prison api and does not update the hearing `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    val reportedAdjudication = initDataForHearings().createHearing()

    prisonApiMockServer.stubAmendHearingFailure(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber, 100)
    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 25, 10, 0)

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 3,
          "dateTimeOfHearing" to dateTimeOfHearing.plusDays(1),
          "oicHearingType" to OicHearingType.GOV_ADULT.name,
        )
      )
      .exchange()
      .expectStatus().is5xxServerError

    webTestClient.get()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings[0].locationId")
      .isEqualTo(IntegrationTestData.UPDATED_LOCATION_ID)
      .jsonPath("$.reportedAdjudication.hearings[0].dateTimeOfHearing")
      .isEqualTo("2010-11-19T10:00:00")
      .jsonPath("$.reportedAdjudication.hearings[0].id").isNotEmpty
  }

  @Test
  fun `delete a hearing `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    val reportedAdjudication = initDataForHearings().createHearing()

    prisonApiMockServer.stubDeleteHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber, 100)

    assert(reportedAdjudication.reportedAdjudication.hearings.size == 1)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id!!}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.UNSCHEDULED.name)
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)
  }

  @Test
  fun `delete a hearing fails on prison api and does not delete record`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    val reportedAdjudication = initDataForHearings().createHearing()

    prisonApiMockServer.stubDeleteHearingFailure(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber, 100)

    assert(reportedAdjudication.reportedAdjudication.hearings.size == 1)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id!!}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().is5xxServerError

    webTestClient.get()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(1)
  }

  @Test
  fun `create hearing outcome`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    val reportedAdjudication = initDataForHearings().createHearing()

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id}/outcome")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "test",
          "code" to HearingOutcomeCode.COMPLETE,
          "reason" to HearingOutcomeAdjournReason.LEGAL_ADVICE,
          "details" to "details",
          "finding" to HearingOutcomeFinding.DISMISSED,
          "plea" to HearingOutcomePlea.UNFIT,
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.id").isNotEmpty
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.adjudicator")
      .isEqualTo("test")
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.reason")
      .isEqualTo(HearingOutcomeAdjournReason.LEGAL_ADVICE.name)
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.details")
      .isEqualTo("details")
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.finding")
      .isEqualTo(HearingOutcomeFinding.DISMISSED.name)
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.plea")
      .isEqualTo(HearingOutcomePlea.UNFIT.name)
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.code").isEqualTo(HearingOutcomeCode.COMPLETE.name)
  }

  @Test
  fun `create hearing outcome for referral`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    val reportedAdjudication = initDataForHearings().createHearing()

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id}/outcome")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "test",
          "code" to HearingOutcomeCode.REFER_POLICE,
          "details" to "details",
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.id").isNotEmpty
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcome").isNotEmpty
      .jsonPath("$.reportedAdjudication.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcome.details").isEqualTo("details")
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.adjudicator")
      .isEqualTo("test")
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.reason").doesNotExist()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.plea").doesNotExist()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.finding").doesNotExist()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.details")
      .isEqualTo("details")
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.code").isEqualTo(HearingOutcomeCode.REFER_POLICE.name)
  }

  @Test
  fun `update hearing outcome`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    var reportedAdjudication = initDataForHearings().createHearing()
    reportedAdjudication = integrationTestData().createHearingOutcome(
      reportedAdjudication.reportedAdjudication.adjudicationNumber, reportedAdjudication.reportedAdjudication.hearings.first().id!!
    )

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id}/outcome")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "updated",
          "code" to HearingOutcomeCode.COMPLETE,
          "finding" to HearingOutcomeFinding.NOT_PROCEED_WITH,
          "plea" to HearingOutcomePlea.ABSTAIN,
        )
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.id").isNotEmpty
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.adjudicator")
      .isEqualTo("updated")
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.reason").doesNotExist()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.details").doesNotExist()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.finding")
      .isEqualTo(HearingOutcomeFinding.NOT_PROCEED_WITH.name)
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.plea")
      .isEqualTo(HearingOutcomePlea.ABSTAIN.name)
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.code").isEqualTo(HearingOutcomeCode.COMPLETE.name)
  }

  @Test
  @Disabled // currently not implemented fully so status will not update
  fun `update hearing outcome to a referral`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    var reportedAdjudication = initDataForHearings().createHearing()
    reportedAdjudication = integrationTestData().createHearingOutcome(
      reportedAdjudication.reportedAdjudication.adjudicationNumber, reportedAdjudication.reportedAdjudication.hearings.first().id!!
    )

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id}/outcome")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "updated",
          "code" to HearingOutcomeCode.REFER_INAD,
          "details" to "details updated"
        )
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.id").isNotEmpty
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcome").isNotEmpty
      .jsonPath("$.reportedAdjudication.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.reason").doesNotExist()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.plea").doesNotExist()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.finding").doesNotExist()
      .jsonPath("$.reportedAdjudication.outcome.details").isEqualTo("details updated")
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.adjudicator")
      .isEqualTo("updated")
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.details").isEqualTo("details updated")
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.code").isEqualTo(HearingOutcomeCode.REFER_INAD.name)
  }

  @Test
  fun `referral transaction is rolled back when hearing outcome succeeds and outcome creation fails`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    val reportedAdjudication = initDataForHearings().createHearing()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/status")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "status" to ReportedAdjudicationStatus.REFER_POLICE,
          "statusReason" to "status reason",
          "statusDetails" to "status details"
        )
      )
      .exchange()
      .expectStatus().is2xxSuccessful

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/${reportedAdjudication.reportedAdjudication.hearings.first().id}/outcome")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "test",
          "code" to HearingOutcomeCode.REFER_INAD,
          "details" to "details",
        )
      )
      .exchange()
      .expectStatus().isBadRequest

    webTestClient.get()
      .uri("/reported-adjudications/1524242")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcome").doesNotExist()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome").doesNotExist()
  }

  @Test
  fun `get all hearings`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    val reportedAdjudication = initDataForHearings().createHearing()

    webTestClient.get()
      .uri("/reported-adjudications/hearings/agency/${IntegrationTestData.DEFAULT_ADJUDICATION.agencyId}?hearingDate=${reportedAdjudication.reportedAdjudication.hearings.first().dateTimeOfHearing.toLocalDate()}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.hearings.size()").isEqualTo(1)
      .jsonPath("$.hearings[0].id")
      .isEqualTo(reportedAdjudication.reportedAdjudication.hearings.first().id!!)
      .jsonPath("$.hearings[0].prisonerNumber")
      .isEqualTo(reportedAdjudication.reportedAdjudication.prisonerNumber)
      .jsonPath("$.hearings[0].adjudicationNumber")
      .isEqualTo(reportedAdjudication.reportedAdjudication.adjudicationNumber)
      .jsonPath("$.hearings[0].dateTimeOfDiscovery")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfDiscoveryISOString)
      .jsonPath("$.hearings[0].dateTimeOfHearing")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfHearingISOString)
      .jsonPath("$.hearings[0].oicHearingType")
      .isEqualTo(reportedAdjudication.reportedAdjudication.hearings.first().oicHearingType.name)
  }

  @Test
  fun `remove referral with hearing`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    val reportedAdjudication = initDataForHearings().createHearing()
    integrationTestData().createHearingOutcome(
      reportedAdjudication.reportedAdjudication.adjudicationNumber, reportedAdjudication.reportedAdjudication.hearings.first().id!!, HearingOutcomeCode.REFER_POLICE
    )

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcome").doesNotExist()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome").doesNotExist()
  }

  @Test
  fun `remove referral with hearing and referral outcome`() {
    TODO("implement me")
  }

  private fun initDataForHearings(): IntegrationTestScenario {
    prisonApiMockServer.stubPostAdjudication(IntegrationTestData.DEFAULT_ADJUDICATION)

    val intTestData = integrationTestData()
    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, draftUserHeaders)

    return draftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .addDamages()
      .addEvidence()
      .addWitnesses()
      .completeDraft()
      .acceptReport(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber.toString())
  }
}
