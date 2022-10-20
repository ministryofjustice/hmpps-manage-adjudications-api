package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/draft-adjudications")
@Validated
class DraftAdjudicationBaseController
