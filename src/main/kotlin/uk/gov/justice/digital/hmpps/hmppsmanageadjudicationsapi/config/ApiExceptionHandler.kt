package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.ForbiddenException

@RestControllerAdvice
class ApiExceptionHandler {

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleEntityNotFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .contentType(MediaType.APPLICATION_JSON)
      .body(ErrorResponse(status = HttpStatus.NOT_FOUND.value(), developerMessage = e.message))
  }

  @ExceptionHandler(WebClientResponseException::class)
  fun handleException(e: WebClientResponseException): ResponseEntity<ErrorResponse> {
    val errorMessage = "Forwarded HTTP call response exception"

    log.error(errorMessage, e.responseBodyAsString)

    return ResponseEntity
      .status(e.statusCode)
      .body(
        ErrorResponse(
          status = e.statusCode.value(),
          userMessage = "Forwarded HTTP call response error: ${e.message}",
        ),
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
        ),
      )
  }

  @ExceptionHandler(java.lang.Exception::class)
  fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: ${e.message}",
        ),
      )
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDenied(e: AccessDeniedException): ResponseEntity<ErrorResponse?>? {
    log.error("AccessDeniedException", e)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = "Access is denied",
        ),
      )
  }

  @ExceptionHandler(ForbiddenException::class)
  fun handleAccessDenied(e: ForbiddenException): ResponseEntity<ErrorResponse?>? {
    log.error("ForbiddenException", e)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = "Operation forbidden: ${e.message}",
        ),
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
        ),
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
        ),
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
        ),
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
        ),
      )
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Error Response")
data class ErrorResponse(
  @Schema(description = "Status of Error", example = "500", required = true)
  val status: Int,
  @Schema(description = "Error Code", example = "500", required = false)
  val errorCode: Int? = null,
  @Schema(description = "User Message of error", example = "Bad Data", required = false, maxLength = 200, pattern = "^[a-zA-Z\\d. _-]{1,200}\$")
  val userMessage: String? = null,
  @Schema(description = "More detailed error message", example = "This is a stack trace", required = false, maxLength = 4000, pattern = "^[a-zA-Z\\d. _-]*\$")
  val developerMessage: String? = null,
  @Schema(description = "More information about the error", example = "More info", required = false, maxLength = 4000, pattern = "^[a-zA-Z\\d. _-]*\$")
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
