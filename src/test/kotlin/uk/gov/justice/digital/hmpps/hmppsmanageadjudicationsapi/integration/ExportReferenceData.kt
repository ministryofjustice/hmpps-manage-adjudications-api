package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Characteristic
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Measurement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotCompletedOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.QuashedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReasonForChange
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReferGovReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodes
import java.io.File

/**
 * Writes reference-data.csv, the companion to the SchemaSpy report and data-dictionary.csv.
 *
 * Every code in this schema is a JPA string enum or an integer resolved in Kotlin - there are no
 * reference tables in the database - so a schema report alone leaves an analyst unable to decode
 * columns such as reported_offence.offence_code or punishment.type. This exports those lookups.
 *
 * Excluded from normal test runs; run with `./gradlew -Pinit-db=true test` (see build.gradle.kts).
 */
class ExportReferenceData {

  @Test
  fun `exports reference data`() {
    val rows = mutableListOf<Row>()

    rows += enumRows("reported_adjudications.status", ReportedAdjudicationStatus.entries)
    rows += enumRows("reported_adjudications.gender", Gender.entries)
    rows += Row("reported_adjudications.incident_role_code", "25a", "Attempts to commit any of the foregoing offences")
    rows += Row("reported_adjudications.incident_role_code", "25b", "Incites another prisoner to commit any of the foregoing offences")
    rows += Row("reported_adjudications.incident_role_code", "25c", "Assists another prisoner to commit, or to attempt to commit, any of the foregoing offences")

    rows += enumRows("outcome.code", OutcomeCode.entries)
    rows += enumRows("outcome.not_proceed_reason", NotProceedReason.entries)
    rows += enumRows("outcome.quashed_reason", QuashedReason.entries)
    rows += enumRows("outcome.refer_gov_reason", ReferGovReason.entries)

    rows += enumRows("hearing.oic_hearing_type", OicHearingType.entries)
    rows += enumRows("hearing_outcome.code", HearingOutcomeCode.entries)
    rows += enumRows("hearing_outcome.adjourn_reason", HearingOutcomeAdjournReason.entries)
    rows += enumRows("hearing_outcome.plea", HearingOutcomePlea.entries)

    rows += enumRows("punishment.type", PunishmentType.entries)
    rows += enumRows("punishment.privilege_type", PrivilegeType.entries)
    rows += enumRows("punishment.rehab_not_completed_outcome", NotCompletedOutcome.entries)
    rows += enumRows("punishment_schedule.measurement", Measurement.entries)
    rows += enumRows("punishment_comments.reason_for_change", ReasonForChange.entries)

    rows += enumRows("damages.code / reported_damages.code", DamageCode.entries)
    rows += enumRows("evidence.code / reported_evidence.code", EvidenceCode.entries)
    rows += enumRows("witness.code / reported_witness.code", WitnessCode.entries)
    rows += enumRows(
      "protected_characteristics.characteristic / draft_protected_characteristics.characteristic",
      Characteristic.entries,
    )

    rows += offenceCodeRows()

    val output = File(System.getProperty("referenceDataOutput") ?: "reference-data.csv")
    output.bufferedWriter().use { writer ->
      writer.write("column_ref,code,description,notes\n")
      rows.forEach { writer.write("${it.toCsv()}\n") }
    }
    println("Wrote ${rows.size} reference data rows to ${output.absolutePath}")
  }

  /**
   * offence.offence_code and reported_offence.offence_code hold the integers in
   * OffenceCodes.uniqueOffenceCodes, so one enum entry expands to several stored values.
   */
  private fun offenceCodeRows(): List<Row> = OffenceCodes.entries.flatMap { offence ->
    offence.uniqueOffenceCodes.map { code ->
      Row(
        columnRef = "offence.offence_code / reported_offence.offence_code",
        code = code.toString(),
        description = offence.paragraphDescription.getParagraphDescription(Gender.MALE),
        notes = "name=${offence.name}; nomisCode=${offence.nomisCode}; paragraph=${offence.paragraph}; " +
          "withOthersNomisCode=${offence.getNomisCodeWithOthers()}; " +
          "applicableVersions=${offence.applicableVersions.joinToString("|")}",
      )
    }
  }

  private fun <T : Enum<T>> enumRows(columnRef: String, values: List<T>) = values.map { Row(columnRef, it.name, "") }

  private data class Row(
    val columnRef: String,
    val code: String,
    val description: String,
    val notes: String = "",
  ) {
    fun toCsv() = listOf(columnRef, code, description, notes).joinToString(",") { escape(it) }

    private fun escape(value: String) = "\"${value.replace("\"", "\"\"")}\""
  }
}
