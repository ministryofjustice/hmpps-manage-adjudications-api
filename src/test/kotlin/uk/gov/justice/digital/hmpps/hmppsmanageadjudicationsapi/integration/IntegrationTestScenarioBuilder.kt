package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.DraftAdjudicationResponse

class IntegrationTestScenarioBuilder(
  private val intTestData: IntegrationTestData,
  private val intTestBase: IntegrationTestBase,
  private val headers: ((HttpHeaders) -> Unit) = intTestBase.setHeaders()
) {
  fun start(testAdjudication: AdjudicationIntTestDataSet): IntegrationTestScenario {
    val draftCreationResponse = intTestData.startNewAdjudication(testAdjudication)
    return IntegrationTestScenario(intTestData, intTestBase, headers, draftCreationResponse, testAdjudication)
  }
}

class IntegrationTestScenario(
  private val intTestData: IntegrationTestData,
  private val intTestBase: IntegrationTestBase,
  private val headers: ((HttpHeaders) -> Unit),
  private val draftCreationResponse: DraftAdjudicationResponse,
  private val testAdjudicationDataSet: AdjudicationIntTestDataSet
) {
  fun setOffenceData(): IntegrationTestScenario {
    intTestData.setOffenceDetails(draftCreationResponse, testAdjudicationDataSet, headers)
    return this
  }

  fun addIncidentStatement(): IntegrationTestScenario {
    intTestData.addIncidentStatement(draftCreationResponse, testAdjudicationDataSet, headers)
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

  fun getDraftId(): Long {
    return draftCreationResponse.draftAdjudication.id
  }

  fun getDraftAdjudicationDetails(): WebTestClient.ResponseSpec {
    return intTestData.getDraftAdjudicationDetails(draftCreationResponse)
  }

}