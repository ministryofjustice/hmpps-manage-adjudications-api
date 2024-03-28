package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft

import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookup
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

interface TestsToImplement {
  fun `throws an entity not found if the draft adjudication for the supplied id does not exists`()
}

abstract class DraftAdjudicationTestBase : TestsToImplement {
  internal val draftAdjudicationRepository: DraftAdjudicationRepository = mock()
  internal val offenceCodeLookup: OffenceCodeLookup = OffenceCodeLookup()
  internal val authenticationFacade: AuthenticationFacade = mock()

  @BeforeEach
  fun beforeEach() {
    whenever(authenticationFacade.activeCaseload).thenReturn("MDI")
  }

  companion object {
    internal val clock: Clock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault())

    val now = LocalDateTime.now()

    val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
    val DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE = LocalDateTime.of(2010, 10, 14, 10, 0)

    val INCIDENT_ROLE_CODE = "25a"
    val INCIDENT_ROLE_PARAGRAPH_NUMBER = "25(a)"
    val INCIDENT_ROLE_PARAGRAPH_DESCRIPTION = "Attempts to commit any of the foregoing offences:"
    val INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER = "B23456"
    val INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME = "Associated Prisoner"
  }
}
