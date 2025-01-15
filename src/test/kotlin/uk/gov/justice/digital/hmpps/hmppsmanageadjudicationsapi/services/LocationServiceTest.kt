package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

class LocationServiceTest {

  private val webClient: WebClient = mock()
  private val locationService = LocationService(webClient)

  @Nested
  inner class GetNomisLocationDetailTests {

    @Test
    fun `Given a valid locationId, When getNomisLocationDetail is called, Then it returns the LocationResponse`() {
      // Given
      val locationId = "1234"
      val expectedResponse = LocationResponse(
        dpsLocationId = "A1234BC",
        nomisLocationId = 1234,
        label = "My Label",
        mappingType = "TYPE",
        whenCreated = LocalDateTime.now()
      )

      // Mock the fluent WebClient chain:
      val requestHeadersUriSpec = mock<WebClient.RequestHeadersUriSpec<*>>()
      val requestHeadersSpec = mock<WebClient.RequestHeadersSpec<*>>()
      val responseSpec = mock<WebClient.ResponseSpec>()

      whenever(webClient.get()).thenReturn(requestHeadersUriSpec)
      whenever(requestHeadersUriSpec.uri("/mapping/locations/nomis/{locationId}", locationId))
        .thenReturn(requestHeadersSpec)
      whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      whenever(responseSpec.bodyToMono(LocationResponse::class.java))
        .thenReturn(Mono.just(expectedResponse))

      // When
      val result = locationService.getNomisLocationDetail(locationId)

      // Then
      assertNotNull(result)
      assertEquals(expectedResponse.dpsLocationId, result?.dpsLocationId)
      assertEquals(expectedResponse.nomisLocationId, result?.nomisLocationId)
      assertEquals(expectedResponse.label, result?.label)
      assertEquals(expectedResponse.mappingType, result?.mappingType)
    }

    @Test
    fun `Given the location is not found, When WebClient throws 404, Then getNomisLocationDetail returns null`() {
      // Given
      val locationId = "NOT_FOUND"
      val errorJson = """{
 "status": 404,
 "errorCode": "LOCATION_NOT_FOUND",
 "userMessage": "Location not found",
 "developerMessage": "No location found for id",
 "moreInfo": "N/A"
 }""".trimIndent()

      val notFoundException = WebClientResponseException.create(
        HttpStatus.NOT_FOUND.value(),
        "Not Found",
        null,
        errorJson.toByteArray(StandardCharsets.UTF_8),
        null
      )

      // Mock the fluent WebClient chain:
      val requestHeadersUriSpec = mock<WebClient.RequestHeadersUriSpec<*>>()
      val requestHeadersSpec = mock<WebClient.RequestHeadersSpec<*>>()
      val responseSpec = mock<WebClient.ResponseSpec>()

      whenever(webClient.get()).thenReturn(requestHeadersUriSpec)
      whenever(requestHeadersUriSpec.uri("/mapping/locations/nomis/{locationId}", locationId))
        .thenReturn(requestHeadersSpec)

      whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)

      // Return a Mono.error() to simulate a 404 from the downstream service
      whenever(responseSpec.bodyToMono(LocationResponse::class.java))
        .thenReturn(Mono.error(notFoundException))

      // When
      val result = locationService.getNomisLocationDetail(locationId)

      // Then
      assertNull(result, "Expected null when location is not found (404)")
    }

    @Test
    fun `Given an unexpected exception, When getNomisLocationDetail is called, Then it throws a RuntimeException`() {
      // Given
      val locationId = "ANY_ID"

      val requestHeadersUriSpec = mock<WebClient.RequestHeadersUriSpec<*>>()
      val requestHeadersSpec = mock<WebClient.RequestHeadersSpec<*>>()
      val responseSpec = mock<WebClient.ResponseSpec>()

      whenever(webClient.get()).thenReturn(requestHeadersUriSpec)

      whenever(requestHeadersUriSpec.uri("/mapping/locations/nomis/{locationId}", locationId))
        .thenReturn(requestHeadersSpec)

      whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)

      // Force a generic exception
      whenever(responseSpec.bodyToMono(LocationResponse::class.java))
        .thenThrow(RuntimeException("Something went wrong"))

      // When / Then
      val ex = assertThrows<RuntimeException> {
        locationService.getNomisLocationDetail(locationId)
      }
      assertTrue(
        ex.message!!.contains("Failed to fetch location details for ID: $locationId"),
        "Should wrap unexpected exception in a RuntimeException"
      )
    }
  }
}