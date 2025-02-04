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

class PrisonerSearchServiceTest {

  private val webClient: WebClient = mock()
  private val prisonerSearchService = PrisonerSearchService(webClient)

  @Nested
  inner class GetNomisLocationDetailTests {

    @Test
    fun `Given a valid prisonerNumber, When getPrisonerDetail is called, Then it returns the PrisonerResponse`() {
      // Given
      val prisonerNumber = "A1234BC"
      val expectedResponse = PrisonerResponse(
        firstName = "Sam",
        lastName = "Gomez",
      )

      // Mock the fluent WebClient chain:
      val requestHeadersUriSpec = mock<WebClient.RequestHeadersUriSpec<*>>()
      val requestHeadersSpec = mock<WebClient.RequestHeadersSpec<*>>()
      val responseSpec = mock<WebClient.ResponseSpec>()

      whenever(webClient.get()).thenReturn(requestHeadersUriSpec)
      whenever(requestHeadersUriSpec.uri("/api/offenders/{prisonerNumber}", prisonerNumber))
        .thenReturn(requestHeadersSpec)
      whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      whenever(responseSpec.bodyToMono(PrisonerResponse::class.java))
        .thenReturn(Mono.just(expectedResponse))

      // When
      val result = prisonerSearchService.getPrisonerDetail(prisonerNumber)

      // Then
      assertNotNull(result)
      assertEquals(expectedResponse.firstName, result?.firstName)
      assertEquals(expectedResponse.lastName, result?.lastName)
    }

    @Test
    fun `Given the prisoner is not found, When WebClient throws 404, Then getPrisonerDetail returns null`() {
      // Given
      val prisonerNumber = "NOT_FOUND"
      val errorJson = "{\n" +
        "  \"status\": 404,\n" +
        "  \"errorCode\": \"PRISONER_NOT_FOUND\",\n" +
        "  \"userMessage\": \"Prisoner not found\",\n" +
        "  \"developerMessage\": \"No prisoner found for id\",\n" +
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

      whenever(webClient.get()).thenReturn(requestHeadersUriSpec)
      whenever(requestHeadersUriSpec.uri("/api/offenders/{prisonerNumber}", prisonerNumber))
        .thenReturn(requestHeadersSpec)

      whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)

      // Return a Mono.error() to simulate a 404 from the downstream service
      whenever(responseSpec.bodyToMono(PrisonerResponse::class.java))
        .thenReturn(Mono.error(notFoundException))

      // When
      val result = prisonerSearchService.getPrisonerDetail(prisonerNumber)

      // Then
      assertNull(result, "Expected null when prisoner is not found (404)")
    }

    @Test
    fun `Given an unexpected exception, When getPrisonerDetail is called, Then it throws a RuntimeException`() {
      // Given
      val prisonerNumber = "ANY_ID"

      val requestHeadersUriSpec = mock<WebClient.RequestHeadersUriSpec<*>>()
      val requestHeadersSpec = mock<WebClient.RequestHeadersSpec<*>>()
      val responseSpec = mock<WebClient.ResponseSpec>()

      whenever(webClient.get()).thenReturn(requestHeadersUriSpec)

      whenever(requestHeadersUriSpec.uri("/api/offenders/{prisonerNumber}", prisonerNumber))
        .thenReturn(requestHeadersSpec)

      whenever(requestHeadersSpec.retrieve()).thenReturn(responseSpec)

      // Force a generic exception
      whenever(responseSpec.bodyToMono(PrisonerResponse::class.java))
        .thenThrow(RuntimeException("Something went wrong"))

      // When / Then
      val ex = assertThrows<RuntimeException> {
        prisonerSearchService.getPrisonerDetail(prisonerNumber)
      }
      assertTrue(
        ex.message!!.contains("Failed to fetch prisoner details for ID: $prisonerNumber"),
        "Should wrap unexpected exception in a RuntimeException",
      )
    }
  }
}
