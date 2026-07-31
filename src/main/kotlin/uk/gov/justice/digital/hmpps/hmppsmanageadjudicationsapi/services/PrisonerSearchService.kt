package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

@JsonIgnoreProperties(ignoreUnknown = true)
data class PrisonerResponse(
  val firstName: String,
  val lastName: String,
  val prisonId: String = "",
)

@Service
class PrisonerSearchService(
  @Qualifier("prisonerSearchWebClient") private val webClient: WebClient,
) {

  private val logger: Logger = LoggerFactory.getLogger(this::class.java)

  /**
   * Fetches Prisoner details by prisonerNumber.
   * @param prisonerNumber The Prisoner ID.
   * @return The prisoner details or `null` if not found.
   */
  fun getPrisonerDetail(prisonerNumber: String): PrisonerResponse? {
    logger.info("Fetching prisoner details for ID: $prisonerNumber")
    return try {
      webClient.get()
        .uri("/prisoner/{prisonerNumber}", prisonerNumber)
        .retrieve()
        .bodyToMono(PrisonerResponse::class.java)
        .block()
    } catch (ex: WebClientResponseException) {
      if (ex.statusCode == HttpStatus.NOT_FOUND) {
        logger.warn("Prisoner details not found for ID: {}", prisonerNumber)
        null
      } else {
        logger.error(
          "Error fetching prisoner details for ID: {} - downstream returned {}",
          prisonerNumber,
          ex.statusCode,
        )
        throw ex
      }
    } catch (ex: Exception) {
      logger.error("Unexpected error while fetching prisoner details for ID: $prisonerNumber", ex)
      throw RuntimeException("Failed to fetch prisoner details for ID: $prisonerNumber", ex)
    }
  }
}
