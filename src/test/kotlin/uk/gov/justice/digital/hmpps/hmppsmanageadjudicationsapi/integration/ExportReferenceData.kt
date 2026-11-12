package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.IncidentRoleRuleLookup
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodes
import java.io.File

/** Exports the reporting catalogue alongside the SchemaSpy report. */
class ExportReferenceData {
  @Test
  fun `exports reference data`() {
    export(File(System.getProperty("referenceDataOutput") ?: "reference-data.csv"))
  }

  internal fun export(output: File) {
    val catalogue = ObjectMapper().readTree(javaClass.getResourceAsStream("/reference-data/reporting-codes.json"))
    val columns = linkedMapOf(
      "punishment_type" to "punishment.type",
      "privilege_type" to "punishment.privilege_type",
      "not_completed_outcome" to "punishment.rehab_not_completed_outcome",
      "measurement" to "punishment_schedule.measurement",
      "reason_for_change" to "punishment_comments.reason_for_change",
      "reported_adjudication_status" to "reported_adjudications.status",
      "gender" to "reported_adjudications.gender",
      "oic_hearing_type" to "hearing.oic_hearing_type",
      "hearing_outcome_code" to "hearing_outcome.code",
      "hearing_outcome_plea" to "hearing_outcome.plea",
      "hearing_outcome_adjourn_reason" to "hearing_outcome.adjourn_reason",
      "outcome_code" to "outcome.code",
      "not_proceed_reason" to "outcome.not_proceed_reason",
      "quashed_reason" to "outcome.quashed_reason",
      "refer_gov_reason" to "outcome.refer_gov_reason",
      "damage_code" to "damages.code / reported_damages.code",
      "evidence_code" to "evidence.code / reported_evidence.code",
      "witness_code" to "witness.code / reported_witness.code",
      "characteristic" to "protected_characteristics.characteristic / draft_protected_characteristics.characteristic",
    )
    val rows = mutableListOf<Row>()
    columns.forEach { (key, column) ->
      catalogue[key].forEach { rows += Row(column, it["code"].asText(), it["description"].asText(), "constant_$key") }
    }
    mapOf(
      "reported_adjudication_status" to "reported_adjudications.status_before_migration",
      "gender" to "draft_adjudications.gender",
    ).forEach { (key, column) ->
      catalogue[key].forEach { rows += Row(column, it["code"].asText(), it["description"].asText(), "constant_$key") }
    }
    val offences = OffenceCodes.entries.filter { it != OffenceCodes.MIGRATED_OFFENCE }.flatMap { offence ->
      offence.uniqueOffenceCodes.map { (offence.name.substringBefore('_') to it) to offence }
    }.toMap()
    catalogue["offence"].forEach {
      val offence = requireNotNull(offences[it["rule_set"].asText() to it["offence_code"].asInt()])
      rows += Row(
        "offence.offence_code / reported_offence.offence_code",
        it["offence_code"].asText(),
        it["description"].asText(),
        "name=${offence.name}; nomisCode=${offence.nomisCode}; paragraph=${offence.paragraph}; " +
          "withOthersNomisCode=${offence.getNomisCodeWithOthers()}; " +
          "applicableVersions=${offence.applicableVersions.joinToString("|")}; " +
          "ruleSet=${it["rule_set"].asText()}; retired=${it["retired"].asBoolean()}",
      )
    }
    val migrated = OffenceCodes.MIGRATED_OFFENCE
    rows += Row(
      "offence.offence_code / reported_offence.offence_code",
      "0",
      "Migrated offence: use the NOMIS description stored on the record",
      "name=${migrated.name}; nomisCode=${migrated.nomisCode}; paragraph=${migrated.paragraph}; " +
        "withOthersNomisCode=${migrated.getNomisCodeWithOthers()}; " +
        "applicableVersions=${migrated.applicableVersions.joinToString("|")}; " +
        "No constant_offence row; use reported_offence.nomis_offence_code and nomis_offence_description",
    )
    rows += Row(
      "reported_offence.offence_code",
      "0",
      "Migrated offence: use the NOMIS description stored on the record",
      "No constant_offence row; use reported_offence.nomis_offence_code and nomis_offence_description",
    )
    IncidentRoleRuleLookup.roleCodes().forEach { code ->
      val adult = requireNotNull(IncidentRoleRuleLookup.getOffenceRuleDetails(code, false))
      rows += Row("reported_adjudications.incident_role_code", code, adult.paragraphDescription.trim().removeSuffix(":"), "")
    }
    listOf(false, true).forEach { isYouth ->
      IncidentRoleRuleLookup.roleCodes().forEach { code ->
        val details = requireNotNull(IncidentRoleRuleLookup.getOffenceRuleDetails(code, isYouth))
        rows += Row(
          "reported_adjudications.incident_role_code / incident_role.role_code",
          code,
          details.paragraphDescription.trim(),
          "ruleSet=${if (isYouth) "YOI" else "ADULT"}; paragraph=${details.paragraphNumber}; " +
            "null means committed, not necessarily alone; a missing draft incident role is unresolved",
        )
      }
    }

    output.bufferedWriter().use { writer ->
      writer.write("column_ref,code,description,notes\n")
      rows.forEach { writer.write("${it.toCsv()}\n") }
    }
    println("Wrote ${rows.size} reference data rows to ${output.absolutePath}")
  }

  private data class Row(val columnRef: String, val code: String, val description: String, val notes: String) {
    fun toCsv() = listOf(columnRef, code, description, notes).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
  }
}
