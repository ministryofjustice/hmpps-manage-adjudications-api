package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DraftAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import java.time.LocalDateTime

class IntegrationTestScenarioBuilder(
  private val intTestData: IntegrationTestData,
  private val intTestBase: IntegrationTestBase,
  private val activeCaseload: String? = "MDI",
  private val headers: ((HttpHeaders) -> Unit) = intTestBase.setHeaders(activeCaseload = activeCaseload, roles = listOf("ROLE_ADJUDICATIONS_REVIEWER", "ROLE_VIEW_ADJUDICATIONS")),
) {
  fun startDraft(testAdjudication: AdjudicationIntTestDataSet): IntegrationTestScenario {
    val draftCreationResponse = intTestData.startNewAdjudication(testAdjudication)
    return IntegrationTestScenario(intTestData, headers, draftCreationResponse, testAdjudication)
  }
}

class IntegrationTestScenario(
  private val intTestData: IntegrationTestData,
  private val headers: ((HttpHeaders) -> Unit),
  private val draftCreationResponse: DraftAdjudicationResponse,
  private val testAdjudicationDataSet: AdjudicationIntTestDataSet,
) {
  fun setApplicableRules(): IntegrationTestScenario {
    intTestData.setApplicableRules(draftCreationResponse, testAdjudicationDataSet, headers)
    return this
  }

  fun setIncidentRole(): IntegrationTestScenario {
    intTestData.setIncidentRole(draftCreationResponse, testAdjudicationDataSet, headers)
    return this
  }

  fun setAssociatedPrisoner(): IntegrationTestScenario {
    intTestData.setAssociatedPrisoner(draftCreationResponse, testAdjudicationDataSet, headers)
    return this
  }

  fun setOffenceData(): IntegrationTestScenario {
    intTestData.setOffenceDetails(draftCreationResponse, testAdjudicationDataSet, headers)
    return this
  }

  fun addIncidentStatement(): IntegrationTestScenario {
    intTestData.addIncidentStatement(draftCreationResponse, testAdjudicationDataSet, headers)
    return this
  }

  fun addDamages(): IntegrationTestScenario {
    intTestData.addDamages(draftCreationResponse, testAdjudicationDataSet, headers)
    return this
  }

  fun addEvidence(): IntegrationTestScenario {
    intTestData.addEvidence(draftCreationResponse, testAdjudicationDataSet, headers)
    return this
  }

  fun addWitnesses(): IntegrationTestScenario {
    intTestData.addWitnesses(draftCreationResponse, testAdjudicationDataSet, headers)
    return this
  }

  fun completeDraft(): IntegrationTestScenario {
    intTestData.completeDraftAdjudication(
      draftCreationResponse,
      testAdjudicationDataSet,
      headers,
    )
    return this
  }

  fun acceptReport(reportNumber: String, activeCaseload: String = "MDI"): IntegrationTestScenario {
    intTestData.acceptReport(
      reportNumber,
      activeCaseload,
    )
    return this
  }

  fun createHearing(
    overrideTestDataSet: AdjudicationIntTestDataSet = testAdjudicationDataSet,
    dateTimeOfHearing: LocalDateTime? = null,
    oicHearingType: OicHearingType? = OicHearingType.GOV,
  ): IntegrationTestScenario {
    intTestData.createHearing(
      overrideTestDataSet,
      dateTimeOfHearing,
      oicHearingType,
    )
    return this
  }

  fun createChargeProved(
    overrideTestDataSet: AdjudicationIntTestDataSet = testAdjudicationDataSet,
  ): IntegrationTestScenario {
    intTestData.createChargeProved(
      overrideTestDataSet,
    )
    return this
  }

  fun createPunishments(): IntegrationTestScenario {
    intTestData.createPunishments(testAdjudicationDataSet)

    return this
  }

  fun createNotProceed(): IntegrationTestScenario {
    intTestData.createNotProceed(
      testAdjudicationDataSet,
    )
    return this
  }

  fun createDismissed(): IntegrationTestScenario {
    intTestData.createDismissed(
      testAdjudicationDataSet,
    )
    return this
  }

  fun createAdjourn(): IntegrationTestScenario {
    intTestData.createAdjourn(
      testAdjudicationDataSet,
    )
    return this
  }

  fun createQuashed(): IntegrationTestScenario {
    intTestData.createQuashed(
      testAdjudicationDataSet,
    )
    return this
  }

  fun createReferral(
    code: HearingOutcomeCode,
  ): IntegrationTestScenario {
    intTestData.createReferral(
      testAdjudicationDataSet,
      code,
    )
    return this
  }

  fun createOutcomeReferPolice(): IntegrationTestScenario {
    intTestData.createOutcomeReferPolice(testAdjudicationDataSet)
    return this
  }

  fun createOutcomeProsecution(): WebTestClient.ResponseSpec {
    return intTestData.createOutcomeProsecution(testAdjudicationDataSet)
  }

  fun createOutcomeNotProceed(): WebTestClient.ResponseSpec {
    return intTestData.createOutcomeNotProceed(testAdjudicationDataSet)
  }

  fun issueReport(reportNumber: String): IntegrationTestScenario {
    intTestData.issueReport(
      draftCreationResponse,
      reportNumber,
      headers,
    )
    return this
  }

  fun reportedAdjudicationSetStatus(reportedAdjudicationStatus: ReportedAdjudicationStatus) {
    intTestData.reportedAdjudicationStatus(reportedAdjudicationStatus, testAdjudicationDataSet, headers)
  }

  fun getDraftId(): Long {
    return draftCreationResponse.draftAdjudication.id
  }

  fun getDraftAdjudicationDetails(activeCaseload: String? = "MDI"): WebTestClient.ResponseSpec {
    return intTestData.getDraftAdjudicationDetails(draftCreationResponse, activeCaseload = activeCaseload)
  }
}
