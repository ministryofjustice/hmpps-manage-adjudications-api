package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DraftAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus

class IntegrationTestScenarioBuilder(
  private val intTestData: IntegrationTestData,
  private val intTestBase: IntegrationTestBase,
  private val headers: ((HttpHeaders) -> Unit) = intTestBase.setHeaders()
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
  private val testAdjudicationDataSet: AdjudicationIntTestDataSet
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
      headers
    )
    return this
  }

  fun acceptReport(reportNumber: String): IntegrationTestScenario {
    intTestData.acceptReport(
      reportNumber,
    )
    return this
  }

  fun issueReport(reportNumber: String): IntegrationTestScenario {
    intTestData.issueReport(
      draftCreationResponse,
      reportNumber,
      headers
    )
    return this
  }

  fun reportedAdjudicationSetStatus(reportedAdjudicationStatus: ReportedAdjudicationStatus) {
    intTestData.reportedAdjudicationStatus(reportedAdjudicationStatus, testAdjudicationDataSet, headers)
  }

  fun getDraftId(): Long {
    return draftCreationResponse.draftAdjudication.id
  }

  fun getDraftAdjudicationDetails(): WebTestClient.ResponseSpec {
    return intTestData.getDraftAdjudicationDetails(draftCreationResponse)
  }
}
