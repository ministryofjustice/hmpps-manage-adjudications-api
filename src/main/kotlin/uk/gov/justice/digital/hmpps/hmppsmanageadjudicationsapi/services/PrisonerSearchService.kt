package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class PrisonerResponse(
  val firstName: String,
  val lastName: String,
)

@Service
class PrisonerSearchService(
  @Qualifier("prisonerSearchWebClient") private val webClient: WebClient,
) {

  private val logger: Logger = LoggerFactory.getLogger(this::class.java)
  private val objectMapper = jacksonObjectMapper()

  /**
   * Fetches Prisoner details by prisonerNumber.
   * @param prisonerNumber The Prisoner ID.
   * @return The prisoner details or `null` if not found.
   */
  fun getPrisonerDetail(prisonerNumber: String): PrisonerResponse? {
    logger.info("Fetching prisoner details for ID: $prisonerNumber")
    return try {
      webClient.get()
        .uri("/api/offenders/${prisonerNumber}", prisonerNumber)
        .retrieve()
        .bodyToMono(PrisonerResponse::class.java)
        .block()
    } catch (ex: WebClientResponseException) {
      val errorResponse = handleError(ex)
      logger.error("Error fetching prisoner details for ID: $prisonerNumber - $errorResponse")
      null
    } catch (ex: Exception) {
      logger.error("Unexpected error while fetching prisoner details for ID: $prisonerNumber", ex)
      throw RuntimeException("Failed to fetch prisoner details for ID: $prisonerNumber", ex)
    }
  }

  /**
   * Handles errors returned by the API.
   * @param ex The exception containing the API response.
   * @return The parsed error response.
   */
  private fun handleError(ex: WebClientResponseException): ErrorResponse {
    return try {
      logger.warn("Handling error response: ${ex.statusCode}")
      ex.responseBodyAsString?.let { body ->
        objectMapper.readValue(body, ErrorResponse::class.java)
      } ?: throw IllegalArgumentException("Empty error response body")
    } catch (parseException: Exception) {
      logger.error("Failed to parse error response body for status: ${ex.statusCode}", parseException)
      ErrorResponse(
        status = ex.statusCode.value(),
        errorCode = "UNKNOWN",
        userMessage = "Unable to parse error response from server.",
        developerMessage = parseException.message ?: "Error parsing response body.",
        moreInfo = "No additional information available.",
      )
    }
  }
}
