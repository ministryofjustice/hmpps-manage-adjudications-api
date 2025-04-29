package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.UUID

data class LocationResponse(
  val dpsLocationId: String,
  val nomisLocationId: Int,
)

data class LocationDetailResponse(
  val id: String,
  val prisonId: String,
  val localName: String,
  val pathHierarchy: String,
  val key: String,
)

data class ErrorResponse(
  val status: Int,
  val errorCode: String,
  val userMessage: String,
  val developerMessage: String,
  val moreInfo: String,
)

@Service
class LocationService(
  @Qualifier("prisonLocationWebClient") private val prisonLocationWebClient: WebClient,
  @Qualifier("prisonLocationDetailWebClient") private val locationDetailWebClient: WebClient,
) {

  private val logger: Logger = LoggerFactory.getLogger(this::class.java)
  private val objectMapper = jacksonObjectMapper()

  /**
   * Fetches location details with dps location ID.
   * @param locationUuid The DPS location ID.
   * @return The location details or `null` if not found.
   */
  fun getLocationDetail(locationUuid: UUID): LocationDetailResponse? {
    logger.info("Fetching location details for ID: $locationUuid")
    return try {
      locationDetailWebClient.get()
        .uri("/locations/{locationUuid}?formatLocalName=true", locationUuid)
        .retrieve()
        .bodyToMono(LocationDetailResponse::class.java)
        .block()
    } catch (ex: WebClientResponseException) {
      val errorResponse = handleError(ex)
      logger.error("Error fetching location details for ID: $locationUuid - $errorResponse")
      null
    } catch (ex: Exception) {
      logger.error("Unexpected error while fetching location details for ID: $locationUuid", ex)
      throw RuntimeException("Failed to fetch location details for ID: $locationUuid", ex)
    }
  }

  /**
   * Handles errors returned by the API.
   * @param ex The exception containing the API response.
   * @return The parsed error response.
   */
  private fun handleError(ex: WebClientResponseException): ErrorResponse = try {
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
