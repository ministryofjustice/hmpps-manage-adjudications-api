package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class DraftAdjudicationTest {
  @Test
  fun `throws an illegalStateException when trying to add an incident when one already exists`() {
    val draftAdjudication = DraftAdjudication(id = 1, prisonerNumber = "A12345")
    draftAdjudication.addIncidentDetails(IncidentDetails(1, LocalDateTime.now()))

    Assertions.assertThatThrownBy {
      draftAdjudication.addIncidentDetails(IncidentDetails(1, LocalDateTime.now()))
    }.isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining("DraftAdjudication already contains the incident details")
  }
}
