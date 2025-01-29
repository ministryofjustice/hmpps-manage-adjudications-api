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

class LocationServiceTest {
  // Mock the two WebClients your service expects:
  private val prisonLocationWebClient: WebClient = mock()
  private val locationDetailWebClient: WebClient = mock()

  // Inject them into the service
  private val locationService = LocationService(prisonLocationWebClient, locationDetailWebClient)

  @Nested
  inner class GetNomisLocationDetailTests {

    @Test
    fun `Given a valid locationId, When getNomisLocationDetail is called, Then it returns the LocationResponse`() {
      // Given
      val locationId = "1234"
      val expectedResponse = LocationResponse(
        dpsLocationId = "A1234BC",
        nomisLocationId = 1234,
      )

      // Mock the fluent WebClient chain:
      val requestHeadersUriSpec = mock<WebClient.RequestHeadersUriSpec<*>>()
      val requestHeadersSpec = mock<WebClient.RequestHeadersSpec<*>>()
      val responseSpec = mock<WebClient.ResponseSpec>()

      whenever(prisonLocationWebClient.get()).thenReturn(requestHeadersUriSpec)
      whenever(requestHeadersUriSpec.uri("/api/locations/nomis/{locationId}", locationId))
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
    }

    @Test
    fun `Given the location is not found, When WebClient throws 404, Then getNomisLocationDetail returns null`() {
      // Given
      val locationId = "NOT_FOUND"
      val errorJson = "{\n" +
        "  \"status\": 404,\n" +
        "  \"errorCode\": \"LOCATION_NOT_FOUND\",\n" +
        "  \"userMessage\": \"Location not found\",\n" +
        "  \"developerMessage\": \"No location found for id\",\n" +
        "  \"moreInfo\": \"N/A\"\n" +
        "}"

      val notFoundException = WebClientResponseException.create(
        HttpStatus.NOT_FOUND.value(),
        "Not Found",
        null,
        errorJson.toByteArray(StandardCharsets.UTF_8),
        null,
      )

      // Mock the fluent WebClient chain:
      val requestHeadersUriSpec = mock<WebClient.RequestHeadersUriSpec<*>>()
      val requestHeadersSpec = mock<WebClient.RequestHeadersSpec<*>>()
      val responseSpec = mock<WebClient.ResponseSpec>()

      whenever(prisonLocationWebClient.get()).thenReturn(requestHeadersUriSpec)
      whenever(requestHeadersUriSpec.uri("/api/locations/nomis/{locationId}", locationId))
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

      whenever(prisonLocationWebClient.get()).thenReturn(requestHeadersUriSpec)

      whenever(requestHeadersUriSpec.uri("/api/locations/nomis/{locationId}", locationId))
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
        "Should wrap unexpected exception in a RuntimeException",
      )
    }
  }

  @Nested
  inner class GetLocationDetailTests {

    @Test
    fun `Given a valid DPS locationId, When getLocationDetail is called, Then it returns the LocationDetailResponse`() {
      // Given
      val locationId = "A-DPS-LOC"
      val expectedResponse = LocationDetailResponse(
        id = "A-DPS-LOC",
        prisonId = "MDI",
        localName = "Moorland Location",
        pathHierarchy = "MDI > Moorland location path",
        key = "some-key"
      )

      // Mock the fluent WebClient chain for locationDetailWebClient:
      val requestHeadersUriSpec = mock<WebClient.RequestHeadersUriSpec<*>>()
      val requestHeadersSpec = mock<WebClient.RequestHeadersSpec<*>>()
      val responseSpec = mock<WebClient.ResponseSpec>()

      whenever(locationDetailWebClient.get()).thenReturn(requestHeadersUriSpec)
      whenever(requestHeadersUriSpec.uri("/locations/{locationId}?formatLocalName=true", locationId))
        .thenReturn(requestHeadersSpec)
      whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      whenever(responseSpec.bodyToMono(LocationDetailResponse::class.java))
        .thenReturn(Mono.just(expectedResponse))

      // When
      val result = locationService.getLocationDetail(locationId)

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
      val locationId = "NOT_FOUND"
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
        null,
        errorJson.toByteArray(StandardCharsets.UTF_8),
        null
      )

      // Mock the fluent WebClient chain:
      val requestHeadersUriSpec = mock<WebClient.RequestHeadersUriSpec<*>>()
      val requestHeadersSpec = mock<WebClient.RequestHeadersSpec<*>>()
      val responseSpec = mock<WebClient.ResponseSpec>()

      whenever(locationDetailWebClient.get()).thenReturn(requestHeadersUriSpec)
      whenever(requestHeadersUriSpec.uri("/locations/{locationId}?formatLocalName=true", locationId))
        .thenReturn(requestHeadersSpec)
      whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      whenever(responseSpec.bodyToMono(LocationDetailResponse::class.java))
        .thenReturn(Mono.error(notFoundException))

      // When
      val result = locationService.getLocationDetail(locationId)

      // Then
      assertNull(result, "Expected null when location is not found (404)")
    }

    @Test
    fun `Given an unexpected exception, When getLocationDetail is called, Then it throws a RuntimeException`() {
      // Given
      val locationId = "ANY_DPS_ID"

      val requestHeadersUriSpec = mock<WebClient.RequestHeadersUriSpec<*>>()
      val requestHeadersSpec = mock<WebClient.RequestHeadersSpec<*>>()
      val responseSpec = mock<WebClient.ResponseSpec>()

      whenever(locationDetailWebClient.get()).thenReturn(requestHeadersUriSpec)
      whenever(requestHeadersUriSpec.uri("/locations/{locationId}?formatLocalName=true", locationId))
        .thenReturn(requestHeadersSpec)
      whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      // Force a generic exception
      whenever(responseSpec.bodyToMono(LocationDetailResponse::class.java))
        .thenThrow(RuntimeException("Something went wrong"))

      // When / Then
      val ex = assertThrows<RuntimeException> {
        locationService.getLocationDetail(locationId)
      }
      assertTrue(
        ex.message!!.contains("Failed to fetch location details for ID: $locationId"),
        "Should wrap unexpected exception in a RuntimeException"
      )
    }
  }
}
