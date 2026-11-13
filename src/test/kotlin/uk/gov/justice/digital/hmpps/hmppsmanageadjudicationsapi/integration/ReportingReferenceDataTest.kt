package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.IncidentRoleRuleLookup
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodes

/** Verifies the reporting catalogue against both the Kotlin codes and a Flyway-migrated database. */
class ReportingReferenceDataTest : IntegrationTestBase() {
  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private val catalogue: JsonNode by lazy {
    objectMapper.readTree(javaClass.getResourceAsStream("/reference-data/reporting-codes.json"))
  }

  @Test
  fun `every persisted enum matches its reporting table in code description and order`() {
    val enums = linkedMapOf(
      "punishment_type" to PunishmentType.entries.map { it.name },
      "privilege_type" to PrivilegeType.entries.map { it.name },
      "not_completed_outcome" to NotCompletedOutcome.entries.map { it.name },
      "measurement" to Measurement.entries.map { it.name },
      "reason_for_change" to ReasonForChange.entries.map { it.name },
      "reported_adjudication_status" to ReportedAdjudicationStatus.entries.map { it.name },
      "gender" to Gender.entries.map { it.name },
      "oic_hearing_type" to OicHearingType.entries.map { it.name },
      "hearing_outcome_code" to HearingOutcomeCode.entries.map { it.name },
      "hearing_outcome_plea" to HearingOutcomePlea.entries.map { it.name },
      "hearing_outcome_adjourn_reason" to HearingOutcomeAdjournReason.entries.map { it.name },
      "outcome_code" to OutcomeCode.entries.map { it.name },
      "not_proceed_reason" to NotProceedReason.entries.map { it.name },
      "quashed_reason" to QuashedReason.entries.map { it.name },
      "refer_gov_reason" to ReferGovReason.entries.map { it.name },
      "damage_code" to DamageCode.entries.map { it.name },
      "evidence_code" to EvidenceCode.entries.map { it.name },
      "witness_code" to WitnessCode.entries.map { it.name },
      "characteristic" to Characteristic.entries.map { it.name },
    )
    assertThat(catalogue.fieldNames().asSequence().toSet()).isEqualTo(enums.keys + "offence")
    enums.forEach { (name, codes) ->
      val rows = catalogue[name]
      assertThat(rows.map { it["code"].asText() }).describedAs("$name codes and order").isEqualTo(codes)
      assertThat(rows.map { it["description"].asText() }).allSatisfy { assertThat(it).isNotBlank() }
      val expected = rows.mapIndexed { index, row ->
        mapOf("code" to row["code"].asText(), "description" to row["description"].asText(), "sequence" to index)
      }
      val actual = jdbcTemplate.queryForList("SELECT code, description, sequence FROM constant_$name ORDER BY sequence")
      assertThat(actual).describedAs("constant_$name must match the reporting catalogue").isEqualTo(expected)
    }
  }

  @Test
  fun `offence mappings match rule set source and reporting catalogue`() {
    val rows = catalogue["offence"]
    val expected = OffenceCodes.entries.filter { it != OffenceCodes.MIGRATED_OFFENCE }.flatMap { offence ->
      offence.uniqueOffenceCodes.map { code ->
        mapOf(
          "offence_code" to code,
          "rule_set" to offence.name.substringBefore('_'),
          "nomis_code" to offence.nomisCode,
          "nomis_code_with_others" to offence.getNomisCodeWithOthers(),
          "paragraph" to offence.paragraph,
          "description" to neutralDescription(offence.paragraphDescription.description),
          "retired" to !offence.applicableVersions.contains(2),
        )
      }
    }.sortedWith(compareBy({ it["rule_set"] as String }, { it["offence_code"] as Int }))
    val fromCatalogue = rows.map { row ->
      mapOf(
        "offence_code" to row["offence_code"].asInt(),
        "rule_set" to row["rule_set"].asText(),
        "nomis_code" to row["nomis_code"].asText(),
        "nomis_code_with_others" to row["nomis_code_with_others"].asText(),
        "paragraph" to row["paragraph"].asText(),
        "description" to row["description"].asText(),
        "retired" to row["retired"].asBoolean(),
      )
    }.sortedWith(compareBy({ it["rule_set"] as String }, { it["offence_code"] as Int }))
    val actual = jdbcTemplate.queryForList(
      "SELECT offence_code, rule_set, nomis_code, nomis_code_with_others, paragraph, description, retired FROM constant_offence ORDER BY rule_set, offence_code",
    )
    assertThat(fromCatalogue).isEqualTo(expected)
    assertThat(actual).isEqualTo(expected)
    assertThat(expected.map { it["rule_set"] to it["offence_code"] }).doesNotHaveDuplicates()
    assertThat(actual).noneMatch { it["offence_code"] == 0 }
    assertThat(actual.single { it["offence_code"] == 1001 && it["rule_set"] == "ADULT" }["retired"]).isEqualTo(true)
    assertThat(actual.single { it["offence_code"] == 100124 && it["rule_set"] == "ADULT" }["retired"]).isEqualTo(false)
    assertThat(actual.map { it["description"] as String }).allSatisfy {
      assertThat(it).isNotBlank().doesNotContain("<", ">")
    }
  }

