package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import java.time.LocalDateTime

data class AdjudicationIntTestDataSet(
        val adjudicationNumber: Long,
        val prisonerNumber: String,
        val agencyId: String,
        val locationId: Long,
        val dateTimeOfIncident: LocalDateTime,
        val dateTimeOfIncidentISOString: String,
        val statement: String
)