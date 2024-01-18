package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType

@Configuration
class OpenApiConfiguration(
  buildProperties: BuildProperties,
) {
  private val version: String = buildProperties.version

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("/").description("Current url"),
      ),
    )
    .tags(
      listOf(
        Tag().name("01. Adjudication Summary"),
        Tag().name("10. Draft Adjudication Management").description("Draft Adjudications Management"),
        Tag().name("11. Draft Adjudication Workflow").description("Draft Adjudications Workflow"),
        Tag().name("12. Draft Damages").description("Draft Adjudications - Damages"),
        Tag().name("13. Draft Evidence").description("Draft Adjudications - Evidence"),
        Tag().name("14. Draft Witnesses").description("Draft Adjudications - Witnesses"),
        Tag().name("15. Draft Offence").description("Draft Adjudications - Offence"),

        Tag().name("20. Adjudication Management").description("Adjudication Management"),
        Tag().name("21. Adjudication Workflow").description("Adjudication Workflow"),
        Tag().name("22. Damages").description("Damages - Adjudications"),
        Tag().name("23. Evidence").description("Evidence - Adjudications"),
        Tag().name("24. Hearings").description("Hearings -  Adjudications"),
        Tag().name("25. Witnesses").description("Witnesses - Adjudications"),

        Tag().name("30. Punishments").description("Punishments - Adjudications"),
        Tag().name("31. Outcomes").description("Outcomes - Adjudications"),

        Tag().name("40. Reports").description("Reports - Adjudications"),

        Tag().name("50. Schedule Tasks").description("Scheduled Tasks"),

      ),
    )
    .info(
      Info().title("HMPPS Manage Adjudications API")
        .version(version)
        .license(License().name("MIT").url("https://opensource.org/license/mit-0"))
        .description("API for managing adjudications for prisoners")
        .contact(
          Contact()
            .name("Adjudications Support Team")
            .email("feedback@digital.justice.gov.uk")
            .url("https://github.com/ministryofjustice/hmpps-manage-adjudications-api"),
        ),
    )
    .components(
      Components().addSecuritySchemes(
        "bearer-jwt",
        SecurityScheme()
          .type(SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT")
          .`in`(SecurityScheme.In.HEADER)
          .name("Authorization"),
      ),
    )
    .addSecurityItem(SecurityRequirement().addList("bearer-jwt"))

  @Bean
  fun openAPICustomiser(): OpenApiCustomizer = OpenApiCustomizer {
    it.paths.forEach { (_, path: PathItem) ->
      path.addParametersItem(Parameter().`in`(ParameterIn.HEADER.toString()).name("Active-Caseload").description("Current Caseload for request, to be used when making calls to a specific report").schema(StringSchema()).example("MDI").required(false))

      path.readOperations().forEach { operation ->
        operation.responses.default = createErrorApiResponse("Unexpected error")
        operation.responses.addApiResponse("401", createErrorApiResponse("Unauthorized"))
        operation.responses.addApiResponse("403", createErrorApiResponse("Forbidden"))
        operation.responses.addApiResponse("406", createErrorApiResponse("Not able to process the request because the header “Accept” does not match with any of the content types this endpoint can handle"))
        operation.responses.addApiResponse("429", createErrorApiResponse("Too many requests"))
      }
    }
    it.components.schemas.forEach { (_, schema: Schema<*>) ->
      schema.additionalProperties = false
      val properties = schema.properties ?: mutableMapOf()
      for (propertyName in properties.keys) {
        val propertySchema = properties[propertyName]!!
        if (propertySchema is DateTimeSchema) {
          properties.replace(
            propertyName,
            StringSchema()
              .example("2021-07-05T10:35:17")
              .pattern("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
              .description(propertySchema.description)
              .required(propertySchema.required),
          )
        }
      }
    }
  }

  private fun createErrorApiResponse(message: String): ApiResponse {
    val errorResponseSchema = Schema<Any>()
    errorResponseSchema.name = "ErrorResponse"
    errorResponseSchema.`$ref` = "#/components/schemas/ErrorResponse"
    return ApiResponse()
      .description(message)
      .content(
        Content().addMediaType(MediaType.APPLICATION_JSON_VALUE, io.swagger.v3.oas.models.media.MediaType().schema(errorResponseSchema)),
      )
  }
}
