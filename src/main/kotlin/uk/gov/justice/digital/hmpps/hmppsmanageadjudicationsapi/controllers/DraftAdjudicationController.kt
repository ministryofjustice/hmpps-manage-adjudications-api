package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import com.sun.istack.NotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.DraftAdjudicationService
import java.time.LocalDateTime

data class NewAdjudicationRequest(@NotNull val prisonerNumber: String)
data class DraftAdjudicationResponse(val draftAdjudication: DraftAdjudicationDto)
data class AddIncidentDetailsRequest(
  val locationId: Long,
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val dateTimeOfIncident: LocalDateTime
)

@RestController
@RequestMapping("/draft-adjudications")
@Validated
class DraftAdjudicationController {

  @Autowired
  lateinit var draftAdjudicationService: DraftAdjudicationService

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun startNewAdjudication(@RequestBody newAdjudicationRequest: NewAdjudicationRequest): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.startNewAdjudication(newAdjudicationRequest.prisonerNumber)

    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }

  @PutMapping(value = ["/{id}"])
  fun addIncidentDetails(
    @PathVariable(name = "id") id: Long,
    @RequestBody addIncidentDetailsRequest: AddIncidentDetailsRequest
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.addIncidentDetails(
      id,
      addIncidentDetailsRequest.locationId,
      addIncidentDetailsRequest.dateTimeOfIncident
    )

    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }

  @GetMapping(value = ["/{id}"])
  fun getDraftAdjudicationDetails(@PathVariable(name = "id") id: Long): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.getDraftAdjudicationDetails(id)

    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }
}
