package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.UUID

data class LocationDetailResponse(
  val id: String,
  val prisonId: String,
  val localName: String?,
  val pathHierarchy: String,
  val key: String,
)

@Service
class LocationService(
  @Qualifier("prisonLocationDetailWebClient") private val locationDetailWebClient: WebClient,
) {

  private val logger: Logger = LoggerFactory.getLogger(this::class.java)

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
      if (ex.statusCode == HttpStatus.NOT_FOUND) {
        logger.warn("Location details not found for ID: {}", locationUuid)
        null
      } else {
        logger.error(
          "Error fetching location details for ID: {} - downstream returned {}",
          locationUuid,
          ex.statusCode,
        )
        throw ex
      }
    } catch (ex: Exception) {
      logger.error("Unexpected error while fetching location details for ID: $locationUuid", ex)
      throw RuntimeException("Failed to fetch location details for ID: $locationUuid", ex)
    }
  }
}