  @Test
  fun `reporting join preserves adult youth unresolved and migrated records`() {
    val rows = jdbcTemplate.queryForList(
      """
      SELECT sample.label, reference.nomis_code,
        CASE WHEN sample.offence_code = 0 THEN sample.migrated_description ELSE reference.description END AS description
      FROM (VALUES
        ('adult', 1002, FALSE, CAST(NULL AS text)),
        ('youth', 1002, TRUE, CAST(NULL AS text)),
        ('draft', 1002, CAST(NULL AS boolean), CAST(NULL AS text)),
        ('migrated', 0, FALSE, 'Original NOMIS description'),
        ('unknown', 999999, FALSE, CAST(NULL AS text))
      ) AS sample(label, offence_code, is_youth_offender, migrated_description)
      LEFT JOIN constant_offence reference
        ON reference.offence_code = sample.offence_code
        AND reference.rule_set = CASE sample.is_youth_offender WHEN TRUE THEN 'YOI' WHEN FALSE THEN 'ADULT' END
      ORDER BY sample.label
      """.trimIndent(),
    )
    assertThat(rows).hasSize(5)
    assertThat(rows.single { it["label"] == "adult" }["nomis_code"]).isEqualTo("51:1B")
    assertThat(rows.single { it["label"] == "youth" }["nomis_code"]).isEqualTo("55:1A")
    assertThat(rows.single { it["label"] == "draft" }["description"]).isNull()
    assertThat(rows.single { it["label"] == "unknown" }["description"]).isNull()
    assertThat(rows.single { it["label"] == "migrated" }["description"]).isEqualTo("Original NOMIS description")
  }

  @Test
  fun `incident roles match both rule sets and the lookup service`() {
    val expected = listOf(false, true).flatMap { isYouth ->
      IncidentRoleRuleLookup.roleCodes().map { code ->
        val details = requireNotNull(IncidentRoleRuleLookup.getOffenceRuleDetails(code, isYouth))
        mapOf(
          "code" to code,
          "rule_set" to if (isYouth) "YOI" else "ADULT",
          "paragraph" to details.paragraphNumber,
          "description" to details.paragraphDescription.trim(),
        )
      }
    }.sortedWith(compareBy({ it["rule_set"] }, { it["code"] }))
    val actual = jdbcTemplate.queryForList("SELECT code, rule_set, paragraph, description FROM constant_incident_role ORDER BY rule_set, code")
    assertThat(actual).isEqualTo(expected)
    assertThat(expected).allSatisfy {
      assertThat(it["paragraph"]).isNotBlank()
      assertThat(it["description"]).isNotBlank()
    }
    assertThat(IncidentRoleRuleLookup.getOffenceRuleDetails(null, false)).isNull()
    assertThat(IncidentRoleRuleLookup.getOffenceRuleDetails(null, true)).isNull()
  }

  private fun neutralDescription(value: String): String = value
    .replace("<OBJ>", "the person")
    .replace("<SUB>", "the person")
    .replace("<POS>", "the person's")
    .replace("<REF>", "themselves")
    .replace("<br>", " ")
    .replace("his duty", "their duty")
    .replace("his work", "their work")
    .replace("his will", "the person's will")
    .replace(Regex("\\s+"), " ")
    .trim()
}
