package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
import java.util.*

class LocationServiceTest {
  // Mock the WebClient(s) your service expects:
  private val locationDetailWebClient: WebClient = mock()

  // Inject them into the service
  private val locationService = LocationService(locationDetailWebClient)

  @Nested
  inner class GetLocationDetailTests {

    @Test
    fun `Given a valid DPS locationId, When getLocationDetail is called, Then it returns the LocationDetailResponse`() {
      // Given
      val locationUuid = UUID.fromString("0194ac90-2def-7c63-9f46-b3ccc911fdff")
      val expectedResponse = LocationDetailResponse(
        id = "A-DPS-LOC",
        prisonId = "MDI",
        localName = "Moorland Location",
        pathHierarchy = "MDI > Moorland location path",
        key = "some-key",
      )

      // Mock the fluent WebClient chain for locationDetailWebClient:
      val requestHeadersUriSpec = mock<WebClient.RequestHeadersUriSpec<*>>()
      val requestHeadersSpec = mock<WebClient.RequestHeadersSpec<*>>()
      val responseSpec = mock<WebClient.ResponseSpec>()

      whenever(locationDetailWebClient.get()).thenReturn(requestHeadersUriSpec)
      whenever(requestHeadersUriSpec.uri("/locations/{locationUuid}?formatLocalName=true", locationUuid))
        .thenReturn(requestHeadersSpec)
      whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      whenever(responseSpec.bodyToMono(LocationDetailResponse::class.java))
        .thenReturn(Mono.just(expectedResponse))

      // When
      val result = locationService.getLocationDetail(locationUuid)

      // Then
      assertNotNull(result)
      assertEquals(expectedResponse.id, result?.id)
      assertEquals(expectedResponse.prisonId, result?.prisonId)
      assertEquals(expectedResponse.localName, result?.localName)
      assertEquals(expectedResponse.pathHierarchy, result?.pathHierarchy)
      assertEquals(expectedResponse.key, result?.key)
    }

    @Test
    fun `Given the location is not found, When WebClient throws 404, Then getLocationDetail returns null`() {
      // Given
      val locationUuid = UUID.fromString("0194ac90-2def-7c63-9f46-b3ccc911fdff")
      val errorJson = """
        {
          "status": 404,
          "errorCode": "LOCATION_NOT_FOUND",
          "userMessage": "Location not found",
          "developerMessage": "No location found for id",
          "moreInfo": "N/A"
        }
      """.trimIndent()

      val notFoundException = WebClientResponseException.create(
        HttpStatus.NOT_FOUND.value(),
        "Not Found",
        org.springframework.http.HttpHeaders(),
        errorJson.toByteArray(StandardCharsets.UTF_8),
        null,
      )

      // Mock the fluent WebClient chain:
      val requestHeadersUriSpec = mock<WebClient.RequestHeadersUriSpec<*>>()
      val requestHeadersSpec = mock<WebClient.RequestHeadersSpec<*>>()
      val responseSpec = mock<WebClient.ResponseSpec>()

      whenever(locationDetailWebClient.get()).thenReturn(requestHeadersUriSpec)
      whenever(requestHeadersUriSpec.uri("/locations/{locationUuid}?formatLocalName=true", locationUuid))
        .thenReturn(requestHeadersSpec)
      whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      whenever(responseSpec.bodyToMono(LocationDetailResponse::class.java))
        .thenReturn(Mono.error(notFoundException))

      // When
      val result = locationService.getLocationDetail(locationUuid)

      // Then
      assertNull(result, "Expected null when location is not found (404)")
    }

    @Test
    fun `Given an unexpected exception, When getLocationDetail is called, Then it throws a RuntimeException`() {
      // Given
      val locationUuid = UUID.fromString("0194ac90-2def-7c63-9f46-b3ccc911fdff")

      val requestHeadersUriSpec = mock<WebClient.RequestHeadersUriSpec<*>>()
      val requestHeadersSpec = mock<WebClient.RequestHeadersSpec<*>>()
      val responseSpec = mock<WebClient.ResponseSpec>()

      whenever(locationDetailWebClient.get()).thenReturn(requestHeadersUriSpec)
      whenever(requestHeadersUriSpec.uri("/locations/{locationUuid}?formatLocalName=true", locationUuid))
        .thenReturn(requestHeadersSpec)
      whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      // Force a generic exception
      whenever(responseSpec.bodyToMono(LocationDetailResponse::class.java))
        .thenThrow(RuntimeException("Something went wrong"))

      // When / Then
      val ex = assertThrows<RuntimeException> {
        locationService.getLocationDetail(locationUuid)
      }
      assertTrue(
        ex.message!!.contains("Failed to fetch location details for ID: $locationUuid"),
        "Should wrap unexpected exception in a RuntimeException",
      )
    }
  }
}
