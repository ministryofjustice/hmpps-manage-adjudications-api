package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DamageRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.EvidenceRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.IncidentRoleRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.WitnessRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Characteristic
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import java.time.LocalDateTime

class DraftAdjudicationIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME)
  }

  @Test
  fun `makes a request to start a new draft adjudication`() {
    webTestClient.post()
      .uri("/draft-adjudications")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "prisonerNumber" to "A12345",
          "agencyId" to "MDI",
          "locationId" to 1,
          "dateTimeOfIncident" to DATE_TIME_OF_INCIDENT,
          "dateTimeOfDiscovery" to DATE_TIME_OF_INCIDENT.plusDays(1),
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo("A12345")
      .jsonPath("$.draftAdjudication.gender").isEqualTo(Gender.MALE.name)
      .jsonPath("$.draftAdjudication.startedByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudication.originatingAgencyId").isEqualTo("MDI")
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo("2010-10-12T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfDiscovery").isEqualTo("2010-10-13T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").isEqualTo("2010-10-15T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(1)
  }

  @Test
  fun `create draft adjudication with override agency id set`() {
    webTestClient.post()
      .uri("/draft-adjudications")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "prisonerNumber" to "A12345",
          "agencyId" to "MDI",
          "locationId" to 1,
          "dateTimeOfIncident" to DATE_TIME_OF_INCIDENT,
          "dateTimeOfDiscovery" to DATE_TIME_OF_INCIDENT.plusDays(1),
          "overrideAgencyId" to "BXI",
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.draftAdjudication.overrideAgencyId").isEqualTo("BXI")
      .jsonPath("$.draftAdjudication.originatingAgencyId").isEqualTo("MDI")
  }

  @Test
  fun `get draft adjudication details`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val userHeaders = setHeaders(username = testAdjudication.createdByUserId, activeCaseload = testAdjudication.agencyId)
    val intTestBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = userHeaders,
    )

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()

    webTestClient.get()
      .uri("/draft-adjudications/${intTestScenario.getDraftId()}")
      .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
  }

  @Test
  fun `get previously submitted draft adjudication details`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()

    val userHeaders = setHeaders(username = testAdjudication.createdByUserId, activeCaseload = testAdjudication.agencyId)
    val intTestBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = userHeaders,
    )

    intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val draftAdjudicationResponse = intTestData.recallCompletedDraftAdjudication(testAdjudication, headers = setHeaders(activeCaseload = testAdjudication.agencyId))

    testAdjudication.chargeNumber?.let {
      webTestClient.get()
        .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}")
        .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()
        .jsonPath("$.draftAdjudication.id").isNumber
        .jsonPath("$.draftAdjudication.chargeNumber").isEqualTo(it)
    }
  }

  @Test
  fun `add offence details to the draft adjudication`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val userHeaders = setHeaders(username = testAdjudication.createdByUserId, activeCaseload = testAdjudication.agencyId)
    val intTestBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = userHeaders,
    )

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()

    webTestClient.put()
      .uri("/draft-adjudications/${intTestScenario.getDraftId()}/offence-details")
      .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
      .bodyValue(
        mapOf(
          "offenceDetails" to testAdjudication.offence,
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.offenceDetails.offenceRule.paragraphNumber")
      .isEqualTo(testAdjudication.offence.paragraphNumber)
      .jsonPath("$.draftAdjudication.offenceDetails.offenceRule.paragraphDescription")
      .isEqualTo(testAdjudication.offence.paragraphDescription)
      .jsonPath("$.draftAdjudication.offenceDetails.victimPrisonersNumber")
      .isEqualTo(testAdjudication.offence.victimPrisonersNumber!!)
      .jsonPath("$.draftAdjudication.offenceDetails.victimStaffUsername")
      .isEqualTo(testAdjudication.offence.victimStaffUsername!!)
      .jsonPath("$.draftAdjudication.offenceDetails.victimOtherPersonsName")
      .isEqualTo(testAdjudication.offence.victimOtherPersonsName!!)
  }

  @Test
  fun `add offence details with protected characteristics`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val userHeaders = setHeaders(username = testAdjudication.createdByUserId, activeCaseload = testAdjudication.agencyId)
    val intTestBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = userHeaders,
    )

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()

    webTestClient.put()
      .uri("/draft-adjudications/${intTestScenario.getDraftId()}/offence-details")
      .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
      .bodyValue(
        mapOf(
          "offenceDetails" to testAdjudication.offence.also {
            it.protectedCharacteristics = mutableListOf(
              Characteristic.AGE,
            )
          },
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.draftAdjudication.offenceDetails.protectedCharacteristics[0]").isEqualTo(Characteristic.AGE.name)
  }

  @Test
  fun `add the incident statement to the draft adjudication`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()

    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)

    webTestClient.post()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/incident-statement")
      .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
      .bodyValue(
        mapOf(
          "statement" to "test",
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo(testAdjudication.prisonerNumber)
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident")
      .isEqualTo(testAdjudication.dateTimeOfIncidentISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline")
      .isEqualTo(testAdjudication.handoverDeadlineISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(testAdjudication.locationId)
      .jsonPath("$.draftAdjudication.incidentStatement.statement").isEqualTo("test")
  }

  @Test
  fun `edit the incident statement and mark as complete`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val intTestBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      activeCaseload = testAdjudication.agencyId,
    )

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .addIncidentStatement()

    webTestClient.put()
      .uri("/draft-adjudications/${intTestScenario.getDraftId()}/incident-statement")
      .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
      .bodyValue(
        mapOf(
          "statement" to "new statement",
          "completed" to true,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo(testAdjudication.prisonerNumber)
      .jsonPath("$.draftAdjudication.incidentStatement.statement").isEqualTo("new statement")
      .jsonPath("$.draftAdjudication.incidentStatement.completed").isEqualTo(true)
  }

  @Test
  fun `edit the incident details`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()

    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)

    webTestClient.put()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/incident-details")
      .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
      .bodyValue(
        mapOf(
          "locationId" to 3,
          "dateTimeOfIncident" to DATE_TIME_OF_INCIDENT.plusMonths(1),
          "dateTimeOfDiscovery" to DATE_TIME_OF_INCIDENT.plusMonths(1).plusDays(1),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo(testAdjudication.prisonerNumber)
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo("2010-11-12T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfDiscovery").isEqualTo("2010-11-13T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").isEqualTo("2010-11-15T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(3)
  }

  @Test
  fun `edit the incident role and delete all offences`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val intTestBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      activeCaseload = testAdjudication.agencyId,
    )

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setOffenceData()

    val draftId = intTestScenario.getDraftId()

    webTestClient.put()
      .uri("/draft-adjudications/$draftId/incident-role")
      .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
      .bodyValue(
        mapOf(
          "incidentRole" to IncidentRoleRequest("25b"),
          "removeExistingOffences" to true,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.offenceDetails").doesNotExist()
  }

  @Test
  fun `set the associated prisoner`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val intTestBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      activeCaseload = testAdjudication.agencyId,
    )

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()

    val draftId = intTestScenario.getDraftId()

    webTestClient.put()
      .uri("/draft-adjudications/$draftId/associated-prisoner")
      .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
      .bodyValue(
        mapOf(
          "associatedPrisonersNumber" to "A1234AA",
          "associatedPrisonersName" to "Associated Prisoner Name",
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.incidentRole.roleCode").isEqualTo(IntegrationTestData.ADJUDICATION_1.incidentRoleCode)
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").isEqualTo("A1234AA")
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersName").isEqualTo("Associated Prisoner Name")
  }

  @Test
  fun `complete draft adjudication`() {
    val intTestData = integrationTestData()
    val firstDraftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val intTestBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = firstDraftUserHeaders,
    )

    val intTestScenario = intTestBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addDamages()
      .addIncidentStatement()

    webTestClient.post()
      .uri("/draft-adjudications/${intTestScenario.getDraftId()}/complete-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.chargeNumber").isEqualTo(intTestScenario.getGeneratedChargeNumber())

    intTestScenario.getDraftAdjudicationDetails().expectStatus().isNotFound
  }

  @Test
  fun `complete draft update of existing adjudication`() {
    val intTestData = integrationTestData()
    val firstDraftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val intTestBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = firstDraftUserHeaders,
    )

    val intTestScenario = intTestBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION, "BXI")
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addDamages()
      .addIncidentStatement()

    getReportedAdjudicationRequestStatus().isNotFound

    val scenario = intTestScenario.completeDraft()
    getReportedAdjudicationRequestStatus().isOk

    val draftAdjudicationResponse =
      intTestData.recallCompletedDraftAdjudication(IntegrationTestData.DEFAULT_ADJUDICATION)
    intTestData.editIncidentDetails(draftAdjudicationResponse, IntegrationTestData.UPDATED_ADJUDICATION)
    intTestData.setIncidentRole(draftAdjudicationResponse, IntegrationTestData.UPDATED_ADJUDICATION)
    intTestData.setOffenceDetails(draftAdjudicationResponse, IntegrationTestData.UPDATED_ADJUDICATION)
    intTestData.addDamages(draftAdjudicationResponse, IntegrationTestData.UPDATED_ADJUDICATION)
    intTestData.editIncidentStatement(draftAdjudicationResponse, IntegrationTestData.UPDATED_ADJUDICATION)

    webTestClient.post()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/complete-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.chargeNumber").isEqualTo(scenario.getGeneratedChargeNumber())
      .jsonPath("$.overrideAgencyId").isEqualTo("BXI")
      .jsonPath("$.damages[0].code")
      .isEqualTo(DamageCode.CLEANING.name)
      .jsonPath("$.damages[0].details")
      .isEqualTo("details")
      .jsonPath("$.damages[0].reporter")
      .isEqualTo("B_MILLS")
      .jsonPath("$.damages[1].code")
      .isEqualTo(DamageCode.REDECORATION.name)
      .jsonPath("$.damages[1].details")
      .isEqualTo("details")
      .jsonPath("$.damages[1].reporter")
      .isEqualTo("ITAG_USER")

    intTestData.getDraftAdjudicationDetails(draftAdjudicationResponse).expectStatus().isNotFound
    getReportedAdjudicationRequestStatus().isOk
  }

  @Test
  fun `returns all in progress draft adjudications created by the current user in the given caseload`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()

    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)

    webTestClient.get()
      .uri("/draft-adjudications/my-reports?startDate=2020-12-01")
      .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
      .jsonPath("$.content[0].id").isEqualTo(draftAdjudicationResponse.draftAdjudication.id)
  }

  @Test
  fun `gets adult offence rule details`() {
    webTestClient.get()
      .uri("/draft-adjudications/offence-rule/9001?youthOffender=false")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.paragraphNumber").isEqualTo(9)
      .jsonPath("$.paragraphDescription").isEqualTo("Is found with any substance in his urine which demonstrates that a controlled drug, pharmacy medication, prescription only medicine, psychoactive substance or specified substance has, whether in prison or while on temporary release under rule 9, been administered to him by himself or by another person (but subject to Rule 52)")
  }

  @Test
  fun `gets youth offence rule details`() {
    webTestClient.get()
      .uri("/draft-adjudications/offence-rule/3001?youthOffender=true")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.paragraphNumber").isEqualTo(4)
      .jsonPath("$.paragraphDescription").isEqualTo("Denies access to any part of the young offender institution to any officer or any person (other than an inmate) who is at the young offender institution for the purpose of working there")
  }

  @Test
  fun `gets default offence rule details for an invalid offence code`() {
    webTestClient.get()
      .uri("/draft-adjudications/offence-rule/999?youthOffender=false")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.paragraphNumber").isEqualTo("")
      .jsonPath("$.paragraphDescription").isEqualTo("")
  }

  @Test
  fun `gets all adult offence rules`() {
    webTestClient.get()
      .uri("/draft-adjudications/offence-rules?youthOffender=false&version=1")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(28)
  }

  @Test
  fun `set the applicable rule and delete all offences`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val intTestBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      activeCaseload = testAdjudication.agencyId,
    )

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()

    val draftId = intTestScenario.getDraftId()

    webTestClient.get()
      .uri("/draft-adjudications/$draftId")
      .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
      .exchange()
      .expectBody()
      .jsonPath("$.draftAdjudication.isYouthOffender").isEqualTo(false)

    webTestClient.put()
      .uri("/draft-adjudications/$draftId/applicable-rules")
      .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
      .bodyValue(
        mapOf(
          "isYouthOffenderRule" to true,
          "removeExistingOffences" to true,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.isYouthOffender").isEqualTo(true)
      .jsonPath("$.draftAdjudication.offenceDetails").doesNotExist()
  }

  @Test
  fun `add damages to the draft adjudication`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val intTestBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      activeCaseload = testAdjudication.agencyId,
    )

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()

    val draftId = intTestScenario.getDraftId()

    webTestClient.put()
      .uri("/draft-adjudications/$draftId/damages")
      .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
      .bodyValue(
        mapOf(
          "damages" to listOf(
            DamageRequestItem(
              code = DamageCode.CLEANING,
              details = "details",
            ),
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.damagesSaved").isEqualTo(true)
      .jsonPath("$.draftAdjudication.damages[0].code")
      .isEqualTo(DamageCode.CLEANING.name)
      .jsonPath("$.draftAdjudication.damages[0].details")
      .isEqualTo("details")
      .jsonPath("$.draftAdjudication.damages[0].reporter")
      .isEqualTo("ITAG_USER")
  }

  @Test
  fun `add evidence to the draft adjudication`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val intTestBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      activeCaseload = testAdjudication.agencyId,
    )

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()

    val draftId = intTestScenario.getDraftId()

    webTestClient.put()
      .uri("/draft-adjudications/$draftId/evidence")
      .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
      .bodyValue(
        mapOf(
          "evidence" to listOf(
            EvidenceRequestItem(
              code = EvidenceCode.PHOTO,
              details = "details",
            ),
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.evidenceSaved").isEqualTo(true)
      .jsonPath("$.draftAdjudication.evidence[0].code")
      .isEqualTo(EvidenceCode.PHOTO.name)
      .jsonPath("$.draftAdjudication.evidence[0].details")
      .isEqualTo("details")
      .jsonPath("$.draftAdjudication.evidence[0].reporter")
      .isEqualTo("ITAG_USER")
  }

  @Test
  fun `add witnesses to the draft adjudication`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val intTestBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      activeCaseload = testAdjudication.agencyId,
    )

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()

    val draftId = intTestScenario.getDraftId()

    webTestClient.put()
      .uri("/draft-adjudications/$draftId/witnesses")
      .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
      .bodyValue(
        mapOf(
          "witnesses" to listOf(
            WitnessRequestItem(
              code = WitnessCode.OFFICER,
              firstName = "prison",
              lastName = "officer",
            ),
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.witnessesSaved").isEqualTo(true)
      .jsonPath("$.draftAdjudication.witnesses[0].code")
      .isEqualTo(WitnessCode.OFFICER.name)
      .jsonPath("$.draftAdjudication.witnesses[0].firstName")
      .isEqualTo("prison")
      .jsonPath("$.draftAdjudication.witnesses[0].reporter")
      .isEqualTo("ITAG_USER")
  }

  @Test
  fun `set gender to female`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()

    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)

    webTestClient.put()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/gender")
      .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
      .bodyValue(
        mapOf(
          "gender" to Gender.FEMALE.name,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.gender").isEqualTo(Gender.FEMALE.name)
  }

  @Test
  fun `set created on behalf of`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()

    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)
    val officer = "officer"
    val reason = "some reason"

    webTestClient.put()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/created-on-behalf-of")
      .headers(setHeaders(activeCaseload = testAdjudication.agencyId))
      .bodyValue(
        mapOf(
          "createdOnBehalfOfOfficer" to officer,
          "createdOnBehalfOfReason" to reason,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.createdOnBehalfOfOfficer").isEqualTo(officer)
      .jsonPath("$.draftAdjudication.createdOnBehalfOfReason").isEqualTo(reason)
  }

  @Test
  fun `delete draft adjudication`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val username = testAdjudication.createdByUserId
    val userHeaders = setHeaders(username = username, activeCaseload = testAdjudication.agencyId)
    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication, userHeaders)

    val draftId = draftAdjudicationResponse.draftAdjudication.id

    webTestClient.delete()
      .uri("/draft-adjudications/$draftId")
      .headers(setHeaders(username = username, activeCaseload = testAdjudication.agencyId))
      .exchange()
      .expectStatus().isOk

    webTestClient.get()
      .uri("/draft-adjudications/$draftId")
      .headers(setHeaders(username = username, activeCaseload = testAdjudication.agencyId))
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `not owner cannot delete draft adjudication`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val username = testAdjudication.createdByUserId
    val userHeaders = setHeaders(username = username)
    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication, userHeaders)

    val draftId = draftAdjudicationResponse.draftAdjudication.id

    webTestClient.delete()
      .uri("/draft-adjudications/$draftId")
      .headers(setHeaders(username = "not_owner", activeCaseload = testAdjudication.agencyId))
      .exchange()
      .expectStatus().isForbidden

    webTestClient.get()
      .uri("/draft-adjudications/$draftId")
      .headers(setHeaders(username = username, activeCaseload = testAdjudication.agencyId))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `ALO edits submitted report offence and receives updated reported adjudication`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()

    val userHeaders = setHeaders(username = testAdjudication.createdByUserId, activeCaseload = testAdjudication.agencyId)
    val intTestBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = userHeaders,
    )

    intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val draftAdjudicationResponse = intTestData.recallCompletedDraftAdjudication(testAdjudication, headers = setHeaders(activeCaseload = testAdjudication.agencyId))

    webTestClient.put()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/applicable-rules")
      .headers(setHeaders(activeCaseload = IntegrationTestData.ADJUDICATION_1.agencyId))
      .bodyValue(
        mapOf(
          "isYouthOffenderRule" to true,
          "removeExistingOffences" to true,
        ),
      )
      .exchange()
      .expectStatus().isOk

    webTestClient.put()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/incident-role")
      .headers(setHeaders(activeCaseload = IntegrationTestData.ADJUDICATION_1.agencyId))
      .bodyValue(
        mapOf(
          "incidentRole" to IncidentRoleRequest("25b"),
          "removeExistingOffences" to true,
        ),
      )
      .exchange()
      .expectStatus().isOk

    testAdjudication.chargeNumber?.let {
      webTestClient.post()
        .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/alo-offence-details")
        .headers(
          setHeaders(
            username = "ITAG_ALO",
            roles = listOf("ROLE_ADJUDICATIONS_REVIEWER"),
            activeCaseload = IntegrationTestData.ADJUDICATION_1.agencyId,
          ),
        )
        .bodyValue(
          mapOf(
            "offenceDetails" to IntegrationTestData.ADJUDICATION_2.offence,
          ),
        )
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.chargeNumber").isEqualTo(it)
        .jsonPath("$.offenceDetails.offenceRule.paragraphNumber")
        .isEqualTo(IntegrationTestData.ADJUDICATION_2.offence.paragraphNumber)
        .jsonPath("$.offenceDetails.offenceRule.paragraphDescription")
        .isEqualTo(IntegrationTestData.ADJUDICATION_2.offence.paragraphDescription)
    }
  }

  private fun getReportedAdjudicationRequestStatus() =
    webTestClient.get()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus()

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
  }
}
