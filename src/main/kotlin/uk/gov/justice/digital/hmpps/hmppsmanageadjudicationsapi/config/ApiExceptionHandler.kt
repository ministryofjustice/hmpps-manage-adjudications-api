package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.reactive.function.client.WebClientResponseException
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException

@RestControllerAdvice
class ApiExceptionHandler {
  @ExceptionHandler(WebClientResponseException::class)
  fun handleException(e: WebClientResponseException): ResponseEntity<ErrorResponse> {
    val errorMessage = "Forwarded HTTP call response exception"

    log.error(errorMessage, e.responseBodyAsString)

    return ResponseEntity
      .status(e.rawStatusCode)
      .body(
        ErrorResponse(
          status = e.rawStatusCode,
          userMessage = "Forwarded HTTP call response error: ${e.message}",
        )
      )
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
        )
      )
  }

 /* @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDenied(e: AccessDeniedException): ResponseEntity<ErrorResponse?>? {
    log.error("AccessDeniedException", e)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = "Invalid role",
        )
      )
  } */

  // this is turning 403 into 500.
  @ExceptionHandler(java.lang.Exception::class)
  fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: ${e.message}",
        )
      )
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleRequestBodyMalformed(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Unable to read request body: ${e.message}",
        )
      )
  }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFound(e: EntityNotFoundException): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "Not found: ${e.message}",
        )
      )
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse?>? {
    log.info("Validation exception: {}", e.message)

    val errors = e.bindingResult.allErrors.map { it.defaultMessage }
    val errorMessage = errors.joinToString("\n")

    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = errorMessage,
        )
      )
  }

  @ExceptionHandler(IllegalStateException::class)
  fun handleIllegalStateException(e: IllegalStateException): ResponseEntity<ErrorResponse?>? {
    log.info("Validation exception: {}", e.message)

    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = e.message,
        )
      )
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
