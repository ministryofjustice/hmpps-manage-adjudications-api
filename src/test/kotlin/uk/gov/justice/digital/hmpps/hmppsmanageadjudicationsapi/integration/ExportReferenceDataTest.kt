package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodes
import java.nio.file.Path

class ExportReferenceDataTest {
  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `export retains existing identifiers and offence metadata while adding reporting details`() {
    val output = tempDir.resolve("reference-data.csv").toFile()
    ExportReferenceData().export(output)
    val lines = output.readLines()

    assertThat(lines.first()).isEqualTo("column_ref,code,description,notes")
    ReportedAdjudicationStatus.entries.forEach { status ->
      assertThat(lines).anyMatch { it.startsWith("\"reported_adjudications.status\",\"${status.name}\",") }
      assertThat(lines).anyMatch { it.startsWith("\"reported_adjudications.status_before_migration\",\"${status.name}\",") }
    }
    Gender.entries.forEach { gender ->
      assertThat(lines).anyMatch { it.startsWith("\"reported_adjudications.gender\",\"${gender.name}\",") }
      assertThat(lines).anyMatch { it.startsWith("\"draft_adjudications.gender\",\"${gender.name}\",") }
    }
    listOf("25a", "25b", "25c").forEach { code ->
      assertThat(lines.count { it.startsWith("\"reported_adjudications.incident_role_code\",\"$code\",") }).isEqualTo(1)
      assertThat(lines.count { it.startsWith("\"reported_adjudications.incident_role_code / incident_role.role_code\",\"$code\",") }).isEqualTo(2)
    }
    OffenceCodes.entries.forEach { offence ->
      offence.uniqueOffenceCodes.forEach { code ->
        val prefix = "\"offence.offence_code / reported_offence.offence_code\",\"$code\","
        val metadata = "name=${offence.name}; nomisCode=${offence.nomisCode}; paragraph=${offence.paragraph}; " +
          "withOthersNomisCode=${offence.getNomisCodeWithOthers()}; " +
          "applicableVersions=${offence.applicableVersions.joinToString("|")}"
        assertThat(lines).anyMatch { it.startsWith(prefix) && it.contains(metadata) }
      }
    }
    assertThat(lines).anyMatch { it.contains("null means committed, not necessarily alone; a missing draft incident role is unresolved") }
    assertThat(lines).noneMatch { it.contains("null means acting alone") }
  }
}
