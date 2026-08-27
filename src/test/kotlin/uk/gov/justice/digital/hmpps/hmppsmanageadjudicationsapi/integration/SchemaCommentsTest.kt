package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Guards the data dictionary published to GitHub Pages (see db/migration/V130__schema_comments.sql
 * and V132__schema_comments_sensitivity.sql).
 *
 * Descriptions live in the database as COMMENT ON statements so SchemaSpy, the CSV export and any
 * Glue crawl share one source of truth. Nothing else would notice a new column arriving undocumented,
 * and in this schema an undocumented column is quite likely to be criminal offence data.
 *
 * Extends [IntegrationTestBase] because it is the only @SpringBootTest in the repo, so it is what
 * gives this test a real Flyway-migrated Postgres. It also pins the test to the "test" profile, which
 * matters: the default datasource in application.yml is an H2 URL, and pg_description does not exist
 * there.
 */
class SchemaCommentsTest : IntegrationTestBase() {

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Test
  fun `every table has a description`() {
    val undocumented = jdbcTemplate.queryForList(
      """
      SELECT c.relname
      FROM pg_class c
      JOIN pg_namespace n ON n.oid = c.relnamespace
      WHERE n.nspname = 'public'
        AND c.relkind = 'r'
        AND c.relname <> 'flyway_schema_history'
        AND obj_description(c.oid) IS NULL
      ORDER BY c.relname
      """.trimIndent(),
      String::class.java,
    )

    assertThat(undocumented)
      .describedAs("tables with no COMMENT ON - add one in a new migration")
      .isEmpty()
  }

  @Test
  fun `every column has a description`() {
    assertThat(columnComments().filter { it.comment == null }.map { it.name })
      .describedAs("columns with no COMMENT ON - add one in a new migration")
      .isEmpty()
  }

  @Test
  fun `every column description carries a sensitivity classification`() {
    val misclassified = columnComments()
      .filter { it.comment != null && !SENSITIVITY.containsMatchIn(it.comment) }
      .map { it.name }

    assertThat(misclassified)
      .describedAs("column comments must end with one of $SENSITIVITY - see V132__schema_comments_sensitivity.sql")
      .isEmpty()
  }

  private data class ColumnComment(
    val name: String,
    val comment: String?,
  )

  private fun columnComments(): List<ColumnComment> = jdbcTemplate.query(
    """
    SELECT c.table_name || '.' || c.column_name        AS name,
           col_description(pc.oid, c.ordinal_position) AS comment
    FROM information_schema.columns c
    JOIN pg_class pc
      ON pc.relname = c.table_name
     AND pc.relnamespace = 'public'::regnamespace
     AND pc.relkind = 'r'
    WHERE c.table_schema = 'public'
      AND c.table_name <> 'flyway_schema_history'
    ORDER BY c.table_name, c.ordinal_position
    """.trimIndent(),
  ) { rs, _ -> ColumnComment(rs.getString("name"), rs.getString("comment")) }

  private companion object {
    val SENSITIVITY = Regex("""\[Sensitivity: (NONE|PERSONAL|STAFF|SPECIAL-CATEGORY|OFFICIAL-SENSITIVE)]$""")
  }
}
